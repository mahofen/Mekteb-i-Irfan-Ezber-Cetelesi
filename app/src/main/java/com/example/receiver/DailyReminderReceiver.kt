package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.db.AppDatabase
import com.example.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DailyReminderReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    val prefs = context.getSharedPreferences(NotificationHelper.PREFS_NAME, Context.MODE_PRIVATE)
    val isEnabled = prefs.getBoolean(NotificationHelper.KEY_REMINDER_ENABLED, true)

    if (!isEnabled) return

    val hour = prefs.getInt(NotificationHelper.KEY_REMINDER_HOUR, 17)
    val minute = prefs.getInt(NotificationHelper.KEY_REMINDER_MINUTE, 0)
    val audience = prefs.getString(NotificationHelper.KEY_REMINDER_AUDIENCE, "ALL") ?: "ALL"

    // Execute in background coroutine to query database
    val pendingResult = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val database = AppDatabase.getDatabase(context, this)
        val students = database.studentDao().getAllStudents().first()
        val memorizations = database.memorizationDao().getAllMemorizationRecords().first()

        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val completedToday = memorizations.count { it.date == todayDate && it.status == "COMPLETED" }
        val inProgressCount = memorizations.count { it.status == "IN_PROGRESS" }

        val (title, message) = NotificationHelper.generateReminderMessage(
          audience = audience,
          totalStudents = students.size,
          completedToday = completedToday,
          inProgressCount = inProgressCount
        )

        NotificationHelper.showReminderNotification(
          context = context,
          title = title,
          message = message
        )

        // Reschedule for next day
        NotificationHelper.scheduleDailyReminder(context, hour, minute)
      } catch (e: Exception) {
        e.printStackTrace()
      } finally {
        pendingResult.finish()
      }
    }
  }
}
