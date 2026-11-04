package com.yasinmihci.journal.data.repository

import com.yasinmihci.journal.data.local.dao.HabitDao
import com.yasinmihci.journal.data.local.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

class HabitRepository(private val dao: HabitDao) {
    fun observeActive(): Flow<List<HabitEntity>> = dao.observeActive()

    suspend fun save(habit: HabitEntity) = dao.upsert(habit)

    suspend fun active(): List<HabitEntity> = dao.active()

    suspend fun get(id: Long): HabitEntity? = dao.get(id)

    suspend fun archive(id: Long) = dao.archive(id)
}
