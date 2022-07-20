package com.common.download.base

import com.common.download.bean.DownloadGroupTaskInfo
import com.common.download.DownloadUtils
import com.common.download.R

/**
 * 通知接口
 * 如用户需要自定义通知，可以实现此接口并设置 [DownloadUtils.notificationHelper]
 */
interface NotificationHelper {

    /**
     * 创建通知
     * @param downloadGroupTaskInfo 当前下载的 group 信息
     */
    fun createNotification(downloadGroupTaskInfo: DownloadGroupTaskInfo)
    /**
     * 下載出錯了的消息
     * @param message 当前下载錯誤的信息
     */
    fun getDownloadFailedText(message: String?) = "Failed:$message"
    /** 已加入下載隊列，等待下載中的消息 */
    fun getDownloadPendingText() = "Added to download list"
    /** 開始下載 等待鏈接中 */
    fun getDownloadWaitingText() = "Waiting to download"
    /** 開始下載 等待 wifi 鏈接中的消息 */
    fun getDownloadWaitingWifiText() = "Waiting for Wi-Fi connection"
    /** 取消下載中的消息 */
    fun getDownloadCancellingText() = "Cancelling"
    /** 暫停下載的消息 */
    fun getDownloadPausedText() = "Download has been paused"
    /** 下載完成的消息 */
    fun getDownloadCompleteText() = "Finished"
    /** 通知暫停按鈕顯示文本 */
    fun getPausedTitle(): String = "Pause"
    /** 通知開始按鈕顯示文本 */
    fun getStartTitle(): String = "Start"
    /** 通知取消按鈕顯示文本 */
    fun getCancelTitle(): CharSequence? = "Cancel"
    /** 通知下載中顯示的布標 */
    fun getStartDownloadIcon(): Int = R.drawable.ic_download
}