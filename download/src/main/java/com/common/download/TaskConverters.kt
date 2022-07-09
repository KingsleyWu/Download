package com.common.download

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class TaskConverters {
    private val mGson : Gson by lazy { Gson() }
    private val mType: Type by lazy { object : TypeToken<List<TaskInfo>>() {}.type }

    @TypeConverter
    fun stringToObject(value: String): List<TaskInfo> {
        return mGson.fromJson(value, mType)
    }

    @TypeConverter
    fun objectToString(list: List<TaskInfo>): String {
        return mGson.toJson(list)
    }
}