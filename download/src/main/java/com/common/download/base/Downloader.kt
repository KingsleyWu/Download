package com.common.download.base

import com.common.download.bean.DownloadResponse
import com.common.download.DownloadUtils

/**
 * 下载的接口
 * 如用户需要自定义通知，可以实现此接口并设置 [DownloadUtils.downloader]
 */
interface Downloader {
    /**
     * 下載
     * @param start 開始位置
     * @param url 下載的 Url
     */
    suspend fun download(start: String? = "0", url: String): DownloadResponse
    /**
     * 下載
     * @param url 下載的 Url
     */
    suspend fun get(url: String): DownloadResponse
}