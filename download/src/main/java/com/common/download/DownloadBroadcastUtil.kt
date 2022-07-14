package com.common.download

import android.content.Intent
import java.io.Serializable

object DownloadBroadcastUtil {

    fun sendBroadcast(action: DownloadAction) {
        val intent = Intent(action.action)
        action.taskInfo?.let {
            intent.putExtra(KEY_TASK_ID, it.id)
            if (it.status == Status.FAILED) {
                intent.putExtra(KEY_ERROR, it.message)
            }
        }
        appContext.sendBroadcast(intent)
    }

}

sealed class DownloadAction(val action: String, open val taskInfo: TaskInfo? = null) : Serializable {

    companion object {
        const val DOWNLOAD_START = "download.start"
        const val DOWNLOAD_CANCEL = "download.cancel"
        const val DOWNLOAD_PAUSE = "download.pause"
        const val DOWNLOAD_SUCCESS = "download.success"
        const val DOWNLOAD_FAILED = "download.failed"
        const val DOWNLOAD_JUMP_TO = "download.jump_to"
        const val DOWNLOAD_PENDING_INTENT = "download.pending_intent"
    }

    data class Start(override val taskInfo: TaskInfo? = null) : DownloadAction(DOWNLOAD_START)
    data class Cancel(override val taskInfo: TaskInfo? = null) : DownloadAction(DOWNLOAD_CANCEL)
    data class Pause(override val taskInfo: TaskInfo? = null) : DownloadAction(DOWNLOAD_PAUSE)
    data class Success(override val taskInfo: TaskInfo? = null) : DownloadAction(DOWNLOAD_SUCCESS)
    data class Failed(override val taskInfo: TaskInfo? = null) : DownloadAction(DOWNLOAD_FAILED)
    data class PendingIntent(override val taskInfo: TaskInfo? = null) : DownloadAction(DOWNLOAD_PENDING_INTENT)
}