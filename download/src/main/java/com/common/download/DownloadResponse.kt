package com.common.download

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
     * 失败
     * @param code 错误码
     * @param message 错误消息
     */
    data class Error(val code: Int, val message: String = "") : DownloadResponse()
}
