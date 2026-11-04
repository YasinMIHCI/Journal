package com.yasinmihci.journal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.yasinmihci.journal.data.local.entity.CourseEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class CourseDao {
    @Query("SELECT * FROM courses WHERE startMs >= :fromMs AND startMs < :toMs ORDER BY startMs")
    abstract fun observeBetween(fromMs: Long, toMs: Long): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE startMs >= :fromMs AND startMs < :toMs ORDER BY startMs")
    abstract suspend fun between(fromMs: Long, toMs: Long): List<CourseEntity>

    @Query("DELETE FROM courses")
    protected abstract suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertAll(courses: List<CourseEntity>)

    /** L'export ICS fait foi : remplace tout, en une transaction. */
    @Transaction
    open suspend fun replaceAll(courses: List<CourseEntity>) {
        deleteAll()
        insertAll(courses)
    }
}
