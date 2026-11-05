package com.yasinmihci.journal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Une « activité » côté UI (piano, dessin…), avec un objectif hebdomadaire. */
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val weeklyGoalMinutes: Int,
    val active: Boolean = true,
    /** Son de la notif de créneau (URI RingtoneManager) ; null = son par défaut. */
    val soundUri: String? = null,
)
