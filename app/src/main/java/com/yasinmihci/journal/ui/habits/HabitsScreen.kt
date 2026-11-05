package com.yasinmihci.journal.ui.habits

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yasinmihci.journal.JournalApplication
import com.yasinmihci.journal.R
import com.yasinmihci.journal.data.local.entity.HabitEntity
import com.yasinmihci.journal.data.repository.HabitRepository
import com.yasinmihci.journal.ui.format.formatDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HabitsViewModel(
    private val repository: HabitRepository,
    private val appScope: CoroutineScope,
) : ViewModel() {
    val habits: StateFlow<List<HabitEntity>> = repository.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(habit: HabitEntity) {
        appScope.launch { repository.save(habit) }
    }

    fun archive(id: Long) {
        appScope.launch { repository.archive(id) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as JournalApplication).container
                HabitsViewModel(c.habitRepository, c.appScope)
            }
        }
    }
}

/** Liste des activités : ajout, modification, suppression. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    onBack: () -> Unit,
    viewModel: HabitsViewModel = viewModel(factory = HabitsViewModel.Factory),
) {
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    // null = fermé ; id 0 = nouvelle activité.
    var editing by remember { mutableStateOf<HabitEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.habits_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = HabitEntity(name = "", weeklyGoalMinutes = 60) }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.habit_new))
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            if (habits.isEmpty()) {
                Text(
                    stringResource(R.string.activities_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
            habits.forEach { h ->
                ListItem(
                    headlineContent = { Text(h.name) },
                    trailingContent = { Text(stringResource(R.string.habit_goal, formatDuration(h.weeklyGoalMinutes))) },
                    modifier = Modifier.clickable { editing = h },
                )
            }
        }
    }

    editing?.let { h ->
        HabitDialog(
            habit = h,
            onDismiss = { editing = null },
            onSave = { viewModel.save(it); editing = null },
            onDelete = { viewModel.archive(h.id); editing = null },
        )
    }
}

/** Nom lisible d'un son ; null = son par défaut. */
@Composable
private fun soundTitle(context: Context, uri: String?): String {
    val default = stringResource(R.string.habit_sound_default)
    return remember(uri) {
        uri?.let { runCatching { RingtoneManager.getRingtone(context, Uri.parse(it))?.getTitle(context) }.getOrNull() }
            ?: default
    }
}

@Composable
private fun HabitDialog(
    habit: HabitEntity,
    onDismiss: () -> Unit,
    onSave: (HabitEntity) -> Unit,
    onDelete: () -> Unit,
) {
    var name by remember(habit) { mutableStateOf(habit.name) }
    var goal by remember(habit) { mutableStateOf(habit.weeklyGoalMinutes.toString()) }
    var sound by remember(habit) { mutableStateOf(habit.soundUri) }
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            // « Par défaut » ou rien = son par défaut des notifications.
            sound = uri?.takeIf { it != Settings.System.DEFAULT_NOTIFICATION_URI }?.toString()
        }
    }
    val goalMinutes = goal.trim().toIntOrNull()
    val valid = name.isNotBlank() && goalMinutes != null && goalMinutes in 1..10_080
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (habit.id == 0L) R.string.habit_new else R.string.habit_edit)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.habit_name)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = goal,
                    onValueChange = { goal = it },
                    label = { Text(stringResource(R.string.habit_goal_label)) },
                    suffix = { Text(stringResource(R.string.unit_min)) },
                    singleLine = true,
                    isError = goalMinutes == null || goalMinutes !in 1..10_080,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.padding(top = 8.dp),
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.habit_sound)) },
                    supportingContent = { Text(soundTitle(context, sound)) },
                    modifier = Modifier.padding(top = 8.dp).clickable {
                        picker.launch(
                            Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                                .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                .putExtra(
                                    RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                    sound?.let(Uri::parse) ?: Settings.System.DEFAULT_NOTIFICATION_URI,
                                ),
                        )
                    },
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { onSave(habit.copy(name = name.trim(), weeklyGoalMinutes = goalMinutes!!, soundUri = sound)) },
            ) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            if (habit.id != 0L) {
                TextButton(onClick = onDelete) {
                    Text(stringResource(R.string.habit_delete), color = MaterialTheme.colorScheme.error)
                }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}
