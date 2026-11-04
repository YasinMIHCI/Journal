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
import com.yasinmihci.journal.data.repository.HabitRepository
import com.yasinmihci.journal.data.repository.JournalRepository
import com.yasinmihci.journal.data.periods.PeriodsRepository
import com.yasinmihci.journal.data.settings.Bedtime
import com.yasinmihci.journal.data.settings.BedtimeRepository
import com.yasinmihci.journal.data.settings.SettingsRepository
import com.yasinmihci.journal.domain.day.WakePlan
import com.yasinmihci.journal.domain.day.WakePlanner
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
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
    settingsRepository: SettingsRepository,
    private val bedtimeRepository: BedtimeRepository,
    periodsRepository: PeriodsRepository,
    private val appScope: CoroutineScope,
) : ViewModel() {

    var date by mutableStateOf(LocalDate.now())
        private set

    var journalText by mutableStateOf("")
        private set

    /** Faux tant que le texte du jour n'est pas chargé : évite d'écraser l'entrée existante. */
    var journalLoaded by mutableStateOf(false)
        private set

    val habits: StateFlow<List<HabitEntity>> = habitRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Type de journée + réveil/départ du jour affiché. Null tant que non calculé. */
    val wakePlan: StateFlow<WakePlan?> = combine(
        snapshotFlow { date },
        settingsRepository.settings,
        bedtimeRepository.bedtime,
        flow { emit(periodsRepository.get()) },
    ) { day, settings, bedtime, periods ->
        WakePlanner.plan(
            date = day,
            dayType = periods.dayType(day),
            settings = settings,
            firstCourse = null, // TODO étape 3 : premier cours lu depuis l'ICS
            bedtime = bedtime?.takeIf { it.wakeDate == day }?.time,
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
                    container.settingsRepository,
                    container.bedtimeRepository,
                    container.periodsRepository,
                    container.appScope,
                )
            }
        }
    }
}
