package com.yasinmihci.journal.di

import android.content.Context
import com.yasinmihci.journal.alarm.AlarmScheduler
import com.yasinmihci.journal.data.calendar.CalendarRepository
import com.yasinmihci.journal.data.calendar.SyncStateRepository
import com.yasinmihci.journal.data.local.AppDatabase
import com.yasinmihci.journal.data.repository.HabitRepository
import com.yasinmihci.journal.data.repository.JournalRepository
import com.yasinmihci.journal.data.periods.PeriodsRepository
import com.yasinmihci.journal.data.settings.BedtimeRepository
import com.yasinmihci.journal.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Injection de dépendances manuelle. Une seule instance, créée par [com.yasinmihci.journal.JournalApplication]. */
class AppContainer(context: Context) {
    /** Scope qui survit aux écrans : écritures qui ne doivent pas être annulées. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val database = AppDatabase.build(context)

    val journalRepository = JournalRepository(database.journalDao(), appScope)
    val habitRepository = HabitRepository(database.habitDao())
    val settingsRepository = SettingsRepository.create(context)
    val bedtimeRepository = BedtimeRepository.create(context)
    val periodsRepository = PeriodsRepository(context.applicationContext)
    val syncStateRepository = SyncStateRepository.create(context)
    val calendarRepository = CalendarRepository(database.courseDao(), settingsRepository, syncStateRepository)
    val alarmScheduler = AlarmScheduler(
        context.applicationContext,
        settingsRepository,
        bedtimeRepository,
        periodsRepository,
        calendarRepository,
    )
}
