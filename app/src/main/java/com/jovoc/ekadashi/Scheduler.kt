package com.jovoc.ekadashi

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.ZonedDateTime

object Scheduler {

    const val CHANNEL_ID = "vrata_reminders"
    const val JOB_ID = 1001

    const val EXTRA_ID = "extra_id"
    const val EXTRA_TYPE = "extra_type"
    const val EXTRA_DATE = "extra_date"
    const val EXTRA_KIND = "extra_kind"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notif_channel_name)
            val descriptionText = context.getString(R.string.notif_channel_desc)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun scheduleAll(context: Context) {
        createNotificationChannel(context)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // Cancel previous alarms
        cancelAll(context)

        val zone = Config.getTimezone(context)
        val now = ZonedDateTime.now(zone)
        val vratas = Repo.load(context)
        val reminders = calculateReminders(vratas, zone, now)

        val newScheduledIds = mutableSetOf<String>()

        for (reminder in reminders) {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra(EXTRA_ID, reminder.id)
                putExtra(EXTRA_TYPE, reminder.type)
                putExtra(EXTRA_DATE, reminder.vrataDate.toString())
                putExtra(EXTRA_KIND, reminder.kind.name)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reminder.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminder.triggerAtMillis,
                    pendingIntent
                )
                newScheduledIds.add(reminder.id.toString())
            } catch (e: Exception) {
                // Ignore scheduling exceptions
            }
        }

        Config.setScheduledIds(context, newScheduledIds)
    }

    fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val savedIds = Config.getScheduledIds(context)

        for (idStr in savedIds) {
            val id = idStr.toIntOrNull() ?: continue
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                id,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }

        Config.setScheduledIds(context, emptySet())
    }

    fun scheduleRefreshJob(context: Context) {
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
        val componentName = ComponentName(context, RefreshJob::class.java)
        val builder = JobInfo.Builder(JOB_ID, componentName)
            .setPeriodic(24 * 60 * 60 * 1000L) // 24 hours
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPersisted(true)

        try {
            jobScheduler.schedule(builder.build())
        } catch (e: Exception) {
            // Handle potential job scheduling issues
        }
    }
}
