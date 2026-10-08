package com.jovoc.ekadashi

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.LocalDate

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val id = intent.getIntExtra(Scheduler.EXTRA_ID, 0)
        val type = intent.getStringExtra(Scheduler.EXTRA_TYPE) ?: return
        val dateStr = intent.getStringExtra(Scheduler.EXTRA_DATE) ?: return
        val kindStr = intent.getStringExtra(Scheduler.EXTRA_KIND) ?: return

        val vrataDate = try {
            LocalDate.parse(dateStr)
        } catch (e: Exception) {
            return
        }

        val kind = try {
            ReminderKind.valueOf(kindStr)
        } catch (e: Exception) {
            return
        }

        val lang = Config.getLanguage(context)
        val wrappedContext = L10n.attachBaseContext(context, lang)
        val locale = L10n.getLocale(lang)
        val vrataRecord = Repo.load(context).firstOrNull { it.type == type && it.date == vrataDate }
        val vrataName = if (vrataRecord != null) {
            L10n.getVrataName(wrappedContext, vrataRecord, lang)
        } else {
            L10n.getVrataName(wrappedContext, type)
        }
        val targetZone = Config.getTimezone(context)

        val contentText: String = when (kind) {
            ReminderKind.TWO_DAY -> {
                val formattedDate = L10n.formatDate(vrataDate, locale)
                wrappedContext.getString(R.string.notif_two_day, vrataName, formattedDate)
            }
            ReminderKind.ONE_DAY -> {
                val formattedDate = L10n.formatDate(vrataDate, locale)
                wrappedContext.getString(R.string.notif_one_day, vrataName, formattedDate)
            }
            ReminderKind.PARANA -> {
                val parana = vrataRecord?.parana
                val startTime = parana?.startIn(targetZone)
                val endTime = parana?.endIn(targetZone)
                if (startTime != null && endTime != null) {
                    wrappedContext.getString(
                        R.string.notif_parana_range,
                        L10n.formatTime(startTime, locale),
                        L10n.formatTime(endTime, locale)
                    )
                } else if (startTime != null) {
                    wrappedContext.getString(R.string.notif_parana_after, L10n.formatTime(startTime, locale))
                } else if (endTime != null) {
                    wrappedContext.getString(R.string.notif_parana_before, L10n.formatTime(endTime, locale))
                } else {
                    return
                }
            }
        }

        Scheduler.createNotificationChannel(context)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = Notification.Builder(context, Scheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_vrata)
            .setContentTitle(vrataName)
            .setContentText(contentText)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(id, builder.build())
    }
}

class SystemReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Scheduler.scheduleAll(context)
                Scheduler.scheduleRefreshJob(context)
            }
            Intent.ACTION_TIMEZONE_CHANGED -> {
                Config.updateTimezoneIfOther(context)
                Scheduler.scheduleAll(context)
                Scheduler.scheduleRefreshJob(context)
            }
        }
    }
}
