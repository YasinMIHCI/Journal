package com.yasinmihci.journal.data.settings

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.yasinmihci.journal.domain.settings.AppSettings
import com.yasinmihci.journal.domain.settings.CompanyDay
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

class SettingsPreferencesTest {

    @Test
    fun emptyPreferences_giveSpecDefaults() {
        val s = emptyPreferences().toAppSettings()
        assertEquals(AppSettings(), s)
        assertEquals(LocalTime.of(6, 15), s.companyDay(DayOfWeek.MONDAY).wake)
        assertEquals(LocalTime.of(7, 40), s.companyDay(DayOfWeek.FRIDAY).departure)
        assertEquals(30, s.schoolTravelMinutes)
        assertEquals(50, s.schoolPrepMinutes)
        assertEquals(LocalTime.of(10, 30), s.weekendLatestWake)
    }

    @Test
    fun roundTrip_keepsEveryField() {
        val custom = AppSettings(
            companyDays = AppSettings.DEFAULT_COMPANY_DAYS +
                (DayOfWeek.THURSDAY to CompanyDay(LocalTime.of(5, 50), LocalTime.of(6, 40), LocalTime.of(19, 5))),
            schoolTravelMinutes = 25,
            schoolPrepMinutes = 45,
            schoolDefaultFirstCourse = LocalTime.of(9, 15),
            sleepCycleMinutes = 85,
            fallAsleepMinutes = 10,
            maxSleepCycles = 5,
            weekendLatestWake = LocalTime.of(9, 45),
            icsUrl = "https://example.org/a.ics",
            icsGroups = listOf("IT4 FISA - Cyber"),
            syncTime = LocalTime.of(5, 30),
            activityWindowStart = LocalTime.of(9, 0),
            activityWindowEnd = LocalTime.of(21, 30),
            minSlotMinutes = 45,
            wakeAlarmEnabled = false,
            departureReminderEnabled = false,
        )
        val prefs = mutablePreferencesOf().apply { write(custom) }
        assertEquals(custom, prefs.toAppSettings())
    }

    @Test
    fun emptyGroupList_staysEmpty() {
        val prefs = mutablePreferencesOf().apply { write(AppSettings(icsGroups = emptyList())) }
        assertEquals(emptyList<String>(), prefs.toAppSettings().icsGroups)
    }
}
