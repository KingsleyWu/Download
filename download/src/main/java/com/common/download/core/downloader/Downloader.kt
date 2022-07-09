package com.common.download.core.downloader

import com.common.download.TaskInfo
import com.common.download.Progress
import retrofit2.Response
import okhttp3.ResponseBody

interface Downloader {

    fun download(taskInfo: TaskInfo, response: Response<ResponseBody>): Progress
}