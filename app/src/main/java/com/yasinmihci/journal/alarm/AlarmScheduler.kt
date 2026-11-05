package com.yasinmihci.journal.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.yasinmihci.journal.MainActivity
import com.yasinmihci.journal.data.calendar.CalendarRepository
import com.yasinmihci.journal.data.periods.PeriodsRepository
import com.yasinmihci.journal.data.repository.BlockRepository
import com.yasinmihci.journal.data.settings.BedtimeRepository
import com.yasinmihci.journal.data.settings.SettingsRepository
import com.yasinmihci.journal.domain.calendar.PARIS
import com.yasinmihci.journal.domain.activity.FreeSlots
import com.yasinmihci.journal.domain.day.AlarmPlanner
import com.yasinmihci.journal.domain.day.NextAlarms
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.ZonedDateTime

/**
 * Programme le prochain réveil (AlarmManager.setAlarmClock : exact, traverse Doze,
 * affiché par le système) et le prochain départ (alarme exacte).
 * Une seule alarme de chaque type à la fois ; recalculée à chaque changement.
 */
class AlarmScheduler(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val bedtimeRepository: BedtimeRepository,
    private val periodsRepository: PeriodsRepository,
    private val calendarRepository: CalendarRepository,
    private val blockRepository: BlockRepository,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val mutex = Mutex()

    private val _next = MutableStateFlow<NextAlarms?>(null)

    /** Alarmes actuellement programmées (null tant que pas calculé ; champs null = désactivé/aucune). */
    val next: StateFlow<NextAlarms?> = _next

    suspend fun reschedule(now: ZonedDateTime = ZonedDateTime.now(PARIS)) = mutex.withLock {
        val settings = settingsRepository.current()
        val today = now.withZoneSameInstant(PARIS).toLocalDate()
        val courses = calendarRepository.coursesBetween(today, today.plusDays(HORIZON_DAYS + 1L))
        val bedtime = bedtimeRepository.current()
        val blocks = blockRepository.current()
        val computed = AlarmPlanner.next(
            now = now,
            settings = settings,
            periods = periodsRepository.get(),
            courses = courses,
            bedtimeFor = { day -> bedtime?.takeIf { it.wakeDate == day }?.time },
            horizonDays = HORIZON_DAYS,
            blocksFor = blocks::on,
        )
        val wake = computed.wake.takeIf { settings.wakeAlarmEnabled }
        val departure = computed.departure.takeIf { settings.departureReminderEnabled }
        val slot = if (settings.slotNotificationsEnabled) {
            FreeSlots.next(
                now = now,
                settings = settings,
                periods = periodsRepository.get(),
                courses = courses,
                bedtimeFor = { day -> bedtime?.takeIf { it.wakeDate == day }?.time },
                horizonDays = HORIZON_DAYS - 1,
                blocksFor = blocks::on,
            )
        } else {
            null
        }

        val wakePi = operation(AlarmReceiver.ACTION_WAKE)
        if (wake != null) {
            val show = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(wake.toInstant().toEpochMilli(), show), wakePi)
        } else {
            alarmManager.cancel(wakePi)
        }

        setExact(operation(AlarmReceiver.ACTION_DEPARTURE), departure?.toInstant()?.toEpochMilli())
        val slotPi = operation(AlarmReceiver.ACTION_SLOT) {
            putExtra(AlarmReceiver.EXTRA_SLOT_END_MS, slot?.end?.toInstant()?.toEpochMilli() ?: 0L)
        }
        setExact(slotPi, slot?.start?.toInstant()?.toEpochMilli())
        _next.value = NextAlarms(wake, departure, slot)
    }

    /** Alarme exacte (ou approximative si l'autorisation manque) ; [atMs] null = annuler. */
    private fun setExact(pi: PendingIntent, atMs: Long?) {
        when {
            atMs == null -> alarmManager.cancel(pi)
            canScheduleExact() -> alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, pi)
            else -> alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, pi)
        }
    }

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun operation(action: String, extras: Intent.() -> Unit = {}): PendingIntent = PendingIntent.getBroadcast(
        context, action.hashCode(),
        Intent(context, AlarmReceiver::class.java).setAction(action).apply(extras),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        private const val HORIZON_DAYS = 8
    }
}
