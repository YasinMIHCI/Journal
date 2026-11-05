package com.yasinmihci.journal.ui.week

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yasinmihci.journal.R
import com.yasinmihci.journal.domain.blocks.Block
import com.yasinmihci.journal.domain.blocks.Tag
import com.yasinmihci.journal.ui.common.showDatePicker
import com.yasinmihci.journal.ui.common.showTimePicker
import com.yasinmihci.journal.ui.format.formatTime
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val DATE = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.FRENCH)

/** Création / modification d'un bloc ponctuel ou permanent. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BlockDialog(
    initial: Block,
    tags: List<Tag>,
    onSaveTag: (Tag, (Long) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onSave: (Block) -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    var b by remember(initial) {
        // Un bloc ponctuel garde une date de début pour s'il devient permanent.
        mutableStateOf(initial.copy(from = initial.from ?: initial.date ?: LocalDate.now()))
    }
    var permanent by remember(initial) { mutableStateOf(initial.isPermanent) }
    var newTag by remember { mutableStateOf<Tag?>(null) }

    val valid = tags.any { it.id == b.tagId } && b.end > b.start && (!permanent || b.weekdays.isNotEmpty())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial.id == 0L) R.string.block_new else R.string.block_edit)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Label(R.string.block_tag)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tags.forEach { tag ->
                        FilterChip(
                            selected = tag.id == b.tagId,
                            onClick = { b = b.copy(tagId = tag.id) },
                            label = { Text(tag.name) },
                            leadingIcon = { ColorDot(tag.color) },
                        )
                    }
                    AssistChip(
                        onClick = { newTag = Tag(0, "", TAG_PALETTE[tags.size % TAG_PALETTE.size], 0) },
                        label = { Text(stringResource(R.string.tag_add)) },
                    )
                }
                OutlinedTextField(
                    value = b.title,
                    onValueChange = { b = b.copy(title = it) },
                    label = { Text(stringResource(R.string.block_title)) },
                    singleLine = true,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { showTimePicker(context, b.start) { b = b.copy(start = it) } }) {
                        Text(formatTime(b.start))
                    }
                    Text("→")
                    TextButton(onClick = { showTimePicker(context, b.end) { b = b.copy(end = it) } }) {
                        Text(formatTime(b.end))
                    }
                }
                if (b.end <= b.start) {
                    Text(stringResource(R.string.block_bad_time), color = MaterialTheme.colorScheme.error)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !permanent, onClick = { permanent = false }, label = { Text(stringResource(R.string.block_once)) })
                    FilterChip(selected = permanent, onClick = { permanent = true }, label = { Text(stringResource(R.string.block_weekly)) })
                }
                if (!permanent) {
                    val date = b.date ?: b.from ?: LocalDate.now()
                    TextButton(onClick = { showDatePicker(context, date) { b = b.copy(date = it) } }) {
                        Text(date.format(DATE))
                    }
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        DayOfWeek.entries.forEach { d ->
                            FilterChip(
                                selected = d in b.weekdays,
                                onClick = { b = b.copy(weekdays = if (d in b.weekdays) b.weekdays - d else b.weekdays + d) },
                                label = { Text(d.getDisplayName(TextStyle.SHORT, Locale.FRENCH).trimEnd('.')) },
                            )
                        }
                    }
                    val from = b.from ?: LocalDate.now()
                    TextButton(onClick = { showDatePicker(context, from) { b = b.copy(from = it) } }) {
                        Text(stringResource(R.string.block_from, from.format(DATE)))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { showDatePicker(context, b.until ?: from) { b = b.copy(until = it) } }) {
                            Text(
                                b.until?.let { stringResource(R.string.block_until, it.format(DATE)) }
                                    ?: stringResource(R.string.block_no_end),
                            )
                        }
                        if (b.until != null) {
                            TextButton(onClick = { b = b.copy(until = null) }) { Text(stringResource(R.string.block_clear_end)) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onSave(
                        if (permanent) b.copy(date = null)
                        else b.copy(date = b.date ?: b.from, weekdays = emptySet(), until = null),
                    )
                },
            ) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            Row {
                if (initial.id != 0L) {
                    TextButton(onClick = onDelete) {
                        Text(stringResource(R.string.habit_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )

    newTag?.let { tag ->
        TagDialog(
            initial = tag,
            onDismiss = { newTag = null },
            onSave = { t -> onSaveTag(t) { id -> b = b.copy(tagId = id) }; newTag = null },
            onDelete = null,
        )
    }
}

/** Création / modification d'un tag : nom, trajet, couleur. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagDialog(
    initial: Tag,
    onDismiss: () -> Unit,
    onSave: (Tag) -> Unit,
    /** null = pas de suppression (nouveau tag). */
    onDelete: (() -> Unit)?,
) {
    var name by remember(initial) { mutableStateOf(initial.name) }
    var travel by remember(initial) { mutableStateOf(initial.travelMinutes.toString()) }
    var color by remember(initial) { mutableStateOf(initial.color) }
    var confirmDelete by remember { mutableStateOf(false) }
    val travelMinutes = travel.trim().toIntOrNull()
    val valid = name.isNotBlank() && travelMinutes != null && travelMinutes in 0..240

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial.id == 0L) R.string.tag_new else R.string.tag_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.habit_name)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = travel,
                    onValueChange = { travel = it },
                    label = { Text(stringResource(R.string.tag_travel)) },
                    suffix = { Text(stringResource(R.string.unit_min)) },
                    singleLine = true,
                    isError = travelMinutes == null || travelMinutes !in 0..240,
                    supportingText = { Text(stringResource(R.string.tag_travel_help)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TAG_PALETTE.forEach { c ->
                        Box(
                            Modifier
                                .size(32.dp)
                                .background(Color(c), CircleShape)
                                .border(
                                    width = if (c == color) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape,
                                )
                                .clickable { color = c },
                        )
                    }
                }
                if (confirmDelete) {
                    Text(stringResource(R.string.tag_delete_warning), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { onSave(initial.copy(name = name.trim(), travelMinutes = travelMinutes!!, color = color)) },
            ) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = { if (confirmDelete) onDelete() else confirmDelete = true }) {
                        Text(stringResource(R.string.habit_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}

@Composable
private fun Label(text: Int) {
    Text(stringResource(text), style = MaterialTheme.typography.labelLarge)
}

@Composable
fun ColorDot(color: Int) {
    Box(Modifier.size(12.dp).background(Color(color), CircleShape))
}
