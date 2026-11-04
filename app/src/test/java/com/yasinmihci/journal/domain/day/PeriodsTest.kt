package com.yasinmihci.journal.domain.day

import com.yasinmihci.journal.data.periods.PeriodsRepository
import com.yasinmihci.journal.data.periods.parsePeriods
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.time.LocalDate

/** Vérifie le vrai fichier de l'année 2026-2027. */
class PeriodsTest {

    private val periods: Periods = parsePeriods(
        File("src/main/assets/${PeriodsRepository.ASSET}").readText(),
    )

    private fun type(date: String) = periods.dayType(LocalDate.parse(date))

    @Test
    fun company() {
        assertEquals(DayType.COMPANY, type("2026-11-04"))
        assertEquals(DayType.COMPANY, type("2026-10-05")) // premier jour
        assertEquals(DayType.COMPANY, type("2027-08-31"))
    }

    @Test
    fun school() {
        assertEquals(DayType.SCHOOL, type("2026-08-31"))
        assertEquals(DayType.SCHOOL, type("2026-10-02")) // dernier jour
        assertEquals(DayType.SCHOOL, type("2026-11-16"))
    }

    @Test
    fun darkGreyCells_areSchool() {
        assertEquals(DayType.SCHOOL, type("2027-05-04"))
        assertEquals(DayType.SCHOOL, type("2027-09-02"))
    }

    @Test
    fun weekend_winsOverEverything() {
        assertEquals(DayType.WEEKEND, type("2026-11-07"))
        assertEquals(DayType.WEEKEND, type("2027-05-08")) // samedi férié
    }

    @Test
    fun holidays() {
        assertEquals(DayType.HOLIDAY, type("2026-11-11"))
        assertEquals(DayType.HOLIDAY, type("2027-05-06")) // Ascension, dans une période école
        assertEquals(DayType.HOLIDAY, type("2027-03-29")) // lundi de Pâques
    }

    @Test
    fun outsideCalendar_isNone() {
        assertEquals(DayType.NONE, type("2026-08-28"))
        assertEquals(DayType.NONE, type("2027-09-06"))
    }
}
