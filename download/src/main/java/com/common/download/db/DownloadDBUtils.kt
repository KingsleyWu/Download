package com.common.download.db

import com.common.download.bean.DownloadTaskGroupInfo
import com.common.download.base.appContext
import com.common.download.bean.DownloadTaskInfo
import com.common.download.bean.DownloadGroupInfo

object DownloadDBUtils {

    const val DB_NAME = "TaskRecord.db"
    const val TASKS_TABLE_NAME = "tasks_record"
    const val TASK_TABLE_NAME = "task_record"

    private val database by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        TasksDataBase.getInstance(appContext)
    }

    private val tasksDao: TasksDao by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        database.tasksDao()
    }

    private val taskDao: TaskDao by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        database.taskDao()
    }

    private val groupDao: GroupTaskDao by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        database.groupDao()
    }

    fun insertOrReplaceTasks(taskGroupInfo: DownloadTaskGroupInfo) {
        tasksDao.insert(taskGroupInfo)
    }

    fun deleteTasks(taskGroupInfo: DownloadTaskGroupInfo) {
        tasksDao.delete(taskGroupInfo)
    }

    fun deleteTasks(id: Long) {
        tasksDao.delete(id)
    }

    fun get(id: Long): DownloadGroupInfo? {
        return groupDao.get(id)
    }

    fun insertOrReplaceTask(info: DownloadTaskInfo) {
        taskDao.insert(info)
    }

    fun deleteTask(info: DownloadTaskInfo) {
        taskDao.delete(info)
    }

    fun deleteTask(url: String) {
        taskDao.delete(url)
    }

    fun get(url: String): DownloadTaskInfo? {
        return taskDao.get(url)
    }


}