package com.yasinmihci.journal.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/** Tag perso (Volley…) : couleur + trajet avant/après chaque bloc. */
@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Int,
    val travelMinutes: Int,
)

/**
 * Bloc perso. Ponctuel : [date] renseignée. Permanent : [weekdays] (bit 0 = lundi … bit 6 = dimanche)
 * entre [fromDate] et [untilDate] (null = sans limite).
 */
@Entity(tableName = "blocks", indices = [Index("tagId")])
data class BlockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tagId: Long,
    val title: String,
    val startMinute: Int,
    val endMinute: Int,
    val date: LocalDate?,
    val weekdays: Int,
    val fromDate: LocalDate?,
    val untilDate: LocalDate?,
)
