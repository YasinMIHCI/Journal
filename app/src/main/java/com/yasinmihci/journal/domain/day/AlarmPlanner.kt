package com.yasinmihci.journal.domain.day

import com.yasinmihci.journal.domain.blocks.BlockOccurrence
import com.yasinmihci.journal.domain.calendar.Course
import com.yasinmihci.journal.domain.calendar.PARIS
import com.yasinmihci.journal.domain.calendar.firstCourseStart
import com.yasinmihci.journal.domain.settings.AppSettings
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/** Prochaines alarmes à programmer (heure de Paris). Null = rien dans l'horizon. */
data class NextAlarms(
    val wake: ZonedDateTime?,
    val departure: ZonedDateTime?,
    /** Prochain créneau libre pour les activités. */
    val slot: com.yasinmihci.journal.domain.activity.Slot? = null,
)

object AlarmPlanner {

    /**
     * Premier réveil et premier départ strictement après [now], en parcourant
     * aujourd'hui + [horizonDays] jours. Réveil et départ sont indépendants : après
     * le réveil de 6h15, le départ de 7h05 du même jour reste à venir.
     *
     * @param courses cours déjà filtrés par groupe, couvrant l'horizon.
     * @param bedtimeFor heure de coucher saisie pour la nuit qui précède une date.
     */
    fun next(
        now: ZonedDateTime,
        settings: AppSettings,
        periods: Periods,
        courses: List<Course>,
        bedtimeFor: (LocalDate) -> LocalTime?,
        horizonDays: Int = 8,
        blocksFor: (LocalDate) -> List<BlockOccurrence> = { emptyList() },
    ): NextAlarms {
        val nowParis = now.withZoneSameInstant(PARIS)
        val today = nowParis.toLocalDate()
        var wake: ZonedDateTime? = null
        var departure: ZonedDateTime? = null
        for (i in 0..horizonDays) {
            val day = today.plusDays(i.toLong())
            val plan = WakePlanner.plan(
                day, periods.dayType(day), settings, firstCourseStart(courses, day), bedtimeFor(day), blocksFor(day),
            )
            val wakeTime = when (plan) {
                is WakePlan.Departure -> plan.wake
                is WakePlan.Rest -> plan.wake
            }
            val w = day.atTime(wakeTime).atZone(PARIS)
            if (wake == null && w.isAfter(nowParis)) wake = w
            if (departure == null && plan is WakePlan.Departure) {
                val d = day.atTime(plan.departure).atZone(PARIS)
                if (d.isAfter(nowParis)) departure = d
            }
            if (wake != null && departure != null) break
        }
        return NextAlarms(wake, departure)
    }
}
