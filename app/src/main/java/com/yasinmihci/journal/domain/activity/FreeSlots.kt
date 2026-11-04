package com.yasinmihci.journal.domain.activity

import com.yasinmihci.journal.domain.calendar.Course
import com.yasinmihci.journal.domain.calendar.PARIS
import com.yasinmihci.journal.domain.day.DayType
import com.yasinmihci.journal.domain.day.Periods
import com.yasinmihci.journal.domain.day.WakePlan
import com.yasinmihci.journal.domain.day.WakePlanner
import com.yasinmihci.journal.domain.calendar.firstCourseStart
import com.yasinmihci.journal.domain.settings.AppSettings
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/** Créneau libre (heure de Paris). */
data class Slot(val start: ZonedDateTime, val end: ZonedDateTime) {
    val minutes: Long get() = Duration.between(start, end).toMinutes()
}

/**
 * Créneaux libres pour les activités, dans la fenêtre [AppSettings.activityWindowStart]–[AppSettings.activityWindowEnd],
 * jamais avant le réveil.
 * - Entreprise : occupé du départ au retour.
 * - École : occupé du départ jusqu'à fin du dernier cours + trajet (on est à l'école entre deux cours).
 *   Sans cours connu : toute la journée occupée (prudence).
 * - Jour libre : libre dès le réveil.
 */
object FreeSlots {

    fun forDay(day: LocalDate, plan: WakePlan, dayCourses: List<Course>, settings: AppSettings): List<Slot> {
        val wake = when (plan) {
            is WakePlan.Departure -> plan.wake
            is WakePlan.Rest -> plan.wake
        }
        val from = maxOf(settings.activityWindowStart, wake)
        val to = settings.activityWindowEnd
        if (from >= to) return emptyList()

        val busy: Pair<LocalTime, LocalTime>? = when {
            plan !is WakePlan.Departure -> null
            plan.dayType == DayType.COMPANY -> plan.departure to settings.companyDay(day.dayOfWeek).back
            plan.defaultCourse != null -> return emptyList()
            else -> {
                val lastEnd = dayCourses.filter { it.date == day }.maxOfOrNull { it.end }
                    ?.atZone(PARIS)?.toLocalTime() ?: return emptyList()
                val back = lastEnd.plusMinutes(settings.schoolTravelMinutes.toLong())
                plan.departure to (if (back < lastEnd) LocalTime.MAX else back) // retour après minuit
            }
        }

        val free = mutableListOf<Pair<LocalTime, LocalTime>>()
        if (busy == null) {
            free += from to to
        } else {
            val (bStart, bEnd) = busy
            if (from < bStart) free += from to minOf(bStart, to)
            if (bEnd < to) free += maxOf(bEnd, from) to to
        }
        return free
            .filter { (s, e) -> Duration.between(s, e).toMinutes() >= settings.minSlotMinutes }
            .map { (s, e) -> Slot(day.atTime(s).atZone(PARIS), day.atTime(e).atZone(PARIS)) }
    }

    /** Prochain créneau qui commence strictement après [now], sur [horizonDays] jours. */
    fun next(
        now: ZonedDateTime,
        settings: AppSettings,
        periods: Periods,
        courses: List<Course>,
        bedtimeFor: (LocalDate) -> LocalTime?,
        horizonDays: Int = 7,
    ): Slot? {
        val nowParis = now.withZoneSameInstant(PARIS)
        for (i in 0..horizonDays) {
            val day = nowParis.toLocalDate().plusDays(i.toLong())
            val plan = WakePlanner.plan(day, periods.dayType(day), settings, firstCourseStart(courses, day), bedtimeFor(day))
            forDay(day, plan, courses, settings).firstOrNull { it.start.isAfter(nowParis) }?.let { return it }
        }
        return null
    }
}
