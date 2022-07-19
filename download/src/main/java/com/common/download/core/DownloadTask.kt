package com.common.download.core

import android.text.TextUtils
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.common.download.bean.DownloadResponse
import com.common.download.DownloadUtils
import com.common.download.base.DownloadScope
import com.common.download.bean.DownloadStatus
import com.common.download.bean.DownloadTaskGroupInfo
import com.common.download.bean.DownloadTaskInfo
import com.common.download.db.DownloadDBUtils
import com.common.download.utils.DownloadAction
import com.common.download.utils.DownloadBroadcastUtil
import com.common.download.utils.DownloadLog
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.*

class DownloadTask(val groupInfo: DownloadTaskGroupInfo) {
    private val coroutineScope: CoroutineScope = DownloadScope

    /** 当前下载的 job */
    private var job: Job? = null

    /**
     *  监听器
     */
    private val liveData = MutableLiveData<DownloadTaskGroupInfo>()

    internal fun start() {
        if (job == null || !job!!.isActive) {
            job = coroutineScope.launch {
                flow {
                    DownloadLog.d("開始下載， 當前的狀態 ${groupInfo.status}")
                    // 如果沒有開始下載過就直接設置為開始,其他狀態則表示已經開始下載過了
                    if (groupInfo.status == DownloadStatus.NONE) {
                        groupInfo.status = DownloadStatus.STARTED
                    }
                    emit(groupInfo)
                    DownloadLog.d("第一次判斷 url 或 重定向的url 是否存在重複內容")
                    // 判斷 url 是否存在重複
                    if (validate()) {
                        DownloadLog.d("下載的 url 或 重定向的url 存在重複內容 $groupInfo")
                        emit(groupInfo)
                        return@flow
                    }
                    // 是否是存在重定向 url
                    var isRedirect = false
                    // 初始化一下內容 contentLength
                    DownloadLog.d("獲取內容長度開始")
                    for (info in groupInfo.tasks) {
                        if (info.contentLength < 0) {
                            // 一開始想使用 head 請求，但會出現 404 的問題，所以還是使用 get 請求， 字段就不改了
                            val head = DownloadUtils.downloader.get(info.url)
                            if (head is DownloadResponse.Head) {
                                // 獲取內容長度 contentLength
                                info.contentLength = head.contentLength
                                // 如 newUrl 與 url 不一致 ，則複值 redirectUrl，表示這個是重定向鏈接
                                if (info.url != head.newUrl) {
                                    info.redirectUrl = head.newUrl
                                    DownloadLog.d("獲取內容長度，存在重定向 url = ${info.url}， redirectUrl = ${info.redirectUrl}")
                                    isRedirect = true
                                }
                            } else if (head is DownloadResponse.Error) {
                                // 出現錯誤則直接報錯，讓用戶重試
                                groupInfo.current = info
                                info.update(DownloadStatus.FAILED, head.message, System.currentTimeMillis())
                                groupInfo.update(DownloadStatus.FAILED, head.message, System.currentTimeMillis())
                                DownloadLog.d("獲取內容長度失敗， url = ${info.url}")
                                emit(groupInfo)
                                return@flow
                            }
                        }
                    }
                    DownloadLog.d("獲取內容長度結束")
                    DownloadLog.d("第二次判斷 url 或 重定向的url 是否存在重複內容")
                    // 如果已經重新重定向過，則需要重新判斷 url 是否存在重複
                    if (isRedirect && validate()) {
                        DownloadLog.d("下載的 url 或 重定向的url 存在重複內容 $groupInfo")
                        emit(groupInfo)
                        return@flow
                    }
                    DownloadLog.d("開始下載")
                    for (info in groupInfo.tasks) {
                        // 賦值當下載的內容
                        groupInfo.current = info
                        val url = info.redirectUrl.ifEmpty { info.url }
                        DownloadLog.d("開始下載的 url = $url， 當前下載的 status = ${info.status}")
                        when (info.status) {
                            // 无状态, 開始下載, 暫停, 错误
                            DownloadStatus.NONE,
                            DownloadStatus.STARTED,
                            DownloadStatus.PAUSED,
                            DownloadStatus.DOWNLOADING,
                            DownloadStatus.COMPLETED,
                            DownloadStatus.PENDING,
                            DownloadStatus.FAILED -> {
                                var startPosition = info.currentLength
                                //验证断点有效性
                                if (startPosition < 0) {
                                    startPosition = 0
                                }
                                //下载的文件是否已经被删除
                                if (startPosition > 0 && !TextUtils.isEmpty(info.path)) {
                                    if (info.status == DownloadStatus.COMPLETED && startPosition == info.contentLength && File(info.path!!).exists()) {
                                        continue
                                    } else {
                                        val filePath =
                                            "${info.path!!}${if (DownloadUtils.needDownloadSuffix) ".download" else ""}"
                                        if (!File(filePath).exists()) {
                                            if (startPosition == info.contentLength && File(info.path!!).exists()) {
                                                info.update(DownloadStatus.COMPLETED, "", System.currentTimeMillis())
                                                groupInfo.updateTime = System.currentTimeMillis()
                                                continue
                                            } else {
                                                startPosition = 0
                                            }
                                        }
                                    }
                                }
                                val result = DownloadUtils.downloader.download(
                                    start = "bytes=$startPosition-",
                                    url = url
                                )
                                if (result is DownloadResponse.Success) {
                                    //文件长度 由於帶了 “bytes=startPosition-” 這個header 所以需要判斷
                                    if (info.contentLength < 0) {
                                        info.contentLength = result.contentLength
                                    }
                                    //保存的文件名称
                                    if (TextUtils.isEmpty(info.fileName)) {
                                        info.fileName = UrlUtils.getUrlFileName(url)
                                    }
                                    //创建File,如果已经指定文件path,将会使用指定的path,如果没有指定将会使用默认的下载目录
                                    val file: File
                                    val tempFile: File
                                    if (TextUtils.isEmpty(info.path)) {
                                        val fileName =
                                            "${info.fileName!!}${if (DownloadUtils.needDownloadSuffix) ".download" else ""}"
                                        val dirName =
                                            DownloadUtils.downloadFolder + if (groupInfo.dirName.isNullOrEmpty()) "" else "${File.separator}${groupInfo.dirName}"
                                        file = File(dirName, info.fileName!!)
                                        tempFile = File(dirName, fileName)
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
                                            info.update(DownloadStatus.COMPLETED, "", System.currentTimeMillis())
                                            groupInfo.updateTime = System.currentTimeMillis()
                                            rename(info)
                                            emit(groupInfo)
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
                                            while (bufferedInputStream.read(buffer, 0, bufferSize)
                                                    .also { bytes ->
                                                        readLength = bytes
                                                    } != -1 && this@launch.isActive//isActive保证任务能被及时取消
                                            ) {
                                                randomAccessFile.write(buffer, 0, readLength)
                                                info.currentLength += readLength
                                                val currentTime = System.currentTimeMillis()
                                                // 間隔 DownloadUtils.updateTime 時間後更新進度
                                                if (currentTime - info.updateTime > DownloadUtils.updateTime) {
                                                    info.update(DownloadStatus.DOWNLOADING, "", currentTime)
                                                    groupInfo.update(DownloadStatus.DOWNLOADING, "", currentTime)
                                                    emit(groupInfo)
                                                }
                                            }
                                        } catch (e: Exception) {
                                            info.update(DownloadStatus.FAILED, e.message, System.currentTimeMillis())
                                            groupInfo.update(DownloadStatus.FAILED, e.message, System.currentTimeMillis())
                                            emit(groupInfo)
                                            break
                                        } finally {
                                            inputStream.close()
                                            randomAccessFile.close()
                                            bufferedInputStream.close()
                                        }

                                        if (this@launch.isActive) {
                                            info.currentLength = info.contentLength
                                            info.update(DownloadStatus.COMPLETED, "", System.currentTimeMillis())
                                            groupInfo.updateTime = System.currentTimeMillis()
                                            rename(info)
                                            emit(groupInfo)
                                        }
                                    }
                                } else if (result is DownloadResponse.Error) {
                                    info.update(DownloadStatus.FAILED, result.message, System.currentTimeMillis())
                                    groupInfo.update(DownloadStatus.FAILED, result.message, System.currentTimeMillis())
                                    emit(groupInfo)
                                    break
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
                        groupInfo.let { info ->
                            info.update(
                                DownloadStatus.FAILED,
                                it.message,
                                System.currentTimeMillis()
                            )
                            info.current?.update(
                                DownloadStatus.FAILED,
                                it.message,
                                System.currentTimeMillis()
                            )
                            emit(info)
                            DownloadBroadcastUtil.sendBroadcast(DownloadAction.Failed(info))
                        }
                        // 出錯了，繼續下一個下載
                        DownloadUtils.launchNext(groupInfo)
                    }
                    .onCompletion {
                        DownloadLog.d("start5 onCompletion : status = ${groupInfo.status}, id = ${groupInfo.id}")
                        // 完成下載或出錯了，繼續下一個下載
                        if (groupInfo.status == DownloadStatus.COMPLETED || groupInfo.status == DownloadStatus.FAILED) {
                            DownloadUtils.launchNext(groupInfo)
                            if (groupInfo.status == DownloadStatus.FAILED) {
                                DownloadBroadcastUtil.sendBroadcast(DownloadAction.Failed(groupInfo))
                            }
                        }
                    }
                    .collect {
                        if (it.tasks.filter { info -> info.status == DownloadStatus.COMPLETED }.size == it.tasks.size) {
                            it.status = DownloadStatus.COMPLETED
                        }
                        it.updateProgress()
                        DownloadDBUtils.insertOrReplaceTasks(it)
                        DownloadLog.d("當前 Group 狀態 status : ${it.status}， id = ${it.id}， percent = ${it.progress.percentStr()}")
                        coroutineScope.launch(Dispatchers.Main) {
                            // 结果
                            liveData.value = it
                            when (it.status) {
                                DownloadStatus.COMPLETED -> DownloadBroadcastUtil.sendBroadcast(
                                    DownloadAction.Success(it)
                                )
                                DownloadStatus.STARTED -> DownloadBroadcastUtil.sendBroadcast(
                                    DownloadAction.Start(it)
                                )
                                else -> {}
                            }
                        }
                    }
            }
        }
    }

    private fun validate(): Boolean {
        val urlList = groupInfo.tasks.map { it.redirectUrl.ifEmpty { it.url } }
        val urlSet = urlList.toSet()
        if (urlSet.size != groupInfo.tasks.size) {
            groupInfo.status = DownloadStatus.FAILED
            groupInfo.message = "Duplicate url !!!"
            groupInfo.updateTime = System.currentTimeMillis()
            return true
        }
        return false
    }

    /**
     * 是否是在下載中
     */
    fun downloading(): Boolean {
        return groupInfo.status == DownloadStatus.DOWNLOADING
    }

    /**
     * 是否是出錯了
     */
    fun failed(): Boolean {
        return groupInfo.status == DownloadStatus.FAILED
    }

    /**
     * 當前狀態
     */
    fun status() = groupInfo.status

    /**
     * 暫停下载
     */
    internal fun pause() {
        DownloadLog.d("暫停下载， 當前 Group id = ${groupInfo.id}， percent = ${groupInfo.progress.percentStr()}")
        job?.cancel()
        groupInfo.let {
            coroutineScope.launch(Dispatchers.IO) {
                it.update(DownloadStatus.PAUSED, "", System.currentTimeMillis())
                it.current?.update(DownloadStatus.PAUSED, "", System.currentTimeMillis())
                it.updateProgress()
                DownloadDBUtils.insertOrReplaceTasks(it)
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
        DownloadLog.d("等待下载， 當前 Group id = ${groupInfo.id}， percent = ${groupInfo.progress.percentStr()}")
        groupInfo.let {
            coroutineScope.launch(Dispatchers.IO) {
                it.update(DownloadStatus.PENDING, "", System.currentTimeMillis())
                it.current?.update(DownloadStatus.PENDING, "", System.currentTimeMillis())
                DownloadDBUtils.insertOrReplaceTasks(it)
                withContext(Dispatchers.Main) {
                    liveData.value = it
                }
            }
        }
    }

    /**
     * 更新一次內容
     */
    internal fun update() {
        coroutineScope.launch(Dispatchers.Main) {
            liveData.value = groupInfo
        }
    }

    /**
     * 取消下载
     */
    internal fun cancel(needCallback: Boolean = true) {
        job?.cancel()
        DownloadLog.d("取消下载， 當前 Group id = ${groupInfo.id}， percent = ${groupInfo.progress.percentStr()}")
        groupInfo.let {
            coroutineScope.launch(Dispatchers.IO) {
                it.reset()
                DownloadDBUtils.deleteTasks(it)
                if (needCallback) {
                    withContext(Dispatchers.Main) {
                        liveData.value = it
                    }
                }
                //同时删除已下载的文件
                it.tasks.forEach { task ->
                    DownloadDBUtils.deleteTask(task)
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
    private fun rename(taskInfo: DownloadTaskInfo) {
        if (DownloadUtils.needDownloadSuffix) {
            taskInfo.path?.let { path ->
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
     * @param lifecycleOwner lifecycleOwner
     * @param observer observer
     */
    fun observer(
        lifecycleOwner: LifecycleOwner,
        observer: Observer<DownloadTaskGroupInfo>
    ): DownloadTask {
        coroutineScope.launch(Dispatchers.Main) {
            liveData.observe(lifecycleOwner, observer)
        }
        return this
    }

    /**
     * 移除下载任务观察者
     * @param observer observer
     */
    fun removeObserver(observer: Observer<DownloadTaskGroupInfo>): DownloadTask {
        coroutineScope.launch(Dispatchers.Main) {
            liveData.removeObserver(observer)
        }
        return this
    }

    /**
     * 添加下载任务观察者並下载
     * @param lifecycleOwner lifecycleOwner
     * @param observer observer
     */
    fun download(lifecycleOwner: LifecycleOwner, observer: Observer<DownloadTaskGroupInfo>) {
        DownloadUtils.download(observer(lifecycleOwner, observer))
    }

    /**
     * 下载
     */
    fun download() {
        DownloadUtils.download(this)
    }

}

