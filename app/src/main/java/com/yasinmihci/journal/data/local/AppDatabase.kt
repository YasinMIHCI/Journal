package com.yasinmihci.journal.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.yasinmihci.journal.data.local.dao.HabitDao
import com.yasinmihci.journal.data.local.dao.JournalDao
import com.yasinmihci.journal.data.local.entity.HabitEntity
import com.yasinmihci.journal.data.local.entity.JournalEntryEntity

@Database(
    entities = [JournalEntryEntity::class, HabitEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun journalDao(): JournalDao
    abstract fun habitDao(): HabitDao

    companion object {
        // Pas de fallbackToDestructiveMigration : le journal ne doit jamais être effacé.
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "journal.db").build()
    }
}
