package com.yasinmihci.journal.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yasinmihci.journal.JournalApplication
import kotlinx.coroutines.launch

/**
 * Les alarmes AlarmManager sont effacées au redémarrage, et décalées par un
 * changement d'heure / de fuseau : on reprogramme.
 */
class SystemEventsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED) return
        val container = (context.applicationContext as JournalApplication).container
        val pending = goAsync()
        container.appScope.launch {
            try {
                container.alarmScheduler.reschedule()
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val HANDLED = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
        )
    }
}
