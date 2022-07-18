package com.common.download.utils

import android.content.Intent
import com.common.download.KEY_ERROR
import com.common.download.KEY_TASK_ID
import com.common.download.base.appContext
import com.common.download.bean.DownloadStatus
import com.common.download.bean.DownloadTaskGroupInfo
import java.io.Serializable

object DownloadBroadcastUtil {

    /**
     * 發送廣播
     */
    fun sendBroadcast(action: DownloadAction) {
        val intent = Intent(action.action)
        action.taskGroupInfo?.let {
            intent.putExtra(KEY_TASK_ID, it.id)
            if (it.status == DownloadStatus.FAILED) {
                intent.putExtra(KEY_ERROR, it.message)
            }
        }
        appContext.sendBroadcast(intent)
    }

}

sealed class DownloadAction(val action: String, open val taskGroupInfo: DownloadTaskGroupInfo? = null) : Serializable {

    companion object {
        const val DOWNLOAD_START = "download.start"
        const val DOWNLOAD_CANCEL = "download.cancel"
        const val DOWNLOAD_PAUSE = "download.pause"
        const val DOWNLOAD_SUCCESS = "download.success"
        const val DOWNLOAD_FAILED = "download.failed"
        const val DOWNLOAD_JUMP_TO = "download.jump_to"
        const val DOWNLOAD_PENDING_INTENT = "download.pending_intent"
    }

    data class Start(override val taskGroupInfo: DownloadTaskGroupInfo? = null) : DownloadAction(
        DOWNLOAD_START
    )
    data class Cancel(override val taskGroupInfo: DownloadTaskGroupInfo? = null) : DownloadAction(
        DOWNLOAD_CANCEL
    )
    data class Pause(override val taskGroupInfo: DownloadTaskGroupInfo? = null) : DownloadAction(
        DOWNLOAD_PAUSE
    )
    data class Success(override val taskGroupInfo: DownloadTaskGroupInfo? = null) : DownloadAction(
        DOWNLOAD_SUCCESS
    )
    data class Failed(override val taskGroupInfo: DownloadTaskGroupInfo? = null) : DownloadAction(
        DOWNLOAD_FAILED
    )
    data class PendingIntent(override val taskGroupInfo: DownloadTaskGroupInfo? = null) : DownloadAction(
        DOWNLOAD_PENDING_INTENT
    )
}