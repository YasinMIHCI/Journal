package com.yasinmihci.journal.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Cours ADE brut (tous groupes) ; le filtre par groupe est appliqué à la lecture. */
@Entity(tableName = "courses", indices = [Index("startMs")])
data class CourseEntity(
    @PrimaryKey val uid: String,
    val startMs: Long,
    val endMs: Long,
    val summary: String,
    val location: String,
    /** Lignes de la description (groupes, enseignants), séparées par '\n'. */
    val description: String,
)
