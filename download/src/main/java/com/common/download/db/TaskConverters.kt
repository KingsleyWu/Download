package com.common.download.db

import androidx.room.TypeConverter
import com.common.download.bean.DownloadTaskInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class TaskConverters {
    private val mGson : Gson by lazy { Gson() }
    private val mType: Type by lazy { object : TypeToken<List<DownloadTaskInfo>>() {}.type }

    @TypeConverter
    fun stringToList(value: String?): List<DownloadTaskInfo>? {
        return mGson.fromJson(value, mType)
    }

    @TypeConverter
    fun listToString(list: List<DownloadTaskInfo>?): String {
        return mGson.toJson(list)
    }

    @TypeConverter
    fun stringToObject(value: String?): DownloadTaskInfo? {
        return mGson.fromJson(value, DownloadTaskInfo::class.java)
    }

    @TypeConverter
    fun objectToString(info: DownloadTaskInfo?): String {
        return mGson.toJson(info)
    }
}