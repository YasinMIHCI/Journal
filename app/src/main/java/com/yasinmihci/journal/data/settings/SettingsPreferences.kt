package com.yasinmihci.journal.data.settings

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.yasinmihci.journal.domain.settings.AppSettings
import com.yasinmihci.journal.domain.settings.CompanyDay
import java.time.DayOfWeek
import java.time.LocalTime

// Heures stockées en minutes depuis minuit. Clé absente = valeur par défaut de AppSettings.

private fun companyKey(day: DayOfWeek, field: String) = intPreferencesKey("company_${day.value}_$field")

private val SCHOOL_TRAVEL = intPreferencesKey("school_travel_min")
private val SCHOOL_PREP = intPreferencesKey("school_prep_min")
private val SLEEP_CYCLE = intPreferencesKey("sleep_cycle_min")
private val FALL_ASLEEP = intPreferencesKey("fall_asleep_min")
private val WEEKEND_LATEST_WAKE = intPreferencesKey("weekend_latest_wake")
private val ICS_URL = stringPreferencesKey("ics_url")
private val ICS_GROUPS = stringPreferencesKey("ics_groups")
private val SYNC_TIME = intPreferencesKey("sync_time")
private val ACTIVITY_START = intPreferencesKey("activity_window_start")
private val ACTIVITY_END = intPreferencesKey("activity_window_end")
private val MIN_SLOT = intPreferencesKey("min_slot_min")

private fun LocalTime.toMinutes(): Int = hour * 60 + minute
private fun Int.toTime(): LocalTime = LocalTime.of((this / 60).coerceIn(0, 23), (this % 60).coerceIn(0, 59))

private fun Preferences.time(key: Preferences.Key<Int>, default: LocalTime): LocalTime =
    this[key]?.toTime() ?: default

fun Preferences.toAppSettings(): AppSettings {
    val d = AppSettings()
    return AppSettings(
        companyDays = AppSettings.WORK_DAYS.associateWith { day ->
            val def = d.companyDay(day)
            CompanyDay(
                wake = time(companyKey(day, "wake"), def.wake),
                departure = time(companyKey(day, "departure"), def.departure),
                back = time(companyKey(day, "back"), def.back),
            )
        },
        schoolTravelMinutes = this[SCHOOL_TRAVEL] ?: d.schoolTravelMinutes,
        schoolPrepMinutes = this[SCHOOL_PREP] ?: d.schoolPrepMinutes,
        sleepCycleMinutes = this[SLEEP_CYCLE] ?: d.sleepCycleMinutes,
        fallAsleepMinutes = this[FALL_ASLEEP] ?: d.fallAsleepMinutes,
        weekendLatestWake = time(WEEKEND_LATEST_WAKE, d.weekendLatestWake),
        icsUrl = this[ICS_URL] ?: d.icsUrl,
        icsGroups = this[ICS_GROUPS]?.split('\n')?.filter { it.isNotBlank() } ?: d.icsGroups,
        syncTime = time(SYNC_TIME, d.syncTime),
        activityWindowStart = time(ACTIVITY_START, d.activityWindowStart),
        activityWindowEnd = time(ACTIVITY_END, d.activityWindowEnd),
        minSlotMinutes = this[MIN_SLOT] ?: d.minSlotMinutes,
    )
}

fun MutablePreferences.write(s: AppSettings) {
    AppSettings.WORK_DAYS.forEach { day ->
        val c = s.companyDay(day)
        this[companyKey(day, "wake")] = c.wake.toMinutes()
        this[companyKey(day, "departure")] = c.departure.toMinutes()
        this[companyKey(day, "back")] = c.back.toMinutes()
    }
    this[SCHOOL_TRAVEL] = s.schoolTravelMinutes
    this[SCHOOL_PREP] = s.schoolPrepMinutes
    this[SLEEP_CYCLE] = s.sleepCycleMinutes
    this[FALL_ASLEEP] = s.fallAsleepMinutes
    this[WEEKEND_LATEST_WAKE] = s.weekendLatestWake.toMinutes()
    this[ICS_URL] = s.icsUrl
    this[ICS_GROUPS] = s.icsGroups.joinToString("\n")
    this[SYNC_TIME] = s.syncTime.toMinutes()
    this[ACTIVITY_START] = s.activityWindowStart.toMinutes()
    this[ACTIVITY_END] = s.activityWindowEnd.toMinutes()
    this[MIN_SLOT] = s.minSlotMinutes
}
