package com.common.download.core.downloader

import com.common.download.Progress
import com.common.download.TaskInfo
import okhttp3.ResponseBody
import retrofit2.Response

class NormalDownloader : Downloader {

    override fun download(taskInfo: TaskInfo, response: Response<ResponseBody>): Progress {
        return Progress()
    }

}