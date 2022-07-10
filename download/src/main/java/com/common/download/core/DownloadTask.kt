package com.common.download.core

import android.text.TextUtils
import com.common.download.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.*

class DownloadTask(
    var coroutineScope: CoroutineScope = DownloadScope,
    var callback: (TasksInfo) -> Unit = {},
    private val tasksInfo: TasksInfo
) {

    /** 当前下载的 job */
    private var job: Job? = null

    fun start() {
        if (job?.isActive == false) {
            job = coroutineScope.launch {
                flow {
                    emit(tasksInfo)
                    if (tasksInfo.status != Status.COMPLETED) {
                        tasksInfo.tasks.forEach {
                            when (it.status) {
                                /** 无状态, 暫停, 错误, 刪除了 */
                                Status.NONE,
                                Status.PAUSED,
                                Status.FAILED,
                                Status.DELETED -> {
                                    var startPosition = it.currentLength
                                    //验证断点有效性
                                    if (startPosition < 0) {
                                        startPosition = 0
                                    }
                                    //下载的文件是否已经被删除
                                    if (startPosition > 0 && !TextUtils.isEmpty(it.path)) {
                                        val filePath =
                                            "${it.path!!}${if (DownloadUtils.needDownloadSuffix) ".download" else ""}"
                                        if (!File(filePath).exists()) {
                                            if (startPosition == it.contentLength && File(it.path!!).exists()) {
                                                it.status = Status.COMPLETED
                                                it.message = ""
//                                                DownloadDBHelper.insertOrReplaceDownloadInfo(it)
                                                return@forEach
                                            } else {
                                                startPosition = 0
                                            }
                                        }
                                    }
//                                    LogUtils.d(TAG, "start download startPosition = $startPosition")
                                    val result = DownloadUtils.downloader.download(
                                        start = "bytes=$startPosition-",
                                        url = it.url
                                    )
                                    if (result is DownloadResponse.Success) {
                                        //文件长度 由於帶了 “bytes=startPosition-” 這個header 所以需要判斷
                                        if (it.contentLength < 0) {
                                            it.contentLength = result.contentLength
                                        }
                                        //保存的文件名称
                                        if (TextUtils.isEmpty(it.fileName)) {
                                            it.fileName = UrlUtils.getUrlFileName(it.url)
                                        }
                                        //创建File,如果已经指定文件path,将会使用指定的path,如果没有指定将会使用默认的下载目录
                                        val file: File
                                        val tempFile: File
                                        if (TextUtils.isEmpty(it.path)) {
                                            val fileName =
                                                "${it.fileName!!}${if (DownloadUtils.needDownloadSuffix) ".download" else ""}"
                                            file = File(DownloadUtils.downloadFolder, it.fileName!!)
                                            tempFile = File(DownloadUtils.downloadFolder, fileName)
                                            it.path = file.absolutePath
                                        } else {
                                            val filePath =
                                                "${it.path!!}${if (DownloadUtils.needDownloadSuffix) ".download" else ""}"
                                            file = File(it.path!!)
                                            tempFile = File(filePath)
                                        }
                                        //再次验证下载的文件是否已经被删除
                                        if (startPosition > 0 && !file.exists() && !tempFile.exists()) {
                                            throw FileNotFoundException("File does not exist")
                                        }
                                        if (result.supportRange) {
                                            //再次验证断点有效性
                                            if (startPosition > it.contentLength) {
                                                throw IllegalArgumentException("Start position greater than content length")
                                            }
                                        } else {
                                            startPosition = 0
                                        }
                                        //验证下载完成的任务与实际文件的匹配度
                                        if (startPosition == it.contentLength && startPosition > 0) {
                                            if ((file.exists() || tempFile.exists()) && startPosition == file.length()) {
//                                                LogUtils.d(TAG, "start file.exists() DONE")
                                                it.status = Status.COMPLETED
                                                it.message = ""
//                                                DownloadDBHelper.insertOrReplaceDownloadInfo(it)
                                                rename(it)
                                                emit(tasksInfo)
                                            } else {
                                                throw IOException("The content length is not the same as the file length")
                                            }
                                        } else {
                                            //写入文件
                                            val randomAccessFile = RandomAccessFile(tempFile, "rw")
                                            randomAccessFile.seek(startPosition)
                                            it.currentLength = startPosition
                                            val inputStream = result.byteStream
                                            val bufferSize = 1024 * 8
                                            val buffer = ByteArray(bufferSize)
                                            val bufferedInputStream =
                                                BufferedInputStream(inputStream, bufferSize)
                                            var readLength: Int
                                            try {
                                                while (bufferedInputStream.read(
                                                        buffer,
                                                        0,
                                                        bufferSize
                                                    ).also { bytes ->
                                                        readLength = bytes
                                                    } != -1 && this@launch.isActive//isActive保证任务能被及时取消
                                                ) {
                                                    randomAccessFile.write(buffer, 0, readLength)
                                                    it.currentLength += readLength
                                                    val currentTime = System.currentTimeMillis()
                                                    if (currentTime - it.updateTime > DownloadUtils.updateTime) {
                                                        it.status = Status.DOWNLOADING
                                                        it.message = ""
                                                        it.updateTime = currentTime
//                                                        DownloadDBHelper.insertOrReplaceDownloadInfo(it)
                                                        emit(tasksInfo)
                                                    }
                                                }
                                            } finally {
                                                inputStream.close()
                                                randomAccessFile.close()
                                                bufferedInputStream.close()
                                            }

                                            if (this@launch.isActive) {
                                                it.currentLength = it.contentLength
                                                it.message = ""
                                                it.updateTime = System.currentTimeMillis()
                                                it.status = Status.COMPLETED
//                                                DownloadDBHelper.insertOrReplaceDownloadInfo(it)
                                                rename(it)
                                                emit(tasksInfo)
                                            }
                                        }
                                    } else if (result is DownloadResponse.Error) {
                                        it.status = Status.FAILED
                                        it.message = result.message
//                                        DownloadDBHelper.insertOrReplaceDownloadInfo(it)
                                        emit(tasksInfo)
                                    }
                                }
                            }
                        }
                    }
                }
                    .flowOn(Dispatchers.IO)
                    .cancellable()
                    .catch {
                        it.printStackTrace()
                        // 异常
                        tasksInfo.let { info ->
                            info.status = Status.FAILED
                            info.message = it.message
//                            DownloadDBHelper.insertOrReplaceDownloadInfo(info)
                            emit(info)
                        }
                    }
                    .onCompletion {
                        if (tasksInfo.status == Status.COMPLETED) {
//                            DownloadUtils.launchNext(this@DownloadTask)
                        }
                    }
                    .collect {
                        coroutineScope.launch(Dispatchers.Main) {
                            // 结果
                            callback.invoke(it)
                        }
                    }
            }
        }
    }

    fun downloading(): Boolean {
        return tasksInfo.status == Status.DOWNLOADING
    }

    /**
     * 暫停下载
     */
    fun pause() {
        job?.cancel()
        tasksInfo.let {
            coroutineScope.launch(Dispatchers.IO) {
                it.status = Status.PAUSED
                it.message = ""
//                DownloadDBHelper.insertOrReplaceDownloadInfo(it)
                withContext(Dispatchers.Main) {
                    callback.invoke(it)
                }
            }
        }
    }

    /**
     * 等待下载
     */
    fun waiting() {
        job?.cancel()
        tasksInfo.let {
            coroutineScope.launch(Dispatchers.IO) {
                it.status = Status.PENDING
                it.message = ""
//                DownloadDBHelper.insertOrReplaceDownloadInfo(it)
                withContext(Dispatchers.Main) {
                    callback.invoke(it)
                }
            }
        }
    }


    /**
     * 取消下载
     */
    fun cancel(needCallback: Boolean = true) {
        job?.cancel()
        tasksInfo.let {
            coroutineScope.launch(Dispatchers.IO) {
                it.reset()
//                DownloadDBHelper.deleteDownloadInfo(it)
                if (needCallback) {
                    withContext(Dispatchers.Main) {
                        callback.invoke(it)
                    }
                }
                //同时删除已下载的文件
                it.tasks.forEach { task ->
                    task.path?.let { path ->
                        val file = File(path)
                        if (file.exists()) {
                            file.delete()
                        }
                        val tempFile = File("$path.download")
                        if (tempFile.exists()) {
                            tempFile.delete()
                        }
                    }
                }
            }
        }
    }

    /**
     * 重命名文件
     */
    private fun rename(downloadInfo: TaskInfo) {
        if (DownloadUtils.needDownloadSuffix) {
            downloadInfo.path?.let { path ->
                val file = File(path)
                val tempFile = File("$path.download")
                if (!file.exists() && tempFile.exists()) {
                    tempFile.renameTo(file)
                }
            }
        }
    }
}