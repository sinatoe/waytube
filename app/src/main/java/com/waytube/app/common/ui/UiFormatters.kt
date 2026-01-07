package com.waytube.app.common.ui

import android.icu.text.CompactDecimalFormat
import android.icu.text.RelativeDateTimeFormatter
import android.text.format.DateUtils
import kotlinx.datetime.TimeZone
import kotlinx.datetime.periodUntil
import java.util.Locale
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

fun Duration.toFormattedString(): String = DateUtils.formatElapsedTime(inWholeSeconds)

fun Long.toPluralCount(): Int = if (this >= 1000) 0 else toInt()

fun Long.toCompactString(): String = CompactDecimalFormat
    .getInstance(Locale.getDefault(), CompactDecimalFormat.CompactStyle.SHORT)
    .format(this)

fun Instant.toRelativeTimeString(now: Instant = Clock.System.now()): String {
    val (direction, start, end) = if (this > now) {
        Triple(RelativeDateTimeFormatter.Direction.NEXT, now, this)
    } else {
        Triple(RelativeDateTimeFormatter.Direction.LAST, this, now)
    }

    val period = start.periodUntil(end, TimeZone.currentSystemDefault())

    val (quantity, unit) = sequence {
        yield(period.years to RelativeDateTimeFormatter.RelativeUnit.YEARS)
        yield(period.months to RelativeDateTimeFormatter.RelativeUnit.MONTHS)
        yield(period.days / 7 to RelativeDateTimeFormatter.RelativeUnit.WEEKS)
        yield(period.days to RelativeDateTimeFormatter.RelativeUnit.DAYS)
        yield(period.hours to RelativeDateTimeFormatter.RelativeUnit.HOURS)
        yield(period.minutes to RelativeDateTimeFormatter.RelativeUnit.MINUTES)
        yield(period.seconds to RelativeDateTimeFormatter.RelativeUnit.SECONDS)
    }.first { it.first > 0 || it.second == RelativeDateTimeFormatter.RelativeUnit.SECONDS }

    return RelativeDateTimeFormatter.getInstance().format(quantity.toDouble(), direction, unit)
}
