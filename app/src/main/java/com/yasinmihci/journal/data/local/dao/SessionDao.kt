package com.yasinmihci.journal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.yasinmihci.journal.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

data class HabitTotal(val habitId: Long, val seconds: Long)

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: SessionEntity)

    @Query(
        "SELECT habitId, SUM(durationSeconds) AS seconds FROM sessions " +
            "WHERE startMs >= :fromMs AND startMs < :toMs GROUP BY habitId",
    )
    fun observeTotals(fromMs: Long, toMs: Long): Flow<List<HabitTotal>>

    @Query(
        "SELECT habitId, SUM(durationSeconds) AS seconds FROM sessions " +
            "WHERE startMs >= :fromMs AND startMs < :toMs GROUP BY habitId",
    )
    suspend fun totals(fromMs: Long, toMs: Long): List<HabitTotal>
}
