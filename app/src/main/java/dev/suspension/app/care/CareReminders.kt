package dev.suspension.app.care

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import dev.suspension.app.MainActivity
import dev.suspension.app.R
import dev.suspension.app.data.BikeParts
import dev.suspension.app.data.BikeTraits
import dev.suspension.app.data.ComponentCatalog
import dev.suspension.app.data.GarageRepository
import dev.suspension.app.data.LanguageStore
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/** Daily maintenance check: scheduling (WorkManager, inexact), the notification channel and the notification. */
object CareReminders {
    const val CHANNEL_ID = "maintenance"
    const val WORK_NAME = "care-reminders"
    private const val DEBUG_WORK_NAME = "care-reminders-debug"
    private const val NOTIFICATION_ID = 4101
    private const val CHECK_HOUR = 9

    /** Intent extra: open `Pflege` → `Kalender`. */
    const val EXTRA_OPEN_CARE = "dev.suspension.app.OPEN_CARE"

    /** Intent extra, honoured in debug builds only: run the daily check once, now. */
    const val EXTRA_RUN_CHECK = "dev.suspension.app.RUN_CARE_CHECK"

    /** Android 13+ asks for the permission; older versions have it implicitly. */
    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun needsPermission(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /** The next 09:00 local time after [now] (today's if it has not passed yet). */
    fun nextRun(now: ZonedDateTime): ZonedDateTime {
        val today = now.toLocalDate().atTime(CHECK_HOUR, 0).atZone(now.zone)
        return if (today.isAfter(now)) today else now.toLocalDate().plusDays(1).atTime(CHECK_HOUR, 0).atZone(now.zone)
    }

    /** Every 24 h, first run at the next 09:00; UPDATE keeps one job and replaces its definition. */
    fun schedule(context: Context) = guarded("schedule") {
        val now = ZonedDateTime.now()
        val request = PeriodicWorkRequestBuilder<CareReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(Duration.between(now, nextRun(now)))
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancel(context: Context) = guarded("cancel") {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /** Debug entry point: the same worker, once, right away. */
    fun runOnce(context: Context) = guarded("runOnce") {
        WorkManager.getInstance(context).enqueueUniqueWork(
            DEBUG_WORK_NAME,
            androidx.work.ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<CareReminderWorker>().build(),
        )
    }

    /** Scheduling is a convenience; it must never take the app down. */
    private fun guarded(what: String, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            Log.e("CareReminders", "$what failed", e)
        }
    }

    /** One run of the check: plans, posts at most one grouped notification, remembers what was announced. */
    /** A garage bike as the check sees it: id, name for the notification, traits. */
    data class CheckedBike(val id: String, val name: String, val traits: BikeTraits)

    /** Every bike's calendar is checked; tasks of all bikes go into one notification. */
    fun check(context: Context, repository: CareRepository, bikes: List<CheckedBike>, today: LocalDate) {
        val store = repository.state.value
        val permission = hasPermission(context)
        val announced = mutableListOf<Pair<CheckedBike, ReminderPlan>>()
        for (bike in bikes) {
            val data = store.forBike(bike.id)
            val plan = ReminderPlanner.plan(
                data = data,
                permissionGranted = permission,
                evaluated = DueCalculator.evaluateAll(data.dueInput(today), MaintCatalog.forBike(bike.traits)),
                today = today,
            )
            if (plan.notified != data.notified) repository.setNotified(bike.id, plan.notified)
            if (plan.notify.isNotEmpty()) announced += bike to plan
        }
        runBlocking { repository.awaitSaved() }
        if (announced.isNotEmpty()) post(context, announced)
    }

    /** The garage's bikes with their traits; names fall back to the profile name like the UI. */
    fun garageBikes(context: Context): List<CheckedBike> {
        val localized = LanguageStore.wrap(context)
        return GarageRepository.get(context).state.value.bikes.map { bike ->
            val profile = BikeParts.profile(bike)
            val traits = BikeTraits.of(profile, BikeParts.fork(bike, profile), BikeParts.shock(bike, profile), bike.catalogBikeId?.let(ComponentCatalog::bikeById))
            CheckedBike(bike.id, bike.name ?: localized.getString(profile.nameResId), traits)
        }
    }

    private fun post(context: Context, announced: List<Pair<CheckedBike, ReminderPlan>>) {
        val localized = LanguageStore.wrap(context)
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (!manager.areNotificationsEnabled()) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, localized.getString(R.string.care_notif_channel_name), NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = localized.getString(R.string.care_notif_channel_desc) },
        )
        val title = localized.getString(
            when {
                announced.any { it.second.hasOverdue } && announced.any { it.second.hasSoon } -> R.string.care_notif_both
                announced.any { it.second.hasOverdue } -> R.string.care_notif_overdue
                else -> R.string.care_notif_soon
            },
        )
        // With several bikes in the notification, each task says which bike it belongs to.
        val severalBikes = announced.size > 1
        val names = announced.flatMap { (bike, plan) ->
            plan.notify.map { due ->
                val task = localized.getString(due.task.nameRes(bike.traits))
                if (severalBikes) localized.getString(R.string.care_notif_bike_task, bike.name, task) else task
            }
        }
        val body = ReminderPlanner.bodyNames(names) {
            localized.getString(R.string.care_notif_more, it)
        }
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_OPEN_CARE, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = android.app.Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }
}

/** The daily job. Works on the application context; the UI's [CareRepository] instance is shared. */
class CareReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        CareReminders.check(applicationContext, CareRepository.get(applicationContext), CareReminders.garageBikes(applicationContext), LocalDate.now())
        return Result.success()
    }
}
