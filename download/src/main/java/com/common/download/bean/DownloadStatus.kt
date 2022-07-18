package com.common.download.bean

/** 下載的狀態
 * [DownloadStatus.NONE] 0 无状态,
 * [DownloadStatus.STARTED] 1 開始下载,
 * [DownloadStatus.DOWNLOADING] 2 下载中,
 * [DownloadStatus.PAUSED] 3 暂停,
 * [DownloadStatus.COMPLETED] 4 完成,
 * [DownloadStatus.FAILED] 5 错误,
 * [DownloadStatus.PENDING] 6 等待中
 */
object DownloadStatus {
    /** 无状态 0 */
    const val NONE = 0

    /** 開始下載 1 */
    const val STARTED = 1

    /** 下载中 2 */
    const val DOWNLOADING = 2

    /** 暫停 3 */
    const val PAUSED = 3

    /** 完成 4 */
    const val COMPLETED = 4

    /** 错误 5 */
    const val FAILED = 5

    /** 等待下载中 6 */
    const val PENDING = 6
}