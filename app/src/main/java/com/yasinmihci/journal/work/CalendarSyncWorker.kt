package com.yasinmihci.journal.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.yasinmihci.journal.JournalApplication
import com.yasinmihci.journal.data.calendar.SyncOutcome
import com.yasinmihci.journal.domain.calendar.PARIS
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/** Synchro ICS quotidienne. */
class CalendarSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as JournalApplication).container
        val outcome = container.calendarRepository.sync()
        container.alarmScheduler.reschedule()
        return when (outcome) {
            is SyncOutcome.Success, SyncOutcome.NoUrl -> Result.success()
            // Réseau absent / serveur indisponible : WorkManager réessaie (backoff exponentiel).
            is SyncOutcome.Error -> if (runAttemptCount < 5) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val UNIQUE_NAME = "calendar-sync"

        /**
         * Planifie la synchro tous les jours vers [time] (heure de Paris).
         * [replace] = true quand l'heure a changé ; sinon KEEP (ne touche pas une synchro en cours).
         */
        fun schedule(
            context: Context,
            time: LocalTime,
            replace: Boolean,
            now: ZonedDateTime = ZonedDateTime.now(PARIS),
        ) {
            val request = PeriodicWorkRequestBuilder<CalendarSyncWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delayUntil(now, time).toMinutes(), TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                if (replace) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE else ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        /** Délai jusqu'à la prochaine occurrence de [time] (demain si déjà passée). */
        fun delayUntil(now: ZonedDateTime, time: LocalTime): Duration {
            var next = now.toLocalDate().atTime(time).atZone(now.zone)
            if (!next.isAfter(now)) next = next.plusDays(1)
            return Duration.between(now, next)
        }
    }
}
