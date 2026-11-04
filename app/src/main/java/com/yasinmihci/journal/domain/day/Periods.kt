package com.yasinmihci.journal.domain.day

import java.time.DayOfWeek
import java.time.LocalDate

enum class DayType { COMPANY, SCHOOL, WEEKEND, HOLIDAY, NONE }

/** Intervalle de dates, bornes incluses. */
data class DateRange(val start: LocalDate, val end: LocalDate) {
    operator fun contains(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(end)
}

/** Calendrier de l'alternance (école / entreprise / fériés). */
data class Periods(
    val school: List<DateRange>,
    val company: List<DateRange>,
    val holidays: Set<LocalDate>,
) {
    /**
     * Priorité : week-end > férié > école > entreprise.
     * Jour hors de toute période (vacances, hors année) → [DayType.NONE].
     */
    fun dayType(date: LocalDate): DayType = when {
        date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY -> DayType.WEEKEND
        date in holidays -> DayType.HOLIDAY
        school.any { date in it } -> DayType.SCHOOL
        company.any { date in it } -> DayType.COMPANY
        else -> DayType.NONE
    }

    companion object {
        val EMPTY = Periods(emptyList(), emptyList(), emptySet())
    }
}
