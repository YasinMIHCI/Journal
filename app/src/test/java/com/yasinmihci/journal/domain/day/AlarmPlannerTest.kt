package com.yasinmihci.journal.domain.day

import com.yasinmihci.journal.domain.calendar.Course
import com.yasinmihci.journal.domain.calendar.PARIS
import com.yasinmihci.journal.domain.settings.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime

class AlarmPlannerTest {

    private val settings = AppSettings()
    private val periods = Periods(
        school = listOf(DateRange(LocalDate.of(2026, 11, 16), LocalDate.of(2026, 12, 18))),
        company = listOf(DateRange(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 11, 13))),
        holidays = setOf(LocalDate.of(2026, 11, 11)),
    )

    private fun at(s: String): ZonedDateTime = LocalDateTime.parse(s).atZone(PARIS)

    private fun course(start: String, end: String, group: String = "IT4 FISA") = Course(
        uid = start,
        start = at(start).toInstant(),
        end = at(end).toInstant(),
        summary = "Cours",
        location = "",
        descriptionLines = listOf(group),
    )

    private fun next(now: String, courses: List<Course> = emptyList(), bedtime: (LocalDate) -> LocalTime? = { null }) =
        AlarmPlanner.next(at(now), settings, periods, courses, bedtime)

    @Test
    fun companyMorning_beforeWake() {
        // Mercredi 4 nov., entreprise : 6h15 / 7h05.
        val n = next("2026-11-04T05:00")
        assertEquals(at("2026-11-04T06:15"), n.wake)
        assertEquals(at("2026-11-04T07:05"), n.departure)
    }

    @Test
    fun betweenWakeAndDeparture_departureStaysToday() {
        val n = next("2026-11-04T06:30")
        assertEquals(at("2026-11-05T06:45"), n.wake) // jeudi
        assertEquals(at("2026-11-04T07:05"), n.departure)
    }

    @Test
    fun exactlyAtWakeTime_movesToNextDay() {
        assertEquals(at("2026-11-05T06:45"), next("2026-11-04T06:15").wake)
    }

    @Test
    fun holidayEve_restDayAtLatestWake() {
        // Mardi soir 10 nov. → mercredi 11 férié, pas de coucher saisi → 10h30.
        val n = next("2026-11-10T22:00")
        assertEquals(at("2026-11-11T10:30"), n.wake)
        assertEquals(at("2026-11-12T07:40"), n.departure) // jeudi entreprise
    }

    @Test
    fun weekend_withBedtime() {
        val sat = LocalDate.of(2026, 11, 7)
        val n = next("2026-11-06T22:00", bedtime = { if (it == sat) LocalTime.of(23, 0) else null })
        assertEquals(at("2026-11-07T08:15"), n.wake) // 23h15 + 6 cycles
        assertEquals(at("2026-11-09T07:05"), n.departure) // lundi entreprise
    }

    @Test
    fun schoolDay_firstCourse() {
        val courses = listOf(
            course("2026-11-16T10:15", "2026-11-16T12:00"),
            course("2026-11-16T08:00", "2026-11-16T10:00"),
        )
        val n = next("2026-11-15T21:00", courses)
        assertEquals(at("2026-11-16T06:40"), n.wake) // 8h00 − 30 − 50
        assertEquals(at("2026-11-16T07:30"), n.departure)
    }

    @Test
    fun nothingInHorizon_whenOutsideCalendar_onlyWakeAtLatest() {
        // Hors calendrier (été 2026) : jours libres → réveil 10h30, pas de départ.
        val n = next("2026-08-10T12:00")
        assertEquals(at("2026-08-11T10:30"), n.wake)
        assertNull(n.departure)
    }
}
