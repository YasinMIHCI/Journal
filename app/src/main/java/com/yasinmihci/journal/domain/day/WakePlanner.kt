package com.yasinmihci.journal.domain.day

import com.yasinmihci.journal.domain.blocks.BlockOccurrence
import com.yasinmihci.journal.domain.blocks.minusClamped
import com.yasinmihci.journal.domain.settings.AppSettings
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Plan du matin pour une date. */
sealed interface WakePlan {
    val dayType: DayType

    /** Premier bloc perso (ou son trajet) qui a avancé le réveil, sinon null. */
    val earlyBlock: BlockOccurrence?

    /** Jour contraint : entreprise, ou école (cours connus ou heure par défaut). */
    data class Departure(
        override val dayType: DayType,
        val wake: LocalTime,
        val departure: LocalTime,
        /** École sans cours connu : calculé sur [AppSettings.schoolDefaultFirstCourse]. */
        val defaultCourse: LocalTime? = null,
        override val earlyBlock: BlockOccurrence? = null,
    ) : WakePlan

    /** Jour libre (week-end, férié, hors calendrier) : réveil par cycles. */
    data class Rest(
        override val dayType: DayType,
        /** Coucher de la veille au soir (ou après minuit) ; null si pas encore saisi. */
        val bedtime: LocalTime?,
        /** Réveil calculé ; si [bedtime] est null, vaut le réveil au plus tard. */
        val wake: LocalTime,
        /** Nombre de cycles complets ; null si [bedtime] est null. */
        val cycles: Int?,
        override val earlyBlock: BlockOccurrence? = null,
    ) : WakePlan
}

object WakePlanner {

    /**
     * @param firstCourse début du premier cours de la journée (ICS), null si aucun.
     * @param bedtime heure de coucher de la nuit précédant [date], null si non saisie.
     * @param blocks blocs perso du jour, trajets compris : le réveil doit laisser le temps de
     *   préparation ([AppSettings.schoolPrepMinutes]) avant le premier.
     */
    fun plan(
        date: LocalDate,
        dayType: DayType,
        settings: AppSettings,
        firstCourse: LocalTime?,
        bedtime: LocalTime?,
        blocks: List<BlockOccurrence> = emptyList(),
    ): WakePlan {
        val first = blocks.minByOrNull { it.start }
        val blockLatest = first?.let { minusClamped(it.start, settings.schoolPrepMinutes) }
        return when {
            dayType == DayType.COMPANY -> {
                val c = settings.companyDay(date.dayOfWeek)
                departure(dayType, c.wake, c.departure, null, first, blockLatest)
            }
            dayType == DayType.SCHOOL -> {
                // Sans cours connu, on suppose un cours à l'heure par défaut : mieux vaut un réveil
                // inutile qu'un cours raté si la synchro a échoué.
                val course = firstCourse ?: settings.schoolDefaultFirstCourse
                val departure = course.minusMinutes(settings.schoolTravelMinutes.toLong())
                val wake = departure.minusMinutes(settings.schoolPrepMinutes.toLong())
                departure(dayType, wake, departure, course.takeIf { firstCourse == null }, first, blockLatest)
            }
            else -> rest(date, dayType, settings, bedtime, first, blockLatest)
        }
    }

    private fun departure(
        dayType: DayType,
        wake: LocalTime,
        departure: LocalTime,
        defaultCourse: LocalTime?,
        first: BlockOccurrence?,
        blockLatest: LocalTime?,
    ): WakePlan.Departure =
        if (blockLatest != null && blockLatest < wake) {
            WakePlan.Departure(dayType, blockLatest, departure, defaultCourse, first)
        } else {
            WakePlan.Departure(dayType, wake, departure, defaultCourse)
        }

    private fun rest(
        date: LocalDate,
        dayType: DayType,
        settings: AppSettings,
        bedtime: LocalTime?,
        first: BlockOccurrence?,
        blockLatest: LocalTime?,
    ): WakePlan.Rest {
        // Un bloc tôt abaisse le « réveil au plus tard » ; les cycles sont recalculés dessous.
        val early = blockLatest != null && blockLatest < settings.weekendLatestWake
        val latest = if (early) blockLatest!! else settings.weekendLatestWake
        val cause = first.takeIf { early }
        if (bedtime == null) return WakePlan.Rest(dayType, null, latest, null, cause)
        val (wake, cycles) = cycleWake(date, bedtime, settings, latest)
        return WakePlan.Rest(dayType, bedtime, wake, cycles, cause)
    }

    /**
     * Réveil = coucher + endormissement + n cycles, avec n le plus grand possible
     * sans dépasser [latest] ni [AppSettings.maxSleepCycles].
     * Coucher à partir de midi = veille au soir ; avant midi = après minuit.
     * Moins d'un cycle possible → [latest], 0 cycle.
     */
    fun cycleWake(
        date: LocalDate,
        bedtime: LocalTime,
        settings: AppSettings,
        latest: LocalTime = settings.weekendLatestWake,
    ): Pair<LocalTime, Int> {
        val bedDay = if (bedtime >= NOON) date.minusDays(1) else date
        val sleepStart = LocalDateTime.of(bedDay, bedtime).plusMinutes(settings.fallAsleepMinutes.toLong())
        val latestAt = LocalDateTime.of(date, latest)
        val cycle = settings.sleepCycleMinutes.coerceAtLeast(1).toLong()
        val available = Duration.between(sleepStart, latestAt).toMinutes()
        val cycles = if (available <= 0) 0 else (available / cycle).toInt().coerceAtMost(settings.maxSleepCycles)
        if (cycles <= 0) return latest to 0
        return sleepStart.plusMinutes(cycles * cycle).toLocalTime() to cycles
    }

    private val NOON: LocalTime = LocalTime.NOON
}
