package com.waytube.app.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner

@Composable
fun rememberNavigationBackAction(): () -> Unit {
    val navigationEventDispatcher =
        LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher

    val navigationEventInput = remember { DirectNavigationEventInput() }

    DisposableEffect(Unit) {
        navigationEventDispatcher?.addInput(navigationEventInput)

        onDispose { navigationEventDispatcher?.removeInput(navigationEventInput) }
    }

    return navigationEventInput::backCompleted
}
