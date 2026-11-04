package com.yasinmihci.journal.alarm

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yasinmihci.journal.R
import com.yasinmihci.journal.domain.calendar.PARIS
import com.yasinmihci.journal.ui.format.formatTime
import com.yasinmihci.journal.ui.theme.JournalTheme
import java.time.LocalTime

/** Écran de sonnerie, affiché par-dessus l'écran verrouillé. Le son vient de la notification. */
class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val text = intent.getStringExtra(EXTRA_TEXT).orEmpty()
        setContent {
            JournalTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(formatTime(LocalTime.now(PARIS)), style = MaterialTheme.typography.displayLarge)
                        Text(text, style = MaterialTheme.typography.titleLarge)
                        Button(onClick = ::stop) {
                            Text(stringResource(R.string.wake_stop), style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
        }
    }

    private fun stop() {
        AlarmNotifications.stopWake(this)
        finish()
    }

    companion object {
        const val EXTRA_TEXT = "text"
    }
}
