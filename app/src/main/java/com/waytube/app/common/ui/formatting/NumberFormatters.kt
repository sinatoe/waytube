package com.waytube.app.common.ui.formatting

import android.icu.text.CompactDecimalFormat
import java.util.Locale

fun Long.toPluralCount(): Int = if (this >= 1000) 0 else toInt()

fun Long.toCompactString(): String = CompactDecimalFormat
    .getInstance(Locale.ENGLISH, CompactDecimalFormat.CompactStyle.SHORT)
    .format(this)
