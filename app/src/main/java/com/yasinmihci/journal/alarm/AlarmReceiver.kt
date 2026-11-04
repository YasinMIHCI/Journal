package com.yasinmihci.journal.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yasinmihci.journal.JournalApplication
import com.yasinmihci.journal.R
import com.yasinmihci.journal.domain.calendar.PARIS
import com.yasinmihci.journal.data.local.entity.HabitEntity
import com.yasinmihci.journal.di.AppContainer
import com.yasinmihci.journal.domain.activity.pickActivity
import com.yasinmihci.journal.domain.activity.remainingMinutes
import com.yasinmihci.journal.ui.format.formatDuration
import com.yasinmihci.journal.ui.format.formatTime
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
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
            ACTION_SLOT -> {
                val endMs = intent.getLongExtra(EXTRA_SLOT_END_MS, 0L)
                val pending = goAsync()
                container.appScope.launch {
                    try {
                        notifySlot(context, container, endMs)
                        container.alarmScheduler.reschedule(ZonedDateTime.now(PARIS).plusMinutes(1))
                    } finally {
                        pending.finish()
                    }
                }
                return
            }
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

    /** Propose l'activité à qui il reste le plus de temps cette semaine ; rien si tout est fait. */
    private suspend fun notifySlot(context: Context, container: AppContainer, endMs: Long) {
        val today = LocalDate.now(PARIS)
        val totals = container.sessionRepository.weekTotals(today)
        val habits = container.habitRepository.active()
        fun remaining(h: HabitEntity) = remainingMinutes(h.weeklyGoalMinutes, totals[h.id] ?: 0L)
        val habit = pickActivity(habits, ::remaining) ?: return
        val end = if (endMs > 0) formatTime(Instant.ofEpochMilli(endMs).atZone(PARIS).toLocalTime()) else ""
        AlarmNotifications.showSlot(
            context,
            habitId = habit.id,
            title = context.getString(R.string.slot_title, end),
            text = context.getString(R.string.slot_text, habit.name, formatDuration(remaining(habit))),
        )
    }

    companion object {
        const val ACTION_SLOT = "com.yasinmihci.journal.action.SLOT"
        const val EXTRA_SLOT_END_MS = "slot_end_ms"
        const val ACTION_WAKE = "com.yasinmihci.journal.action.WAKE"
        const val ACTION_DEPARTURE = "com.yasinmihci.journal.action.DEPARTURE"
        const val ACTION_STOP = "com.yasinmihci.journal.action.STOP_WAKE"
    }
}
