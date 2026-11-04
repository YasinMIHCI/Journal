package com.yasinmihci.journal.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yasinmihci.journal.JournalApplication
import com.yasinmihci.journal.data.calendar.CalendarRepository
import com.yasinmihci.journal.data.calendar.SyncState
import com.yasinmihci.journal.data.calendar.SyncStateRepository
import com.yasinmihci.journal.data.settings.SettingsRepository
import com.yasinmihci.journal.domain.day.NextAlarms
import com.yasinmihci.journal.domain.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val appScope: CoroutineScope,
    private val calendarRepository: CalendarRepository,
    syncStateRepository: SyncStateRepository,
    /** Alarmes programmées (null tant que pas calculé). */
    val nextAlarms: StateFlow<NextAlarms?>,
) : ViewModel() {

    /** Null tant que les réglages ne sont pas lus : évite d'éditer les valeurs par défaut. */
    val settings: StateFlow<AppSettings?> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val syncState: StateFlow<SyncState?> = syncStateRepository.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val syncing = MutableStateFlow(false)

    fun syncNow() {
        if (syncing.value) return
        syncing.value = true
        appScope.launch {
            try {
                calendarRepository.sync()
            } finally {
                syncing.value = false
            }
        }
    }

    fun update(transform: (AppSettings) -> AppSettings) {
        // appScope : l'écriture n'est pas annulée si l'écran se ferme.
        appScope.launch { repository.update(transform) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as JournalApplication).container
                SettingsViewModel(
                    container.settingsRepository,
                    container.appScope,
                    container.calendarRepository,
                    container.syncStateRepository,
                    container.alarmScheduler.next,
                )
            }
        }
    }
}
