package com.common.download

import android.app.Service
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import com.common.download.utils.DownloadAction

class DownloadNotificationService: Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val taskId = intent?.getStringExtra(KEY_TASK_ID) ?: ""
        when (action) {
            DownloadAction.DOWNLOAD_JUMP_TO -> {
                //跳转到其他应用市场下载
                val notifyId = intent.getIntExtra(KEY_NOTIFY_ID, 0)
                val uri = intent.data
                try {
                    val it = Intent(Intent.ACTION_VIEW, uri)
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                } catch (e: ActivityNotFoundException) {
                   e.printStackTrace()
                } finally {
                    NotificationManagerCompat.from(this).cancel(notifyId)
                }
                return START_NOT_STICKY
            }
            DownloadAction.DOWNLOAD_PENDING_INTENT -> {
                //从通知栏点击启动一个下载
                if (taskId.isNotEmpty()) {
                    DownloadUtils.download(taskId)
                    val notifyId = intent.getIntExtra(KEY_NOTIFY_ID, 0)
                    NotificationManagerCompat.from(this).cancel(notifyId)
                }
            }
            DownloadAction.DOWNLOAD_PAUSE -> {
                if (taskId.isNotEmpty()) {
                    DownloadUtils.pause(taskId)
                }
            }
            DownloadAction.DOWNLOAD_CANCEL -> {
                if (taskId.isNotEmpty()) {
                    DownloadUtils.cancel(taskId)
                    NotificationManagerCompat.from(this).cancel(taskId.hashCode())
                }
            }
            DownloadAction.DOWNLOAD_START -> {
                if (taskId.isNotEmpty()) {
                    DownloadUtils.download(taskId)
                }
            }
            else -> {
                return START_NOT_STICKY
            }
        }
        return START_STICKY
    }
}