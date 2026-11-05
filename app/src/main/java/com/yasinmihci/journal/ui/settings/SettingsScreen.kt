package com.yasinmihci.journal.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yasinmihci.journal.R
import com.yasinmihci.journal.alarm.AlarmPermission
import com.yasinmihci.journal.data.calendar.SyncState
import com.yasinmihci.journal.domain.day.NextAlarms
import com.yasinmihci.journal.domain.calendar.PARIS
import com.yasinmihci.journal.domain.settings.AppSettings
import com.yasinmihci.journal.ui.common.showTimePicker
import com.yasinmihci.journal.domain.settings.CompanyDay
import com.yasinmihci.journal.ui.format.formatDuration
import com.yasinmihci.journal.ui.format.formatTime
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Dialogue d'édition ouvert (texte ou durée). Les heures passent par le TimePickerDialog système. */
private sealed interface Edit {
    val title: Int
    data class Number(
        override val title: Int,
        val initial: Int,
        val range: IntRange,
        val suffix: Int?,
        val onSet: (Int) -> Unit,
    ) : Edit
    data class Text(
        override val title: Int,
        val initial: String,
        val multiline: Boolean,
        val onSet: (String) -> Unit,
    ) : Edit
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenTags: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val syncing by viewModel.syncing.collectAsStateWithLifecycle()
    val nextAlarms by viewModel.nextAlarms.collectAsStateWithLifecycle()
    var edit by remember { mutableStateOf<Edit?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        val s = settings ?: return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
        ) {
            SettingsContent(
                s,
                update = viewModel::update,
                openEdit = { edit = it },
                syncStatus = syncStatusText(syncState, syncing),
                onSync = viewModel::syncNow,
                nextAlarms = nextAlarms,
                onOpenTags = onOpenTags,
            )
        }
    }

    edit?.let { e ->
        EditDialog(e, onDismiss = { edit = null })
    }
}

@Composable
private fun SettingsContent(
    s: AppSettings,
    update: ((AppSettings) -> AppSettings) -> Unit,
    openEdit: (Edit) -> Unit,
    syncStatus: String,
    onSync: () -> Unit,
    nextAlarms: NextAlarms?,
    onOpenTags: () -> Unit,
) {
    val context = LocalContext.current
    @Composable
    fun time(label: Int, value: LocalTime, set: (AppSettings, LocalTime) -> AppSettings) =
        Row3(label, formatTime(value)) { showTimePicker(context, value) { t -> update { set(it, t) } } }

    // Alarmes
    Section(R.string.settings_alarms)
    PermissionWarnings()
    SwitchRow(R.string.settings_wake_alarm, s.wakeAlarmEnabled, nextAlarmText(nextAlarms?.wake)) { v ->
        update { it.copy(wakeAlarmEnabled = v) }
    }
    SwitchRow(R.string.settings_departure_reminder, s.departureReminderEnabled, nextAlarmText(nextAlarms?.departure)) { v ->
        update { it.copy(departureReminderEnabled = v) }
    }
    SwitchRow(R.string.settings_slot_notifications, s.slotNotificationsEnabled, nextAlarmText(nextAlarms?.slot?.start)) { v ->
        update { it.copy(slotNotificationsEnabled = v) }
    }

    // Entreprise
    Section(R.string.settings_company)
    CompanyHeader()
    AppSettings.WORK_DAYS.forEach { day ->
        CompanyRow(day, s.companyDay(day)) { field, initial ->
            showTimePicker(context, initial) { t ->
                update { cur ->
                    val c = cur.companyDay(day)
                    val new = when (field) {
                        0 -> c.copy(wake = t)
                        1 -> c.copy(departure = t)
                        else -> c.copy(back = t)
                    }
                    cur.copy(companyDays = cur.companyDays + (day to new))
                }
            }
        }
    }

    // École
    Section(R.string.settings_school)
    MinutesRow(R.string.settings_school_travel, s.schoolTravelMinutes, openEdit) { v -> update { it.copy(schoolTravelMinutes = v) } }
    MinutesRow(R.string.settings_school_prep, s.schoolPrepMinutes, openEdit) { v -> update { it.copy(schoolPrepMinutes = v) } }
    time(R.string.settings_school_default, s.schoolDefaultFirstCourse) { a, t -> a.copy(schoolDefaultFirstCourse = t) }

    // Week-end
    Section(R.string.settings_weekend)
    MinutesRow(R.string.settings_sleep_cycle, s.sleepCycleMinutes, openEdit) { v -> update { it.copy(sleepCycleMinutes = v) } }
    MinutesRow(R.string.settings_fall_asleep, s.fallAsleepMinutes, openEdit) { v -> update { it.copy(fallAsleepMinutes = v) } }
    Row3(R.string.settings_max_cycles, s.maxSleepCycles.toString()) {
        openEdit(Edit.Number(R.string.settings_max_cycles, s.maxSleepCycles, 1..12, null) { v -> update { it.copy(maxSleepCycles = v) } })
    }
    time(R.string.settings_latest_wake, s.weekendLatestWake) { a, t -> a.copy(weekendLatestWake = t) }

    // Blocs perso
    Section(R.string.settings_blocks)
    ListItem(
        headlineContent = { Text(stringResource(R.string.tags_title)) },
        supportingContent = { Text(stringResource(R.string.settings_tags_help)) },
        modifier = Modifier.clickable(onClick = onOpenTags),
    )

    // Emploi du temps
    Section(R.string.settings_calendar)
    Row3(R.string.settings_ics_url, s.icsUrl.ifBlank { stringResource(R.string.settings_empty) }) {
        openEdit(Edit.Text(R.string.settings_ics_url, s.icsUrl, multiline = false) { v -> update { it.copy(icsUrl = v.trim()) } })
    }
    Row3(R.string.settings_ics_groups, s.icsGroups.joinToString(", ").ifBlank { stringResource(R.string.settings_empty) }) {
        openEdit(Edit.Text(R.string.settings_ics_groups, s.icsGroups.joinToString("\n"), multiline = true) { v ->
            update { it.copy(icsGroups = v.lines().map(String::trim).filter(String::isNotEmpty)) }
        })
    }
    time(R.string.settings_sync_time, s.syncTime) { a, t -> a.copy(syncTime = t) }
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_sync_now)) },
        supportingContent = { Text(syncStatus) },
        modifier = Modifier.clickable(onClick = onSync),
    )

    // Activités
    Section(R.string.settings_activities)
    time(R.string.settings_window_start, s.activityWindowStart) { a, t -> a.copy(activityWindowStart = t) }
    time(R.string.settings_window_end, s.activityWindowEnd) { a, t -> a.copy(activityWindowEnd = t) }
    MinutesRow(R.string.settings_min_slot, s.minSlotMinutes, openEdit) { v -> update { it.copy(minSlotMinutes = v) } }
}

@Composable
private fun SwitchRow(label: Int, checked: Boolean, supporting: String, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(label)) },
        supportingContent = { Text(supporting) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
        modifier = Modifier.clickable { onChange(!checked) },
    )
}

@Composable
private fun nextAlarmText(at: ZonedDateTime?): String =
    if (at == null) stringResource(R.string.next_none)
    else stringResource(R.string.next_wake, formatDateTime(at.toInstant()))

/** Autorisations manquantes, revérifiées à chaque retour sur l'écran. */
@Composable
private fun PermissionWarnings() {
    val context = LocalContext.current
    var missing by remember { mutableStateOf(emptyList<AlarmPermission>()) }
    LifecycleResumeEffect(Unit) {
        missing = AlarmPermission.entries.filterNot { it.isGranted(context) }
        onPauseOrDispose { }
    }
    missing.forEach { p ->
        ListItem(
            headlineContent = {
                Text(
                    stringResource(
                        when (p) {
                            AlarmPermission.NOTIFICATIONS -> R.string.perm_notifications
                            AlarmPermission.EXACT_ALARMS -> R.string.perm_exact
                            AlarmPermission.FULL_SCREEN -> R.string.perm_full_screen
                        },
                    ),
                    color = MaterialTheme.colorScheme.error,
                )
            },
            supportingContent = { Text(stringResource(R.string.perm_fix)) },
            modifier = Modifier.clickable { context.startActivity(p.settingsIntent(context)) },
        )
    }
}

@Composable
private fun syncStatusText(state: SyncState?, syncing: Boolean): String = when {
    syncing -> stringResource(R.string.sync_running)
    state == null -> ""
    state.lastError != null -> stringResource(R.string.sync_error, state.lastError)
    state.lastSuccessMs != null -> stringResource(
        R.string.sync_ok,
        state.courses,
        formatDateTime(Instant.ofEpochMilli(state.lastSuccessMs)),
    )
    else -> stringResource(R.string.sync_never)
}

private val SYNC_DAY = DateTimeFormatter.ofPattern("EEE d MMM", Locale.FRENCH)

private fun formatDateTime(instant: Instant): String {
    val z = instant.atZone(PARIS)
    return "${z.format(SYNC_DAY)} ${formatTime(z.toLocalTime())}"
}

@Composable
private fun Section(title: Int) {
    HorizontalDivider(Modifier.padding(top = 8.dp))
    Text(
        stringResource(title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun Row3(label: Int, value: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(label)) },
        trailingContent = {
            Text(value, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 180.dp))
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun MinutesRow(label: Int, value: Int, openEdit: (Edit) -> Unit, onSet: (Int) -> Unit) {
    Row3(label, formatDuration(value)) { openEdit(Edit.Number(label, value, 0..720, R.string.unit_min, onSet)) }
}

private val COL = 72.dp

@Composable
private fun CompanyHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        listOf(R.string.settings_wake, R.string.settings_departure, R.string.settings_back).forEach {
            Text(
                stringResource(it),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(COL),
            )
        }
    }
}

@Composable
private fun CompanyRow(day: DayOfWeek, c: CompanyDay, onPick: (field: Int, initial: LocalTime) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            day.getDisplayName(TextStyle.FULL, Locale.FRENCH).replaceFirstChar { it.titlecase(Locale.FRENCH) },
            modifier = Modifier.weight(1f),
        )
        listOf(c.wake, c.departure, c.back).forEachIndexed { i, t ->
            TextButton(onClick = { onPick(i, t) }, modifier = Modifier.width(COL)) { Text(formatTime(t)) }
        }
    }
}


@Composable
private fun EditDialog(edit: Edit, onDismiss: () -> Unit) {
    val initial = when (edit) {
        is Edit.Number -> edit.initial.toString()
        is Edit.Text -> edit.initial
    }
    // Texte entièrement sélectionné : on tape directement la nouvelle valeur.
    var field by remember(edit) { mutableStateOf(TextFieldValue(initial, TextRange(0, initial.length))) }
    val text = field.text
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(edit) { focusRequester.requestFocus() }
    val minutes = text.trim().toIntOrNull()
    val valid = when (edit) {
        is Edit.Number -> minutes != null && minutes in edit.range
        is Edit.Text -> true
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(edit.title)) },
        text = {
            OutlinedTextField(
                value = field,
                onValueChange = { field = it },
                modifier = Modifier.focusRequester(focusRequester),
                singleLine = edit !is Edit.Text || !edit.multiline,
                minLines = if (edit is Edit.Text && edit.multiline) 3 else 1,
                isError = !valid,
                suffix = if (edit is Edit.Number && edit.suffix != null) {
                    { Text(stringResource(edit.suffix)) }
                } else null,
                supportingText = if (edit is Edit.Text && edit.multiline) {
                    { Text(stringResource(R.string.settings_one_per_line)) }
                } else null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = when (edit) {
                        is Edit.Number -> KeyboardType.Number
                        is Edit.Text -> if (edit.multiline) KeyboardType.Text else KeyboardType.Uri
                    },
                ),
            )
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    when (edit) {
                        is Edit.Number -> edit.onSet(minutes!!)
                        is Edit.Text -> edit.onSet(text)
                    }
                    onDismiss()
                },
            ) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
