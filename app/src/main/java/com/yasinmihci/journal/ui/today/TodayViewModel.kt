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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class TodayViewModel(
    private val journalRepository: JournalRepository,
    habitRepository: HabitRepository,
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
                TodayViewModel(container.journalRepository, container.habitRepository)
            }
        }
    }
}
