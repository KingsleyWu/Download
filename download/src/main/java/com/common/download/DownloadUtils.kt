package com.common.download

import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Environment
import android.text.TextUtils
import com.common.download.base.NotificationHelper
import com.common.download.base.appContext
import com.common.download.bean.DGBuilder
import com.common.download.bean.DownloadGroupTaskInfo
import com.common.download.bean.DownloadStatus
import com.common.download.bean.DownloadTaskInfo
import com.common.download.core.DownloadTask
import com.common.download.db.DownloadDBUtils
import com.common.download.downloader.RetrofitDownloader
import com.common.download.utils.*
import java.io.File
import java.util.concurrent.ConcurrentHashMap


/** id key */
const val KEY_TASK_ID = "taskId"

/** error key */
const val KEY_ERROR = "error"

/** notify_id key */
const val KEY_NOTIFY_ID = "notify_id"

/** 默認類型 */
const val DEFAULT_TYPE = "default"

object DownloadUtils {

    /**
     * 下載器
     */
    var downloader = RetrofitDownloader

    /**
     * 通知幫助類
     */
    var notificationHelper: NotificationHelper? = DownloadNotificationHelper()

    /**
     * 是否需要 .download 後綴，默認為 true
     */
    @JvmField
    var needDownloadSuffix = true

    /**
     * 最大同時運行的下載數量 1
     */
    @JvmField
    var MAX_TASK = 1

    /**
     * 默認進度更新時間 500毫秒
     */
    @JvmField
    var updateTime = 500L

    /**
     * 下載的路徑
     */
    val downloadFolder: String? by lazy {
        appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.absolutePath
    }

    /**
     * 當前所有的 DownloadTask
     */
    private val taskMap = ConcurrentHashMap<String, DownloadTask>()

    /**
     * 當前運行的 DownloadTask
     */
    private val runningTaskMap = ConcurrentHashMap<String, DownloadTask>()

    /**
     * 等待中的 DownloadTask
     */
    private val pendingTaskMap = ConcurrentHashMap<String, DownloadTask>()

    /**
     * wifi 是否可用
     */
    var sWifiAvailable = NetworkUtils.isWifiAvailable(appContext)
        internal set

    /**
     * 網絡 是否可用
     */
    var sConnectivityAvailable = NetworkUtils.isNetworkAvailable(appContext)
        internal set

    /**
     * 網絡監聽
     */
    private val mConnectivityChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            changeConnectivity()
        }
    }

    /**
     * 網絡監聽
     */
    private val mNetworkChangeCallback = object : ConnectivityManager.NetworkCallback() {

        override fun onAvailable(network: Network) {
            changeConnectivity()
        }

        override fun onLost(network: Network) {
            changeConnectivity()
        }
    }

    init {
        initConnectivityChangeReceiver()
    }

    /**
     * 運行下一個下載
     */
    internal fun launchNext(taskInfo: DownloadGroupTaskInfo) {
        val id = taskInfo.id
        runningTaskMap.remove(id)
        if (runningTaskMap.size < MAX_TASK && pendingTaskMap.size > 0) {
            val pendingIterator = pendingTaskMap.iterator()
            for (entrySet in pendingIterator) {
                val nextTask = entrySet.value
                if (id != entrySet.key && nextTask.groupInfo.status != DownloadStatus.COMPLETED) {
                    if (runningTaskMap[entrySet.key] == null) {
                        runningTaskMap[entrySet.key] = nextTask
                        nextTask.groupInfo.status = DownloadStatus.NONE
                        pendingIterator.remove()
                    }
                    DownloadLog.d("繼續下一個下載， 下一個 Group id = ${nextTask.groupInfo.id}， percent = ${nextTask.groupInfo.progress.percentStr()}")
                    nextTask.start()
                    if (runningTaskMap.size >= MAX_TASK) {
                        DownloadLog.d("到達最多允許下載數據，只允許有${MAX_TASK}個下載，當前下載中的數量為：${runningTaskMap.size}, 等待中的數量為：${pendingTaskMap.size}")
                        break
                    }
                }
            }
        } else {
            DownloadLog.d("只允許有${MAX_TASK}個下載，當前下載中的數量為：${runningTaskMap.size}, 等待中的數量為：${pendingTaskMap.size}")
        }
    }

    /**
     * 删除并取消任务
     */
    private fun removeAndCancel(id: String, needCallback: Boolean = true): DownloadTask? {
        runningTaskMap.remove(id)
        pendingTaskMap.remove(id)
        // 如当前没在运行，则查看 db 里是否有数据，有则直接删除
        val downloadTask = taskMap.remove(id) ?: getTaskFromDb(id)
        downloadTask?.cancel(needCallback)
        return downloadTask
    }

    /**
     * 取消任务
     */
    @JvmStatic
    fun cancel(id: String, needCallback: Boolean = true) {
        val downloadTask = removeAndCancel(id, needCallback)
        DownloadBroadcastUtil.sendBroadcast(DownloadAction.Cancel(downloadTask?.groupInfo))
    }

    /**
     * 取消任务
     */
    @JvmStatic
    fun pause(id: String) {
        taskMap[id]?.let {
            it.pause()
            DownloadBroadcastUtil.sendBroadcast(DownloadAction.Pause(it.groupInfo))
            launchNext(it.groupInfo)
        }
    }

    /**
     * 等待下载,如等待网络链接
     */
    internal fun waiting(id: String) {
        taskMap[id]?.let {
            it.update(DownloadStatus.WAITING)
            DownloadBroadcastUtil.sendBroadcast(DownloadAction.Waiting(it.groupInfo))
        }
    }

    internal fun pending(id: String) {
        taskMap[id]?.let {
            it.pending()
            DownloadBroadcastUtil.sendBroadcast(DownloadAction.Waiting(it.groupInfo))
            launchNext(it.groupInfo)
        }
    }

    /**
     * 下載任务
     */
    @JvmStatic
    fun download(id: String) {
        getTaskFromMapOrDb(id)?.download()
    }

    /**
     * 下載
     * 如需下需要通過 request() 方法獲取 DownloadTask，然後通過 DownloadTask.download() 進行下載
     */
    internal fun download(task: DownloadTask) {
        var downloadTask = runningTaskMap[task.groupInfo.id]
        if (downloadTask != null) {
            if (downloadTask.status() != DownloadStatus.DOWNLOADING) {
                DownloadLog.d("繼續下載， 當前 Group id = ${task.groupInfo.id}， percent = ${task.groupInfo.progress.percentStr()}")
                downloadTask.start()
            }
        } else {
            if (task.groupInfo.status != DownloadStatus.COMPLETED) {
                downloadTask = task
                if (runningTaskMap.size < MAX_TASK) {
                    runningTaskMap[task.groupInfo.id] = downloadTask
                    if (downloadTask.status() != DownloadStatus.DOWNLOADING) {
                        DownloadLog.d("加入到下載， 當前 Group id = ${task.groupInfo.id}， percent = ${task.groupInfo.progress.percentStr()}")
                        downloadTask.start()
                    }
                } else {
                    DownloadLog.d("加入到等待隊列， 當前 Group id = ${task.groupInfo.id}， percent = ${task.groupInfo.progress.percentStr()}")
                    if (task.groupInfo.status != DownloadStatus.PENDING) {
                        downloadTask.pending()
                    }
                    pendingTaskMap[task.groupInfo.id] = downloadTask
                }
            } else {
                DownloadLog.d("已經下載完成了， 當前 Group 狀態 status : ${task.groupInfo.status}， id = ${task.groupInfo.id}， percent = ${task.groupInfo.progress.percentStr()}")
                task.update()
            }
        }
    }

    /**
     * 通過 groupInfo 獲取 DownloadTask，當沒有獲取到時會進行創建，
     * 如沒有則創建並加入 db
     */
    @JvmStatic
    fun request(builder: DGBuilder): DownloadTask {
        return request(builder.build())
    }

    /**
     * 通過 groupInfo 獲取 DownloadTask，當沒有獲取到時會進行創建，
     * 如沒有則創建並加入 db
     */
    @JvmStatic
    fun request(groupInfo: DownloadGroupTaskInfo): DownloadTask {
        return getTaskFromMapOrDb(groupInfo.id) ?: createDownloadTask(groupInfo)
    }

    /**
     * 通過單個 url 獲取 DownloadTask，當沒有獲取到時會進行創建，
     * 如沒有則創建並加入 db
     */
    @JvmStatic
    fun request(
        url: String,
        type: String = DEFAULT_TYPE,
        action: String? = "",
        title: String = "",
        childType: String = "",
        flag: String = "",
        path: String? = null,
        fileName: String? = null
    ): DownloadTask {
        val id = buildId(url, type)
        return request(id, DGBuilder().id(id).addChild(
                DownloadTaskInfo(url, id, 0, action, title, childType, flag, path, fileName)
            )
        )
    }

    /**
     * 通過 id 獲取 DownloadTask
     * 如沒有則創建並加入 db
     */
    private fun request(id: String, builder: DGBuilder): DownloadTask {
        return getTaskFromMapOrDb(id) ?: createDownloadTask(builder.build())
    }

    /**
     * 創建並加入 db
     */
    private fun createDownloadTask(groupInfo: DownloadGroupTaskInfo): DownloadTask {
        return DownloadTask(groupInfo).also {
            taskMap[groupInfo.id] = it
            // 防止之前文件存在，初始化前进行设置 currentLength
            groupInfo.tasks.forEach { info ->
                if (!TextUtils.isEmpty(info.path)) {
                    val filePath = "${info.path!!}${if (needDownloadSuffix) ".download" else ""}"
                    val tempFile = File(filePath)
                    if (tempFile.exists()) {
                        info.currentLength = tempFile.length()
                    }
                }
            }
            DownloadDBUtils.insertOrReplaceTasks(groupInfo)
        }
    }

    /**
     * 通過 id 獲取 DownloadTask
     * tips： id 可以通過 buildId(urls, type) 或 buildId(url, type) 獲取
     */
    @JvmStatic
    fun requestById(id: String): DownloadTask? {
        return getTaskFromMapOrDb(id)
    }

    /**
     * 從 taskMap 中獲取或 db 中獲取
     */
    private fun getTaskFromMapOrDb(id: String): DownloadTask? {
        return taskMap[id] ?: getTaskFromDb(id)
    }

    /**
     * 從 db 中獲取
     */
    private fun getTaskFromDb(id: String): DownloadTask? {
        return DownloadDBUtils.getGroupInfo(id)?.let {
            return createDownloadTask(it)
        }
    }

    /**
     * 通過 urls，type 獲取 unitId
     */
    @JvmStatic
    fun buildId(urls: List<String>, type: String = DEFAULT_TYPE): String {
        val id = "${urls.sumOfUrl { it }.hashCode()}_$type"
        DownloadLog.d("buildId id : $id， urls = $urls， type = $type")
        return id
    }

    /**
     * 通過 url，type 獲取 unitId
     */
    @JvmStatic
    fun buildId(url: String, type: String = DEFAULT_TYPE): String {
        val id = "${url.hashCode()}_$type"
        DownloadLog.d("buildId id : $id， url = $url， type = $type")
        return id
    }

    /**
     * 拼接 url
     */
    private fun Iterable<String>.sumOfUrl(selector: (String) -> String): String {
        val sum = StringBuilder()
        for (element in this) {
            sum.append(selector(element))
        }
        return sum.toString()
    }

    /* 網絡監聽 start */

    @Suppress("DEPRECATION")
    @SuppressLint("ObsoleteSdkInt")
    private fun initConnectivityChangeReceiver() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val connectivityManager =
                appContext.getSystemService(Service.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager?.registerDefaultNetworkCallback(mNetworkChangeCallback)
            } else {
                val builder = NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .addTransportType(NetworkCapabilities.TRANSPORT_BLUETOOTH)
                    .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
                    .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
                connectivityManager?.registerNetworkCallback(
                    builder.build(),
                    mNetworkChangeCallback
                )
            }
        } else {
            // 注册网络变化监听
            appContext.registerReceiver(
                mConnectivityChangeReceiver,
                IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun changeConnectivity() {
        try {
            val cm =
                appContext.getSystemService(Service.CONNECTIVITY_SERVICE) as? ConnectivityManager
                    ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val activeNetwork = cm.activeNetwork
                if (activeNetwork != null) {
                    sConnectivityAvailable = true
                    sWifiAvailable = cm.getNetworkCapabilities(activeNetwork)
                        ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        ?: false
                } else {
                    sWifiAvailable = false
                    sConnectivityAvailable = false
                }
            } else if (cm.activeNetworkInfo != null) {
                sConnectivityAvailable = true
                val networkType = cm.activeNetworkInfo!!.type
                //wifi active
                sWifiAvailable = networkType == ConnectivityManager.TYPE_WIFI
            } else {
                sConnectivityAvailable = false
                sWifiAvailable = false
            }
        } catch (e: Exception) {
            DownloadLog.e(e)
        } finally {
            performNetworkAction()
        }
    }

    private fun performNetworkAction() {
        DownloadLog.e("performNetworkAction sConnectivityAvailable = $sConnectivityAvailable, sWifiAvailable = $sWifiAvailable ")
        if (runningTaskMap.isNotEmpty()) {
            for (groupTask in runningTaskMap.values) {
                if (groupTask.status() != DownloadStatus.COMPLETED && groupTask.status() != DownloadStatus.PAUSED) {
                    groupTask.performNetworkAction()
                }
            }
        }
    }

    /* 網絡監聽 end */
}
