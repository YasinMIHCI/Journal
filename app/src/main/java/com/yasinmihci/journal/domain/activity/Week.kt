package com.yasinmihci.journal.domain.activity

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Semaine du lundi au dimanche. */
fun weekStart(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/** Minutes restantes pour atteindre l'objectif hebdo (jamais négatif). Minutes entamées non comptées. */
fun remainingMinutes(goalMinutes: Int, doneSeconds: Long): Int =
    (goalMinutes - (doneSeconds / 60).toInt()).coerceAtLeast(0)

/** Activité à proposer : celle à qui il reste le plus de temps. Null si tous les objectifs sont atteints. */
fun <T> pickActivity(items: List<T>, remaining: (T) -> Int): T? =
    items.filter { remaining(it) > 0 }.maxByOrNull(remaining)
