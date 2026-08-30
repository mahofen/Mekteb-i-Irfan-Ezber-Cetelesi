package com.example.data.supabase

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseAuthApi {
  @POST("auth/v1/token?grant_type=password")
  suspend fun login(
    @Body request: SupabaseAuthRequest
  ): Response<SupabaseAuthTokenResponse>

  @POST("auth/v1/signup")
  suspend fun signup(
    @Body request: SupabaseAuthRequest
  ): Response<SupabaseAuthTokenResponse>

  @POST("auth/v1/logout")
  suspend fun logout(): Response<Unit>

  @GET("auth/v1/user")
  suspend fun getCurrentUser(): Response<SupabaseUserDto>
}

interface SupabaseRestApi {
  // ----------------------------------------------------
  // PROFILES
  // ----------------------------------------------------
  @GET("rest/v1/profiles?select=*")
  suspend fun getProfile(
    @Query("id") idFilter: String
  ): Response<List<SupabaseProfileDto>>

  @POST("rest/v1/profiles")
  suspend fun upsertProfile(
    @Header("Prefer") prefer: String = "resolution=merge-duplicates,return=representation",
    @Body profile: SupabaseProfileDto
  ): Response<List<SupabaseProfileDto>>

  // ----------------------------------------------------
  // STUDENTS
  // ----------------------------------------------------
  @GET("rest/v1/students?select=*")
  suspend fun getStudents(
    @Query("school_code") schoolCodeFilter: String? = null,
    @Query("id") idFilter: String? = null
  ): Response<List<SupabaseStudentDto>>

  @POST("rest/v1/students")
  suspend fun upsertStudents(
    @Header("Prefer") prefer: String = "resolution=merge-duplicates,return=representation",
    @Body students: List<SupabaseStudentDto>
  ): Response<List<SupabaseStudentDto>>

  // ----------------------------------------------------
  // ATTENDANCE
  // ----------------------------------------------------
  @GET("rest/v1/attendance_records?select=*")
  suspend fun getAttendance(
    @Query("school_code") schoolCodeFilter: String? = null,
    @Query("student_id") studentIdFilter: String? = null
  ): Response<List<SupabaseAttendanceDto>>

  @POST("rest/v1/attendance_records")
  suspend fun upsertAttendance(
    @Header("Prefer") prefer: String = "resolution=merge-duplicates,return=representation",
    @Body attendance: List<SupabaseAttendanceDto>
  ): Response<List<SupabaseAttendanceDto>>

  // ----------------------------------------------------
  // MEMORIZATION
  // ----------------------------------------------------
  @GET("rest/v1/memorization_records?select=*")
  suspend fun getMemorization(
    @Query("school_code") schoolCodeFilter: String? = null,
    @Query("student_id") studentIdFilter: String? = null
  ): Response<List<SupabaseMemorizationDto>>

  @POST("rest/v1/memorization_records")
  suspend fun upsertMemorization(
    @Header("Prefer") prefer: String = "resolution=merge-duplicates,return=representation",
    @Body memorization: List<SupabaseMemorizationDto>
  ): Response<List<SupabaseMemorizationDto>>

  // ----------------------------------------------------
  // DAILY DUTY RECORDS
  // ----------------------------------------------------
  @GET("rest/v1/daily_duty_records?select=*")
  suspend fun getDailyDuties(
    @Query("school_code") schoolCodeFilter: String? = null,
    @Query("student_id") studentIdFilter: String? = null
  ): Response<List<SupabaseDailyDutyDto>>

  @POST("rest/v1/daily_duty_records")
  suspend fun upsertDailyDuties(
    @Header("Prefer") prefer: String = "resolution=merge-duplicates,return=representation",
    @Body duties: List<SupabaseDailyDutyDto>
  ): Response<List<SupabaseDailyDutyDto>>

  // ----------------------------------------------------
  // CURRICULUM
  // ----------------------------------------------------
  @GET("rest/v1/curriculum_items?select=*")
  suspend fun getCurriculum(
    @Query("school_code") schoolCodeFilter: String? = null
  ): Response<List<SupabaseCurriculumDto>>

  @POST("rest/v1/curriculum_items")
  suspend fun upsertCurriculum(
    @Header("Prefer") prefer: String = "resolution=merge-duplicates,return=representation",
    @Body items: List<SupabaseCurriculumDto>
  ): Response<List<SupabaseCurriculumDto>>
}
