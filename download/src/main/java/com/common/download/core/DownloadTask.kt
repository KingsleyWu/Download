package com.common.download.core

import android.text.TextUtils
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.common.download.*
import com.common.download.db.DownloadDBUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.*

class DownloadTask(val taskInfo: TaskInfo) {
    internal var coroutineScope: CoroutineScope = DownloadScope

    /** 当前下载的 job */
    private var job: Job? = null

    /**
     *  监听器
     */
    private val liveData = MutableLiveData<TaskInfo>()

    internal fun start() {
        if (job == null || !job!!.isActive) {
            job = coroutineScope.launch {
                flow {
                    emit(taskInfo)
                    if (taskInfo.status != Status.COMPLETED) {
                        // 初始化一下內容 contentLength
                        for (info in taskInfo.tasks) {
                            if (info.contentLength < 0) {
                                val head = DownloadUtils.downloader.head(info.url)
                                if (head is DownloadResponse.Head) {
                                    info.contentLength = head.contentLength
                                }  else if (head is DownloadResponse.Error) {
                                    info.status = Status.FAILED
                                    taskInfo.status = Status.FAILED
                                    info.message = head.message
                                    taskInfo.message = head.message
                                    emit(taskInfo)
                                    DownloadBroadcastUtil.sendBroadcast(DownloadAction.Failed(taskInfo))
                                    return@flow
                                }
                            }
                        }
                        for (info in taskInfo.tasks) {
                            taskInfo.current = info
                            Log.d("TAG", "start1 status : ${info.status}, status: ${taskInfo.status}")
                            when (info.status) {
                                /** 无状态, 暫停, 错误 */
                                Status.NONE,
                                Status.PAUSED,
                                Status.FAILED -> {
                                    if (true) {
                                        info.status = Status.STARTED
                                        emit(taskInfo)
                                        Log.d("TAG", "start2 status : ${info.status}, status: ${taskInfo.status}")
                                        delay(2000)
                                        info.status = Status.DOWNLOADING
                                        taskInfo.status = Status.DOWNLOADING
                                        Log.d("TAG", "start3 status : ${info.status}, status: ${taskInfo.status}")
                                        delay(2000)
                                        info.status = Status.COMPLETED
                                        Log.d("TAG", "start4 status : ${info.status}, status: ${taskInfo.status}")
                                        emit(taskInfo)
                                        continue
                                    }
                                    var startPosition = info.currentLength
                                    //验证断点有效性
                                    if (startPosition < 0) {
                                        startPosition = 0
                                    }
                                    //下载的文件是否已经被删除
                                    if (startPosition > 0 && !TextUtils.isEmpty(info.path)) {
                                        val filePath =
                                            "${info.path!!}${if (DownloadUtils.needDownloadSuffix) ".download" else ""}"
                                        if (!File(filePath).exists()) {
                                            if (startPosition == info.contentLength && File(info.path!!).exists()) {
                                                info.status = Status.COMPLETED
                                                info.message = ""
                                                continue
                                            } else {
                                                startPosition = 0
                                            }
                                        }
                                    }
//                                    LogUtils.d(TAG, "start download startPosition = $startPosition")
                                    val result = DownloadUtils.downloader.download(
                                        start = "bytes=$startPosition-",
                                        url = info.url
                                    )
                                    if (result is DownloadResponse.Success) {
                                        //文件长度 由於帶了 “bytes=startPosition-” 這個header 所以需要判斷
                                        if (info.contentLength < 0) {
                                            info.contentLength = result.contentLength
                                        }
                                        //保存的文件名称
                                        if (TextUtils.isEmpty(info.fileName)) {
                                            info.fileName = UrlUtils.getUrlFileName(info.url)
                                        }
                                        //创建File,如果已经指定文件path,将会使用指定的path,如果没有指定将会使用默认的下载目录
                                        val file: File
                                        val tempFile: File
                                        if (TextUtils.isEmpty(info.path)) {
                                            val fileName =
                                                "${info.fileName!!}${if (DownloadUtils.needDownloadSuffix) ".download" else ""}"
                                            file = File(DownloadUtils.downloadFolder, info.fileName!!)
                                            tempFile = File(DownloadUtils.downloadFolder, fileName)
                                            info.path = file.absolutePath
                                        } else {
                                            val filePath =
                                                "${info.path!!}${if (DownloadUtils.needDownloadSuffix) ".download" else ""}"
                                            file = File(info.path!!)
                                            tempFile = File(filePath)
                                        }
                                        //再次验证下载的文件是否已经被删除
                                        if (startPosition > 0 && !file.exists() && !tempFile.exists()) {
                                            throw FileNotFoundException("File does not exist")
                                        }
                                        if (result.supportRange) {
                                            //再次验证断点有效性
                                            if (startPosition > info.contentLength) {
                                                throw IllegalArgumentException("Start position greater than content length")
                                            }
                                        } else {
                                            startPosition = 0
                                        }
                                        //验证下载完成的任务与实际文件的匹配度
                                        if (startPosition == info.contentLength && startPosition > 0) {
                                            if ((file.exists() || tempFile.exists()) && startPosition == file.length()) {
//                                                LogUtils.d(TAG, "start file.exists() DONE")
                                                info.status = Status.COMPLETED
                                                info.message = ""
                                                rename(info)
                                                emit(taskInfo)
                                            } else {
                                                throw IOException("The content length is not the same as the file length")
                                            }
                                        } else {
                                            //写入文件
                                            val randomAccessFile = RandomAccessFile(tempFile, "rw")
                                            randomAccessFile.seek(startPosition)
                                            info.currentLength = startPosition
                                            val inputStream = result.byteStream
                                            val bufferSize = 1024 * 8
                                            val buffer = ByteArray(bufferSize)
                                            val bufferedInputStream =
                                                BufferedInputStream(inputStream, bufferSize)
                                            var readLength: Int
                                            try {
                                                while (bufferedInputStream.read(buffer, 0, bufferSize).also { bytes ->
                                                        readLength = bytes
                                                    } != -1 && this@launch.isActive//isActive保证任务能被及时取消
                                                ) {
                                                    randomAccessFile.write(buffer, 0, readLength)
                                                    info.currentLength += readLength
                                                    val currentTime = System.currentTimeMillis()
                                                    if (currentTime - info.updateTime > DownloadUtils.updateTime) {
                                                        info.status = Status.DOWNLOADING
                                                        info.message = ""
                                                        info.updateTime = currentTime
                                                        emit(taskInfo)
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                info.status = Status.FAILED
                                                taskInfo.status = Status.FAILED
                                                info.message = e.message
                                                taskInfo.message = e.message
                                                emit(taskInfo)
                                                DownloadBroadcastUtil.sendBroadcast(DownloadAction.Failed(taskInfo))
                                                break
                                            } finally {
                                                inputStream.close()
                                                randomAccessFile.close()
                                                bufferedInputStream.close()
                                            }

                                            if (this@launch.isActive) {
                                                info.currentLength = info.contentLength
                                                info.message = ""
                                                info.updateTime = System.currentTimeMillis()
                                                info.status = Status.COMPLETED
                                                rename(info)
                                                emit(taskInfo)
                                            }
                                        }
                                    } else if (result is DownloadResponse.Error) {
                                        info.status = Status.FAILED
                                        taskInfo.status = Status.FAILED
                                        info.message = result.message
                                        taskInfo.message = result.message
                                        emit(taskInfo)
                                        DownloadBroadcastUtil.sendBroadcast(DownloadAction.Failed(taskInfo))
                                        break
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
                        taskInfo.let { info ->
                            info.status = Status.FAILED
                            info.message = it.message
                            emit(info)
                            DownloadBroadcastUtil.sendBroadcast(DownloadAction.Failed(info))
                        }
                    }
                    .onCompletion {
                        Log.d("TAG", "start5 onCompletion : ${taskInfo.status}")
                        if (taskInfo.status == Status.COMPLETED || taskInfo.status == Status.FAILED) {
                            DownloadUtils.launchNext(this@DownloadTask)
                        }
                    }
                    .collect {
                        if (it.tasks.filter { info -> info.status == Status.COMPLETED }.size == it.tasks.size) {
                            it.status = Status.COMPLETED
                            DownloadBroadcastUtil.sendBroadcast(DownloadAction.Success(it))
                        }
                        DownloadDBUtils.insertOrReplace(it)
                        Log.d("TAG", "start5 status : ${it.status}")
                        coroutineScope.launch(Dispatchers.Main) {
                            // 结果
                            liveData.value = it
                        }
                    }
            }
        }
    }

    fun downloading(): Boolean {
        return taskInfo.status == Status.DOWNLOADING
    }

    fun failed(): Boolean {
        return taskInfo.status == Status.FAILED
    }

    fun status() = taskInfo.status

    /**
     * 暫停下载
     */
    internal fun pause() {
        job?.cancel()
        taskInfo.let {
            coroutineScope.launch(Dispatchers.IO) {
                it.status = Status.PAUSED
                it.message = ""
                DownloadDBUtils.insertOrReplace(it)
                withContext(Dispatchers.Main) {
                    liveData.value = it
                }
            }
        }
    }

    /**
     * 等待下载
     */
    internal fun pending() {
        job?.cancel()
        taskInfo.let {
            coroutineScope.launch(Dispatchers.IO) {
                it.status = Status.PENDING
                it.message = ""
                DownloadDBUtils.insertOrReplace(it)
                withContext(Dispatchers.Main) {
                    liveData.value = it
                }
            }
        }
    }

    /**
     * 取消下载
     */
    internal fun cancel(needCallback: Boolean = true) {
        job?.cancel()
        taskInfo.let {
            coroutineScope.launch(Dispatchers.IO) {
                it.reset()
                DownloadDBUtils.delete(it)
                if (needCallback) {
                    withContext(Dispatchers.Main) {
                        liveData.value = it
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
    private fun rename(downloadInfo: DownloadInfo) {
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

    /**
     * 添加下载任务观察者
     */
    fun observer(lifecycleOwner: LifecycleOwner, observer: Observer<TaskInfo>): DownloadTask {
        coroutineScope.launch(Dispatchers.Main) {
            liveData.observe(lifecycleOwner, observer)
        }
        return this
    }

    /**
     * 移除下载任务观察者
     */
    fun removeObserver(observer: Observer<TaskInfo>): DownloadTask {
        coroutineScope.launch(Dispatchers.Main) {
            liveData.removeObserver(observer)
        }
        return this
    }

}