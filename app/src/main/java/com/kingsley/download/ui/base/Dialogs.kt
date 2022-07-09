package com.kingsley.download.ui.base

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun BaseDialog(
    properties : DialogProperties = DialogProperties(),
    title: String? = null,
    leftText: String? = null,
    onLeft: () -> Unit = {},
    rightText: String? = null,
    onRight: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
    onDismiss: () -> Unit
) = BaseDialog(
    properties = properties,
    top = {
        if (title != null) {
            Text(text = title)
        }
    },
    content = {
        Spacer(modifier = Modifier.size(16.dp))
        content()
    },
    bottom = {
        if (!leftText.isNullOrBlank() || !rightText.isNullOrBlank()) {
            Divider(thickness = Dp.Hairline)
            Row(modifier = Modifier.align(Alignment.End)) {
                if (!leftText.isNullOrBlank()) {
                    Button(
                        onClick = { onLeft() }
                    ) {
                        Text(text = leftText)
                    }
                }
                if (!rightText.isNullOrBlank()) {
                    Button(
                        onClick = { onRight() }
                    ) {
                        Text(text = rightText)
                    }
                }
            }
        }
    },
    onDismiss = onDismiss
)

@Composable
fun Dialog(
    properties : DialogProperties = DialogProperties(),
    title: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
    line: @Composable ColumnScope.() -> Unit = {
        Divider(thickness = Dp.Hairline)
    },
    bottom: @Composable ColumnScope.() -> Unit = {},
    onDismiss: () -> Unit
) {
    BaseDialog(
        properties = properties,
        top = title,
        content = {
            Spacer(modifier = Modifier.size(16.dp))
            content()
            Spacer(modifier = Modifier.size(16.dp))
            line()
        },
        bottom = bottom,
        onDismiss = onDismiss
    )
}

@Composable
fun BaseDialog(
    properties : DialogProperties = DialogProperties(),
    top: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
    bottom: @Composable ColumnScope.() -> Unit = {},
    onDismiss: () -> Unit,
) = Dialog(
    properties = properties,
    onDismissRequest = onDismiss
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.surface, shape = RoundedCornerShape(8.dp))
    ) {
        top()
        content()
        bottom()
    }
}

@Preview(showBackground = true)
@Composable
fun BaseDialogPreview() {
    BaseDialog(
        top = {
            Text(text = "我是標題")
            Spacer(modifier = Modifier.size(16.dp))
        },
        content = {
            Text(text = "我是內容我是內容我是內容我是內容我是內容我是內容我是內容我是內容我是內容我是內容我是內容我是內容我是內容我是內容我是內容我是內容我是內容我是內容我是內容我是內容我是內容")
            Spacer(modifier = Modifier.size(16.dp))
        },
        bottom = {
            Divider(color = Color.Gray, thickness = Dp.Hairline)
            Row(modifier = Modifier.align(Alignment.End)) {
                Button(
                    onClick = {

                    }
                ) {
                    Text(text = "取消")
                }
                Spacer(modifier = Modifier.size(16.dp))
                Button(
                    onClick = {

                    }
                ) {
                    Text(text = "確定")
                }
            }
        },
    ) {

    }
}