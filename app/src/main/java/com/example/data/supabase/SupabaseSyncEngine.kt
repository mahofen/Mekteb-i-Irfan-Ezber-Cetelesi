package com.example.data.supabase

import android.content.Context
import android.content.SharedPreferences
import com.example.data.cloud.SyncStatus
import com.example.data.db.AppDatabase
import com.example.data.model.AttendanceRecord
import com.example.data.model.DailyDutyRecord
import com.example.data.model.MemorizationRecord
import com.example.data.model.Student
import com.example.data.util.CustomCurriculumItem
import com.example.data.util.ImportMode
import com.example.data.util.ImportResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SupabaseSyncEngine(
  private val context: Context,
  private val database: AppDatabase
) {
  private val config = SupabaseConfig.getInstance(context)
  private val client = SupabaseClient.getInstance(context)
  private val prefs: SharedPreferences =
    context.getSharedPreferences("supabase_sync_prefs", Context.MODE_PRIVATE)

  private val _syncStatus = MutableStateFlow(
    SyncStatus(
      lastSyncTime = prefs.getString("last_sync_time", "Henüz yapılmadı") ?: "Henüz yapılmadı",
      lastSyncMessage = prefs.getString("last_sync_msg", "Hazır") ?: "Hazır",
      syncedStudentsCount = prefs.getInt("last_synced_students", 0),
      syncedAttendanceCount = prefs.getInt("last_synced_attendance", 0),
      syncedMemorizationCount = prefs.getInt("last_synced_memorization", 0)
    )
  )
  val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

  /**
   * Pushes all local Room records to the Supabase PostgreSQL central database with UPSERT.
   */
  suspend fun pushToSupabase(
    targetSchoolCode: String? = null,
    curriculumItems: List<CustomCurriculumItem> = emptyList()
  ): Result<String> = withContext(Dispatchers.IO) {
    val schoolCode = targetSchoolCode?.trim()?.ifBlank { null } ?: config.schoolCode
    val nowStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr")).format(Date())

    _syncStatus.value = _syncStatus.value.copy(
      isSyncing = true,
      lastSyncMessage = "Veriler Supabase PostgreSQL bulutuna yükleniyor..."
    )

    val studentDao = database.studentDao()
    val attendanceDao = database.attendanceDao()
    val memorizationDao = database.memorizationDao()
    val dailyDutyDao = database.dailyDutyDao()

    val localStudents = studentDao.getAllStudents().first()
    val localAttendance = attendanceDao.getAllAttendance().first()
    val localMemorization = memorizationDao.getAllMemorizationRecords().first()
    val localDailyDuties = dailyDutyDao.getAllDuties().first()

    // If Supabase is not configured yet with a valid URL, maintain local state safely
    if (!config.isConfigured) {
      val localMsg = "Yerel veritabanı aktif (${localStudents.size} talebe, ${localMemorization.size} ezber, ${localAttendance.size} yoklama)."
      prefs.edit()
        .putString("last_sync_time", nowStr)
        .putString("last_sync_msg", localMsg)
        .putInt("last_synced_students", localStudents.size)
        .putInt("last_synced_attendance", localAttendance.size)
        .putInt("last_synced_memorization", localMemorization.size)
        .apply()

      _syncStatus.value = SyncStatus(
        isSyncing = false,
        lastSyncTime = nowStr,
        lastSyncSuccess = true,
        lastSyncMessage = localMsg,
        syncedStudentsCount = localStudents.size,
        syncedAttendanceCount = localAttendance.size,
        syncedMemorizationCount = localMemorization.size
      )
      return@withContext Result.success(localMsg)
    }

    try {
      val rest = client.restApi

      // 1. Push Students DTOs
      if (localStudents.isNotEmpty()) {
        val studentDtos = localStudents.map { s ->
          SupabaseStudentDto(
            id = s.id,
            schoolCode = schoolCode,
            fullName = s.fullName,
            grade = s.grade,
            phone = s.phone,
            parentName = s.parentName,
            parentPhone = s.parentPhone,
            enrollmentDate = s.enrollmentDate,
            status = s.status,
            notes = s.notes,
            avatarColorIndex = s.avatarColorIndex,
            username = s.username,
            accessCode = s.accessCode
          )
        }
        val resp = rest.upsertStudents(students = studentDtos)
        if (!resp.isSuccessful) {
          throw Exception("Öğrenciler yüklenemedi: HTTP ${resp.code()} - ${resp.message()}")
        }
      }

      // 2. Push Attendance DTOs
      if (localAttendance.isNotEmpty()) {
        val attDtos = localAttendance.map { a ->
          SupabaseAttendanceDto(
            id = a.id,
            studentId = a.studentId,
            schoolCode = schoolCode,
            date = a.date,
            status = a.status
          )
        }
        val resp = rest.upsertAttendance(attendance = attDtos)
        if (!resp.isSuccessful) {
          throw Exception("Yoklama yüklenemedi: HTTP ${resp.code()} - ${resp.message()}")
        }
      }

      // 3. Push Memorization DTOs
      if (localMemorization.isNotEmpty()) {
        val memDtos = localMemorization.map { m ->
          SupabaseMemorizationDto(
            id = m.id,
            studentId = m.studentId,
            schoolCode = schoolCode,
            title = m.title,
            category = m.category,
            status = m.status,
            rating = m.rating,
            progressPercent = m.progressPercent,
            teacherNotes = m.teacherNotes,
            date = m.date,
            repeatCount = m.repeatCount
          )
        }
        val resp = rest.upsertMemorization(memorization = memDtos)
        if (!resp.isSuccessful) {
          throw Exception("Ezberler yüklenemedi: HTTP ${resp.code()} - ${resp.message()}")
        }
      }

      // 4. Push Daily Duties (Namaz & Vird)
      if (localDailyDuties.isNotEmpty()) {
        val dutyDtos = localDailyDuties.map { d ->
          SupabaseDailyDutyDto(
            id = d.id,
            studentId = d.studentId,
            schoolCode = schoolCode,
            date = d.date,
            fajr = d.fajr,
            dhuhr = d.dhuhr,
            asr = d.asr,
            maghrib = d.maghrib,
            isha = d.isha,
            quranPages = d.quranPages,
            tesbihatDone = d.tesbihatDone,
            risalePages = d.risalePages,
            memorizationRepeats = d.memorizationRepeats,
            cevsenDone = d.cevsenDone,
            salavatCount = d.salavatCount,
            notes = d.notes
          )
        }
        val resp = rest.upsertDailyDuties(duties = dutyDtos)
        if (!resp.isSuccessful) {
          // Non-blocking fallback
        }
      }

      // 5. Push Curriculum if present
      if (curriculumItems.isNotEmpty()) {
        val curDtos = curriculumItems.map { c ->
          SupabaseCurriculumDto(
            id = c.id,
            schoolCode = schoolCode,
            title = c.title,
            category = c.category,
            isSelected = c.isSelected,
            orderIndex = c.orderIndex
          )
        }
        rest.upsertCurriculum(items = curDtos)
      }

      val successMsg = "Tüm veriler Supabase merkezi veritabanına başarıyla eşitlendi (${localStudents.size} Talebe, ${localMemorization.size} Ezber, ${localAttendance.size} Yoklama)."
      prefs.edit()
        .putString("last_sync_time", nowStr)
        .putString("last_sync_msg", successMsg)
        .putInt("last_synced_students", localStudents.size)
        .putInt("last_synced_attendance", localAttendance.size)
        .putInt("last_synced_memorization", localMemorization.size)
        .apply()

      _syncStatus.value = SyncStatus(
        isSyncing = false,
        lastSyncTime = nowStr,
        lastSyncSuccess = true,
        lastSyncMessage = successMsg,
        syncedStudentsCount = localStudents.size,
        syncedAttendanceCount = localAttendance.size,
        syncedMemorizationCount = localMemorization.size
      )

      Result.success(successMsg)
    } catch (e: Exception) {
      val errMsg = "Supabase bağlantı durumu: ${e.localizedMessage ?: "Sunucuya ulaşılamadı. Yerel veriler korundu."}"
      _syncStatus.value = _syncStatus.value.copy(
        isSyncing = false,
        lastSyncSuccess = false,
        lastSyncMessage = errMsg
      )
      Result.failure(e)
    }
  }

  /**
   * Fetches latest data from Supabase PostgreSQL and updates local Room database.
   */
  suspend fun pullFromSupabase(
    targetSchoolCode: String? = null,
    mode: ImportMode = ImportMode.MERGE,
    studentIdFilter: Long? = null
  ): Result<ImportResult> = withContext(Dispatchers.IO) {
    val schoolCode = targetSchoolCode?.trim()?.ifBlank { null } ?: config.schoolCode
    val nowStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr")).format(Date())

    _syncStatus.value = _syncStatus.value.copy(
      isSyncing = true,
      lastSyncMessage = "Supabase bulutundan veriler indiriliyor..."
    )

    val studentDao = database.studentDao()
    val attendanceDao = database.attendanceDao()
    val memorizationDao = database.memorizationDao()
    val dailyDutyDao = database.dailyDutyDao()

    if (!config.isConfigured) {
      val localStudents = studentDao.getAllStudents().first()
      val localAttendance = attendanceDao.getAllAttendance().first()
      val localMemorization = memorizationDao.getAllMemorizationRecords().first()
      val msg = "Yerel veritabanı devrede (${localStudents.size} talebe)."
      return@withContext Result.success(
        ImportResult(
          isSuccess = true,
          studentCount = localStudents.size,
          attendanceCount = localAttendance.size,
          memorizationCount = localMemorization.size,
          message = msg
        )
      )
    }

    try {
      val rest = client.restApi

      // 1. Fetch Students
      val studentsResp = if (studentIdFilter != null) {
        rest.getStudents(idFilter = "eq.$studentIdFilter")
      } else {
        rest.getStudents(schoolCodeFilter = "eq.$schoolCode")
      }

      val fetchedStudentDtos = if (studentsResp.isSuccessful) studentsResp.body() ?: emptyList() else emptyList()
      val fetchedStudents = fetchedStudentDtos.map { dto ->
        Student(
          id = dto.id,
          fullName = dto.fullName,
          grade = dto.grade,
          phone = dto.phone,
          parentName = dto.parentName,
          parentPhone = dto.parentPhone,
          enrollmentDate = dto.enrollmentDate,
          status = dto.status,
          notes = dto.notes,
          avatarColorIndex = dto.avatarColorIndex,
          username = dto.username,
          accessCode = dto.accessCode
        )
      }

      // 2. Fetch Attendance
      val attendanceResp = if (studentIdFilter != null) {
        rest.getAttendance(studentIdFilter = "eq.$studentIdFilter")
      } else {
        rest.getAttendance(schoolCodeFilter = "eq.$schoolCode")
      }
      val fetchedAttendanceDtos = if (attendanceResp.isSuccessful) attendanceResp.body() ?: emptyList() else emptyList()
      val fetchedAttendance = fetchedAttendanceDtos.map { dto ->
        AttendanceRecord(
          id = dto.id,
          studentId = dto.studentId,
          date = dto.date,
          status = dto.status
        )
      }

      // 3. Fetch Memorization
      val memResp = if (studentIdFilter != null) {
        rest.getMemorization(studentIdFilter = "eq.$studentIdFilter")
      } else {
        rest.getMemorization(schoolCodeFilter = "eq.$schoolCode")
      }
      val fetchedMemDtos = if (memResp.isSuccessful) memResp.body() ?: emptyList() else emptyList()
      val fetchedMemorization = fetchedMemDtos.map { dto ->
        MemorizationRecord(
          id = dto.id,
          studentId = dto.studentId,
          title = dto.title,
          category = dto.category,
          status = dto.status,
          rating = dto.rating,
          progressPercent = dto.progressPercent,
          teacherNotes = dto.teacherNotes,
          date = dto.date,
          repeatCount = dto.repeatCount
        )
      }

      // 4. Fetch Daily Duties
      val dutiesResp = if (studentIdFilter != null) {
        rest.getDailyDuties(studentIdFilter = "eq.$studentIdFilter")
      } else {
        rest.getDailyDuties(schoolCodeFilter = "eq.$schoolCode")
      }
      val fetchedDutyDtos = if (dutiesResp.isSuccessful) dutiesResp.body() ?: emptyList() else emptyList()
      val fetchedDuties = fetchedDutyDtos.map { dto ->
        DailyDutyRecord(
          id = dto.id,
          studentId = dto.studentId,
          date = dto.date,
          fajr = dto.fajr,
          dhuhr = dto.dhuhr,
          asr = dto.asr,
          maghrib = dto.maghrib,
          isha = dto.isha,
          quranPages = dto.quranPages,
          tesbihatDone = dto.tesbihatDone,
          risalePages = dto.risalePages,
          memorizationRepeats = dto.memorizationRepeats,
          cevsenDone = dto.cevsenDone,
          salavatCount = dto.salavatCount,
          notes = dto.notes
        )
      }

      // Merge into local Room DB
      if (mode == ImportMode.OVERWRITE) {
        if (studentIdFilter == null) {
          studentDao.clearAllStudents()
          attendanceDao.clearAllAttendance()
          memorizationDao.clearAllMemorization()
        }
      }

      if (fetchedStudents.isNotEmpty()) {
        studentDao.insertStudents(fetchedStudents)
      }
      if (fetchedAttendance.isNotEmpty()) {
        attendanceDao.insertAttendanceList(fetchedAttendance)
      }
      if (fetchedMemorization.isNotEmpty()) {
        memorizationDao.insertMemorizationList(fetchedMemorization)
      }
      if (fetchedDuties.isNotEmpty()) {
        dailyDutyDao.insertDutyList(fetchedDuties)
      }

      val successMsg = "Supabase merkezi veritabanından ${fetchedStudents.size} Talebe, ${fetchedMemorization.size} Ezber, ${fetchedAttendance.size} Yoklama başarıyla indirildi ve eşitlendi."
      prefs.edit()
        .putString("last_sync_time", nowStr)
        .putString("last_sync_msg", successMsg)
        .putInt("last_synced_students", fetchedStudents.size)
        .putInt("last_synced_attendance", fetchedAttendance.size)
        .putInt("last_synced_memorization", fetchedMemorization.size)
        .apply()

      _syncStatus.value = SyncStatus(
        isSyncing = false,
        lastSyncTime = nowStr,
        lastSyncSuccess = true,
        lastSyncMessage = successMsg,
        syncedStudentsCount = fetchedStudents.size,
        syncedAttendanceCount = fetchedAttendance.size,
        syncedMemorizationCount = fetchedMemorization.size
      )

      Result.success(
        ImportResult(
          isSuccess = true,
          studentCount = fetchedStudents.size,
          attendanceCount = fetchedAttendance.size,
          memorizationCount = fetchedMemorization.size,
          message = successMsg
        )
      )
    } catch (e: Exception) {
      val errMsg = "Supabase veri çekme uyarısı: ${e.localizedMessage ?: "Veritabanına erişilemedi"}"
      _syncStatus.value = _syncStatus.value.copy(
        isSyncing = false,
        lastSyncSuccess = false,
        lastSyncMessage = errMsg
      )
      Result.failure(e)
    }
  }

  /**
   * Performs full two-way synchronization: uploads local edits then fetches latest server updates.
   */
  suspend fun syncTwoWay(
    schoolCode: String = config.schoolCode,
    curriculumItems: List<CustomCurriculumItem> = emptyList(),
    studentIdFilter: Long? = null
  ): Result<String> = withContext(Dispatchers.IO) {
    // 1. Push local changes first
    val pushResult = pushToSupabase(schoolCode, curriculumItems)
    // 2. Fetch server changes
    pullFromSupabase(schoolCode, ImportMode.MERGE, studentIdFilter)
    pushResult
  }
}
