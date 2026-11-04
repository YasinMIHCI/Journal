package com.yasinmihci.journal.domain.calendar

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

val PARIS: ZoneId = ZoneId.of("Europe/Paris")

/** Un cours ADE. [descriptionLines] = groupes + enseignants (une ligne chacun). */
data class Course(
    val uid: String,
    val start: Instant,
    val end: Instant,
    val summary: String,
    val location: String,
    val descriptionLines: List<String>,
) {
    val date: LocalDate get() = start.atZone(PARIS).toLocalDate()
    val startTime: LocalTime get() = start.atZone(PARIS).toLocalTime()
    val endTime: LocalTime get() = end.atZone(PARIS).toLocalTime()

    /** Gardé si une ligne de la description est exactement un des [groups] (casse ignorée). */
    fun matches(groups: List<String>): Boolean = descriptionLines.any { line ->
        groups.any { it.trim().equals(line.trim(), ignoreCase = true) }
    }
}

/** Début du premier cours de [date] parmi [courses], ou null. */
fun firstCourseStart(courses: List<Course>, date: LocalDate): LocalTime? =
    courses.filter { it.date == date }.minByOrNull { it.start }?.startTime
