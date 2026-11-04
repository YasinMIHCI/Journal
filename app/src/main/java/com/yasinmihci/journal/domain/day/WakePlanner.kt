package com.yasinmihci.journal.domain.day

import com.yasinmihci.journal.domain.settings.AppSettings
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Plan du matin pour une date. */
sealed interface WakePlan {
    val dayType: DayType

    /** Jour contraint : entreprise, ou école avec au moins un cours. */
    data class Departure(
        override val dayType: DayType,
        val wake: LocalTime,
        val departure: LocalTime,
    ) : WakePlan

    /** Jour libre (week-end, férié, école sans cours, hors calendrier) : réveil par cycles. */
    data class Rest(
        override val dayType: DayType,
        /** Coucher de la veille au soir (ou après minuit) ; null si pas encore saisi. */
        val bedtime: LocalTime?,
        /** Réveil calculé ; si [bedtime] est null, vaut le réveil au plus tard. */
        val wake: LocalTime,
        /** Nombre de cycles complets ; null si [bedtime] est null. */
        val cycles: Int?,
    ) : WakePlan
}

object WakePlanner {

    /**
     * @param firstCourse début du premier cours de la journée (ICS), null si aucun.
     * @param bedtime heure de coucher de la nuit précédant [date], null si non saisie.
     */
    fun plan(
        date: LocalDate,
        dayType: DayType,
        settings: AppSettings,
        firstCourse: LocalTime?,
        bedtime: LocalTime?,
    ): WakePlan = when {
        dayType == DayType.COMPANY -> {
            val c = settings.companyDay(date.dayOfWeek)
            WakePlan.Departure(dayType, c.wake, c.departure)
        }
        dayType == DayType.SCHOOL && firstCourse != null -> {
            val departure = firstCourse.minusMinutes(settings.schoolTravelMinutes.toLong())
            val wake = departure.minusMinutes(settings.schoolPrepMinutes.toLong())
            WakePlan.Departure(dayType, wake, departure)
        }
        else -> rest(date, dayType, settings, bedtime)
    }

    private fun rest(date: LocalDate, dayType: DayType, settings: AppSettings, bedtime: LocalTime?): WakePlan.Rest {
        if (bedtime == null) return WakePlan.Rest(dayType, null, settings.weekendLatestWake, null)
        val (wake, cycles) = cycleWake(date, bedtime, settings)
        return WakePlan.Rest(dayType, bedtime, wake, cycles)
    }

    /**
     * Réveil = coucher + endormissement + n cycles, avec n le plus grand possible
     * sans dépasser le réveil au plus tard ni [AppSettings.maxSleepCycles].
     * Coucher à partir de midi = veille au soir ; avant midi = après minuit.
     * Moins d'un cycle possible → réveil au plus tard, 0 cycle.
     */
    fun cycleWake(date: LocalDate, bedtime: LocalTime, settings: AppSettings): Pair<LocalTime, Int> {
        val bedDay = if (bedtime >= NOON) date.minusDays(1) else date
        val sleepStart = LocalDateTime.of(bedDay, bedtime).plusMinutes(settings.fallAsleepMinutes.toLong())
        val latest = LocalDateTime.of(date, settings.weekendLatestWake)
        val cycle = settings.sleepCycleMinutes.coerceAtLeast(1).toLong()
        val available = Duration.between(sleepStart, latest).toMinutes()
        val cycles = if (available <= 0) 0 else (available / cycle).toInt().coerceAtMost(settings.maxSleepCycles)
        if (cycles <= 0) return settings.weekendLatestWake to 0
        return sleepStart.plusMinutes(cycles * cycle).toLocalTime() to cycles
    }

    private val NOON: LocalTime = LocalTime.NOON
}
