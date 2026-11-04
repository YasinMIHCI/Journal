package com.yasinmihci.journal.data.local

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.yasinmihci.journal.data.local.dao.CourseDao
import com.yasinmihci.journal.data.local.dao.HabitDao
import com.yasinmihci.journal.data.local.dao.JournalDao
import com.yasinmihci.journal.data.local.dao.SessionDao
import com.yasinmihci.journal.data.local.entity.CourseEntity
import com.yasinmihci.journal.data.local.entity.HabitEntity
import com.yasinmihci.journal.data.local.entity.JournalEntryEntity
import com.yasinmihci.journal.data.local.entity.SessionEntity

@Database(
    entities = [JournalEntryEntity::class, HabitEntity::class, CourseEntity::class, SessionEntity::class],
    version = 3,
    exportSchema = true,
    // Ajouts purs de tables, aucune donnée existante touchée.
    // 1 → 2 : courses. 2 → 3 : sessions.
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun journalDao(): JournalDao
    abstract fun habitDao(): HabitDao
    abstract fun courseDao(): CourseDao
    abstract fun sessionDao(): SessionDao

    companion object {
        // Pas de fallbackToDestructiveMigration : le journal ne doit jamais être effacé.
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "journal.db").build()
    }
}
