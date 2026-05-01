package com.waytube.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.waytube.app.common.ui.theming.AppTheme
import com.waytube.app.navigation.ui.NavigationHost
import com.waytube.app.navigation.ui.NavigationViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val navigationViewModel by viewModel<NavigationViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            navigationViewModel.provideIntent(intent)
        }

        enableEdgeToEdge()
        setContent {
            AppTheme {
                NavigationHost(
                    viewModel = navigationViewModel
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        navigationViewModel.provideIntent(intent)
    }
}
