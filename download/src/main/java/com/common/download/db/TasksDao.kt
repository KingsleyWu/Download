package com.common.download.db

import androidx.room.*
import com.common.download.bean.DownloadTaskGroupInfo

/**
 * 下载信息的 db Dao
 */
@Dao
interface TasksDao {
    /**
     * 插入或替换
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(taskGroupInfo: DownloadTaskGroupInfo): Long
    /**
     * 更新
     */
    @Update
    fun update(taskGroupInfo: DownloadTaskGroupInfo): Int
    /**
     * 删除
     */
    @Delete
    fun delete(taskGroupInfo: DownloadTaskGroupInfo): Int
    /**
     * 删除
     */
    @Query("DELETE FROM tasks_record WHERE id = :id")
    fun delete(id: String): Int
    /**
     * 查询所有任务
     */
    @Query("SELECT * FROM tasks_record")
    fun getAll(): List<DownloadTaskGroupInfo>
    /**
     * 通过状态查询任务
     */
    @Query("SELECT * FROM tasks_record WHERE status IN(:status)")
    fun getAllWithStatus(vararg status: Int): List<DownloadTaskGroupInfo>
    /**
     * 通过id查询任务
     */
    @Query("SELECT * FROM tasks_record WHERE id = :id")
    fun get(id: String): DownloadTaskGroupInfo?
    /**
     * 通过ids查询任务列表
     */
    @Query("SELECT * FROM tasks_record WHERE id IN(:id)")
    fun get(vararg id: String): List<DownloadTaskGroupInfo>
    /**
     * 通过id更新 data
     */
    @Query("UPDATE tasks_record SET data = :data WHERE id = :id")
    fun update(id: String, data: String): Int
}