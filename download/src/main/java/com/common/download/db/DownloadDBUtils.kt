package com.common.download.db

import com.common.download.TaskInfo
import com.common.download.appContext

object DownloadDBUtils {

    const val DB_NAME = "TaskRecord.db"
    const val TASK_TABLE_NAME = "task_record"

    private val database by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        TasksDataBase.getInstance(appContext)
    }

    private val tasksDao: TasksDao by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        database.tasksDao()
    }

    fun insertOrReplace(taskInfo: TaskInfo) {
        tasksDao.insert(taskInfo)
    }

    fun delete(taskInfo: TaskInfo) {
        tasksDao.delete(taskInfo)
    }

    fun delete(id: Long) {
        tasksDao.delete(id)
    }

    fun get(id: Long) : TaskInfo? {
       return tasksDao.get(id)
    }


}