package com.common.download.bean

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.common.download.DEFAULT_TYPE
import com.common.download.db.DownloadDBUtils

@Entity(
    tableName = DownloadDBUtils.TASKS_TABLE_NAME,
    indices = [Index("id", unique = true)]
)
open class DownloadTaskGroupInfo(
    /** id */
    @PrimaryKey
    var id: Long = 0,
    /** 编号Id  */
    var unitId: String? = "",
    /** 類型  */
    var type: String = DEFAULT_TYPE,
    /** 保存文件的父文件夹名称  */
    var dirName: String? = "",
    /** 當前下載的內容 */
    var current: DownloadTaskInfo? = null,
    /** 下載的狀態 [DownloadStatus.NONE] 0 无状态,[DownloadStatus.STARTED] 1 開始下载,[DownloadStatus.DOWNLOADING] 2 下载中,[DownloadStatus.PAUSED] 3 暂停,[DownloadStatus.COMPLETED] 4 完成,[DownloadStatus.FAILED] 5 错误，[DownloadStatus.PENDING] 6 等待中 */
    var status: Int = DownloadStatus.NONE,
    /** 下載錯誤的信息 */
    var message: String? = "",
    /** 下載進度 */
    @Embedded
    var progress: DownloadProgress = DownloadProgress(),
    /** 創建時間 */
    var createTime: Long = System.currentTimeMillis(),
    /** 上一次更新的時間 */
    var updateTime: Long = 0,
    /** 是否需要 wifi */
    var needWifi: Boolean = false,
    /** 是否顯示通知 */
    var showNotification: Boolean = true,
    /** 通知完成後的標題 */
    var notificationTitle: String = "",
    /** 跟下载相关的数据信息 */
    var data: String? = null,
    /** 是否是异常结束 */
    var abnormalExit: Boolean = false
) {
    constructor() : this(0)

    /**
     * 重置任务
     */
    open fun reset() {
        status = DownloadStatus.NONE
        progress = DownloadProgress()
        createTime = System.currentTimeMillis()
        updateTime = 0
    }

}