package com.example.data.supabase

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SupabaseProfileDto(
  @Json(name = "id") val id: String,
  @Json(name = "email") val email: String? = null,
  @Json(name = "full_name") val fullName: String = "",
  @Json(name = "role") val role: String = "teacher", // teacher, student, admin
  @Json(name = "school_code") val schoolCode: String = "irfan_default",
  @Json(name = "student_id") val studentId: Long? = null,
  @Json(name = "created_at") val createdAt: String? = null,
  @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseStudentDto(
  @Json(name = "id") val id: Long,
  @Json(name = "school_code") val schoolCode: String = "irfan_default",
  @Json(name = "full_name") val fullName: String,
  @Json(name = "grade") val grade: String = "5. Sınıf",
  @Json(name = "phone") val phone: String = "",
  @Json(name = "parent_name") val parentName: String = "",
  @Json(name = "parent_phone") val parentPhone: String = "",
  @Json(name = "enrollment_date") val enrollmentDate: String = "",
  @Json(name = "status") val status: String = "Aktif",
  @Json(name = "notes") val notes: String = "",
  @Json(name = "avatar_color_index") val avatarColorIndex: Int = 0,
  @Json(name = "username") val username: String = "",
  @Json(name = "access_code") val accessCode: String = "1234",
  @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseAttendanceDto(
  @Json(name = "id") val id: Long = 0,
  @Json(name = "student_id") val studentId: Long,
  @Json(name = "school_code") val schoolCode: String = "irfan_default",
  @Json(name = "date") val date: String,
  @Json(name = "status") val status: String = "GELDI",
  @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseMemorizationDto(
  @Json(name = "id") val id: Long = 0,
  @Json(name = "student_id") val studentId: Long,
  @Json(name = "school_code") val schoolCode: String = "irfan_default",
  @Json(name = "title") val title: String,
  @Json(name = "category") val category: String = "Sure",
  @Json(name = "status") val status: String = "DEVAM_EDIYOR",
  @Json(name = "rating") val rating: Int = 5,
  @Json(name = "progress_percent") val progressPercent: Int = 0,
  @Json(name = "teacher_notes") val teacherNotes: String = "",
  @Json(name = "date") val date: String = "",
  @Json(name = "repeat_count") val repeatCount: Int = 0,
  @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseDailyDutyDto(
  @Json(name = "id") val id: Long = 0,
  @Json(name = "student_id") val studentId: Long,
  @Json(name = "school_code") val schoolCode: String = "irfan_default",
  @Json(name = "date") val date: String,
  @Json(name = "fajr") val fajr: Boolean = false,
  @Json(name = "dhuhr") val dhuhr: Boolean = false,
  @Json(name = "asr") val asr: Boolean = false,
  @Json(name = "maghrib") val maghrib: Boolean = false,
  @Json(name = "isha") val isha: Boolean = false,
  @Json(name = "quran_pages") val quranPages: Int = 0,
  @Json(name = "tesbihat_done") val tesbihatDone: Boolean = false,
  @Json(name = "risale_pages") val risalePages: Int = 0,
  @Json(name = "memorization_repeats") val memorizationRepeats: Int = 0,
  @Json(name = "cevsen_done") val cevsenDone: Boolean = false,
  @Json(name = "salavat_count") val salavatCount: Int = 0,
  @Json(name = "notes") val notes: String = "",
  @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseCurriculumDto(
  @Json(name = "id") val id: String,
  @Json(name = "school_code") val schoolCode: String = "irfan_default",
  @Json(name = "title") val title: String,
  @Json(name = "category") val category: String,
  @Json(name = "is_selected") val isSelected: Boolean = true,
  @Json(name = "order_index") val orderIndex: Int = 0,
  @Json(name = "updated_at") val updatedAt: String? = null
)

// Auth DTOs
@JsonClass(generateAdapter = true)
data class SupabaseAuthTokenResponse(
  @Json(name = "access_token") val accessToken: String? = null,
  @Json(name = "token_type") val tokenType: String? = null,
  @Json(name = "expires_in") val expiresIn: Long? = null,
  @Json(name = "refresh_token") val refreshToken: String? = null,
  @Json(name = "user") val user: SupabaseUserDto? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseUserDto(
  @Json(name = "id") val id: String,
  @Json(name = "email") val email: String? = null,
  @Json(name = "user_metadata") val userMetadata: Map<String, Any?>? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseAuthRequest(
  @Json(name = "email") val email: String,
  @Json(name = "password") val password: String,
  @Json(name = "data") val data: Map<String, Any?>? = null
)
