package com.waytube.app.common.ui.action

import android.content.Context
import android.content.Intent

fun Context.shareText(text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }

    startActivity(Intent.createChooser(intent, null))
}
