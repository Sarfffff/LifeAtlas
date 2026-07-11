package com.xiaoyin.lifeatlas.core.reminder

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
import com.xiaoyin.lifeatlas.MainActivity
import com.xiaoyin.lifeatlas.R
import com.xiaoyin.lifeatlas.core.datastore.AppSettingsRepository
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object LocalReminderScheduler {
    const val actionMemoryOfDay = "com.xiaoyin.lifeatlas.REMINDER_MEMORY_OF_DAY"
    const val actionWeeklyReview = "com.xiaoyin.lifeatlas.REMINDER_WEEKLY_REVIEW"
    private const val dailyRequestCode = 3101
    private const val weeklyRequestCode = 3102

    fun applySettings(context: Context, memoryOfDay: Boolean, weeklyReview: Boolean) {
        if (memoryOfDay) scheduleDaily(context) else cancel(context, actionMemoryOfDay, dailyRequestCode)
        if (weeklyReview) scheduleWeekly(context) else cancel(context, actionWeeklyReview, weeklyRequestCode)
    }

    private fun scheduleDaily(context: Context) {
        scheduleRepeating(
            context = context,
            action = actionMemoryOfDay,
            requestCode = dailyRequestCode,
            firstAt = nextTime(Calendar.HOUR_OF_DAY to 20),
            interval = AlarmManager.INTERVAL_DAY
        )
    }

    private fun scheduleWeekly(context: Context) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.WEEK_OF_YEAR, 1)
        }
        scheduleRepeating(context, actionWeeklyReview, weeklyRequestCode, calendar.timeInMillis, 7 * AlarmManager.INTERVAL_DAY)
    }

    private fun nextTime(time: Pair<Int, Int>): Long {
        return Calendar.getInstance().apply {
            set(time.first, time.second)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
    }

    private fun scheduleRepeating(context: Context, action: String, requestCode: Int, firstAt: Long, interval: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            firstAt,
            interval,
            reminderPendingIntent(context, action, requestCode)
        )
    }

    private fun cancel(context: Context, action: String, requestCode: Int) {
        context.getSystemService(AlarmManager::class.java)
            .cancel(reminderPendingIntent(context, action, requestCode))
    }

    private fun reminderPendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, LifeAtlasReminderReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

class LifeAtlasReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                runCatching {
                    val settings = AppSettingsRepository(context).reminderSettings.first()
                    LocalReminderScheduler.applySettings(
                        context,
                        settings.memoryOfTheDayEnabled,
                        settings.weeklyReviewEnabled
                    )
                }
                pendingResult.finish()
            }
            return
        }

        val (title, text, notificationId) = when (intent.action) {
            LocalReminderScheduler.actionWeeklyReview -> Triple("本周的旷野", "回看这一周留下的脚印，也许会发现被忽略的小风景。", 3202)
            else -> Triple("今日记忆", "翻开岁迹，看看往年的今天走到了哪里。", 3201)
        }
        createChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val openApp = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(channelId, "记忆回顾", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "岁迹的今日记忆和每周回顾提醒"
                }
            )
        }
    }

    private companion object {
        const val channelId = "lifeatlas_memory_review"
    }
}
