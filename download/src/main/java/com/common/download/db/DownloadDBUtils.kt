package com.common.download.db

import com.common.download.bean.DownloadGroupTaskInfo
import com.common.download.base.appContext
import com.common.download.bean.DownloadTaskInfo

object DownloadDBUtils {

    const val DB_NAME = "TaskRecord.db"
    const val TASKS_TABLE_NAME = "tasks_record"

    private val database by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        TasksDataBase.getInstance(appContext)
    }

    private val tasksDao: TasksDao by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        database.tasksDao()
    }

    fun insertOrReplaceTasks(taskGroupInfo: DownloadGroupTaskInfo) {
        tasksDao.insert(taskGroupInfo)
    }

    fun deleteTasks(taskGroupInfo: DownloadGroupTaskInfo) {
        tasksDao.delete(taskGroupInfo)
    }

    fun deleteTasks(id: String) {
        tasksDao.delete(id)
    }

    fun getGroupInfo(id: String): DownloadGroupTaskInfo? {
        return tasksDao.get(id)
    }

}