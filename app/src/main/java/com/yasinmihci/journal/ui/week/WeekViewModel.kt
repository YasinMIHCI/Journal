package com.yasinmihci.journal.ui.week

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yasinmihci.journal.JournalApplication
import com.yasinmihci.journal.data.calendar.CalendarRepository
import com.yasinmihci.journal.data.periods.PeriodsRepository
import com.yasinmihci.journal.data.repository.BlockRepository
import com.yasinmihci.journal.data.settings.BedtimeRepository
import com.yasinmihci.journal.data.settings.SettingsRepository
import com.yasinmihci.journal.domain.activity.weekStart
import com.yasinmihci.journal.domain.blocks.Block
import com.yasinmihci.journal.domain.blocks.BlockSchedule
import com.yasinmihci.journal.domain.blocks.Tag
import com.yasinmihci.journal.domain.calendar.PARIS
import com.yasinmihci.journal.domain.week.DayAgenda
import com.yasinmihci.journal.domain.week.buildDayAgenda
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class WeekViewModel(
    settingsRepository: SettingsRepository,
    periodsRepository: PeriodsRepository,
    calendarRepository: CalendarRepository,
    private val blockRepository: BlockRepository,
    bedtimeRepository: BedtimeRepository,
    private val appScope: CoroutineScope,
) : ViewModel() {

    /** Lundi de la semaine affichée. */
    var weekStart by mutableStateOf(weekStart(LocalDate.now(PARIS)))
        private set

    val schedule: StateFlow<BlockSchedule> = blockRepository.schedule
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BlockSchedule.EMPTY)

    /** Les 7 jours de la semaine affichée ; null tant que non calculé. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val days: StateFlow<List<DayAgenda>?> = snapshotFlow { weekStart }
        .flatMapLatest { ws ->
            combine(
                calendarRepository.observeBetween(ws, ws.plusDays(7)),
                settingsRepository.settings,
                blockRepository.schedule,
                bedtimeRepository.bedtime,
                flow { emit(periodsRepository.get()) },
            ) { courses, settings, blocks, bedtime, periods ->
                (0L until 7L).map { i ->
                    val day = ws.plusDays(i)
                    buildDayAgenda(day, settings, periods, courses, blocks, bedtime?.takeIf { it.wakeDate == day }?.time)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun shiftWeek(weeks: Long) {
        weekStart = weekStart.plusWeeks(weeks)
    }

    fun thisWeek() {
        weekStart = weekStart(LocalDate.now(PARIS))
    }

    fun saveBlock(block: Block) {
        appScope.launch { blockRepository.saveBlock(block) }
    }

    fun deleteBlock(id: Long) {
        appScope.launch { blockRepository.deleteBlock(id) }
    }

    /** [onSaved] reçoit l'id du tag (nouveau ou existant), sur le thread principal. */
    fun saveTag(tag: Tag, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch { onSaved(blockRepository.saveTag(tag)) }
    }

    fun deleteTag(id: Long) {
        appScope.launch { blockRepository.deleteTag(id) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as JournalApplication).container
                WeekViewModel(
                    c.settingsRepository,
                    c.periodsRepository,
                    c.calendarRepository,
                    c.blockRepository,
                    c.bedtimeRepository,
                    c.appScope,
                )
            }
        }
    }
}
