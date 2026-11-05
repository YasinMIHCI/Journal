package com.yasinmihci.journal.ui.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yasinmihci.journal.R
import com.yasinmihci.journal.data.local.entity.HabitEntity
import com.yasinmihci.journal.domain.calendar.Course
import com.yasinmihci.journal.domain.day.DayType
import com.yasinmihci.journal.domain.day.WakePlan
import com.yasinmihci.journal.ui.common.showTimePicker
import com.yasinmihci.journal.ui.format.formatDuration
import com.yasinmihci.journal.ui.format.formatTime
import com.yasinmihci.journal.ui.theme.JournalTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TodayScreen(
    /** Jour à ouvrir (depuis la vue semaine). */
    openDate: LocalDate,
    onOpenWeek: () -> Unit,
    onOpenHabits: () -> Unit,
    onStartSession: (Long) -> Unit,
    viewModel: TodayViewModel = viewModel(factory = TodayViewModel.Factory),
) {
    LaunchedEffect(openDate) { viewModel.goTo(openDate) }
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val wakePlan by viewModel.wakePlan.collectAsStateWithLifecycle()
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    TodayContent(
        date = viewModel.date,
        isToday = viewModel.date == LocalDate.now(),
        wakePlan = wakePlan,
        courses = courses,
        habits = habits,
        journalText = viewModel.journalText,
        journalLoaded = viewModel.journalLoaded,
        onPreviousDay = { viewModel.shiftDay(-1) },
        onNextDay = { viewModel.shiftDay(1) },
        onToday = viewModel::goToToday,
        onJournalChange = viewModel::onJournalChange,
        onOpenWeek = onOpenWeek,
        onSetBedtime = viewModel::setBedtime,
        onOpenHabits = onOpenHabits,
        onStartSession = onStartSession,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TodayContent(
    date: LocalDate,
    isToday: Boolean,
    wakePlan: WakePlan?,
    courses: List<Course>,
    habits: List<HabitProgress>,
    journalText: String,
    journalLoaded: Boolean,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit,
    onJournalChange: (String) -> Unit,
    onOpenWeek: () -> Unit,
    onSetBedtime: (LocalTime) -> Unit,
    onOpenHabits: () -> Unit,
    onStartSession: (Long) -> Unit,
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
                    IconButton(onClick = onOpenWeek) {
                        Icon(Icons.Filled.DateRange, contentDescription = stringResource(R.string.week))
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
            wakePlan?.let { DaySection(it, onSetBedtime) }
            if (courses.isNotEmpty()) CoursesSection(courses)
            HabitsSection(habits, onOpenHabits, onStartSession)
            JournalSection(journalText, journalLoaded, onJournalChange)
        }
    }
}

@Composable
private fun DaySection(plan: WakePlan, onSetBedtime: (LocalTime) -> Unit) {
    val context = LocalContext.current
    Column {
        SectionTitle(stringResource(dayTypeLabel(plan.dayType)))
        when (plan) {
            is WakePlan.Departure -> ListItem(
                headlineContent = {
                    Text(stringResource(R.string.plan_departure, formatTime(plan.wake), formatTime(plan.departure)))
                },
                supportingContent = {
                    val details = listOfNotNull(
                        plan.defaultCourse?.let { stringResource(R.string.plan_default_course, formatTime(it)) },
                        plan.earlyBlock?.let { stringResource(R.string.plan_early_block, it.title, formatTime(it.start)) },
                    )
                    if (details.isNotEmpty()) Text(details.joinToString(" · "))
                },
            )
            is WakePlan.Rest -> ListItem(
                headlineContent = {
                    Text(
                        if (plan.bedtime == null) stringResource(R.string.plan_bedtime_missing)
                        else stringResource(R.string.plan_rest, formatTime(plan.bedtime), formatTime(plan.wake)),
                    )
                },
                supportingContent = {
                    val details = buildList {
                        if (plan.cycles == null) add(stringResource(R.string.plan_latest, formatTime(plan.wake)))
                        else add(pluralStringResource(R.plurals.plan_cycles, plan.cycles, plan.cycles))
                        plan.earlyBlock?.let { add(stringResource(R.string.plan_early_block, it.title, formatTime(it.start))) }
                    }
                    Text(details.joinToString(" · "))
                },
                modifier = Modifier.clickable {
                    showTimePicker(context, plan.bedtime ?: DEFAULT_BEDTIME, onSetBedtime)
                },
            )
        }
    }
}

private val DEFAULT_BEDTIME: LocalTime = LocalTime.of(23, 0)

private fun dayTypeLabel(type: DayType): Int = when (type) {
    DayType.COMPANY -> R.string.day_company
    DayType.SCHOOL -> R.string.day_school
    DayType.WEEKEND -> R.string.day_weekend
    DayType.HOLIDAY -> R.string.day_holiday
    DayType.NONE -> R.string.day_none
}

@Composable
private fun CoursesSection(courses: List<Course>) {
    Column {
        SectionTitle(stringResource(R.string.section_courses))
        courses.forEach { c ->
            ListItem(
                headlineContent = { Text(c.summary) },
                supportingContent = {
                    val time = stringResource(R.string.course_time, formatTime(c.startTime), formatTime(c.endTime))
                    Text(if (c.location.isBlank()) time else "$time · ${c.location}")
                },
            )
        }
    }
}

@Composable
private fun HabitsSection(habits: List<HabitProgress>, onOpenHabits: () -> Unit, onStartSession: (Long) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(stringResource(R.string.section_activities))
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onOpenHabits) { Text(stringResource(R.string.habit_edit)) }
        }
        if (habits.isEmpty()) {
            Text(
                stringResource(R.string.activities_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            habits.forEach { p ->
                ListItem(
                    headlineContent = { Text(p.habit.name) },
                    supportingContent = {
                        Text(
                            if (p.remainingMinutes == 0) stringResource(R.string.habit_done)
                            else stringResource(R.string.habit_remaining, formatDuration(p.remainingMinutes)),
                        )
                    },
                    trailingContent = {
                        Text(
                            stringResource(
                                R.string.habit_progress,
                                formatDuration((p.doneSeconds / 60).toInt()),
                                formatDuration(p.habit.weeklyGoalMinutes),
                            ),
                        )
                    },
                    modifier = Modifier.clickable { onStartSession(p.habit.id) },
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
            courses = emptyList(),
            wakePlan = WakePlan.Rest(DayType.WEEKEND, null, LocalTime.of(10, 30), null),
            journalText = "", journalLoaded = true,
            onPreviousDay = {}, onNextDay = {}, onToday = {}, onJournalChange = {}, onOpenWeek = {}, onSetBedtime = {}, onOpenHabits = {}, onStartSession = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TodayFilledPreview() {
    JournalTheme {
        TodayContent(
            date = LocalDate.of(2026, 11, 4), isToday = false,
            courses = emptyList(),
            wakePlan = WakePlan.Departure(DayType.COMPANY, LocalTime.of(6, 15), LocalTime.of(7, 5)),
            habits = listOf(HabitProgress(HabitEntity(1, "Piano", 60), 900), HabitProgress(HabitEntity(2, "Dessin", 90), 0)),
            journalText = "Cours de réseau, 30 min de piano.", journalLoaded = true,
            onPreviousDay = {}, onNextDay = {}, onToday = {}, onJournalChange = {}, onOpenWeek = {}, onSetBedtime = {}, onOpenHabits = {}, onStartSession = {},
        )
    }
}
