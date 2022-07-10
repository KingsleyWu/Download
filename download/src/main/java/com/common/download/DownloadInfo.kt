package com.common.download

import androidx.room.*
import com.common.download.db.DownloadDBUtils

object Status {
    /** 无状态 */
    const val NONE = 0
    /** 開始下載 */
    const val STARTED = 1
    /** 下载中 */
    const val DOWNLOADING = 2
    /** 暫停 */
    const val PAUSED = 3
    /** 完成 */
    const val COMPLETED = 4
    /** 错误 */
    const val FAILED = 5
    /** 刪除了 */
    const val DELETED = 6
    /** 等待下载中 */
    const val PENDING = 7
}

/**
 * @param id id
 * @param tasks tasks
 * @param status 下載的狀態 [None] 0 无状态,[Started] 1 開始下载,[Downloading] 2 下载中,[Paused] 3 暂停,[Completed] 4 完成,[Failed] 5 错误,[Deleted] 6 刪除了，[Pending] 7 等待中
 * @param message 下載錯誤的信息
 * @param progress 下載進度
 * @param createTime 下載進度
 * @param updateTime 上一次更新的時間
 * @param data 跟下载相关的数据信息
 * @param abnormalExit 异常结束
 */
@Entity(
    tableName = DownloadDBUtils.TABLE_NAME,
    indices = [Index("id", unique = true)]
)
@TypeConverters(TaskConverters::class)
class TasksInfo @Ignore constructor(
    @PrimaryKey
    var id: Int,
    var tasks: List<TaskInfo> = mutableListOf(),
    var status: Int = Status.NONE,
    var message: String? = "",
    @Embedded
    var progress: Progress = Progress(),
    var createTime: Long = System.currentTimeMillis(),
    var updateTime: Long = 0,
    var data: String? = null,
    var abnormalExit: Boolean = false
) {
    constructor(): this(0)
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
 * @param url 下載用的url
 * @param action 如需要跳轉的動作可以放到此位置
 * @param title 下載文件時的通知標題
 * @param type 下載的文件類型，用於標識下載文件
 * @param flag flag
 * @param groupId 可以用於做識別是否是組合下載
 * @param path 保存的地址
 * @param fileName 下載保存的文件名稱
 * @param contentLength 下載文件的長度
 * @param currentLength 已經下載的長度
 * @param childUrl 子下載Url
 */
class TaskInfo(
    var url: String = "",
    var action: String? = "",
    var title: String = "",
    var type: String? = "",
    var flag: String? = "",
    var groupId: String = "",
    var path: String? = null,
    var fileName: String? = null,
    var contentLength: Long = -1,
    var currentLength: Long = 0,
    var status: Int = Status.NONE,
    var message: String? = "",
    var isChild: Boolean = false,
    var childUrl: String? = null,
    var data: String? = null,
    var updateTime: Long = 0
) {

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