package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.DayPinViewModel
import com.example.ui.navigation.DayPinApp
import com.example.ui.theme.DayPinTheme

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_OPEN_DESK_BOARD = "extra_open_desk_board"
    }

    private var openDeskFromIntent by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        openDeskFromIntent = intent?.getBooleanExtra(EXTRA_OPEN_DESK_BOARD, false) ?: false

        setContent {
            val viewModel: DayPinViewModel = viewModel()
            val preferences by viewModel.preferences.collectAsStateWithLifecycle()
            DayPinTheme(themeMode = preferences.themeMode) {
                DayPinApp(
                    viewModel = viewModel,
                    openDeskFromIntent = openDeskFromIntent,
                    onConsumeDeskIntent = { openDeskFromIntent = false }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_DESK_BOARD, false)) {
            openDeskFromIntent = true
        }
    }
}
