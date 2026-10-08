package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.NovaBrowserScreen
import com.example.ui.theme.NovaBrowserTheme
import com.example.viewmodel.NovaBrowserViewModel
import com.example.viewmodel.NovaBrowserViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val factory = NovaBrowserViewModelFactory(applicationContext)
            val viewModel: NovaBrowserViewModel = viewModel(factory = factory)

            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val tabs by viewModel.tabs.collectAsStateWithLifecycle()
            val activeTabId by viewModel.activeTabId.collectAsStateWithLifecycle()
            val activeTab = tabs.find { it.id == activeTabId }

            NovaBrowserTheme(
                themeMode = settings.themeMode,
                accentColor = settings.accentColor,
                isIncognito = activeTab?.isIncognito == true
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NovaBrowserScreen(viewModel = viewModel)
                }
            }
        }
    }
}
