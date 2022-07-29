package com.common.download.bean

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 单独的下載的信息
 */
@Entity
data class DownloadTaskInfo(
    /** 下載用的url */
    @PrimaryKey
    var url: String = "",
    /** groupId */
    var groupId: String = "",
    /** 在 group tasks 的位置 */
    var index: Int = 0,
    /** 如需要跳轉的動作可以放到此位置 */
    var action: String? = "",
    /** 下載文件時的通知標題 */
    var title: String = "",
    /** 下載的文件類型，用於標識下載文件 */
    var type: String? = "",
    /** flag */
    var flag: String? = "",
    /** 保存的地址 */
    var path: String? = null,
    /** 下載保存的文件名稱 */
    var fileName: String? = null,
    /** 下載文件的長度 */
    var contentLength: Long = -1,
    /** 已經下載的長度 */
    var currentLength: Long = 0,
    /** 下載的狀態 [DownloadStatus.NONE] 0 无状态,[DownloadStatus.WAITING] 1 開始下载,[DownloadStatus.DOWNLOADING] 2 下载中,[DownloadStatus.PAUSED] 3 暂停,[DownloadStatus.COMPLETED] 4 完成,[DownloadStatus.FAILED] 5 错误，[DownloadStatus.PENDING] 6 等待中 */
    var status: Int = DownloadStatus.NONE,
    /** 异常信息 */
    var message: String? = "",
    /** 跟下载相关的数据信息 */
    var data: String? = null,
    /** 上一次更新的時間 */
    var updateTime: Long = 0,
    /** 下載用的url, 重定向後的 url */
    var redirectUrl: String = ""
) {

    constructor() : this("")

    /**
     * 重置任务
     */
    fun reset() {
        contentLength = -1
        currentLength = 0
        updateTime = 0
        status = DownloadStatus.NONE
    }

    /**
     * 更新
     */
    fun update(status: Int, message: String?, updateTime: Long) {
        this.status = status
        this.message = message
        this.updateTime = updateTime
    }
}

/**
 * DownloadTaskInfo 的 Builder 類
 */
class DIBuilder {

    private var taskInfo = DownloadTaskInfo()

    /** 下載用的url */
    fun url(url: String?): DIBuilder {
        taskInfo.url = url ?: ""
        return this
    }

    /** groupId */
    fun groupId(groupId: String): DIBuilder {
        taskInfo.groupId = groupId
        return this
    }

    /** 在 group tasks 的位置 */
    fun index(index: Int): DIBuilder {
        taskInfo.index = index
        return this
    }

    /** 如需要跳轉的動作可以放到此位置 */
    fun action(action: String?): DIBuilder {
        taskInfo.action = action ?: ""
        return this
    }

    /** 下載文件時的通知標題 */
    fun title(title: String?): DIBuilder {
        taskInfo.title = title ?: ""
        return this
    }

    /** 下載的文件類型，用於標識下載文件 */
    fun type(type: String?): DIBuilder {
        taskInfo.type = type ?: ""
        return this
    }

    /** flag */
    fun flag(flag: String?): DIBuilder {
        taskInfo.flag = flag ?: ""
        return this
    }

    /** 保存的地址 */
    fun path(path: String?): DIBuilder {
        taskInfo.path = path
        return this
    }

    /** 下載保存的文件名稱 */
    fun fileName(fileName: String?): DIBuilder {
        taskInfo.fileName = fileName
        return this
    }

    /** 跟下载相关的数据信息 */
    fun data(data: String?): DIBuilder {
        taskInfo.data = data
        return this
    }

    fun build(): DownloadTaskInfo {
        if (taskInfo.url.isEmpty()) {
            throw IllegalArgumentException("url must not empty")
        }
        if (taskInfo.groupId.isEmpty()) {
            throw IllegalArgumentException("groupId must not empty")
        }
        return taskInfo
    }
}