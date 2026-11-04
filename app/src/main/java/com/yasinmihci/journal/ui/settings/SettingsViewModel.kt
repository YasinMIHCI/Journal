package com.yasinmihci.journal.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yasinmihci.journal.JournalApplication
import com.yasinmihci.journal.data.settings.SettingsRepository
import com.yasinmihci.journal.domain.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val appScope: CoroutineScope,
) : ViewModel() {

    /** Null tant que les réglages ne sont pas lus : évite d'éditer les valeurs par défaut. */
    val settings: StateFlow<AppSettings?> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun update(transform: (AppSettings) -> AppSettings) {
        // appScope : l'écriture n'est pas annulée si l'écran se ferme.
        appScope.launch { repository.update(transform) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as JournalApplication).container
                SettingsViewModel(container.settingsRepository, container.appScope)
            }
        }
    }
}
