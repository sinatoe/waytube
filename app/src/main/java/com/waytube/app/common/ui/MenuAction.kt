package com.waytube.app.common.ui

import androidx.compose.ui.graphics.painter.Painter

data class MenuAction(
    val label: String,
    val iconPainter: Painter,
    val onClick: () -> Unit
)
