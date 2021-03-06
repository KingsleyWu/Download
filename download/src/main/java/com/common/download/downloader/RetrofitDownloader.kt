package com.common.download.downloader

import com.common.download.bean.DownloadResponse
import com.common.download.base.Downloader
import com.common.download.utils.DownloadLog
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.*

object RetrofitDownloader : Downloader {

    private val downloadApi: DownloadApi by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        Retrofit.Builder()
            .baseUrl("https://google.com")
            .client(OkHttpClient.Builder().build())
            .build()
            .create(DownloadApi::class.java)
    }

    override suspend fun download(start: String?, url: String): DownloadResponse {
        val response = downloadApi.download(start ?: "0", url)
        if (response.isSuccessful) {
            val responseBody = response.body()
            val headers = response.headers()
            DownloadLog.d("download headers: $headers")
            responseBody ?: return DownloadResponse.Error(response.code(), "ResponseBody is null")
            val supportRange = !headers["Content-Range"].isNullOrEmpty()
            return DownloadResponse.Success(responseBody.contentLength(), supportRange, responseBody.byteStream())
        } else {
            return DownloadResponse.Error(response.code(), if (response.message() == "") "${response.code()}" else response.message())
        }
    }

    override suspend fun get(url: String): DownloadResponse {
        val response = downloadApi.get(url)
        DownloadLog.d("head response : $response")
        return if (response.isSuccessful) {
            val headers = response.headers()
            DownloadLog.d( "head: $headers")
            val supportRange = !headers["Accept-Ranges"].isNullOrEmpty()
            val contentLength = headers["Content-length"]
            val newUrl = response.raw().request.url.toString()
            DownloadResponse.Head(newUrl,contentLength?.toLongOrNull() ?: 0, supportRange)
        } else {
            DownloadResponse.Error(response.code(), if (response.message() == "") "${response.code()}" else response.message())
        }
    }

}

/**
 * ??????????????????
 */
interface DownloadApi {

    @Streaming
    @GET
    suspend fun download(@Header("RANGE") start: String? = "0", @Url url: String?): Response<ResponseBody>

    @GET
    suspend fun get(@Url url: String): Response<Void>
}