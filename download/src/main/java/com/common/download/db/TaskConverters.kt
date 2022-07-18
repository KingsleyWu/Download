package com.common.download.db

import androidx.room.TypeConverter
import com.common.download.bean.DownloadTaskInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class TaskConverters {
    private val mGson : Gson by lazy { Gson() }

    @TypeConverter
    fun stringToObject(value: String?): DownloadTaskInfo? {
        return mGson.fromJson(value, DownloadTaskInfo::class.java)
    }

    @TypeConverter
    fun objectToString(info: DownloadTaskInfo?): String {
        return mGson.toJson(info)
    }
}