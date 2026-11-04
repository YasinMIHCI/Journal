package com.yasinmihci.journal.data.repository

import com.yasinmihci.journal.data.local.dao.HabitDao
import com.yasinmihci.journal.data.local.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

class HabitRepository(private val dao: HabitDao) {
    fun observeActive(): Flow<List<HabitEntity>> = dao.observeActive()

    suspend fun save(habit: HabitEntity) = dao.upsert(habit)

    suspend fun delete(habit: HabitEntity) = dao.delete(habit)
}
