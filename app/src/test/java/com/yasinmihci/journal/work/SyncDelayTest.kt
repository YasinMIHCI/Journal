package com.yasinmihci.journal.work

import com.yasinmihci.journal.domain.calendar.PARIS
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

class SyncDelayTest {
    private fun at(s: String) = LocalDateTime.parse(s).atZone(PARIS)

    @Test
    fun beforeSyncTime_sameDay() {
        assertEquals(Duration.ofMinutes(90), CalendarSyncWorker.delayUntil(at("2026-11-04T04:30"), LocalTime.of(6, 0)))
    }

    @Test
    fun afterSyncTime_nextDay() {
        assertEquals(Duration.ofHours(13), CalendarSyncWorker.delayUntil(at("2026-11-04T17:00"), LocalTime.of(6, 0)))
    }

    @Test
    fun exactlyAtSyncTime_nextDay() {
        assertEquals(Duration.ofDays(1), CalendarSyncWorker.delayUntil(at("2026-11-04T06:00"), LocalTime.of(6, 0)))
    }

    @Test
    fun dstChange_wallClock() {
        // Nuit du 24 au 25 octobre 2026 : 3h → 2h. 6h locale reste 6h locale.
        assertEquals(Duration.ofHours(8), CalendarSyncWorker.delayUntil(at("2026-10-24T23:00"), LocalTime.of(6, 0)))
    }
}
