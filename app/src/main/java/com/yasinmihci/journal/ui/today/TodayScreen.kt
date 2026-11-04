package com.yasinmihci.journal.ui.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yasinmihci.journal.R
import com.yasinmihci.journal.data.local.entity.HabitEntity
import com.yasinmihci.journal.ui.format.formatDuration
import com.yasinmihci.journal.ui.theme.JournalTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TodayScreen(viewModel: TodayViewModel = viewModel(factory = TodayViewModel.Factory)) {
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    TodayContent(
        date = viewModel.date,
        isToday = viewModel.date == LocalDate.now(),
        habits = habits,
        journalText = viewModel.journalText,
        journalLoaded = viewModel.journalLoaded,
        onPreviousDay = { viewModel.shiftDay(-1) },
        onNextDay = { viewModel.shiftDay(1) },
        onToday = viewModel::goToToday,
        onJournalChange = viewModel::onJournalChange,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TodayContent(
    date: LocalDate,
    isToday: Boolean,
    habits: List<HabitEntity>,
    journalText: String,
    journalLoaded: Boolean,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit,
    onJournalChange: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable(onClick = onToday),
                    ) {
                        Text(formatDay(date))
                        if (isToday) {
                            Text(
                                stringResource(R.string.today),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onPreviousDay) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = stringResource(R.string.previous_day),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNextDay) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = stringResource(R.string.next_day),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            HabitsSection(habits)
            JournalSection(journalText, journalLoaded, onJournalChange)
        }
    }
}

@Composable
private fun HabitsSection(habits: List<HabitEntity>) {
    Column {
        SectionTitle(stringResource(R.string.section_activities))
        if (habits.isEmpty()) {
            Text(
                stringResource(R.string.activities_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            habits.forEach { habit ->
                ListItem(
                    headlineContent = { Text(habit.name) },
                    trailingContent = {
                        Text(stringResource(R.string.habit_goal, formatDuration(habit.weeklyGoalMinutes)))
                    },
                )
            }
        }
    }
}

@Composable
private fun JournalSection(text: String, loaded: Boolean, onChange: (String) -> Unit) {
    Column {
        SectionTitle(stringResource(R.string.section_journal))
        OutlinedTextField(
            value = text,
            onValueChange = onChange,
            readOnly = !loaded,
            placeholder = { Text(stringResource(R.string.journal_hint)) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp)
                .padding(top = 8.dp),
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
}

private val dayFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)

private fun formatDay(date: LocalDate): String =
    date.format(dayFormatter).replaceFirstChar { it.titlecase(Locale.FRENCH) }

@Preview(showBackground = true)
@Composable
private fun TodayEmptyPreview() {
    JournalTheme {
        TodayContent(
            date = LocalDate.of(2026, 11, 4), isToday = true, habits = emptyList(),
            journalText = "", journalLoaded = true,
            onPreviousDay = {}, onNextDay = {}, onToday = {}, onJournalChange = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TodayFilledPreview() {
    JournalTheme {
        TodayContent(
            date = LocalDate.of(2026, 11, 4), isToday = false,
            habits = listOf(HabitEntity(1, "Piano", 60), HabitEntity(2, "Dessin", 90)),
            journalText = "Cours de réseau, 30 min de piano.", journalLoaded = true,
            onPreviousDay = {}, onNextDay = {}, onToday = {}, onJournalChange = {},
        )
    }
}
