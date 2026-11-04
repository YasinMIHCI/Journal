package com.yasinmihci.journal.data.calendar

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Résultat de la dernière synchro ICS. */
data class SyncState(
    val lastSuccessMs: Long?,
    val courses: Int,
    /** Erreur de la dernière tentative, null si elle a réussi. */
    val lastError: String?,
)

class SyncStateRepository(private val dataStore: DataStore<Preferences>) {

    val state: Flow<SyncState> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { SyncState(it[LAST_SUCCESS], it[COURSES] ?: 0, it[LAST_ERROR]) }

    suspend fun recordSuccess(timeMs: Long, courses: Int) {
        dataStore.edit {
            it[LAST_SUCCESS] = timeMs
            it[COURSES] = courses
            it.remove(LAST_ERROR)
        }
    }

    /** Heure de synchro (minutes) actuellement planifiée dans WorkManager, null si jamais planifiée. */
    suspend fun scheduledMinutes(): Int? = dataStore.data.first()[SCHEDULED]

    suspend fun setScheduledMinutes(minutes: Int) {
        dataStore.edit { it[SCHEDULED] = minutes }
    }

    suspend fun recordError(message: String) {
        dataStore.edit { it[LAST_ERROR] = message }
    }

    companion object {
        private val LAST_SUCCESS = longPreferencesKey("last_success_ms")
        private val COURSES = intPreferencesKey("courses")
        private val LAST_ERROR = stringPreferencesKey("last_error")
        private val SCHEDULED = intPreferencesKey("scheduled_sync_min")

        fun create(context: Context) = SyncStateRepository(
            PreferenceDataStoreFactory.create(produceFile = { context.preferencesDataStoreFile("sync") }),
        )
    }
}
