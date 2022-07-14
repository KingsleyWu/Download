package com.common.download.db

import androidx.room.*
import com.common.download.TaskInfo

/**
 * 下载信息的 db Dao
 */
@Dao
interface TasksDao {
    /**
     * 插入或替换
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(taskInfo: TaskInfo): Long
    /**
     * 更新
     */
    @Update
    fun update(taskInfo: TaskInfo): Int
    /**
     * 删除
     */
    @Delete
    fun delete(taskInfo: TaskInfo): Int
    /**
     * 删除
     */
    @Query("DELETE FROM task_record WHERE id = :id")
    fun delete(id: Long): Int
    /**
     * 查询所有任务
     */
    @Query("SELECT * FROM task_record")
    fun getAll(): List<TaskInfo>
    /**
     * 通过状态查询任务
     */
    @Query("SELECT * FROM task_record WHERE status IN(:status)")
    fun getAllWithStatus(vararg status: Int): List<TaskInfo>
    /**
     * 通过id查询任务
     */
    @Query("SELECT * FROM task_record WHERE id = :id")
    fun get(id: Long): TaskInfo?
    /**
     * 通过ids查询任务列表
     */
    @Query("SELECT * FROM task_record WHERE id IN(:id)")
    fun get(vararg id: Long): List<TaskInfo>
    /**
     * 通过id更新 data
     */
    @Query("UPDATE task_record SET data = :data WHERE id = :id")
    fun update(id: Long, data: String): Int
}