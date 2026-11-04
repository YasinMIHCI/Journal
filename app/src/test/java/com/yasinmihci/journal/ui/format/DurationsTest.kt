package com.yasinmihci.journal.ui.format

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationsTest {
    @Test
    fun formats() {
        assertEquals("0 min", formatDuration(0))
        assertEquals("45 min", formatDuration(45))
        assertEquals("1 h", formatDuration(60))
        assertEquals("1 h 05", formatDuration(65))
        assertEquals("2 h 30", formatDuration(150))
    }
}
