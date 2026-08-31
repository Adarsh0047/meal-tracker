package io.github.adarsh0047.mealtracker

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.ZonedDateTime
import java.util.UUID

data class MealReminder(
    val id: String = UUID.randomUUID().toString(),
    val days: Set<DayOfWeek>,
    val hour: Int,
    val minute: Int,
    val label: String = "Time to update your meals",
    val enabled: Boolean = true
)

class ReminderStore(context: Context) {
    private val preferences = context.getSharedPreferences("meal_reminders", Context.MODE_PRIVATE)

    fun load(): List<MealReminder> = runCatching {
        val array = JSONArray(preferences.getString("items", "[]"))
        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            MealReminder(
                id = item.getString("id"),
                days = if (item.has("days")) {
                    val days = item.getJSONArray("days")
                    (0 until days.length()).map { DayOfWeek.valueOf(days.getString(it)) }.toSet()
                } else {
                    setOf(DayOfWeek.valueOf(item.getString("day")))
                },
                hour = item.getInt("hour"), minute = item.getInt("minute"),
                label = item.optString("label", "Time to update your meals"), enabled = item.optBoolean("enabled", true)
            )
        }
    }.getOrDefault(emptyList())

    fun save(reminders: List<MealReminder>) {
        val array = JSONArray()
        reminders.forEach { reminder ->
            array.put(JSONObject().apply {
                put("id", reminder.id)
                put("days", JSONArray().apply { reminder.days.sortedBy { it.value }.forEach { put(it.name) } })
                put("hour", reminder.hour)
                put("minute", reminder.minute); put("label", reminder.label); put("enabled", reminder.enabled)
            })
        }
        preferences.edit().putString("items", array.toString()).apply()
    }
}

object ReminderScheduler {
    const val CHANNEL_ID = "meal_reminders"
    private const val EXTRA_ID = "reminder_id"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Meal reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Scheduled reminders to update your meal tracker"
                enableVibration(true)
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    fun canScheduleExact(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    fun schedule(context: Context, reminder: MealReminder) {
        cancel(context, reminder.id)
        if (!reminder.enabled) return
        val manager = context.getSystemService(AlarmManager::class.java)
        reminder.days.forEach { day ->
            val trigger = nextOccurrence(reminder, day).toInstant().toEpochMilli()
            val pending = alarmIntent(context, reminder.id, day)
            if (canScheduleExact(context)) manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
            else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
        }
    }

    fun cancel(context: Context, id: String) {
        val manager = context.getSystemService(AlarmManager::class.java)
        DayOfWeek.entries.forEach { manager.cancel(alarmIntent(context, id, it)) }
    }

    fun rescheduleAll(context: Context) {
        ReminderStore(context).load().filter { it.enabled }.forEach { schedule(context, it) }
    }

    private fun alarmIntent(context: Context, id: String, day: DayOfWeek) = PendingIntent.getBroadcast(
        context, "$id-${day.name}".hashCode(), Intent(context, ReminderReceiver::class.java).putExtra(EXTRA_ID, id),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun nextOccurrence(reminder: MealReminder, day: DayOfWeek): ZonedDateTime {
        val now = ZonedDateTime.now()
        var target = now.with(day).withHour(reminder.hour).withMinute(reminder.minute).withSecond(0).withNano(0)
        if (!target.isAfter(now)) target = target.plusWeeks(1)
        return target
    }

    fun idFrom(intent: Intent): String? = intent.getStringExtra(EXTRA_ID)
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = ReminderScheduler.idFrom(intent) ?: return
        val reminder = ReminderStore(context).load().firstOrNull { it.id == id && it.enabled } ?: return
        ReminderScheduler.createChannel(context)
        if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            val openApp = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Meal Tracker")
                .setContentText(reminder.label)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(openApp)
                .build()
            NotificationManagerCompat.from(context).notify(id.hashCode(), notification)
        }
        ReminderScheduler.schedule(context, reminder)
    }
}

class ReminderRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent.action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) {
            ReminderScheduler.rescheduleAll(context)
        }
    }
}
