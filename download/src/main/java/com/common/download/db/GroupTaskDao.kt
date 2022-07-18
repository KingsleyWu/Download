package com.common.download.db

import androidx.room.*
import com.common.download.bean.DownloadGroupInfo

@Dao
interface GroupTaskDao {

    /**
     * 通过状态查询任务
     */
    @Transaction
    @Query("SELECT * FROM tasks_record WHERE type = :type AND status IN (:status) ORDER BY createTime ASC")
    fun queryGroupTaskInfoByTypeAndStatus(type: String, vararg status: Int): MutableList<DownloadGroupInfo>

    /**
     * 通過id查询任务
     */
    @Transaction
    @Query("SELECT * FROM tasks_record WHERE id = :id")
    fun queryGroupTaskInfoByGroupId(id: String): DownloadGroupInfo?

    @Transaction
    @Query("SELECT * FROM tasks_record WHERE id = :id")
    fun get(id: Long): DownloadGroupInfo?

}