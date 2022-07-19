package com.kingsley.download

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import coil.compose.rememberAsyncImagePainter
import com.common.download.DownloadUtils
import com.common.download.bean.DGBuilder
import com.common.download.bean.GTBuilder
import com.common.download.utils.DownloadLog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kingsley.download.ui.base.Dialog
import com.kingsley.download.ui.theme.DownloadTheme

class MainActivity : ComponentActivity() {

    var sameUrls = mutableListOf<String>().apply {
        add("https://imtt.dd.qq.com/16891/apk/BC1986D2E9B28DFB56761AC526609026.apk")
        add("https://imtt.dd.qq.com/16891/apk/BC1986D2E9B28DFB56761AC526609026.apk")
        add("https://imtt.dd.qq.com/16891/apk/BC1986D2E9B28DFB56761AC526609026.apk")
    }

    var urls = mutableListOf<String>().apply {
        add("https://ab253dfb3b9c9f7e2580baeb3aa0c165.dd.cdntips.com/imtt.dd.qq.com/16891/apk/B20AD9A014A9CCA09DBAD4EA18A56FD9.apk")
        add("https://imtt.dd.qq.com/16891/apk/BC1986D2E9B28DFB56761AC526609026.apk")
    }
    val sameUrlsUnitId = DownloadUtils.buildId(sameUrls)
    val sameUrlsGroupInfo = DGBuilder()
        .id(sameUrlsUnitId)
        .addAll {
            sameUrls.map {
                GTBuilder()
                    .groupId(sameUrlsUnitId)
                    .url(it)
                    .build()
            }
        }.build()

    val urlsUnitId = DownloadUtils.buildId(urls)
    val urlsGroupInfo = DGBuilder()
        .id(urlsUnitId)
        .addAll {
            urls.map {
                GTBuilder()
                    .groupId(urlsUnitId)
                    .url(it)
                    .build()
            }
        }.build()
    val type = object : TypeToken<List<DemoListItem>>() {}.type
    val mockData = Gson().fromJson<List<DemoListItem>>(mock_json, type)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        DownloadLog.init(true, "下載")
//        DownloadUtils.cancel(DownloadUtils.buildUnitId(urls))
        setContent {
            DownloadTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    LazyColumn{
                        items(mockData) {
                            var progress by remember(it.url) {
                                mutableStateOf(DownloadUtils.request(it.url).groupInfo.progress.percentStr())
                            }
                            DownloadItem(it, progress){ item ->
                                val downloadTask = DownloadUtils.request(item.url)
                                    .observer(this@MainActivity) { info ->
                                        Log.d("TAG", "mockData: $info")
                                        progress = info.progress.percentStr()
                                    }
                                if (downloadTask.downloading()) {
                                    DownloadUtils.pause(downloadTask.groupInfo.id)
                                } else {
                                    downloadTask.download()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

class DemoListItem(
    val name: String,
    val icon: String,
    val url: String,
    val size: String
) {
    var progress: String? = ""
}

@Composable
fun DownloadItem(item: DemoListItem, progress: String, click: (DemoListItem) -> Unit = {}) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 5.dp)
                .clickable {
                    click(item)
                }
        ) {
            Icon(
                painter = rememberAsyncImagePainter(model = item.icon),
                contentDescription = "",
                Modifier.size(56.dp)
            )
            Text(text = item.name)
        }
        Text(text = progress)
    }
}

@Composable
fun Greeting(name: String, click: () -> Unit = {}) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    Box(contentAlignment = Alignment.Center) {
        Text(text = "Hello $name!", modifier = Modifier
            .background(Color.Blue)
            .padding(16.dp)
            .clickable {
                showDialog = !showDialog
                click()
            })
    }
    if (showDialog) {
        val context = LocalContext.current
        Dialog(
            title = {
                Text(
                    text = "我是標題",
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
                )
            },
            content = {
                var value by rememberSaveable { mutableStateOf("") }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp)
                ) {
                    Text(
                        text = "我是內容我是內容我是內容"
                    )
                    BasicTextField(
                        value = value,
                        onValueChange = {
                            value = it
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    )
                }
            },
            line = {
                Divider(color = Color.Gray, thickness = Dp.Hairline)
            },
            bottom = {
                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                ) {
                    Button(
                        onClick = {
                            showDialog = !showDialog
                            Toast.makeText(context, "取消", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text(text = "取消")
                    }
                    Spacer(modifier = Modifier.size(16.dp))
                    Button(
                        onClick = {
                            showDialog = !showDialog
                            Toast.makeText(context, "確定", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text(text = "確定")
                    }
                }
            },
        ) {
            showDialog = !showDialog
            Toast.makeText(context, "dismiss", Toast.LENGTH_SHORT).show()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    DownloadTheme {
        Greeting("Android")
    }
}