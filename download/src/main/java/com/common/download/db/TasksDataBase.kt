package com.common.download.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.common.download.bean.DownloadTaskInfo
import com.common.download.bean.DownloadStatus.DOWNLOADING
import com.common.download.bean.DownloadStatus.PAUSED
import com.common.download.bean.DownloadStatus.STARTED
import com.common.download.bean.DownloadTaskGroupInfo
import com.common.download.db.DownloadDBUtils.DB_NAME

@Database(entities = [DownloadTaskInfo::class, DownloadTaskGroupInfo::class], version = 1, exportSchema = false)
@TypeConverters(TaskConverters::class)
abstract class TasksDataBase : RoomDatabase() {

    abstract fun tasksDao(): TasksDao

    abstract fun taskDao(): TaskDao

    abstract fun groupDao(): GroupTaskDao

    companion object {
        @Volatile
        private var INSTANCE: TasksDataBase? = null

        fun getInstance(context: Context): TasksDataBase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context.applicationContext, TasksDataBase::class.java, DB_NAME)
                .allowMainThreadQueries()
                .addCallback(callback).build()

        private val callback = object : RoomDatabase.Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                //fix abnormal exit state
                fixAbnormalState(db)
            }
        }
    }
}

internal fun fixAbnormalState(db: SupportSQLiteDatabase) {
    db.beginTransaction()
    try {
        db.execSQL("""UPDATE ${DownloadDBUtils.TASKS_TABLE_NAME} SET status = $PAUSED, abnormalExit = "1" WHERE status = $STARTED""")
        db.execSQL("""UPDATE ${DownloadDBUtils.TASKS_TABLE_NAME} SET status = $PAUSED, abnormalExit = "1" WHERE status = $DOWNLOADING""")
        db.setTransactionSuccessful()
    } finally {
        db.endTransaction()
    }
}