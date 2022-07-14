package com.common.download

import android.app.Service
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat

class DownloadNotificationService: Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val taskId = intent?.getLongExtra(KEY_TASK_ID, -1)
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
                if (taskId != null && taskId != -1L) {
                    DownloadUtils.download(taskId)
                    val notifyId = intent.getIntExtra(KEY_NOTIFY_ID, 0)
                    NotificationManagerCompat.from(this).cancel(notifyId)
                }
            }
            DownloadAction.DOWNLOAD_PAUSE -> {
                if (taskId != null && taskId != -1L) {
                    DownloadUtils.pause(taskId)
                }
            }
            DownloadAction.DOWNLOAD_CANCEL -> {
                if (taskId != null && taskId != -1L) {
                    DownloadUtils.cancel(taskId)
                }
            }
            DownloadAction.DOWNLOAD_START -> {
                if (taskId != null && taskId != -1L) {
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