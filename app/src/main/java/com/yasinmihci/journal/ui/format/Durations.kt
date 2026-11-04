package com.yasinmihci.journal.ui.format

/** 45 → "45 min", 60 → "1 h", 90 → "1 h 30". */
fun formatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "$m min"
        m == 0 -> "$h h"
        else -> "$h h ${m.toString().padStart(2, '0')}"
    }
}

/** 6:15 → "6h15", 18:00 → "18h00". */
fun formatTime(time: java.time.LocalTime): String =
    "${time.hour}h${time.minute.toString().padStart(2, '0')}"
