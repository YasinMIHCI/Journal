package com.yasinmihci.journal.data.local

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.yasinmihci.journal.data.local.dao.BlockDao
import com.yasinmihci.journal.data.local.dao.CourseDao
import com.yasinmihci.journal.data.local.dao.HabitDao
import com.yasinmihci.journal.data.local.dao.JournalDao
import com.yasinmihci.journal.data.local.dao.SessionDao
import com.yasinmihci.journal.data.local.entity.BlockEntity
import com.yasinmihci.journal.data.local.entity.CourseEntity
import com.yasinmihci.journal.data.local.entity.HabitEntity
import com.yasinmihci.journal.data.local.entity.JournalEntryEntity
import com.yasinmihci.journal.data.local.entity.SessionEntity
import com.yasinmihci.journal.data.local.entity.TagEntity

@Database(
    entities = [
        JournalEntryEntity::class, HabitEntity::class, CourseEntity::class, SessionEntity::class,
        TagEntity::class, BlockEntity::class,
    ],
    version = 4,
    exportSchema = true,
    // Ajouts purs (tables / colonne nullable), aucune donnée existante touchée.
    // 1 → 2 : courses. 2 → 3 : sessions. 3 → 4 : tags, blocks, habits.soundUri.
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
    ],
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun journalDao(): JournalDao
    abstract fun habitDao(): HabitDao
    abstract fun courseDao(): CourseDao
    abstract fun sessionDao(): SessionDao
    abstract fun blockDao(): BlockDao

    companion object {
        // Pas de fallbackToDestructiveMigration : le journal ne doit jamais être effacé.
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "journal.db").build()
    }
}
