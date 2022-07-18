package com.common.download.bean

import java.io.InputStream

sealed class DownloadResponse {
    /**
     * 成功
     * @param contentLength 文件内容长度
     * @param supportRange 是否支持断点续传
     * @param byteStream 输入流
     */
    data class Success(val contentLength: Long, val supportRange: Boolean = false, val byteStream: InputStream) : DownloadResponse()

    /**
     * Head
     * @param newUrl url，有可能經過了重定向，所以url不一樣了
     * @param contentLength 文件内容长度
     * @param supportRange 是否支持断点续传
     */
    data class Head(val newUrl: String, val contentLength: Long, val supportRange: Boolean = false) : DownloadResponse()

    /**
     * 失败
     * @param code 错误码
     * @param message 错误消息
     */
    data class Error(val code: Int, val message: String = "") : DownloadResponse()
}
