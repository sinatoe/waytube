package com.waytube.app.common.domain

class Page<T>(
    val items: List<T>,
    val next: (suspend () -> FetchResult<Page<T>>)?
)
