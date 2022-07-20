package com.common.download.base

import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri

val appContext: Context by lazy { DownloadProvider.context }
val appApplication: Application by lazy { DownloadProvider.app }

@SuppressLint("StaticFieldLeak")
class DownloadProvider : ContentProvider() {

    companion object {
        lateinit var context: Context
        lateinit var app: Application
    }

    override fun onCreate(): Boolean {
        install(context!!)
        return true
    }

    private fun install(context: Context) {
        DownloadProvider.context = context
        val application = context.applicationContext as Application
        app = application
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? = null


    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int = 0

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0

    override fun getType(uri: Uri): String? = null
}