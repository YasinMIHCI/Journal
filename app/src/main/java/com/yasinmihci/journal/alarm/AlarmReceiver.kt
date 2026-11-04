package com.yasinmihci.journal.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yasinmihci.journal.JournalApplication
import com.yasinmihci.journal.R
import com.yasinmihci.journal.domain.calendar.PARIS
import com.yasinmihci.journal.ui.format.formatTime
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

/** Déclenchement du réveil / du départ, et bouton « Arrêter ». Non exporté. */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as JournalApplication).container
        when (intent.action) {
            ACTION_STOP -> {
                AlarmNotifications.stopWake(context)
                return
            }
            ACTION_WAKE -> {
                val departure = container.alarmScheduler.next.value?.departure
                val now = ZonedDateTime.now(PARIS)
                // Texte : heure de départ si elle est aujourd'hui.
                val text = if (departure != null && departure.toLocalDate() == now.toLocalDate()) {
                    context.getString(R.string.wake_text_departure, formatTime(departure.toLocalTime()))
                } else {
                    context.getString(R.string.wake_text)
                }
                AlarmNotifications.showWake(context, text)
            }
            ACTION_DEPARTURE -> AlarmNotifications.showDeparture(
                context,
                context.getString(R.string.departure_text),
            )
            else -> return
        }
        // Programme l'alarme suivante (déclenchée pile à l'heure : +1 min pour ne pas la reprogrammer).
        val pending = goAsync()
        container.appScope.launch {
            try {
                container.alarmScheduler.reschedule(ZonedDateTime.now(PARIS).plusMinutes(1))
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_WAKE = "com.yasinmihci.journal.action.WAKE"
        const val ACTION_DEPARTURE = "com.yasinmihci.journal.action.DEPARTURE"
        const val ACTION_STOP = "com.yasinmihci.journal.action.STOP_WAKE"
    }
}
