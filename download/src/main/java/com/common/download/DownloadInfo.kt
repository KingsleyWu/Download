package com.common.download

import androidx.room.*
import com.common.download.db.DownloadDBUtils

/** 下載的狀態
 * [Status.NONE] 0 无状态,
 * [Status.STARTED] 1 開始下载,
 * [Status.DOWNLOADING] 2 下载中,
 * [Status.PAUSED] 3 暂停,
 * [Status.COMPLETED] 4 完成,
 * [Status.FAILED] 5 错误,
 * [Status.PENDING] 6 等待中
 */
object Status {
    /** 无状态 0 */
    const val NONE = 0

    /** 開始下載 1 */
    const val STARTED = 1

    /** 下载中 2 */
    const val DOWNLOADING = 2

    /** 暫停 3 */
    const val PAUSED = 3

    /** 完成 4 */
    const val COMPLETED = 4

    /** 错误 5 */
    const val FAILED = 5

    /** 等待下载中 6 */
    const val PENDING = 6
}

@Entity(
    tableName = DownloadDBUtils.TASK_TABLE_NAME,
    indices = [Index("id", unique = true)]
)
@TypeConverters(TaskConverters::class)
data class TaskInfo(
    /** id */
    @PrimaryKey
    var id: Long = 0,
    /** 编号Id  */
    var unitId: String? = "",
    /** 類型  */
    var type: String = DEFAULT_TYPE,
    /** tasks 需要下載的內容信息 */
    var tasks: List<DownloadInfo> = mutableListOf(),
    /** 當前下載的內容 */
    var current: DownloadInfo? = null,
    /** 下載的狀態 [Status.NONE] 0 无状态,[Status.STARTED] 1 開始下载,[Status.DOWNLOADING] 2 下载中,[Status.PAUSED] 3 暂停,[Status.COMPLETED] 4 完成,[Status.FAILED] 5 错误，[Status.PENDING] 6 等待中 */
    var status: Int = Status.NONE,
    /** 下載錯誤的信息 */
    var message: String? = "",
    /** 下載進度 */
    @Embedded
    var progress: Progress = Progress(),
    /** 創建時間 */
    var createTime: Long = System.currentTimeMillis(),
    /** 上一次更新的時間 */
    var updateTime: Long = 0,
    /** 跟下载相关的数据信息 */
    var data: String? = null,
    /** 是否是异常结束 */
    var abnormalExit: Boolean = false
) {
    constructor() : this(0)
    /**
     * 重置任务
     */
    fun reset() {
        tasks.map { it.reset() }
        status = Status.NONE
        progress = Progress()
        createTime = System.currentTimeMillis()
        updateTime = 0
    }
}

/**
 * 单独的下載的信息
 *
 */
data class DownloadInfo(
    /** 在tasks 的位置 */
    var index: Int = 0,
    /** 下載用的url */
    var url: String = "",
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
    /** 下載的狀態 [Status.NONE] 0 无状态,[Status.STARTED] 1 開始下载,[Status.DOWNLOADING] 2 下载中,[Status.PAUSED] 3 暂停,[Status.COMPLETED] 4 完成,[Status.FAILED] 5 错误，[Status.PENDING] 6 等待中 */
    var status: Int = Status.NONE,
    /** 异常信息 */
    var message: String? = "",
    /** 是否是子task */
    var isChild: Boolean = false,
    /** 跟下载相关的数据信息 */
    var data: String? = null,
    /** 上一次更新的時間 */
    var updateTime: Long = 0
) {

    constructor() : this(0)

    /**
     * 重置任务
     */
    fun reset() {
        contentLength = -1
        currentLength = 0
        updateTime = 0
        status = Status.NONE
    }
}