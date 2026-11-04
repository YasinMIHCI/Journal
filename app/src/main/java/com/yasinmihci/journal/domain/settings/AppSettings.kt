package com.yasinmihci.journal.domain.settings

import java.time.DayOfWeek
import java.time.LocalTime

/** Horaires d'un jour en entreprise. */
data class CompanyDay(
    val wake: LocalTime,
    val departure: LocalTime,
    val back: LocalTime,
)

/** Tous les paramètres modifiables de l'app. Valeurs par défaut = docs/SPEC.md. */
data class AppSettings(
    /** Lundi → vendredi. */
    val companyDays: Map<DayOfWeek, CompanyDay> = DEFAULT_COMPANY_DAYS,
    /** Trajet domicile → école (et retour). */
    val schoolTravelMinutes: Int = 30,
    /** Préparation entre réveil et départ, jours d'école. */
    val schoolPrepMinutes: Int = 50,
    val sleepCycleMinutes: Int = 90,
    val fallAsleepMinutes: Int = 15,
    /** Nombre max de cycles de sommeil (6 × 1 h 30 = 9 h). */
    val maxSleepCycles: Int = 6,
    val weekendLatestWake: LocalTime = LocalTime.of(10, 30),
    val icsUrl: String = "",
    /** Groupes ADE gardés : une ligne de DESCRIPTION doit être exactement l'un d'eux. */
    val icsGroups: List<String> = listOf("IT4 FISA", "IT4 FISA - Cyber", "4A"),
    val syncTime: LocalTime = LocalTime.of(6, 0),
    /** Fenêtre de recherche des créneaux libres pour les activités. */
    val activityWindowStart: LocalTime = LocalTime.of(8, 0),
    val activityWindowEnd: LocalTime = LocalTime.of(20, 0),
    val minSlotMinutes: Int = 30,
) {
    fun companyDay(day: DayOfWeek): CompanyDay =
        companyDays[day] ?: DEFAULT_COMPANY_DAYS.getValue(day)

    companion object {
        val WORK_DAYS: List<DayOfWeek> = listOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
        )

        private val LONG = CompanyDay(LocalTime.of(6, 15), LocalTime.of(7, 5), LocalTime.of(18, 0))
        private val SHORT = CompanyDay(LocalTime.of(6, 45), LocalTime.of(7, 40), LocalTime.of(17, 30))

        val DEFAULT_COMPANY_DAYS: Map<DayOfWeek, CompanyDay> = mapOf(
            DayOfWeek.MONDAY to LONG,
            DayOfWeek.TUESDAY to SHORT,
            DayOfWeek.WEDNESDAY to LONG,
            DayOfWeek.THURSDAY to SHORT,
            DayOfWeek.FRIDAY to SHORT,
        )
    }
}
