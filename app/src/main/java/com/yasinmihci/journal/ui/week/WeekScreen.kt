package com.yasinmihci.journal.ui.week

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yasinmihci.journal.R
import com.yasinmihci.journal.domain.blocks.Block
import com.yasinmihci.journal.domain.calendar.PARIS
import com.yasinmihci.journal.domain.week.AgendaItem
import com.yasinmihci.journal.domain.week.AgendaKind
import com.yasinmihci.journal.domain.week.DayAgenda
import com.yasinmihci.journal.domain.week.layoutLanes
import com.yasinmihci.journal.ui.format.formatTime
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private const val FIRST_HOUR = 6
private const val LAST_HOUR = 24
private val HOUR_HEIGHT = 48.dp
private val LABEL_WIDTH = 34.dp

/** Vue semaine façon ENT : colonnes lun → dim, blocs dont la hauteur suit la durée. Écran d'accueil. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekScreen(
    onOpenDay: (LocalDate) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: WeekViewModel = viewModel(factory = WeekViewModel.Factory),
) {
    val days by viewModel.days.collectAsStateWithLifecycle()
    val schedule by viewModel.schedule.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Block?>(null) }
    var info by remember { mutableStateOf<AgendaItem?>(null) }
    val today = LocalDate.now(PARIS)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        weekTitle(viewModel.weekStart),
                        modifier = Modifier.clickable { viewModel.thisWeek() },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.shiftWeek(-1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.week_previous))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.shiftWeek(1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.week_next))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, stringResource(R.string.settings))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                val day = if (viewModel.weekStart.plusDays(6) < today || viewModel.weekStart > today) viewModel.weekStart else today
                editing = Block(
                    id = 0,
                    tagId = schedule.tags.firstOrNull()?.id ?: 0,
                    title = "",
                    start = LocalTime.of(18, 0),
                    end = LocalTime.of(19, 0),
                    date = day,
                )
            }) { Icon(Icons.Filled.Add, stringResource(R.string.block_new)) }
        },
    ) { padding ->
        val list = days ?: return@Scaffold
        Column(Modifier.fillMaxSize().padding(padding)) {
            DayHeaders(list, today, onOpenDay)
            HorizontalDivider()
            Grid(
                days = list,
                today = today,
                onItemClick = { item ->
                    val id = item.blockId
                    if (id != null) editing = schedule.blocks.firstOrNull { it.id == id }
                    else if (item.kind == AgendaKind.COURSE) info = item
                },
            )
        }
    }

    editing?.let { block ->
        BlockDialog(
            initial = block,
            tags = schedule.tags,
            onSaveTag = viewModel::saveTag,
            onDismiss = { editing = null },
            onSave = { viewModel.saveBlock(it); editing = null },
            onDelete = { viewModel.deleteBlock(block.id); editing = null },
        )
    }
    info?.let { item ->
        AlertDialog(
            onDismissRequest = { info = null },
            title = { Text(item.title) },
            text = {
                Text(
                    stringResource(R.string.course_time, formatTime(item.start), formatTime(item.end)) +
                        if (item.detail.isBlank()) "" else "\n${item.detail}",
                )
            },
            confirmButton = { TextButton(onClick = { info = null }) { Text(stringResource(R.string.ok)) } },
        )
    }
}

private val DAY_FORMAT = DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH)

private fun weekTitle(start: LocalDate): String =
    "${start.dayOfMonth} – ${start.plusDays(6).format(DAY_FORMAT)}"

@Composable
private fun DayHeaders(days: List<DayAgenda>, today: LocalDate, onOpenDay: (LocalDate) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Box(Modifier.width(LABEL_WIDTH))
        days.forEach { d ->
            val isToday = d.date == today
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenDay(d.date) }
                    .padding(vertical = 2.dp),
            ) {
                Text(
                    d.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.FRENCH).trimEnd('.'),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .background(if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape),
                ) {
                    Text(
                        d.date.dayOfMonth.toString(),
                        fontWeight = FontWeight.Medium,
                        color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    )
                }
                // Repère du type de journée (école / entreprise).
                Box(
                    Modifier
                        .padding(top = 2.dp)
                        .size(width = 16.dp, height = 3.dp)
                        .background(dayTypeColor(d.dayType) ?: Color.Transparent, RoundedCornerShape(2.dp)),
                )
            }
        }
    }
}

@Composable
private fun Grid(days: List<DayAgenda>, today: LocalDate, onItemClick: (AgendaItem) -> Unit) {
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    // Ouvre sur 7h.
    LaunchedEffect(Unit) { scroll.scrollTo(with(density) { HOUR_HEIGHT.roundToPx() }) }
    var nowMinute by remember { mutableLongStateOf(minuteOfDay(LocalTime.now(PARIS)).toLong()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            nowMinute = minuteOfDay(LocalTime.now(PARIS)).toLong()
        }
    }
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val totalHeight = HOUR_HEIGHT * (LAST_HOUR - FIRST_HOUR)

    Row(Modifier.fillMaxWidth().verticalScroll(scroll)) {
        Box(Modifier.width(LABEL_WIDTH).height(totalHeight)) {
            for (h in FIRST_HOUR until LAST_HOUR) {
                Text(
                    "${h}h",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.offset(x = 4.dp, y = HOUR_HEIGHT * (h - FIRST_HOUR) - 6.dp),
                )
            }
        }
        days.forEach { day ->
            BoxWithConstraints(
                Modifier
                    .weight(1f)
                    .height(totalHeight)
                    .drawBehind {
                        val hourPx = HOUR_HEIGHT.toPx()
                        for (h in 0..(LAST_HOUR - FIRST_HOUR)) {
                            drawLine(lineColor, Offset(0f, h * hourPx), Offset(size.width, h * hourPx), 1f)
                        }
                        drawLine(lineColor, Offset(0f, 0f), Offset(0f, size.height), 1f)
                    },
            ) {
                val colWidth = maxWidth
                layoutLanes(day.items, { it.start }, { it.end }).forEach { placed ->
                    val item = placed.item
                    val top = yOf(item.start)
                    val bottom = yOf(item.end)
                    if (bottom <= 0.dp) return@forEach
                    val laneWidth = colWidth / placed.lanes
                    AgendaBlock(
                        item = item,
                        modifier = Modifier
                            .offset(x = laneWidth * placed.lane, y = top)
                            .size(width = laneWidth - 1.dp, height = (bottom - top).coerceAtLeast(4.dp))
                            .clickable { onItemClick(item) },
                    )
                }
                // Réveil
                yOf(day.wake).let { y ->
                    Box(
                        Modifier
                            .offset(y = y - 1.dp)
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
                // Maintenant
                if (day.date == today) {
                    yOf(LocalTime.ofSecondOfDay(nowMinute * 60)).let { y ->
                        Box(Modifier.offset(y = y - 1.dp).fillMaxWidth().height(2.dp).background(Color(0xFFE53935)))
                    }
                }
            }
        }
    }
}

private fun minuteOfDay(t: LocalTime) = t.toSecondOfDay() / 60

/** Position verticale d'une heure ; avant le début de la grille = haut, fin de journée (23:59) = bas. */
private fun yOf(t: LocalTime): Dp {
    val minutes = minuteOfDay(t) - FIRST_HOUR * 60
    if (minutes < 0) return 0.dp
    val m = if (t >= LocalTime.of(23, 59)) (LAST_HOUR - FIRST_HOUR) * 60 else minutes
    return HOUR_HEIGHT * (m / 60f)
}

@Composable
private fun AgendaBlock(item: AgendaItem, modifier: Modifier) {
    val bg = itemColor(item)
    val fg = textColorOn(bg)
    val title = when (item.kind) {
        AgendaKind.COMPANY -> stringResource(R.string.day_company)
        AgendaKind.SCHOOL_TRAVEL, AgendaKind.BLOCK_TRAVEL -> ""
        else -> item.title
    }
    Box(modifier.clip(RoundedCornerShape(4.dp)).background(bg).padding(horizontal = 2.dp, vertical = 1.dp)) {
        if (title.isNotEmpty()) {
            Text(
                title,
                color = fg,
                fontSize = 10.sp,
                lineHeight = 11.sp,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
