package com.common.download.downloader

import com.common.download.DownloadResponse
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Streaming
import retrofit2.http.Url
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream

object OkHttpDownloader : Downloader {

    private val downloadApi: DownloadApi by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        Retrofit.Builder()
            .baseUrl("https://download")
            .client(OkHttpClient.Builder().build())
            .build()
            .create(DownloadApi::class.java)
    }

    override suspend fun download(start: String?, url: String): DownloadResponse {
        val response = downloadApi.download(start ?: "0", url)
        if (response.isSuccessful) {
            val responseBody = response.body()
            responseBody ?: return DownloadResponse.Error(response.code(), "ResponseBody is null")
            val supportRange = !response.headers()["Content-Range"].isNullOrEmpty()
            return DownloadResponse.Success(responseBody.contentLength(), supportRange, responseBody.byteStream())
        } else {
            return DownloadResponse.Error(response.code(), if (response.message() == "") "${response.code()}" else response.message())
        }
    }

}

/**
 * 请求数据接口
 */
interface DownloadApi {

    @Streaming
    @GET
    suspend fun download(@Header("RANGE") start: String? = "0", @Url url: String?): Response<ResponseBody>
}