package com.common.download.utils

import android.content.Intent
import com.common.download.KEY_ERROR
import com.common.download.KEY_TASK_ID
import com.common.download.base.appContext
import com.common.download.bean.DownloadStatus
import com.common.download.bean.DownloadGroupTaskInfo
import java.io.Serializable

internal object DownloadBroadcastUtil {

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

    /**
     * 發送廣播,僅限 COMPLETED, WAITING, FAILED
     */
    fun sendBroadcast(groupInfo: DownloadGroupTaskInfo) {
        when (groupInfo.status) {
            DownloadStatus.COMPLETED -> sendBroadcast(DownloadAction.Success(groupInfo))
            DownloadStatus.WAITING -> sendBroadcast(DownloadAction.Waiting(groupInfo))
            DownloadStatus.FAILED -> sendBroadcast(DownloadAction.Failed(groupInfo))
            else -> {}
        }
    }

}

sealed class DownloadAction(val action: String, open val taskGroupInfo: DownloadGroupTaskInfo? = null) : Serializable {

    companion object {
        const val DOWNLOAD_START = "download.start"
        const val DOWNLOAD_WAITING = "download.waiting"
        const val DOWNLOAD_CANCEL = "download.cancel"
        const val DOWNLOAD_PAUSE = "download.pause"
        const val DOWNLOAD_SUCCESS = "download.success"
        const val DOWNLOAD_FAILED = "download.failed"
        const val DOWNLOAD_JUMP_TO = "download.jump_to"
        const val DOWNLOAD_PENDING_INTENT = "download.pending_intent"
    }

    data class Waiting(override val taskGroupInfo: DownloadGroupTaskInfo? = null) : DownloadAction(
        DOWNLOAD_WAITING
    )

    data class Cancel(override val taskGroupInfo: DownloadGroupTaskInfo? = null) : DownloadAction(
        DOWNLOAD_CANCEL
    )

    data class Pause(override val taskGroupInfo: DownloadGroupTaskInfo? = null) : DownloadAction(
        DOWNLOAD_PAUSE
    )

    data class Success(override val taskGroupInfo: DownloadGroupTaskInfo? = null) : DownloadAction(
        DOWNLOAD_SUCCESS
    )

    data class Failed(override val taskGroupInfo: DownloadGroupTaskInfo? = null) : DownloadAction(
        DOWNLOAD_FAILED
    )
}