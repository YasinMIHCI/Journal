package com.yasinmihci.journal.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.yasinmihci.journal.data.local.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE active = 1 ORDER BY name")
    fun observeActive(): Flow<List<HabitEntity>>

    @Upsert
    suspend fun upsert(habit: HabitEntity)

    @Query("SELECT * FROM habits WHERE active = 1 ORDER BY name")
    suspend fun active(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun get(id: Long): HabitEntity?

    /** Suppression douce : les sessions passées restent liées à l'activité. */
    @Query("UPDATE habits SET active = 0 WHERE id = :id")
    suspend fun archive(id: Long)
}
