package com.yasinmihci.journal.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Une session chronométrée d'une activité. */
@Entity(tableName = "sessions", indices = [Index("habitId"), Index("startMs")])
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val startMs: Long,
    val durationSeconds: Long,
)
