package com.yasinmihci.journal.domain.blocks

import com.yasinmihci.journal.domain.activity.FreeSlots
import com.yasinmihci.journal.domain.calendar.PARIS
import com.yasinmihci.journal.domain.day.DateRange
import com.yasinmihci.journal.domain.day.DayType
import com.yasinmihci.journal.domain.day.Periods
import com.yasinmihci.journal.domain.day.WakePlan
import com.yasinmihci.journal.domain.day.WakePlanner
import com.yasinmihci.journal.domain.settings.AppSettings
import com.yasinmihci.journal.domain.week.layoutLanes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class BlocksTest {

    private fun t(h: Int, m: Int = 0) = LocalTime.of(h, m)
    private val volley = Tag(1, "Volley", 0xFF00AA00.toInt(), travelMinutes = 20)
    private val noTravel = Tag(2, "Perso", 0xFF0000AA.toInt(), travelMinutes = 0)
    private val tuesdayVolley = Block(
        id = 10, tagId = 1, title = "", start = t(19), end = t(21),
        weekdays = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
        from = LocalDate.of(2026, 11, 1), until = LocalDate.of(2026, 12, 31),
    )
    private val schedule = BlockSchedule(listOf(volley, noTravel), listOf(tuesdayVolley))
    private val settings = AppSettings()

    @Test
    fun permanent_weekdaysAndDates() {
        assertTrue(tuesdayVolley.occursOn(LocalDate.of(2026, 11, 10))) // mardi
        assertTrue(tuesdayVolley.occursOn(LocalDate.of(2026, 11, 12))) // jeudi
        assertFalse(tuesdayVolley.occursOn(LocalDate.of(2026, 11, 11))) // mercredi
        assertFalse(tuesdayVolley.occursOn(LocalDate.of(2026, 10, 27))) // avant le début
        assertFalse(tuesdayVolley.occursOn(LocalDate.of(2027, 1, 5))) // après la fin
    }

    @Test
    fun oneOff_onlyThatDay() {
        val b = Block(11, 2, "Dentiste", t(9), t(10), date = LocalDate.of(2026, 11, 7))
        assertTrue(b.occursOn(LocalDate.of(2026, 11, 7)))
        assertFalse(b.occursOn(LocalDate.of(2026, 11, 14)))
    }

    @Test
    fun travelBeforeAndAfter() {
        val occ = schedule.on(LocalDate.of(2026, 11, 10))
        assertEquals(
            listOf(
                Triple(t(18, 40), t(19), OccurrenceKind.TRAVEL),
                Triple(t(19), t(21), OccurrenceKind.BLOCK),
                Triple(t(21), t(21, 20), OccurrenceKind.TRAVEL),
            ),
            occ.map { Triple(it.start, it.end, it.kind) },
        )
        assertEquals("Volley", occ[1].title) // titre vide → nom du tag
    }

    @Test
    fun travelClampedToDay() {
        val s = BlockSchedule(listOf(volley), listOf(Block(1, 1, "", t(0, 10), t(23, 50), date = LocalDate.of(2026, 11, 7))))
        val occ = s.on(LocalDate.of(2026, 11, 7))
        assertEquals(LocalTime.MIDNIGHT, occ.first().start)
        assertEquals(t(23, 59), occ.last().end)
    }

    @Test
    fun earlyBlock_movesRestWakeAndCycles() {
        // Samedi : volley 9h00, trajet 20 → 8h40, préparation 50 → au plus tard 7h50.
        val sat = LocalDate.of(2026, 11, 7)
        val s = BlockSchedule(listOf(volley), listOf(Block(1, 1, "", t(9), t(11), date = sat)))
        val blocks = s.on(sat)
        val noBed = WakePlanner.plan(sat, DayType.WEEKEND, settings, null, null, blocks)
        assertEquals(t(7, 50), (noBed as WakePlan.Rest).wake)
        assertEquals(t(8, 40), noBed.earlyBlock?.start)
        // Coucher 23h00 → 23h15 + 5 cycles = 6h45 (6 cycles = 8h15 > 7h50).
        val bed = WakePlanner.plan(sat, DayType.WEEKEND, settings, null, t(23, 0), blocks) as WakePlan.Rest
        assertEquals(t(6, 45), bed.wake)
        assertEquals(5, bed.cycles)
    }

    @Test
    fun lateBlock_doesNotChangeWake() {
        val sat = LocalDate.of(2026, 11, 7)
        val s = BlockSchedule(listOf(volley), listOf(Block(1, 1, "", t(15), t(17), date = sat)))
        val plan = WakePlanner.plan(sat, DayType.WEEKEND, settings, null, null, s.on(sat)) as WakePlan.Rest
        assertEquals(t(10, 30), plan.wake)
        assertNull(plan.earlyBlock)
    }

    @Test
    fun earlyBlock_onCompanyDay_wakeEarlierDepartureSame() {
        // Lundi entreprise 6h15/7h05 ; sport 6h30 sans trajet → réveil 5h40.
        val mon = LocalDate.of(2026, 11, 9)
        val s = BlockSchedule(listOf(noTravel), listOf(Block(1, 2, "Sport", t(6, 30), t(7), date = mon)))
        val plan = WakePlanner.plan(mon, DayType.COMPANY, settings, null, null, s.on(mon)) as WakePlan.Departure
        assertEquals(t(5, 40), plan.wake)
        assertEquals(t(7, 5), plan.departure)
    }

    @Test
    fun freeSlots_excludeBlocksAndTravel() {
        // Mardi 10 nov. entreprise : retour 17h30 → libre 17h30–20h ; volley 18h40–21h20 avec trajets.
        val periods = Periods(emptyList(), listOf(DateRange(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 11, 13))), emptySet())
        val now = LocalDateTime.of(2026, 11, 10, 12, 0).atZone(PARIS)
        val slot = FreeSlots.next(now, settings, periods, emptyList(), { null }, blocksFor = schedule::on)!!
        assertEquals(LocalDateTime.of(2026, 11, 10, 17, 30).atZone(PARIS), slot.start)
        assertEquals(LocalDateTime.of(2026, 11, 10, 18, 40).atZone(PARIS), slot.end)
    }

    @Test
    fun subtract_overlappingBusy() {
        val free = FreeSlots.subtract(t(8) to t(20), listOf(t(9) to t(12), t(11) to t(13), t(19) to t(21)))
        assertEquals(listOf(t(8) to t(9), t(13) to t(19)), free)
    }

    @Test
    fun lanes_overlapSideBySide() {
        data class E(val s: LocalTime, val e: LocalTime)
        val a = E(t(8), t(10))
        val b = E(t(9), t(11))
        val c = E(t(10), t(12)) // réutilise la colonne de a
        val d = E(t(13), t(14)) // groupe séparé
        val placed = layoutLanes(listOf(d, c, b, a), { it.s }, { it.e }).associateBy { it.item }
        assertEquals(0 to 2, placed.getValue(a).let { it.lane to it.lanes })
        assertEquals(1 to 2, placed.getValue(b).let { it.lane to it.lanes })
        assertEquals(0 to 2, placed.getValue(c).let { it.lane to it.lanes })
        assertEquals(0 to 1, placed.getValue(d).let { it.lane to it.lanes })
    }
}
