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
import com.yasinmihci.journal.data.local.entity.CourseEntity
import com.yasinmihci.journal.data.local.entity.HabitEntity
import com.yasinmihci.journal.data.local.entity.JournalEntryEntity

@Database(
    entities = [JournalEntryEntity::class, HabitEntity::class, CourseEntity::class],
    version = 2,
    exportSchema = true,
    // 1 → 2 : ajout de la table courses (ajout pur, aucune donnée existante touchée).
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun journalDao(): JournalDao
    abstract fun habitDao(): HabitDao
    abstract fun courseDao(): CourseDao

    companion object {
        // Pas de fallbackToDestructiveMigration : le journal ne doit jamais être effacé.
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "journal.db").build()
    }
}
