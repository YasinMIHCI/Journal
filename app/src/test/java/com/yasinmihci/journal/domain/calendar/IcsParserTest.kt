package com.yasinmihci.journal.domain.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/** Fixture synthétique au format ADE (CRLF, lignes repliées, échappements). */
class IcsParserTest {

    private val courses: List<Course> = IcsParser.parse(
        javaClass.classLoader!!.getResource("sample_ade.ics")!!.readText(),
    )
    private val byUid = courses.associateBy { it.uid }
    private val groups = listOf("IT4 FISA", "IT4 FISA - Cyber", "4A")

    @Test
    fun allDayEvents_areSkipped() {
        assertEquals(7, courses.size)
        assertNull(byUid["ADE-7"])
    }

    @Test
    fun utcTimes_convertedToParis_winterAndSummer() {
        val anglais = byUid.getValue("ADE-1")
        assertEquals(LocalDate.of(2026, 11, 16), anglais.date)
        assertEquals(LocalTime.of(8, 15), anglais.startTime) // UTC+1
        assertEquals(LocalTime.of(10, 0), anglais.endTime)
        assertEquals(LocalTime.of(8, 0), byUid.getValue("ADE-6").startTime) // UTC+2
    }

    @Test
    fun tzidTimes() {
        assertEquals(LocalTime.of(10, 15), byUid.getValue("ADE-8").startTime)
    }

    @Test
    fun foldedLines_andEscapes() {
        val secu = byUid.getValue("ADE6032303" + "2362d32")
        assertEquals("TD Sécurité des systèmes", secu.summary)
        assertEquals(listOf("IT4 FISA - Cyber", "PROF GAMMA"), secu.descriptionLines)
        assertEquals("Forum, stands; hall", byUid.getValue("ADE-4").summary)
        assertEquals(listOf("IT4 FISA", "CND4", "PROF ALPHA"), byUid.getValue("ADE-1").descriptionLines)
    }

    @Test
    fun groupFilter_exactLineMatch() {
        assertTrue(byUid.getValue("ADE-1").matches(groups)) // tronc commun
        assertFalse(byUid.getValue("ADE-2").matches(groups)) // IOT seul
        assertTrue(byUid.getValue("ADE6032303" + "2362d32").matches(groups)) // Cyber
        assertTrue(byUid.getValue("ADE-4").matches(groups)) // promo 4A
        assertTrue(byUid.getValue("ADE-5").matches(groups)) // Cyber + IOT
        assertFalse(byUid.getValue("ADE-5").matches(listOf("IT4 FISA"))) // "IT4 FISA" ≠ "IT4 FISA - Cyber"
        assertTrue(byUid.getValue("ADE-1").matches(listOf(" it4 fisa "))) // casse et espaces ignorés
    }

    @Test
    fun firstCourse_ignoresOtherGroups() {
        val kept = courses.filter { it.matches(groups) }
        // Lundi 16/11 : IOT à 7h30 ignoré → premier cours 8h15.
        assertEquals(LocalTime.of(8, 15), firstCourseStart(kept, LocalDate.of(2026, 11, 16)))
        // Travail personnel compté comme un cours.
        assertEquals(LocalTime.of(8, 0), firstCourseStart(kept, LocalDate.of(2026, 9, 1)))
        assertNull(firstCourseStart(kept, LocalDate.of(2026, 11, 19)))
    }

    @Test
    fun notIcs_givesNothing() {
        assertEquals(emptyList<Course>(), IcsParser.parse("<html>login</html>"))
    }
}
