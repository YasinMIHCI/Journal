package com.yasinmihci.journal.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.yasinmihci.journal.MainActivity
import com.yasinmihci.journal.R

/** Canaux et notifications du réveil et du départ. */
object AlarmNotifications {
    const val CHANNEL_WAKE = "wake"
    const val CHANNEL_DEPARTURE = "departure"
    const val ID_WAKE = 1
    const val ID_DEPARTURE = 2

    /** Sonnerie coupée automatiquement après ce délai si personne ne l'arrête. */
    private const val WAKE_TIMEOUT_MS = 10 * 60 * 1000L

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val wake = NotificationChannel(
            CHANNEL_WAKE,
            context.getString(R.string.channel_wake),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            setSound(
                alarmSound,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 800, 400, 800)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val departure = NotificationChannel(
            CHANNEL_DEPARTURE,
            context.getString(R.string.channel_departure),
            NotificationManager.IMPORTANCE_HIGH,
        )
        nm.createNotificationChannels(listOf(wake, departure))
    }

    /** Sonnerie : son d'alarme en boucle (FLAG_INSISTENT) + écran plein si verrouillé. */
    fun showWake(context: Context, text: String) {
        val fullScreen = PendingIntent.getActivity(
            context, 0,
            Intent(context, AlarmActivity::class.java)
                .putExtra(AlarmActivity.EXTRA_TEXT, text)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getBroadcast(
            context, 0,
            Intent(context, AlarmReceiver::class.java).setAction(AlarmReceiver.ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_WAKE)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(context.getString(R.string.wake_title))
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .setOngoing(true)
            .setAutoCancel(false)
            .setTimeoutAfter(WAKE_TIMEOUT_MS)
            .addAction(0, context.getString(R.string.wake_stop), stop)
            .build()
        notification.flags = notification.flags or Notification.FLAG_INSISTENT
        notify(context, ID_WAKE, notification)
    }

    fun stopWake(context: Context) {
        NotificationManagerCompat.from(context).cancel(ID_WAKE)
    }

    fun showDeparture(context: Context, text: String) {
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_DEPARTURE)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(context.getString(R.string.departure_title))
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        notify(context, ID_DEPARTURE, notification)
    }

    private fun notify(context: Context, id: Int, notification: Notification) {
        val nm = NotificationManagerCompat.from(context)
        // Sans la permission (Android 13+), rien ne s'affiche : l'écran Réglages le signale.
        if (nm.areNotificationsEnabled()) {
            try {
                nm.notify(id, notification)
            } catch (e: SecurityException) {
                // Permission retirée entre-temps.
            }
        }
    }
}
