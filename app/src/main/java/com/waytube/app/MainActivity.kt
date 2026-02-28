package com.waytube.app

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.waytube.app.common.ui.AppTheme
import com.waytube.app.navigation.ui.NavigationHost
import com.waytube.app.navigation.ui.NavigationViewModel
import com.waytube.app.video.ui.VideoViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val navigationViewModel by viewModel<NavigationViewModel>()
    private val videoViewModel by viewModel<VideoViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            navigationViewModel.provideIntent(intent)
        }

        enableEdgeToEdge()
        setContent {
            AppTheme {
                NavigationHost(
                    viewModel = navigationViewModel,
                    videoViewModel = videoViewModel,
                    onSetVideoImmersiveMode = ::setVideoImmersiveMode
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        navigationViewModel.provideIntent(intent)
    }

    private fun setVideoImmersiveMode(enabled: Boolean) {
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (enabled) {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
}
