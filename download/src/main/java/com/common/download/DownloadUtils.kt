package com.common.download

import android.os.Environment
import com.common.download.base.appContext
import com.common.download.bean.DownloadTaskInfo
import com.common.download.bean.DownloadGroupInfo
import com.common.download.bean.DownloadStatus
import com.common.download.core.DownloadTask
import com.common.download.db.DownloadDBUtils
import com.common.download.downloader.RetrofitDownloader
import com.common.download.utils.DownloadAction
import com.common.download.utils.DownloadBroadcastUtil
import java.util.concurrent.ConcurrentHashMap


/** id key */
const val KEY_TASK_ID = "taskId"
/** error key */
const val KEY_ERROR = "error"
/** notify_id key */
const val KEY_NOTIFY_ID = "notify_id"
/** 默認類型 */
const val DEFAULT_TYPE = "default"

object DownloadUtils {

    /**
     * 下載器
     */
    var downloader = RetrofitDownloader

    /**
     * 是否需要 .download 後綴，默認為 true
     */
    @JvmField
    var needDownloadSuffix = true

    /**
     * 最大同時運行的下載數量 1
     */
    @JvmField
    var MAX_TASK = 1

    /**
     * 默認進度更新時間 500毫秒
     */
    @JvmField
    var updateTime = 500L

    /**
     * 下載的路徑
     */
    val downloadFolder: String? by lazy {
        appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.absolutePath
    }

    /**
     * 當前所有的 DownloadTask
     */
    private val taskMap = ConcurrentHashMap<Long, DownloadTask>()

    /**
     * 當前運行的 DownloadTask
     */
    private val runningTaskMap = ConcurrentHashMap<Long, DownloadTask>()

    /**
     * 等待中的 DownloadTask
     */
    private val pendingTaskMap = ConcurrentHashMap<Long, DownloadTask>()

    /**
     * 運行下一個下載
     */
    internal fun launchNext(taskInfo: DownloadGroupInfo) {
        val taskId = taskInfo.id
        runningTaskMap.remove(taskId)
        if (runningTaskMap.size < MAX_TASK && pendingTaskMap.size > 0) {
            val pendingIterator = pendingTaskMap.iterator()
            for (entrySet in pendingIterator) {
                val nextTask = entrySet.value
                if (taskId != entrySet.key && nextTask.groupInfo.status != DownloadStatus.COMPLETED) {
                    if (runningTaskMap[entrySet.key] == null) {
                        runningTaskMap[entrySet.key] = nextTask
                        pendingIterator.remove()
                    }
                    nextTask.start()
                    if (runningTaskMap.size >= MAX_TASK) {
                        break
                    }
                }
            }
        }
    }

    /**
     * 删除并取消任务
     */
    private fun removeAndCancel(id: Long, needCallback: Boolean = true): DownloadTask? {
        runningTaskMap.remove(id)
        pendingTaskMap.remove(id)
        // 如当前没在运行，则查看 db 里是否有数据，有则直接删除
        val downloadTask = taskMap.remove(id) ?: getTaskFromDb(id)
        downloadTask?.cancel(needCallback)
        return downloadTask
    }

    /**
     * 取消任务
     */
    @JvmStatic
    fun cancel(id: Long, needCallback: Boolean = true) {
        val downloadTask = removeAndCancel(id, needCallback)
        DownloadBroadcastUtil.sendBroadcast(DownloadAction.Cancel(downloadTask?.groupInfo))
    }

    /**
     * 取消任务
     */
    @JvmStatic
    fun pause(id: Long) {
        taskMap[id]?.let {
            it.pause()
            DownloadBroadcastUtil.sendBroadcast(DownloadAction.Pause(it.groupInfo))
            launchNext(it.groupInfo)
        }
    }

    @JvmStatic
    fun download(id: Long): DownloadTask {
        return download(runningTaskMap[id] ?: request(id))
    }

    @JvmStatic
    fun download(url: String, type: String = DEFAULT_TYPE): DownloadTask {
        val id = (url + type).hashCode().toLong()
        return download(runningTaskMap[id] ?: request(id, url, type))
    }

    @JvmStatic
    fun download(task: DownloadTask): DownloadTask {
        var downloadTask = runningTaskMap[task.groupInfo.id]
        if (downloadTask != null) {
            if (downloadTask.status() != DownloadStatus.DOWNLOADING) {
                downloadTask.start()
            }
        } else {
            downloadTask = task
            if (runningTaskMap.size < MAX_TASK) {
                runningTaskMap[task.groupInfo.id] = downloadTask
                if (downloadTask.status() != DownloadStatus.DOWNLOADING) {
                    downloadTask.start()
                }
            } else {
                downloadTask.pending()
                pendingTaskMap[task.groupInfo.id] = downloadTask
            }
        }
        return downloadTask
    }

    private fun request(id: Long, url: String, type: String): DownloadTask {
        return getTaskFromMapOrDb(id) ?: DownloadTask(groupInfo = DownloadGroupInfo().apply {
            this.id = id
            unitId = url + "_" + type
            tasks = mutableListOf<DownloadTaskInfo>().apply {
                this.add(DownloadTaskInfo(url, type = type))
            }
        }).also { taskMap[id] = it }
    }

    private fun request(id: Long): DownloadTask {
        return getTaskFromMapOrDb(id) ?: DownloadTask(groupInfo = DownloadGroupInfo().apply {
            this.id = id
            tasks = mutableListOf<DownloadTaskInfo>().apply {
            }
        }).also { taskMap[id] = it }
    }

    fun getTaskFromMapOrDb(id: Long): DownloadTask? {
        return taskMap[id] ?: getTaskFromDb(id)
    }

    fun getTaskFromDb(id: Long): DownloadTask? {
        return DownloadDBUtils.get(id)?.let { taskInfo ->
            return DownloadTask(taskInfo).also { taskMap[id] = it }
        }
    }
}
