package com.common.download.utils

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.common.download.DownloadNotificationService
import com.common.download.DownloadUtils
import com.common.download.KEY_TASK_ID
import com.common.download.base.NotificationHelper
import com.common.download.base.appContext
import com.common.download.bean.DownloadGroupTaskInfo
import com.common.download.bean.DownloadStatus


/**
 * 下載通知幫助類
 */
open class DownloadNotificationHelper : NotificationHelper {

    companion object {

        /** 通知管理器 */
        private val mNotificationManager: NotificationManagerCompat by lazy {
            NotificationManagerCompat.from(appContext)
        }

        /** 通知渠道 id */
        var CHANNEL_ID: String = appContext.packageName + ".download.notification_channel"

        /** 創建通知渠道 */
        @JvmStatic
        @JvmOverloads
        fun createNotificationChannel(
            channelId: String = CHANNEL_ID,
            channelName: String? = null,
            channelDescription: String? = null
        ) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val importance = NotificationManager.IMPORTANCE_LOW
                val channel = NotificationChannelCompat.Builder(channelId, importance)
                    .setName(channelName)
                    .setDescription(channelDescription)
                    .build()
                // Register the channel with the system;
                // you can't change the importance
                // or other notification behaviors after this
                mNotificationManager.createNotificationChannel(channel)
            }
        }
    }

    override fun createNotification(downloadGroupTaskInfo: DownloadGroupTaskInfo) {
        val notifyId = downloadGroupTaskInfo.id.hashCode()
        if (downloadGroupTaskInfo.status == DownloadStatus.NONE) {
            mNotificationManager.cancel(notifyId)
        } else {
            if (downloadGroupTaskInfo.showNotification) {
                val builder = getNotificationBuilder(
                    context = context ?: appContext,
                    notifyClickAction = downloadGroupTaskInfo.current?.action,
                    notifyId = notifyId,
                    contentTitle = downloadGroupTaskInfo.current?.title,
                    downloadGroupTaskInfo = downloadGroupTaskInfo
                )
                mNotificationManager.notify(notifyId, builder.build())
            }
        }
    }

    open fun getNotificationBuilder(
        context: Context, notifyClickAction: String?,
        notifyId: Int, contentTitle: String?,
        downloadGroupTaskInfo: DownloadGroupTaskInfo,
    ): NotificationCompat.Builder {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSound(null)

        val ongoing = isOngoing(downloadGroupTaskInfo)
        var flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (if (ongoing) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_ONE_SHOT) or PendingIntent.FLAG_MUTABLE
        } else {
            if (ongoing) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_ONE_SHOT
        }
        try {
            //1.click notification
            val clickIntent = if (notifyClickAction.isNullOrEmpty()) {
                context.packageManager.getLaunchIntentForPackage(context.packageName)
            } else {
                Intent(Intent.ACTION_VIEW, Uri.parse(notifyClickAction))
            }
            if (clickIntent != null) {
                val pendingIntentContent = PendingIntent.getActivity(
                    context,
                    notifyId, clickIntent, flag
                )
                builder.setContentIntent(pendingIntentContent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        builder.setOngoing(downloadGroupTaskInfo.status != DownloadStatus.PAUSED && ongoing)
            .setAutoCancel(downloadGroupTaskInfo.status == DownloadStatus.PAUSED || !ongoing)
        val tempContentTitle =
            if (downloadGroupTaskInfo.status == DownloadStatus.COMPLETED) downloadGroupTaskInfo.notificationTitle else contentTitle
        if (!tempContentTitle.isNullOrEmpty()) {
            builder.setContentTitle(tempContentTitle)
        }
        builder.setSmallIcon(if (ongoing && downloadGroupTaskInfo.status != DownloadStatus.PAUSED) android.R.drawable.stat_sys_download else getStartDownloadIcon())
        //2.content text
        setNotificationContentText(builder, downloadGroupTaskInfo)
        //3.progress bar
        setNotificationProgressBar(builder, downloadGroupTaskInfo)
        //5.setup actions click
        if (ongoing) {
            val actionIntent = Intent()
            actionIntent.setClass(context, DownloadNotificationService::class.java)
            actionIntent.putExtra(KEY_TASK_ID, downloadGroupTaskInfo.id)

            //cancel
            actionIntent.action = DownloadAction.DOWNLOAD_CANCEL
            flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pendingIntentCancel =
                PendingIntent.getService(context, notifyId, actionIntent, flag)
            //start|pause
            val title: String
            if (downloadGroupTaskInfo.status == DownloadStatus.DOWNLOADING) {
                actionIntent.action = DownloadAction.DOWNLOAD_PAUSE
                title = getPausedTitle()
            } else {
                actionIntent.action = DownloadAction.DOWNLOAD_START
                title = getStartTitle()
            }
            val pendingIntentPauseOrStart =
                PendingIntent.getService(context, notifyId, actionIntent, flag)
            builder.addAction(0, title, pendingIntentPauseOrStart)
            builder.addAction(0, getCancelTitle(), pendingIntentCancel)
        }
        return builder
    }

    open fun setNotificationContentText(
        builder: NotificationCompat.Builder,
        downloadGroupTaskInfo: DownloadGroupTaskInfo
    ) {
        //progress text value
        val contentText: String? = when (downloadGroupTaskInfo.status) {
            DownloadStatus.DOWNLOADING -> downloadGroupTaskInfo.progress.percentStr()
            DownloadStatus.FAILED -> getDownloadFailedText(downloadGroupTaskInfo.message)
            DownloadStatus.PENDING -> getDownloadPendingText()
            DownloadStatus.WAITING -> if (downloadGroupTaskInfo.needWifi && !DownloadUtils.sWifiAvailable) {
                getDownloadWaitingWifiText()
            } else {
                getDownloadWaitingText()
            }
            DownloadStatus.NONE -> getDownloadCancellingText()
            DownloadStatus.PAUSED -> getDownloadPausedText()
            DownloadStatus.COMPLETED -> getDownloadCompleteText()
            else -> null
        }
        builder.setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
        builder.setContentText(contentText)
    }

    open fun isOngoing(groupTaskInfo: DownloadGroupTaskInfo): Boolean {
        return when (groupTaskInfo.status) {
            DownloadStatus.DOWNLOADING,
            DownloadStatus.PAUSED,
            DownloadStatus.FAILED,
            DownloadStatus.NONE,
            DownloadStatus.PENDING -> true
            else -> false
        }
    }

    open fun setNotificationProgressBar(
        builder: NotificationCompat.Builder,
        groupTaskInfo: DownloadGroupTaskInfo
    ) {
        when (groupTaskInfo.status) {
            DownloadStatus.PENDING, DownloadStatus.NONE -> {
                builder.setProgress(0, 0, true)
            }
            DownloadStatus.DOWNLOADING -> {
                val progressInfo = groupTaskInfo.progress
                val floatProgress = progressInfo.percent()
                //progress
                val ongoing: Boolean = progressInfo.downloadSize < progressInfo.totalSize
                val max = if (ongoing) 100 else 0
                val progress =
                    if (ongoing) if (floatProgress > 0 && floatProgress < 1.0f) 1 else floatProgress.toInt() else 0
                builder.setProgress(max, progress, false)
            }
            else -> {
                builder.setProgress(0, 0, false)
            }
        }
    }
}