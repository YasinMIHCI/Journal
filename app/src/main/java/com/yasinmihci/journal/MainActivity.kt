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
import com.yasinmihci.journal.ui.week.TagsScreen
import com.yasinmihci.journal.ui.week.WeekScreen
import java.time.LocalDate

private enum class Screen { Week, Today, Settings, Tags, Habits, Session }

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
                // Pile de navigation minimale ; la semaine est l'accueil.
                var stack by rememberSaveable { mutableStateOf(listOf(Screen.Week.name)) }
                var sessionHabitId by rememberSaveable { mutableLongStateOf(0L) }
                var dayEpoch by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
                fun push(screen: Screen) { stack = stack + screen.name }
                fun pop() { if (stack.size > 1) stack = stack.dropLast(1) }
                val requested = pendingSession.longValue
                if (requested != 0L) {
                    sessionHabitId = requested
                    stack = listOf(Screen.Week.name, Screen.Session.name)
                    pendingSession.longValue = 0L
                }
                val screen = Screen.valueOf(stack.last())
                // La session gère son retour (confirmation avant d'abandonner).
                BackHandler(enabled = stack.size > 1 && screen != Screen.Session) { pop() }
                when (screen) {
                    Screen.Week -> WeekScreen(
                        onOpenDay = { day -> dayEpoch = day.toEpochDay(); push(Screen.Today) },
                        onOpenSettings = { push(Screen.Settings) },
                    )
                    Screen.Today -> TodayScreen(
                        openDate = LocalDate.ofEpochDay(dayEpoch),
                        onOpenWeek = { pop() },
                        onOpenHabits = { push(Screen.Habits) },
                        onStartSession = { id -> sessionHabitId = id; push(Screen.Session) },
                    )
                    Screen.Settings -> SettingsScreen(onBack = { pop() }, onOpenTags = { push(Screen.Tags) })
                    Screen.Tags -> TagsScreen(onBack = { pop() })
                    Screen.Habits -> HabitsScreen(onBack = { pop() })
                    Screen.Session -> SessionScreen(habitId = sessionHabitId, onDone = { pop() })
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
