package com.yasinmihci.journal.domain.activity

import com.yasinmihci.journal.domain.blocks.BlockOccurrence
import com.yasinmihci.journal.domain.calendar.Course
import com.yasinmihci.journal.domain.calendar.PARIS
import com.yasinmihci.journal.domain.calendar.firstCourseStart
import com.yasinmihci.journal.domain.day.DayType
import com.yasinmihci.journal.domain.day.Periods
import com.yasinmihci.journal.domain.day.WakePlan
import com.yasinmihci.journal.domain.day.WakePlanner
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
 * - Blocs perso et leurs trajets : occupés.
 */
object FreeSlots {

    fun forDay(
        day: LocalDate,
        plan: WakePlan,
        dayCourses: List<Course>,
        settings: AppSettings,
        blocks: List<BlockOccurrence> = emptyList(),
    ): List<Slot> {
        val wake = when (plan) {
            is WakePlan.Departure -> plan.wake
            is WakePlan.Rest -> plan.wake
        }
        val from = maxOf(settings.activityWindowStart, wake)
        val to = settings.activityWindowEnd
        if (from >= to) return emptyList()

        val busy = mutableListOf<Pair<LocalTime, LocalTime>>()
        if (plan is WakePlan.Departure) {
            when {
                plan.dayType == DayType.COMPANY -> busy += plan.departure to settings.companyDay(day.dayOfWeek).back
                plan.defaultCourse != null -> return emptyList()
                else -> {
                    val lastEnd = dayCourses.filter { it.date == day }.maxOfOrNull { it.end }
                        ?.atZone(PARIS)?.toLocalTime() ?: return emptyList()
                    val back = lastEnd.plusMinutes(settings.schoolTravelMinutes.toLong())
                    busy += plan.departure to (if (back < lastEnd) LocalTime.MAX else back) // retour après minuit
                }
            }
        }
        blocks.forEach { busy += it.start to it.end }

        return subtract(from to to, busy)
            .filter { (s, e) -> Duration.between(s, e).toMinutes() >= settings.minSlotMinutes }
            .map { (s, e) -> Slot(day.atTime(s).atZone(PARIS), day.atTime(e).atZone(PARIS)) }
    }

    /** Intervalle [window] privé des intervalles [busy] (qui peuvent se chevaucher). */
    internal fun subtract(
        window: Pair<LocalTime, LocalTime>,
        busy: List<Pair<LocalTime, LocalTime>>,
    ): List<Pair<LocalTime, LocalTime>> {
        var free = listOf(window)
        for ((bs, be) in busy) {
            free = free.flatMap { (s, e) ->
                if (be <= s || bs >= e) {
                    listOf(s to e)
                } else {
                    listOfNotNull((s to bs).takeIf { s < bs }, (be to e).takeIf { be < e })
                }
            }
        }
        return free
    }

    /** Prochain créneau qui commence strictement après [now], sur [horizonDays] jours. */
    fun next(
        now: ZonedDateTime,
        settings: AppSettings,
        periods: Periods,
        courses: List<Course>,
        bedtimeFor: (LocalDate) -> LocalTime?,
        horizonDays: Int = 7,
        blocksFor: (LocalDate) -> List<BlockOccurrence> = { emptyList() },
    ): Slot? {
        val nowParis = now.withZoneSameInstant(PARIS)
        for (i in 0..horizonDays) {
            val day = nowParis.toLocalDate().plusDays(i.toLong())
            val blocks = blocksFor(day)
            val plan = WakePlanner.plan(
                day, periods.dayType(day), settings, firstCourseStart(courses, day), bedtimeFor(day), blocks,
            )
            forDay(day, plan, courses, settings, blocks).firstOrNull { it.start.isAfter(nowParis) }?.let { return it }
        }
        return null
    }
}
