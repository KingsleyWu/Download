@file:Suppress("DEPRECATION")

package com.common.download.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build.VERSION

internal object NetworkUtils {

    fun isWifiAvailable(context: Context): Boolean {
        val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connManager != null) {
            if (VERSION.SDK_INT < 23) {
                return connManager.activeNetworkInfo?.type == 1
            }
            val activeNetwork = connManager.activeNetwork
            if (activeNetwork != null) {
                val networkCapabilities = connManager.getNetworkCapabilities(activeNetwork)
                if (networkCapabilities != null) {
                    return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                }
            }
        }
        return false
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        return if (connectivity == null) {
            false
        } else if (VERSION.SDK_INT >= 23) {
            val activeNetwork = connectivity.activeNetwork
            activeNetwork != null && connectivity.getNetworkCapabilities(activeNetwork) != null
        } else {
            connectivity.activeNetworkInfo?.state == NetworkInfo.State.CONNECTED
        }
    }
}