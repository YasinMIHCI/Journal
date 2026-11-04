package com.yasinmihci.journal.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime

/** Heure de coucher saisie pour la nuit qui précède [wakeDate]. */
data class Bedtime(val wakeDate: LocalDate, val time: LocalTime)

/** Seule la dernière saisie est gardée : elle ne sert qu'au prochain réveil. */
class BedtimeRepository(private val dataStore: DataStore<Preferences>) {

    val bedtime: Flow<Bedtime?> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            val date = prefs[WAKE_DATE]?.let(LocalDate::parse) ?: return@map null
            val minutes = prefs[TIME] ?: return@map null
            Bedtime(date, LocalTime.of(minutes / 60, minutes % 60))
        }

    suspend fun current(): Bedtime? = bedtime.first()

    suspend fun set(bedtime: Bedtime) {
        dataStore.edit {
            it[WAKE_DATE] = bedtime.wakeDate.toString()
            it[TIME] = bedtime.time.hour * 60 + bedtime.time.minute
        }
    }

    companion object {
        private val WAKE_DATE = stringPreferencesKey("wake_date")
        private val TIME = intPreferencesKey("time")

        fun create(context: Context) = BedtimeRepository(
            PreferenceDataStoreFactory.create(produceFile = { context.preferencesDataStoreFile("bedtime") }),
        )
    }
}
