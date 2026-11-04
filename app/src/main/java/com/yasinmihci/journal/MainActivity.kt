package com.yasinmihci.journal

import android.Manifest
import android.content.Intent
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.yasinmihci.journal.ui.habits.HabitsScreen
import com.yasinmihci.journal.ui.session.SessionScreen
import com.yasinmihci.journal.ui.settings.SettingsScreen
import com.yasinmihci.journal.ui.theme.JournalTheme
import com.yasinmihci.journal.ui.today.TodayScreen

private enum class Screen { Today, Settings, Habits, Session }

class MainActivity : ComponentActivity() {
    /** Activité demandée par la notification de créneau (0 = aucune). */
    private val pendingSession = mutableLongStateOf(0L)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readSessionExtra(intent)
    }

    private fun readSessionExtra(intent: Intent?) {
        val id = intent?.getLongExtra(EXTRA_SESSION_HABIT_ID, 0L) ?: 0L
        if (id != 0L) {
            pendingSession.longValue = id
            intent?.removeExtra(EXTRA_SESSION_HABIT_ID)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermission()
        if (savedInstanceState == null) readSessionExtra(intent)
        setContent {
            JournalTheme {
                // Navigation minimale : deux écrans, pas besoin de navigation-compose pour l'instant.
                var screen by rememberSaveable { mutableStateOf(Screen.Today) }
                var sessionHabitId by rememberSaveable { mutableLongStateOf(0L) }
                val requested = pendingSession.longValue
                if (requested != 0L) {
                    sessionHabitId = requested
                    screen = Screen.Session
                    pendingSession.longValue = 0L
                }
                val home = { screen = Screen.Today }
                when (screen) {
                    Screen.Today -> TodayScreen(
                        onOpenSettings = { screen = Screen.Settings },
                        onOpenHabits = { screen = Screen.Habits },
                        onStartSession = { id -> sessionHabitId = id; screen = Screen.Session },
                    )
                    Screen.Settings -> {
                        BackHandler(onBack = home)
                        SettingsScreen(onBack = home)
                    }
                    Screen.Habits -> {
                        BackHandler(onBack = home)
                        HabitsScreen(onBack = home)
                    }
                    // Le retour est géré par l'écran (confirmation avant d'abandonner).
                    Screen.Session -> SessionScreen(habitId = sessionHabitId, onDone = home)
                }
            }
        }
    }

    companion object {
        const val EXTRA_SESSION_HABIT_ID = "session_habit_id"
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
