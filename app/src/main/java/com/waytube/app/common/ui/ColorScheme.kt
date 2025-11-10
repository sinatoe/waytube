package com.waytube.app.common.ui

import androidx.compose.ui.graphics.Color
import com.materialkolor.dynamicColorScheme

private val PRIMARY_COLOR = Color(0xFF5B5BD8)

val lightColorScheme = dynamicColorScheme(
    primary = PRIMARY_COLOR,
    isDark = false
)

val darkColorScheme = dynamicColorScheme(
    primary = PRIMARY_COLOR,
    isDark = true
)
