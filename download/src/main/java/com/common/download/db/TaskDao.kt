package com.common.download.db

import androidx.room.*
import com.common.download.bean.DownloadTaskInfo

/**
 * 下载信息的 db Dao
 */
@Dao
interface TaskDao {
    /**
     * 插入或替换
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(taskInfo: DownloadTaskInfo): Long
    /**
     * 更新
     */
    @Update
    fun update(taskInfo: DownloadTaskInfo): Int
    /**
     * 删除
     */
    @Delete
    fun delete(taskInfo: DownloadTaskInfo): Int
    /**
     * 删除
     */
    @Query("DELETE FROM task_record WHERE url = :url")
    fun delete(url: String): Int
    /**
     * 查询所有任务
     */
    @Query("SELECT * FROM task_record")
    fun getAll(): List<DownloadTaskInfo>
    /**
     * 通过状态查询任务
     */
    @Query("SELECT * FROM task_record WHERE status IN(:status)")
    fun getAllWithStatus(vararg status: Int): List<DownloadTaskInfo>
    /**
     * 通过id查询任务
     */
    @Query("SELECT * FROM task_record WHERE url = :url")
    fun get(url: String): DownloadTaskInfo?
    /**
     * 通过ids查询任务列表
     */
    @Query("SELECT * FROM task_record WHERE url IN(:url)")
    fun get(vararg url: String): List<DownloadTaskInfo>
    /**
     * 通过id更新 data
     */
    @Query("UPDATE task_record SET data = :data WHERE url = :url")
    fun update(url: String, data: String): Int
}