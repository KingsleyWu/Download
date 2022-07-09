package com.common.download.db

import androidx.room.*
import com.common.download.TasksInfo

@Dao
interface TasksDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(tasksInfo: TasksInfo): Long

    @Update
    fun update(tasksInfo: TasksInfo): Int

    @Update
    fun update(list: List<TasksInfo>): Int

    @Delete
    fun delete(tasksInfo: TasksInfo): Int

    @Query("SELECT * FROM tasks_record")
    fun getAll(): List<TasksInfo>

    @Query("SELECT * FROM tasks_record WHERE status IN(:status)")
    fun getAllWithStatus(vararg status: Int): List<TasksInfo>

    @Query("SELECT * FROM tasks_record LIMIT :size OFFSET :start")
    fun page(start: Int, size: Int): List<TasksInfo>

    @Query("SELECT * FROM tasks_record WHERE status IN(:status) LIMIT :size OFFSET :start")
    fun pageWithStatus(start: Int, size: Int, vararg status: Int): List<TasksInfo>

    @Query("SELECT * FROM tasks_record WHERE id = :id")
    fun get(id: Int): TasksInfo

    @Query("SELECT * FROM tasks_record WHERE id IN(:id)")
    fun get(vararg id: Int): List<TasksInfo>

    @Query("UPDATE tasks_record SET data = :data WHERE id = :id")
    fun update(id: Int, data: String): Int
}