package com.yasinmihci.journal.domain.week

import com.yasinmihci.journal.domain.blocks.Block
import com.yasinmihci.journal.domain.blocks.BlockSchedule
import com.yasinmihci.journal.domain.blocks.Tag
import com.yasinmihci.journal.domain.calendar.Course
import com.yasinmihci.journal.domain.calendar.PARIS
import com.yasinmihci.journal.domain.day.DateRange
import com.yasinmihci.journal.domain.day.Periods
import com.yasinmihci.journal.domain.settings.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class DayAgendaTest {
    private fun t(h: Int, m: Int = 0) = LocalTime.of(h, m)
    private val periods = Periods(
        school = listOf(DateRange(LocalDate.of(2026, 11, 16), LocalDate.of(2026, 12, 18))),
        company = listOf(DateRange(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 11, 13))),
        holidays = emptySet(),
    )
    private val volley = Tag(1, "Volley", 0, 20)
    private val blocks = BlockSchedule(
        listOf(volley),
        listOf(Block(1, 1, "", t(19), t(21), weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY))),
    )

    private fun course(a: String, b: String) = Course(
        a, LocalDateTime.parse(a).atZone(PARIS).toInstant(), LocalDateTime.parse(b).atZone(PARIS).toInstant(),
        "TD", "GA129", listOf("IT4 FISA"),
    )

    private fun summary(a: DayAgenda) = a.items.map { Triple(it.kind, it.start, it.end) }

    @Test
    fun companyDay_withVolley() {
        val a = buildDayAgenda(LocalDate.of(2026, 11, 10), AppSettings(), periods, emptyList(), blocks, null)
        assertEquals(t(6, 45), a.wake)
        assertEquals(
            listOf(
                Triple(AgendaKind.COMPANY, t(7, 40), t(17, 30)),
                Triple(AgendaKind.BLOCK_TRAVEL, t(18, 40), t(19)),
                Triple(AgendaKind.BLOCK, t(19), t(21)),
                Triple(AgendaKind.BLOCK_TRAVEL, t(21), t(21, 20)),
            ),
            summary(a),
        )
    }

    @Test
    fun schoolDay_coursesAndTravel() {
        val courses = listOf(course("2026-11-16T08:00", "2026-11-16T10:00"), course("2026-11-16T13:30", "2026-11-16T15:15"))
        val a = buildDayAgenda(LocalDate.of(2026, 11, 16), AppSettings(), periods, courses, BlockSchedule.EMPTY, null)
        assertEquals(
            listOf(
                Triple(AgendaKind.SCHOOL_TRAVEL, t(7, 30), t(8)),
                Triple(AgendaKind.COURSE, t(8), t(10)),
                Triple(AgendaKind.COURSE, t(13, 30), t(15, 15)),
                Triple(AgendaKind.SCHOOL_TRAVEL, t(15, 15), t(15, 45)),
            ),
            summary(a),
        )
        assertEquals(t(6, 40), a.wake)
    }
}
