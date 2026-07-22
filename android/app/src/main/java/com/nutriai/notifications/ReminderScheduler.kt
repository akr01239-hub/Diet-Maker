package com.nutriai.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules local reminder notifications via WorkManager — 100% on-device, no push service.
 * Each reminder is a unique periodic job (24h for daily, 7d for weekly) whose first run is
 * delayed to the next occurrence of its target local time. WorkManager persists across reboots.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val workManager get() = WorkManager.getInstance(context)

    /** Applies the given on/off map: schedules enabled groups, cancels disabled ones. */
    fun apply(settings: Map<ReminderGroup, Boolean>) {
        settings.forEach { (group, enabled) ->
            if (enabled) scheduleGroup(group) else cancelGroup(group)
        }
    }

    fun scheduleGroup(group: ReminderGroup) {
        ReminderWorker.ensureChannel(context)
        ReminderCatalog.jobs(group).forEach { schedule(it) }
    }

    fun cancelGroup(group: ReminderGroup) {
        ReminderCatalog.jobs(group).forEach { workManager.cancelUniqueWork(it.key) }
    }

    private fun schedule(job: ReminderJob) {
        val weekly = job.weeklyDayIso != null
        val repeatMs = if (weekly) TimeUnit.DAYS.toMillis(7) else TimeUnit.DAYS.toMillis(1)
        val delayMs = initialDelayMs(job)

        val data = Data.Builder()
            .putString(ReminderWorker.KEY_TITLE, job.title)
            .putString(ReminderWorker.KEY_TEXT, job.text)
            .putInt(ReminderWorker.KEY_NOTIF_ID, job.key.hashCode())
            .build()

        val request = PeriodicWorkRequestBuilder<ReminderWorker>(repeatMs, TimeUnit.MILLISECONDS)
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(job.key, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    /** Milliseconds from now until the next occurrence of the job's local time (and weekday). */
    private fun initialDelayMs(job: ReminderJob): Long {
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, job.hour)
            set(Calendar.MINUTE, job.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (job.weeklyDayIso != null) {
            // Calendar.DAY_OF_WEEK: Sunday=1..Saturday=7; ISO: Monday=1..Sunday=7.
            val targetCal = if (job.weeklyDayIso == 7) Calendar.SUNDAY else job.weeklyDayIso + 1
            while (next.get(Calendar.DAY_OF_WEEK) != targetCal || !next.after(now)) {
                next.add(Calendar.DAY_OF_MONTH, 1)
            }
        } else if (!next.after(now)) {
            next.add(Calendar.DAY_OF_MONTH, 1)
        }
        return (next.timeInMillis - now.timeInMillis).coerceAtLeast(0)
    }

    companion object {
        const val TAG = "nutriai_reminder"
    }
}
