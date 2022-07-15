package com.common.download

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class TaskConverters {
    private val mGson : Gson by lazy { Gson() }
    private val mType: Type by lazy { object : TypeToken<List<DownloadInfo>>() {}.type }

    @TypeConverter
    fun stringToList(value: String?): List<DownloadInfo>? {
        return mGson.fromJson(value, mType)
    }

    @TypeConverter
    fun listToString(list: List<DownloadInfo>?): String {
        return mGson.toJson(list)
    }

    @TypeConverter
    fun stringToObject(value: String?): DownloadInfo? {
        return mGson.fromJson(value, DownloadInfo::class.java)
    }

    @TypeConverter
    fun objectToString(info: DownloadInfo?): String {
        return mGson.toJson(info)
    }
}