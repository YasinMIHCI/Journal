package com.yasinmihci.journal.domain.week

import com.yasinmihci.journal.domain.blocks.BlockSchedule
import com.yasinmihci.journal.domain.blocks.OccurrenceKind
import com.yasinmihci.journal.domain.blocks.Tag
import com.yasinmihci.journal.domain.blocks.plusClamped
import com.yasinmihci.journal.domain.calendar.Course
import com.yasinmihci.journal.domain.calendar.firstCourseStart
import com.yasinmihci.journal.domain.day.DayType
import com.yasinmihci.journal.domain.day.Periods
import com.yasinmihci.journal.domain.day.WakePlan
import com.yasinmihci.journal.domain.day.WakePlanner
import com.yasinmihci.journal.domain.settings.AppSettings
import java.time.LocalDate
import java.time.LocalTime

enum class AgendaKind { COURSE, SCHOOL_TRAVEL, COMPANY, BLOCK, BLOCK_TRAVEL }

/** Un rectangle du calendrier. */
data class AgendaItem(
    val start: LocalTime,
    val end: LocalTime,
    val kind: AgendaKind,
    val title: String,
    /** Salle (cours). */
    val detail: String = "",
    /** Tag des blocs perso (couleur). */
    val tag: Tag? = null,
    /** Bloc perso d'origine (édition). */
    val blockId: Long? = null,
)

data class DayAgenda(
    val date: LocalDate,
    val dayType: DayType,
    val wake: LocalTime,
    val items: List<AgendaItem>,
)

/** Construit la journée affichée dans la vue semaine (même règles que le réveil et les créneaux). */
fun buildDayAgenda(
    date: LocalDate,
    settings: AppSettings,
    periods: Periods,
    courses: List<Course>,
    blocks: BlockSchedule,
    bedtime: LocalTime?,
): DayAgenda {
    val dayType = periods.dayType(date)
    val dayCourses = courses.filter { it.date == date }.sortedBy { it.start }
    val occurrences = blocks.on(date)
    val plan = WakePlanner.plan(date, dayType, settings, firstCourseStart(courses, date), bedtime, occurrences)
    val items = mutableListOf<AgendaItem>()

    if (plan is WakePlan.Departure && dayType == DayType.COMPANY) {
        items += AgendaItem(plan.departure, settings.companyDay(date.dayOfWeek).back, AgendaKind.COMPANY, "")
    }
    if (dayType == DayType.SCHOOL && dayCourses.isNotEmpty()) {
        val first = dayCourses.first().startTime
        val last = dayCourses.maxOf { it.endTime }
        if (plan is WakePlan.Departure && plan.departure < first) {
            items += AgendaItem(plan.departure, first, AgendaKind.SCHOOL_TRAVEL, "")
        }
        items += AgendaItem(last, plusClamped(last, settings.schoolTravelMinutes), AgendaKind.SCHOOL_TRAVEL, "")
    }
    dayCourses.forEach { c ->
        items += AgendaItem(c.startTime, c.endTime, AgendaKind.COURSE, c.summary, c.location)
    }
    occurrences.forEach { o ->
        val kind = if (o.kind == OccurrenceKind.BLOCK) AgendaKind.BLOCK else AgendaKind.BLOCK_TRAVEL
        items += AgendaItem(o.start, o.end, kind, o.title, tag = o.tag, blockId = o.blockId)
    }
    val wake = when (plan) {
        is WakePlan.Departure -> plan.wake
        is WakePlan.Rest -> plan.wake
    }
    return DayAgenda(date, dayType, wake, items.filter { it.end > it.start }.sortedBy { it.start })
}
