package com.yasinmihci.journal.ui.week

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yasinmihci.journal.R
import com.yasinmihci.journal.domain.blocks.Tag
import com.yasinmihci.journal.ui.format.formatDuration

/** Liste des tags perso (Volley…) : couleur, trajet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsScreen(
    onBack: () -> Unit,
    viewModel: WeekViewModel = viewModel(factory = WeekViewModel.Factory),
) {
    val schedule by viewModel.schedule.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Tag?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tags_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editing = Tag(0, "", TAG_PALETTE[schedule.tags.size % TAG_PALETTE.size], 0)
            }) { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.tag_new)) }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            if (schedule.tags.isEmpty()) {
                Text(
                    stringResource(R.string.tags_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
            schedule.tags.forEach { tag ->
                val blocks = schedule.blocks.count { it.tagId == tag.id }
                ListItem(
                    leadingContent = { ColorDot(tag.color) },
                    headlineContent = { Text(tag.name) },
                    supportingContent = {
                        Text(stringResource(R.string.tag_summary, formatDuration(tag.travelMinutes), blocks))
                    },
                    modifier = Modifier.clickable { editing = tag },
                )
            }
        }
    }

    editing?.let { tag ->
        TagDialog(
            initial = tag,
            onDismiss = { editing = null },
            onSave = { viewModel.saveTag(it); editing = null },
            onDelete = if (tag.id == 0L) null else ({ viewModel.deleteTag(tag.id); editing = null }),
        )
    }
}
