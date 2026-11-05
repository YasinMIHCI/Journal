package com.yasinmihci.journal.ui.week

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.yasinmihci.journal.domain.day.DayType
import com.yasinmihci.journal.domain.week.AgendaItem
import com.yasinmihci.journal.domain.week.AgendaKind

val SCHOOL_COLOR = Color(0xFF3F6FD8)
val COMPANY_COLOR = Color(0xFFE08A1E)

/** Couleurs proposées pour les tags. */
val TAG_PALETTE: List<Int> = listOf(
    0xFF2E9E5B, 0xFFD64545, 0xFF8E44AD, 0xFF16A0A0,
    0xFFD9488F, 0xFF8D6E63, 0xFF3949AB, 0xFFC9A100,
).map { it.toInt() }

/** Trajets : même couleur que le bloc, en plus pâle. */
private const val TRAVEL_ALPHA = 0.4f

fun itemColor(item: AgendaItem): Color = when (item.kind) {
    AgendaKind.COURSE -> SCHOOL_COLOR
    AgendaKind.SCHOOL_TRAVEL -> SCHOOL_COLOR.copy(alpha = TRAVEL_ALPHA)
    AgendaKind.COMPANY -> COMPANY_COLOR
    AgendaKind.BLOCK -> Color(item.tag?.color ?: TAG_PALETTE[0])
    AgendaKind.BLOCK_TRAVEL -> Color(item.tag?.color ?: TAG_PALETTE[0]).copy(alpha = TRAVEL_ALPHA)
}

fun textColorOn(background: Color): Color =
    if (background.alpha < 0.6f || background.luminance() > 0.5f) Color.Black else Color.White

fun dayTypeColor(type: DayType): Color? = when (type) {
    DayType.SCHOOL -> SCHOOL_COLOR
    DayType.COMPANY -> COMPANY_COLOR
    else -> null
}
