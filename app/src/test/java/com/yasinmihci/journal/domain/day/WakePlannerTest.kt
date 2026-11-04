package com.yasinmihci.journal.domain.day

import com.yasinmihci.journal.domain.settings.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class WakePlannerTest {

    private val settings = AppSettings()
    private val monday = LocalDate.of(2026, 11, 2)
    private val tuesday = LocalDate.of(2026, 11, 3)
    private val saturday = LocalDate.of(2026, 11, 7)

    private fun t(h: Int, m: Int) = LocalTime.of(h, m)

    @Test
    fun company_monday() {
        val plan = WakePlanner.plan(monday, DayType.COMPANY, settings, firstCourse = null, bedtime = null)
        assertEquals(WakePlan.Departure(DayType.COMPANY, t(6, 15), t(7, 5)), plan)
    }

    @Test
    fun company_tuesday_ignoresCoursesAndBedtime() {
        val plan = WakePlanner.plan(tuesday, DayType.COMPANY, settings, firstCourse = t(8, 0), bedtime = t(23, 0))
        assertEquals(WakePlan.Departure(DayType.COMPANY, t(6, 45), t(7, 40)), plan)
    }

    @Test
    fun school_specExample() {
        // Cours 8h15 → départ 7h45 → réveil 6h55.
        val plan = WakePlanner.plan(monday, DayType.SCHOOL, settings, firstCourse = t(8, 15), bedtime = null)
        assertEquals(WakePlan.Departure(DayType.SCHOOL, t(6, 55), t(7, 45)), plan)
    }

    @Test
    fun school_usesSettings() {
        val s = settings.copy(schoolTravelMinutes = 20, schoolPrepMinutes = 40)
        val plan = WakePlanner.plan(monday, DayType.SCHOOL, s, firstCourse = t(10, 0), bedtime = null)
        assertEquals(WakePlan.Departure(DayType.SCHOOL, t(9, 0), t(9, 40)), plan)
    }

    @Test
    fun school_withoutCourse_isRest() {
        val plan = WakePlanner.plan(monday, DayType.SCHOOL, settings, firstCourse = null, bedtime = null)
        assertEquals(WakePlan.Rest(DayType.SCHOOL, null, t(10, 30), null), plan)
    }

    @Test
    fun weekend_noBedtime_latestWake() {
        val plan = WakePlanner.plan(saturday, DayType.WEEKEND, settings, firstCourse = null, bedtime = null)
        assertEquals(WakePlan.Rest(DayType.WEEKEND, null, t(10, 30), null), plan)
    }

    @Test
    fun weekend_cappedAtMaxCycles() {
        // 23h30 + 15 min = 23h45 ; 7 cycles tiendraient avant 10h30, plafond 6 → 8h45.
        val plan = WakePlanner.plan(saturday, DayType.WEEKEND, settings, firstCourse = null, bedtime = t(23, 30))
        assertEquals(WakePlan.Rest(DayType.WEEKEND, t(23, 30), t(8, 45), 6), plan)
    }

    @Test
    fun weekend_moreCyclesAllowed_staysBeforeLatest() {
        val s = settings.copy(maxSleepCycles = 10)
        // 23h45 + 7 × 1h30 = 10h15 ; 8 cycles → 11h45 > 10h30.
        assertEquals(t(10, 15) to 7, WakePlanner.cycleWake(saturday, t(23, 30), s))
    }

    @Test
    fun weekend_afterMidnight() {
        // 1h30 + 15 = 1h45 ; 5 cycles → 9h15 (6 → 10h45 > 10h30).
        assertEquals(t(9, 15) to 5, WakePlanner.cycleWake(saturday, t(1, 30), settings))
    }

    @Test
    fun weekend_lessThanOneCycle_latestWake() {
        assertEquals(t(10, 30) to 0, WakePlanner.cycleWake(saturday, t(9, 0), settings))
        assertEquals(t(10, 30) to 0, WakePlanner.cycleWake(saturday, t(11, 0), settings))
    }

    @Test
    fun weekend_exactFit() {
        // 22h00 + 15 = 22h15 ; 8 × 1h30 = 12h → 10h15 ; plafond 8.
        val s = settings.copy(maxSleepCycles = 8)
        assertEquals(t(10, 15) to 8, WakePlanner.cycleWake(saturday, t(22, 0), s))
    }

    @Test
    fun holiday_isRest() {
        val plan = WakePlanner.plan(monday, DayType.HOLIDAY, settings, firstCourse = null, bedtime = t(0, 0))
        // 0h15 + 6 × 1h30 = 9h15.
        assertEquals(WakePlan.Rest(DayType.HOLIDAY, t(0, 0), t(9, 15), 6), plan)
    }
}
