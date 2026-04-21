package com.waytube.app.common.domain

data class Page<T>(
    val items: List<T>,
    val next: (suspend () -> Result<Page<T>>)?
)
