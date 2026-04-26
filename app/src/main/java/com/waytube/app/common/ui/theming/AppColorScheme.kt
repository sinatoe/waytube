package com.waytube.app.common.ui.theming

import androidx.compose.ui.graphics.Color
import com.materialkolor.dynamicColorScheme

private val PRIMARY_COLOR = Color(0xFF5B5BD8)

object AppColorScheme {
    val Light = dynamicColorScheme(
        primary = PRIMARY_COLOR,
        isDark = false
    )

    val Dark = dynamicColorScheme(
        primary = PRIMARY_COLOR,
        isDark = true
    )
}
