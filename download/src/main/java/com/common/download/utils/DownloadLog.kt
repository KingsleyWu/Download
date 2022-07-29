package com.common.download.utils

import android.util.Log

object DownloadLog {
    private var DEBUG = false
    var TAG = "Download : --> "
        private set

    @JvmStatic
    fun init(debug: Boolean, tag: String) {
        DEBUG = debug
        TAG = tag
    }

    fun d(tag: String, message: String) {
        if (DEBUG) {
            Log.d(tag, message)
        }
    }

    fun d(msg: String) {
        if (DEBUG) {
            Log.d(TAG, msg)
        }
    }

    fun i(msg: String) {
        if (DEBUG) {
            Log.i(TAG, msg)
        }
    }

    fun i(tag: String, message: String) {
        if (DEBUG) {
            Log.i(tag, message)
        }
    }

    fun e(msg: String) {
        if (DEBUG) {
            Log.e(TAG, msg)
        }
    }

    fun e(e: Throwable?) {
        if (DEBUG && e != null) {
            Log.e(TAG, e.message ?: "")
        }
    }

    fun e(tag: String, message: String) {
        if (DEBUG) {
            Log.e(tag, message)
        }
    }
}