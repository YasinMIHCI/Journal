package com.yasinmihci.journal.domain.blocks

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** Tag créé par l'utilisateur (ex. Volley) : couleur + trajet ajouté avant et après chaque bloc. */
data class Tag(
    val id: Long,
    val name: String,
    /** Couleur ARGB. */
    val color: Int,
    val travelMinutes: Int,
)

/**
 * Bloc perso. Ponctuel si [date] est renseignée ; sinon permanent : chaque semaine
 * les [weekdays], entre [from] et [until] (bornes incluses, null = sans limite).
 */
data class Block(
    val id: Long,
    val tagId: Long,
    val title: String,
    val start: LocalTime,
    val end: LocalTime,
    val date: LocalDate? = null,
    val weekdays: Set<DayOfWeek> = emptySet(),
    val from: LocalDate? = null,
    val until: LocalDate? = null,
) {
    val isPermanent: Boolean get() = date == null

    fun occursOn(day: LocalDate): Boolean =
        if (date != null) {
            day == date
        } else {
            day.dayOfWeek in weekdays &&
                (from == null || !day.isBefore(from)) &&
                (until == null || !day.isAfter(until))
        }
}

enum class OccurrenceKind { BLOCK, TRAVEL }

/** Un bloc (ou un de ses trajets) placé sur un jour. */
data class BlockOccurrence(
    val blockId: Long,
    val tag: Tag,
    /** Titre du bloc (le tag si vide). Pour un trajet : titre du bloc concerné. */
    val title: String,
    val start: LocalTime,
    val end: LocalTime,
    val kind: OccurrenceKind,
)

/** Tous les blocs perso et leurs tags. */
class BlockSchedule(val tags: List<Tag>, val blocks: List<Block>) {
    private val tagsById = tags.associateBy { it.id }

    /** Blocs du jour + trajets avant/après, triés par début. Trajets bornés à la journée. */
    fun on(day: LocalDate): List<BlockOccurrence> = blocks
        .filter { it.occursOn(day) && it.end > it.start }
        .flatMap { b ->
            val tag = tagsById[b.tagId] ?: return@flatMap emptyList()
            val title = b.title.ifBlank { tag.name }
            buildList {
                if (tag.travelMinutes > 0) {
                    add(BlockOccurrence(b.id, tag, title, minusClamped(b.start, tag.travelMinutes), b.start, OccurrenceKind.TRAVEL))
                }
                add(BlockOccurrence(b.id, tag, title, b.start, b.end, OccurrenceKind.BLOCK))
                if (tag.travelMinutes > 0) {
                    add(BlockOccurrence(b.id, tag, title, b.end, plusClamped(b.end, tag.travelMinutes), OccurrenceKind.TRAVEL))
                }
            }
        }
        .filter { it.end > it.start }
        .sortedBy { it.start }

    companion object {
        val EMPTY = BlockSchedule(emptyList(), emptyList())
    }
}

/** t − minutes, sans repasser à la veille (00:00 au minimum). */
fun minusClamped(t: LocalTime, minutes: Int): LocalTime {
    val m = t.toSecondOfDay() / 60 - minutes
    return if (m <= 0) LocalTime.MIDNIGHT else LocalTime.ofSecondOfDay(m * 60L)
}

/** t + minutes, sans passer au lendemain (23:59 au maximum). */
fun plusClamped(t: LocalTime, minutes: Int): LocalTime {
    val m = t.toSecondOfDay() / 60 + minutes
    return if (m >= 24 * 60) LocalTime.of(23, 59) else LocalTime.ofSecondOfDay(m * 60L)
}
