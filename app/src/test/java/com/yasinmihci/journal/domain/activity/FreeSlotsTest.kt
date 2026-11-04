package com.yasinmihci.journal.domain.activity

import com.yasinmihci.journal.domain.calendar.Course
import com.yasinmihci.journal.domain.calendar.PARIS
import com.yasinmihci.journal.domain.day.DateRange
import com.yasinmihci.journal.domain.day.Periods
import com.yasinmihci.journal.domain.settings.AppSettings
import com.yasinmihci.journal.domain.settings.CompanyDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class FreeSlotsTest {

    private val settings = AppSettings()
    private val periods = Periods(
        school = listOf(DateRange(LocalDate.of(2026, 11, 16), LocalDate.of(2026, 12, 18))),
        company = listOf(DateRange(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 11, 13))),
        holidays = setOf(LocalDate.of(2026, 11, 11)),
    )

    private fun at(s: String) = LocalDateTime.parse(s).atZone(PARIS)
    private fun slot(a: String, b: String) = Slot(at(a), at(b))
    private fun course(a: String, b: String) =
        Course(a, at(a).toInstant(), at(b).toInstant(), "Cours", "", listOf("IT4 FISA"))

    private fun next(now: String, s: AppSettings = settings, courses: List<Course> = emptyList(), bed: LocalTime? = null) =
        FreeSlots.next(at(now), s, periods, courses, { bed })

    @Test
    fun company_eveningAfterReturn() {
        // Lundi 9 nov. : retour 18h00 → 18h00–20h00.
        assertEquals(slot("2026-11-09T18:00", "2026-11-09T20:00"), next("2026-11-09T12:00"))
    }

    @Test
    fun company_tooShortSlot_skipped() {
        // Retour à 19h45 → 15 min < 30 min : rien lundi, créneau mardi 17h30.
        val s = settings.copy(
            companyDays = AppSettings.DEFAULT_COMPANY_DAYS +
                (DayOfWeek.MONDAY to CompanyDay(LocalTime.of(6, 15), LocalTime.of(7, 5), LocalTime.of(19, 45))),
        )
        assertEquals(slot("2026-11-10T17:30", "2026-11-10T20:00"), next("2026-11-09T12:00", s))
    }

    @Test
    fun alreadyStarted_slotIsSkipped() {
        // 18h30 : le créneau de 18h00 a commencé → prochain = mardi.
        assertEquals(slot("2026-11-10T17:30", "2026-11-10T20:00"), next("2026-11-09T18:30"))
    }

    @Test
    fun school_afterLastCoursePlusTravel() {
        val courses = listOf(
            course("2026-11-16T08:00", "2026-11-16T10:00"),
            course("2026-11-16T13:30", "2026-11-16T17:15"),
        )
        // Pas de créneau le midi (à l'école) ; 17h15 + 30 min → 17h45–20h00.
        assertEquals(slot("2026-11-16T17:45", "2026-11-16T20:00"), next("2026-11-16T06:00", courses = courses))
    }

    @Test
    fun school_lateCourse_morningSlot() {
        // Premier cours 13h30 → départ 13h00 ; réveil 12h10 > 8h00 → créneau 12h10–13h00.
        val courses = listOf(course("2026-11-16T13:30", "2026-11-16T15:15"))
        assertEquals(slot("2026-11-16T12:10", "2026-11-16T13:00"), next("2026-11-16T06:00", courses = courses))
    }

    @Test
    fun school_noCourseKnown_noSlot() {
        // Lundi 16 → vendredi 20 sans cours : aucun créneau ; samedi 21 dès 10h30.
        assertEquals(slot("2026-11-21T10:30", "2026-11-21T20:00"), next("2026-11-16T06:00"))
    }

    @Test
    fun weekend_fromWake() {
        assertEquals(slot("2026-11-07T10:30", "2026-11-07T20:00"), next("2026-11-06T21:00"))
        // Coucher 23h00 → réveil 8h15.
        assertEquals(slot("2026-11-07T08:15", "2026-11-07T20:00"), next("2026-11-06T21:00", bed = LocalTime.of(23, 0)))
    }

    @Test
    fun windowSetting() {
        val s = settings.copy(activityWindowEnd = LocalTime.of(18, 20))
        // Lundi : 18h00–18h20 = 20 min < 30 → mardi 17h30–18h20 (50 min).
        assertEquals(slot("2026-11-10T17:30", "2026-11-10T18:20"), next("2026-11-09T12:00", s))
    }

    @Test
    fun nothingWhenWindowEmpty() {
        assertNull(next("2026-11-09T12:00", settings.copy(activityWindowEnd = LocalTime.of(8, 0))))
    }

    @Test
    fun week_mondayToSunday() {
        assertEquals(LocalDate.of(2026, 11, 2), weekStart(LocalDate.of(2026, 11, 8))) // dimanche
        assertEquals(LocalDate.of(2026, 11, 9), weekStart(LocalDate.of(2026, 11, 9))) // lundi
    }

    @Test
    fun remaining_andPick() {
        assertEquals(45, remainingMinutes(60, 15 * 60 + 59))
        assertEquals(0, remainingMinutes(60, 2 * 3600))
        val r = mapOf("piano" to 45, "dessin" to 90, "fini" to 0)
        assertEquals("dessin", pickActivity(r.keys.toList()) { r.getValue(it) })
        assertNull(pickActivity(listOf("fini")) { r.getValue(it) })
    }
}
