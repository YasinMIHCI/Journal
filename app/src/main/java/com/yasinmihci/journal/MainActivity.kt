package com.yasinmihci.journal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.yasinmihci.journal.ui.settings.SettingsScreen
import com.yasinmihci.journal.ui.theme.JournalTheme
import com.yasinmihci.journal.ui.today.TodayScreen

private enum class Screen { Today, Settings }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JournalTheme {
                // Navigation minimale : deux écrans, pas besoin de navigation-compose pour l'instant.
                var screen by rememberSaveable { androidx.compose.runtime.mutableStateOf(Screen.Today) }
                when (screen) {
                    Screen.Today -> TodayScreen(onOpenSettings = { screen = Screen.Settings })
                    Screen.Settings -> {
                        BackHandler { screen = Screen.Today }
                        SettingsScreen(onBack = { screen = Screen.Today })
                    }
                }
            }
        }
    }
}
