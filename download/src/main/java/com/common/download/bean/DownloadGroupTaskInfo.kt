package com.common.download.bean

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.common.download.DEFAULT_TYPE
import com.common.download.DownloadUtils
import com.common.download.db.DownloadDBUtils

@Entity(
    tableName = DownloadDBUtils.TASKS_TABLE_NAME,
    indices = [Index("id", unique = true)]
)
class DownloadGroupTaskInfo(
    /** id urls.hashcode + "_" + type  */
    @PrimaryKey
    var id: String = "",
    /** 编号Id  */
    var unitId: String = "",
    /** 類型  */
    var type: String = DEFAULT_TYPE,
    /** 保存文件的父文件夹名称  */
    var dirName: String? = "",
    /** 當前下載的內容 */
    var current: DownloadTaskInfo? = null,
    /** 下載的狀態 [DownloadStatus.NONE] 0 无状态,[DownloadStatus.WAITING] 1 開始下载,[DownloadStatus.DOWNLOADING] 2 下载中,[DownloadStatus.PAUSED] 3 暂停,[DownloadStatus.COMPLETED] 4 完成,[DownloadStatus.FAILED] 5 错误，[DownloadStatus.PENDING] 6 等待中 */
    var status: Int = DownloadStatus.NONE,
    /** 下載錯誤的信息 */
    var message: String? = "",
    /** 下載的信息 */
    var tasks: List<DownloadTaskInfo> = mutableListOf(),
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
    constructor() : this("")

    /**
     * 重置任务
     */
    fun reset() {
        status = DownloadStatus.NONE
        progress = DownloadProgress()
        createTime = System.currentTimeMillis()
        updateTime = 0
        current = null
        tasks.map { it.reset() }
    }

    /**
     * 更新
     */
    fun update(status: Int, message: String?, updateTime: Long) {
        this.status = status
        this.message = message
        this.updateTime = updateTime
    }

    /**
     * 更新進度
     */
    fun updateProgress() {
        val currentProgress = tasks.sumOfSize { Pair(it.contentLength, it.currentLength) }
        progress.totalSize = currentProgress.first
        progress.downloadSize = currentProgress.second
    }

    private fun Iterable<DownloadTaskInfo>.sumOfSize(selector: (DownloadTaskInfo) -> Pair<Long, Long>): Pair<Long, Long> {
        var sum = Pair<Long, Long>(0, 0)
        for (element in this) {
            val pair = selector(element)
            sum = sum.copy(sum.first + pair.first, sum.second + pair.second)
        }
        return sum
    }

    private fun Iterable<DownloadTaskInfo>.sumOfUrl(selector: (DownloadTaskInfo) -> String): String {
        val sum = StringBuilder()
        for (element in this) {
            sum.append(selector(element))
        }
        return sum.toString()
    }

    fun buildId() : String {
        return "${tasks.sumOfUrl { it.url }.hashCode()}_$type"
    }

    override fun toString(): String {
        return "DownloadGroupInfo(id=$id, unitId=$unitId, type='$type', current=$current, status=$status, message=$message, progress=$progress, createTime=$createTime, updateTime=${DownloadUtils.updateTime}, needWifi=$needWifi, showNotification=$showNotification, notificationTitle='$notificationTitle', data=$data, abnormalExit=$abnormalExit, tasks=$tasks)"
    }
}

class DGBuilder {

    private var groupInfo = DownloadGroupTaskInfo()

    /** id  */
    fun id(id: String): DGBuilder {
        groupInfo.id = id
        return this
    }

    /** 编号Id  */
    fun unitId(unitId: String): DGBuilder {
        groupInfo.unitId = unitId
        return this
    }

    /** 類型  */
    fun type(type: String = DEFAULT_TYPE): DGBuilder {
        groupInfo.type = type
        return this
    }

    /** 保存文件的父文件夹名称  */
    fun dirName(dirName: String? = ""): DGBuilder {
        groupInfo.dirName = dirName
        return this
    }

    /** 是否需要 wifi */
    fun needWifi(needWifi: Boolean = false): DGBuilder {
        groupInfo.needWifi = needWifi
        return this
    }

    /** 是否顯示通知 */
    fun showNotification(showNotification: Boolean = true): DGBuilder {
        groupInfo.showNotification = showNotification
        return this
    }

    /** 通知完成後的標題 */
    fun notificationTitle(notificationTitle: String): DGBuilder {
        groupInfo.notificationTitle = notificationTitle
        return this
    }

    /** 跟下载相关的数据信息 */
    fun data(data: String?): DGBuilder {
        groupInfo.data = data
        return this
    }

    /** 子任務 */
    fun addChild(child: DownloadTaskInfo): DGBuilder {
        val list = groupInfo.tasks.toMutableList()
        list.add(child)
        groupInfo.tasks = list
        return this
    }

    /** 子任務 */
    fun addChild(block: () -> DownloadTaskInfo): DGBuilder {
        val list = groupInfo.tasks.toMutableList()
        list.add(block())
        groupInfo.tasks = list
        return this
    }

    /** 子任務 */
    fun addAll(block: () -> List<DownloadTaskInfo>): DGBuilder {
        val list = groupInfo.tasks.toMutableList()
        list.addAll(block())
        groupInfo.tasks = list
        return this
    }

    fun build() : DownloadGroupTaskInfo {
        if (groupInfo.tasks.isEmpty()) {
            throw IllegalArgumentException("download tasks is empty")
        }
        if (groupInfo.id.isEmpty()) {
            groupInfo.id = groupInfo.buildId()
        }
        return groupInfo
    }

}