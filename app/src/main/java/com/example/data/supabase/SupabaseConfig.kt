package com.example.data.supabase

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

class SupabaseConfig private constructor(context: Context) {
  private val prefs: SharedPreferences =
    context.getSharedPreferences("supabase_config_prefs", Context.MODE_PRIVATE)

  // Default values from BuildConfig (injected via .env / Secrets Gradle Plugin)
  val defaultUrl: String
    get() {
      return try {
        val field = BuildConfig::class.java.getField("SUPABASE_URL")
        val v = field.get(null) as? String ?: ""
        if (v.isNotBlank() && !v.contains("your-project")) v else "https://your-project.supabase.co"
      } catch (e: Exception) {
        "https://your-project.supabase.co"
      }
    }

  val defaultAnonKey: String
    get() {
      return try {
        val field = BuildConfig::class.java.getField("SUPABASE_ANON_KEY")
        val v = field.get(null) as? String ?: ""
        if (v.isNotBlank() && !v.contains("dummy")) v else "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.dummy_anon_key"
      } catch (e: Exception) {
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.dummy_anon_key"
      }
    }

  var projectUrl: String
    get() = prefs.getString("supabase_project_url", defaultUrl)?.trim() ?: defaultUrl
    set(value) = prefs.edit().putString("supabase_project_url", value.trim()).apply()

  var anonKey: String
    get() = prefs.getString("supabase_anon_key", defaultAnonKey)?.trim() ?: defaultAnonKey
    set(value) = prefs.edit().putString("supabase_anon_key", value.trim()).apply()

  var authToken: String?
    get() = prefs.getString("supabase_auth_token", null)
    set(value) = prefs.edit().putString("supabase_auth_token", value).apply()

  var refreshToken: String?
    get() = prefs.getString("supabase_refresh_token", null)
    set(value) = prefs.edit().putString("supabase_refresh_token", value).apply()

  var currentUserId: String?
    get() = prefs.getString("supabase_user_id", null)
    set(value) = prefs.edit().putString("supabase_user_id", value).apply()

  var userRole: String
    get() = prefs.getString("supabase_user_role", "teacher") ?: "teacher"
    set(value) = prefs.edit().putString("supabase_user_role", value).apply()

  var currentStudentId: Long?
    get() {
      val id = prefs.getLong("supabase_student_id", -1L)
      return if (id >= 0) id else null
    }
    set(value) {
      if (value == null) {
        prefs.edit().remove("supabase_student_id").apply()
      } else {
        prefs.edit().putLong("supabase_student_id", value).apply()
      }
    }

  var schoolCode: String
    get() = prefs.getString("supabase_school_code", "irfan_default") ?: "irfan_default"
    set(value) {
      val clean = if (value.trim().isBlank()) "irfan_default" else value.trim().lowercase().replace(" ", "_")
      prefs.edit().putString("supabase_school_code", clean).apply()
    }

  val isConfigured: Boolean
    get() = projectUrl.isNotBlank() &&
            !projectUrl.contains("your-project") &&
            anonKey.isNotBlank() &&
            !anonKey.contains("dummy")

  val hasValidToken: Boolean
    get() = !authToken.isNullOrBlank()

  fun clearSession() {
    authToken = null
    refreshToken = null
    currentUserId = null
    currentStudentId = null
    userRole = "teacher"
  }

  companion object {
    @Volatile
    private var INSTANCE: SupabaseConfig? = null

    fun getInstance(context: Context): SupabaseConfig {
      return INSTANCE ?: synchronized(this) {
        val instance = SupabaseConfig(context.applicationContext)
        INSTANCE = instance
        instance
      }
    }
  }
}
