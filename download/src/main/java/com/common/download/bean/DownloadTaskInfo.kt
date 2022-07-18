package com.common.download.bean

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.common.download.db.DownloadDBUtils

/**
 * 单独的下載的信息
 */
@Entity(
    tableName = DownloadDBUtils.TASK_TABLE_NAME,
    indices = [Index("url", unique = true)]
)
data class DownloadTaskInfo(
    @PrimaryKey
    /** 下載用的url */
    var url: String = "",
    /** 下載用的url, 重定向後的 url */
    var redirectUrl: String = "",
    /** groupId */
    var groupId: Long = 0,
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
    /** 下載的狀態 [DownloadStatus.NONE] 0 无状态,[DownloadStatus.STARTED] 1 開始下载,[DownloadStatus.DOWNLOADING] 2 下载中,[DownloadStatus.PAUSED] 3 暂停,[DownloadStatus.COMPLETED] 4 完成,[DownloadStatus.FAILED] 5 错误，[DownloadStatus.PENDING] 6 等待中 */
    var status: Int = DownloadStatus.NONE,
    /** 异常信息 */
    var message: String? = "",
    /** 跟下载相关的数据信息 */
    var data: String? = null,
    /** 上一次更新的時間 */
    var updateTime: Long = 0
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