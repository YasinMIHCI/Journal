package com.yasinmihci.journal.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.yasinmihci.journal.data.local.entity.JournalEntryEntity
import java.time.LocalDate

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entries WHERE date = :date")
    suspend fun get(date: LocalDate): JournalEntryEntity?

    @Upsert
    suspend fun upsert(entry: JournalEntryEntity)

    @Query("DELETE FROM journal_entries WHERE date = :date")
    suspend fun delete(date: LocalDate)
}
