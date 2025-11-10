package com.waytube.app.common.ui

import android.icu.text.CompactDecimalFormat
import android.text.format.DateUtils
import java.util.Locale
import kotlin.time.Duration

fun Duration.toFormattedString(): String = DateUtils.formatElapsedTime(inWholeSeconds)

fun Long.toPluralCount(): Int = coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

fun Long.toCompactString(): String = CompactDecimalFormat
    .getInstance(Locale.getDefault(), CompactDecimalFormat.CompactStyle.SHORT)
    .format(this)
