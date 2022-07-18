package com.common.download.bean

import androidx.room.Relation

/**
 * 下載组合的信息
 *
 * @param tasks 所有下载的信息
 */
class DownloadGroupInfo(
    @Relation(
        parentColumn = "id",
        entityColumn = "groupId",
        entity = DownloadTaskInfo::class
    )
    var tasks: List<DownloadTaskInfo>
) : DownloadTaskGroupInfo() {

    constructor() : this(mutableListOf())

    /**
     * 重置任务
     */
    override fun reset() {
        super.reset()
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
        val currentProgress = tasks.sumOf { Pair(it.contentLength, it.currentLength) }
        progress.totalSize = currentProgress.first
        progress.downloadSize = currentProgress.second
    }

    private fun Iterable<DownloadTaskInfo>.sumOf(selector: (DownloadTaskInfo) -> Pair<Long, Long>): Pair<Long, Long> {
        var sum = Pair<Long, Long>(0, 0)
        for (element in this) {
            val pair = selector(element)
            sum = sum.copy(sum.first + pair.first, sum.second + pair.second)
        }
        return sum
    }

    override fun toString(): String {
        return "DownloadGroupInfo(id=$id, unitId=$unitId, type='$type', current=$current, status=$status, message=$message, progress=$progress, createTime=$createTime, updateTime=$updateTime, needWifi=$needWifi, showNotification=$showNotification, notificationTitle='$notificationTitle', data=$data, abnormalExit=$abnormalExit, tasks=$tasks)"
    }
}