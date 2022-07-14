package com.kingsley.download

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.common.download.DownloadUtils
import com.kingsley.download.ui.base.Dialog
import com.kingsley.download.ui.theme.DownloadTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        DownloadUtils.cancel(1)
        setContent {
            DownloadTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Greeting("Android", this)
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, lifecycleOwner: LifecycleOwner) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    Box(contentAlignment = Alignment.Center) {
        Text(text = "Hello $name!", modifier = Modifier
            .background(Color.Blue)
            .padding(16.dp)
            .clickable {
                showDialog = !showDialog
                DownloadUtils
                    .download(1)
                    .observer(lifecycleOwner) {
                        Log.d("TAG", "Greeting: $it")
                    }
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
        Greeting("Android", LifecycleOwner {
            return@LifecycleOwner object : Lifecycle(){
                override fun addObserver(observer: LifecycleObserver) {

                }

                override fun removeObserver(observer: LifecycleObserver) {

                }

                override fun getCurrentState(): State {
                    return Lifecycle.State.CREATED
                }

            }
        })
    }
}