package com.kingsley.download

import android.content.Context
import com.common.download.utils.DownloadNotificationHelper

class CustomDownloadNotificationHelper(override val context: Context) : DownloadNotificationHelper() {

    /**
     * 下載出錯了的消息
     * @param message 当前下载錯誤的信息
     */
    override fun getDownloadFailedText(message: String?) = context.getString(R.string.download_failed) + " :$message"
    /** 已加入下載隊列，等待下載中的消息 */
    override fun getDownloadPendingText() = context.getString(R.string.download_task_added)
    /** 開始下載 等待鏈接中 */
    override fun getDownloadWaitingText() = context.getString(R.string.download_waiting)
    /** 開始下載 等待 wifi 鏈接中的消息 */
    override fun getDownloadWaitingWifiText() = context.getString(R.string.download_waiting_wifi)
    /** 取消下載中的消息 */
    override fun getDownloadCancellingText() = context.getString(R.string.download_cancel)
    /** 暫停下載的消息 */
    override fun getDownloadPausedText() = context.getString(R.string.download_paused)
    /** 下載完成的消息 */
    override fun getDownloadCompleteText() = context.getString(R.string.download_complete)
    /** 通知暫停按鈕顯示文本 */
    override fun getPausedTitle(): String = context.getString(R.string.download_pause)
    /** 通知開始按鈕顯示文本 */
    override fun getStartTitle(): String = context.getString(R.string.download_start)
    /** 通知取消按鈕顯示文本 */
    override fun getCancelTitle(): CharSequence = context.getString(R.string.cancel)
    /** 通知下載中顯示的布標 */
    override fun getStartDownloadIcon(): Int = com.common.download.R.drawable.ic_download
}