package com.yasinmihci.journal

import android.app.Application
import com.yasinmihci.journal.alarm.AlarmNotifications
import com.yasinmihci.journal.di.AppContainer
import com.yasinmihci.journal.work.CalendarSyncWorker
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class JournalApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        AlarmNotifications.createChannels(this)
        keepSyncScheduled()
        keepAlarmsScheduled()
    }

    /** Reprogramme réveil et départ à chaque changement de réglages, d'heure de coucher ou de cours. */
    private fun keepAlarmsScheduled() {
        val c = container
        c.appScope.launch {
            combine(
                c.settingsRepository.settings,
                c.bedtimeRepository.bedtime,
                c.syncStateRepository.state,
            ) { _, _, _ -> }
                .collectLatest { c.alarmScheduler.reschedule() }
        }
    }

    /** (Re)planifie la synchro ICS ; remplace la planification seulement si l'heure a changé. */
    private fun keepSyncScheduled() {
        val c = container
        c.appScope.launch {
            c.settingsRepository.settings
                .map { it.syncTime }
                .distinctUntilChanged()
                .collect { time ->
                    val minutes = time.hour * 60 + time.minute
                    val changed = c.syncStateRepository.scheduledMinutes() != minutes
                    CalendarSyncWorker.schedule(this@JournalApplication, time, replace = changed)
                    if (changed) c.syncStateRepository.setScheduledMinutes(minutes)
                }
        }
    }
}
