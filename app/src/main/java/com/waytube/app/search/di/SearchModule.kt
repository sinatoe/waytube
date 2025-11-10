package com.waytube.app.search.di

import com.waytube.app.search.data.NewPipeSearchRepository
import com.waytube.app.search.domain.SearchRepository
import com.waytube.app.search.ui.SearchViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val searchModule = module {
    singleOf(::NewPipeSearchRepository) bind SearchRepository::class
    viewModelOf(::SearchViewModel)
}
