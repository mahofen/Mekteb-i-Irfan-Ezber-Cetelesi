package com.example.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.receiver.DailyReminderReceiver
import java.util.Calendar

object NotificationHelper {

  const val CHANNEL_ID = "mektebiirfan_daily_reminders"
  const val CHANNEL_NAME = "Ezber ve Tekrar Hatırlatıcıları"
  const val CHANNEL_DESCRIPTION = "Günlük bekleyen ezberler ve tekrar dersleri için zarif hatırlatıcılar"
  const val NOTIFICATION_ID = 1001
  const val ALARM_REQUEST_CODE = 2001

  const val PREFS_NAME = "app_settings_prefs"
  const val KEY_REMINDER_ENABLED = "key_reminder_enabled"
  const val KEY_REMINDER_HOUR = "key_reminder_hour"
  const val KEY_REMINDER_MINUTE = "key_reminder_minute"
  const val KEY_REMINDER_AUDIENCE = "key_reminder_audience" // "ALL", "TEACHER", "STUDENT"

  /**
   * Initializes the Notification Channel for Android 8.0+ (API 26+)
   */
  fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      val existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
      if (existingChannel == null) {
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
          description = CHANNEL_DESCRIPTION
          enableLights(true)
          lightColor = Color.rgb(197, 155, 39) // Tezhip Gold
          enableVibration(true)
          vibrationPattern = longArrayOf(0, 250, 150, 250)
          setShowBadge(true)
        }
        notificationManager.createNotificationChannel(channel)
      }
    }
  }

  /**
   * Checks if notification permission is granted
   */
  fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
  }

  /**
   * Sends an immediate elegant local notification
   */
  fun showReminderNotification(
    context: Context,
    title: String,
    message: String,
    subText: String = "Mekteb-i İrfan"
  ) {
    createNotificationChannel(context)

    // Intent to open MainActivity when notification is tapped
    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra("FROM_NOTIFICATION", true)
    }

    val pendingIntent = PendingIntent.getActivity(
      context,
      0,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    val largeIcon = try {
      BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
    } catch (_: Exception) {
      null
    }

    val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(android.R.drawable.ic_popup_reminder)
      .setContentTitle(title)
      .setContentText(message)
      .setSubText(subText)
      .setStyle(
        NotificationCompat.BigTextStyle()
          .bigText(message)
          .setBigContentTitle(title)
          .setSummaryText(subText)
      )
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setColor(Color.rgb(15, 44, 89)) // Dergâh Navy Blue
      .setAutoCancel(true)
      .setSound(defaultSoundUri)
      .setContentIntent(pendingIntent)
      .setVibrate(longArrayOf(0, 250, 150, 250))

    if (largeIcon != null) {
      notificationBuilder.setLargeIcon(largeIcon)
    }

    try {
      val notificationManager = NotificationManagerCompat.from(context)
      if (hasNotificationPermission(context)) {
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
      }
    } catch (e: SecurityException) {
      e.printStackTrace()
    }
  }

  /**
   * Schedules a daily repeating alarm using AlarmManager
   */
  fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    val intent = Intent(context, DailyReminderReceiver::class.java).apply {
      action = "com.example.ACTION_DAILY_REMINDER"
    }

    val pendingIntent = PendingIntent.getBroadcast(
      context,
      ALARM_REQUEST_CODE,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val calendar = Calendar.getInstance().apply {
      timeInMillis = System.currentTimeMillis()
      set(Calendar.HOUR_OF_DAY, hour)
      set(Calendar.MINUTE, minute)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)

      // If the time has already passed today, schedule for tomorrow
      if (timeInMillis <= System.currentTimeMillis()) {
        add(Calendar.DAY_OF_YEAR, 1)
      }
    }

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmManager.setExactAndAllowWhileIdle(
          AlarmManager.RTC_WAKEUP,
          calendar.timeInMillis,
          pendingIntent
        )
      } else {
        alarmManager.setRepeating(
          AlarmManager.RTC_WAKEUP,
          calendar.timeInMillis,
          AlarmManager.INTERVAL_DAY,
          pendingIntent
        )
      }
    } catch (e: Exception) {
      e.printStackTrace()
      // Fallback for strict exact alarm restrictions
      try {
        alarmManager.set(
          AlarmManager.RTC_WAKEUP,
          calendar.timeInMillis,
          pendingIntent
        )
      } catch (_: Exception) {}
    }
  }

  /**
   * Cancels the scheduled daily reminder
   */
  fun cancelDailyReminder(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, DailyReminderReceiver::class.java).apply {
      action = "com.example.ACTION_DAILY_REMINDER"
    }
    val pendingIntent = PendingIntent.getBroadcast(
      context,
      ALARM_REQUEST_CODE,
      intent,
      PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    )
    if (pendingIntent != null) {
      alarmManager.cancel(pendingIntent)
      pendingIntent.cancel()
    }
  }

  /**
   * Formats a gentle, encouraging reminder message based on audience & pending tasks
   */
  fun generateReminderMessage(
    audience: String,
    totalStudents: Int,
    completedToday: Int,
    inProgressCount: Int
  ): Pair<String, String> {
    return when (audience) {
      "TEACHER" -> {
        val title = "📚 Talebe Ezber Takibi & Dinleme Vakti"
        val message = if (inProgressCount > 0) {
          "Mektepte devam eden $inProgressCount ezber dersi bulunmaktadır. Talebelerin bugünkü dinleme ve tekrar kontrollerini yapabilirsiniz."
        } else {
          "Toplam $totalStudents kayıtlı talebe için bugünkü ders dinleme ve yoklama kayıtlarını kontrol etme vakti."
        }
        Pair(title, message)
      }
      "STUDENT" -> {
        val title = "🌿 Günlük Ezber ve Tekrar Hatırlatması"
        val message = "Günde 15-20 dakika düzenli tekrar ile hafızadaki ezberler sağlamlaşır. Bugünün ezber ve tekrar hedefini tamamlamayı unutmayın!"
        Pair(title, message)
      }
      else -> {
        val title = "🕌 Günlük Ezber ve Tekrar Hatırlatması"
        val message = if (completedToday > 0) {
          "Bugün $completedToday ezber başarıyla teslim edildi! Kalan derslerin tekrarı ve yeni ezberler için bereketli bir çalışma dileriz."
        } else {
          "Günlük ezberlerin kalıcılığı için 15 dakikalık düzenli tekrar tavsiye edilir. Bugünkü ders teslimlerini tamamlayabilirsiniz."
        }
        Pair(title, message)
      }
    }
  }
}
