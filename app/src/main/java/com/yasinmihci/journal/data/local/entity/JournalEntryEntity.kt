package com.yasinmihci.journal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/** Une entrée de journal par jour. */
@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    @PrimaryKey val date: LocalDate,
    val text: String,
    val updatedAt: Long,
)
