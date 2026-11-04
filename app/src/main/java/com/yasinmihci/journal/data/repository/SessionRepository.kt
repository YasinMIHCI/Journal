package com.yasinmihci.journal.data.repository

import com.yasinmihci.journal.data.local.dao.SessionDao
import com.yasinmihci.journal.data.local.entity.SessionEntity
import com.yasinmihci.journal.domain.activity.weekStart
import com.yasinmihci.journal.domain.calendar.PARIS
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class SessionRepository(private val dao: SessionDao) {

    suspend fun add(habitId: Long, startMs: Long, durationSeconds: Long) {
        if (durationSeconds <= 0) return
        dao.insert(SessionEntity(habitId = habitId, startMs = startMs, durationSeconds = durationSeconds))
    }

    /** Secondes faites par activité pendant la semaine (lundi → dimanche) qui contient [date]. */
    fun observeWeekTotals(date: LocalDate): Flow<Map<Long, Long>> {
        val (from, to) = weekBounds(date)
        return dao.observeTotals(from, to).map { list -> list.associate { it.habitId to it.seconds } }
    }

    suspend fun weekTotals(date: LocalDate): Map<Long, Long> {
        val (from, to) = weekBounds(date)
        return dao.totals(from, to).associate { it.habitId to it.seconds }
    }

    private fun weekBounds(date: LocalDate): Pair<Long, Long> {
        val start = weekStart(date)
        return start.atStartOfDay(PARIS).toInstant().toEpochMilli() to
            start.plusWeeks(1).atStartOfDay(PARIS).toInstant().toEpochMilli()
    }
}
