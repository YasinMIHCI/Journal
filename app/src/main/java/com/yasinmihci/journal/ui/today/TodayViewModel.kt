package com.yasinmihci.journal.ui.today

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yasinmihci.journal.JournalApplication
import com.yasinmihci.journal.data.local.entity.HabitEntity
import com.yasinmihci.journal.data.repository.BlockRepository
import com.yasinmihci.journal.data.repository.HabitRepository
import com.yasinmihci.journal.data.repository.SessionRepository
import com.yasinmihci.journal.data.repository.JournalRepository
import com.yasinmihci.journal.data.calendar.CalendarRepository
import com.yasinmihci.journal.data.periods.PeriodsRepository
import com.yasinmihci.journal.domain.calendar.Course
import com.yasinmihci.journal.data.settings.Bedtime
import com.yasinmihci.journal.data.settings.BedtimeRepository
import com.yasinmihci.journal.data.settings.SettingsRepository
import com.yasinmihci.journal.domain.day.WakePlan
import com.yasinmihci.journal.domain.day.WakePlanner
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

class TodayViewModel(
    private val journalRepository: JournalRepository,
    habitRepository: HabitRepository,
    sessionRepository: SessionRepository,
    settingsRepository: SettingsRepository,
    private val bedtimeRepository: BedtimeRepository,
    periodsRepository: PeriodsRepository,
    calendarRepository: CalendarRepository,
    blockRepository: BlockRepository,
    private val appScope: CoroutineScope,
) : ViewModel() {

    var date by mutableStateOf(LocalDate.now())
        private set

    var journalText by mutableStateOf("")
        private set

    /** Faux tant que le texte du jour n'est pas chargé : évite d'écraser l'entrée existante. */
    var journalLoaded by mutableStateOf(false)
        private set

    /** Activités + temps fait dans la semaine (lun → dim) du jour affiché. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val habits: StateFlow<List<HabitProgress>> = combine(
        habitRepository.observeActive(),
        snapshotFlow { date }.flatMapLatest { sessionRepository.observeWeekTotals(it) },
    ) { list, totals -> list.map { HabitProgress(it, totals[it.id] ?: 0L) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Cours du jour affiché, filtrés par groupe. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val courses: StateFlow<List<Course>> = snapshotFlow { date }
        .flatMapLatest { calendarRepository.observeCourses(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Type de journée + réveil/départ du jour affiché. Null tant que non calculé. */
    val wakePlan: StateFlow<WakePlan?> = combine(
        snapshotFlow { date },
        courses,
        bedtimeRepository.bedtime,
        combine(
            settingsRepository.settings,
            flow { emit(periodsRepository.get()) },
            blockRepository.schedule,
        ) { s, p, b -> Triple(s, p, b) },
    ) { day, dayCourses, bedtime, (settings, periods, blocks) ->
        WakePlanner.plan(
            date = day,
            dayType = periods.dayType(day),
            settings = settings,
            // Filtre par date : pendant un changement de jour, la liste peut encore être celle de la veille.
            firstCourse = dayCourses.filter { it.date == day }.minByOrNull { it.start }?.startTime,
            bedtime = bedtime?.takeIf { it.wakeDate == day }?.time,
            blocks = blocks.on(day),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Heure de coucher de la nuit qui précède le jour affiché. */
    fun setBedtime(time: LocalTime) {
        val day = date
        appScope.launch { bedtimeRepository.set(Bedtime(day, time)) }
    }

    private var loadJob: Job? = null

    init {
        loadJournal()
    }

    fun onJournalChange(text: String) {
        if (!journalLoaded) return
        journalText = text
        journalRepository.save(date, text)
    }

    /** Ouvre un jour précis (depuis la vue semaine). */
    fun goTo(day: LocalDate) {
        if (date == day) return
        date = day
        loadJournal()
    }

    fun shiftDay(days: Long) {
        date = date.plusDays(days)
        loadJournal()
    }

    fun goToToday() {
        val today = LocalDate.now()
        if (date == today) return
        date = today
        loadJournal()
    }

    private fun loadJournal() {
        loadJob?.cancel()
        journalLoaded = false
        val day = date
        loadJob = viewModelScope.launch {
            journalText = journalRepository.get(day)
            journalLoaded = true
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as JournalApplication).container
                TodayViewModel(
                    container.journalRepository,
                    container.habitRepository,
                    container.sessionRepository,
                    container.settingsRepository,
                    container.bedtimeRepository,
                    container.periodsRepository,
                    container.calendarRepository,
                    container.blockRepository,
                    container.appScope,
                )
            }
        }
    }
}

data class HabitProgress(val habit: HabitEntity, val doneSeconds: Long) {
    val remainingMinutes: Int get() = com.yasinmihci.journal.domain.activity.remainingMinutes(habit.weeklyGoalMinutes, doneSeconds)
}
