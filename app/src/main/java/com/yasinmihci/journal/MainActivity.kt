package com.yasinmihci.journal

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
        requestNotificationPermission()
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

    /** Sans elle (Android 13+), le réveil ne peut pas sonner. Refus : signalé dans Réglages. */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
                .launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
