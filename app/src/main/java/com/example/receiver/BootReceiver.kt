package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.util.NotificationHelper

class BootReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
      intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
      intent.action == "android.intent.action.QUICKBOOT_POWERON"
    ) {
      val prefs = context.getSharedPreferences(NotificationHelper.PREFS_NAME, Context.MODE_PRIVATE)
      val isEnabled = prefs.getBoolean(NotificationHelper.KEY_REMINDER_ENABLED, true)

      if (isEnabled) {
        val hour = prefs.getInt(NotificationHelper.KEY_REMINDER_HOUR, 17)
        val minute = prefs.getInt(NotificationHelper.KEY_REMINDER_MINUTE, 0)
        NotificationHelper.createNotificationChannel(context)
        NotificationHelper.scheduleDailyReminder(context, hour, minute)
      }
    }
  }
}
