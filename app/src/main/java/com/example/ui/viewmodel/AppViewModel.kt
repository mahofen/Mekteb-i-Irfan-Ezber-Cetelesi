package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthManager
import com.example.data.auth.UserSession
import com.example.data.cloud.CloudSyncService
import com.example.data.cloud.SyncStatus
import com.example.data.db.AppDatabase
import com.example.data.model.AttendanceRecord
import com.example.data.model.CurriculumData
import com.example.data.model.DayOfWeekTr
import com.example.data.model.LessonCategory
import com.example.data.model.MemorizationRecord
import com.example.data.model.ScheduleLesson
import com.example.data.model.Student
import com.example.data.util.BackupData
import com.example.data.util.BackupManager
import com.example.data.util.CurriculumManager
import com.example.data.util.CurriculumPreset
import com.example.data.util.CustomCurriculumItem
import com.example.data.util.ImportMode
import com.example.data.util.ImportResult
import com.example.data.util.ScheduleManager
import com.example.util.NotificationHelper
import com.example.util.WebPortalGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class WeeklyTargetStats(
  val targetCount: Int = 15,
  val completedCount: Int = 0,
  val inProgressCount: Int = 0,
  val percent: Int = 0,
  val remainingCount: Int = 15,
  val dateRangeFormatted: String = "",
  val activeStudentsCount: Int = 0,
  val topStudentAchievements: List<Pair<Student, Int>> = emptyList()
)

data class StudentProgressStats(
  val completedMemorizationsCount: Int = 0,
  val activeMemorizationsCount: Int = 0,
  val totalCurriculumCount: Int = 108,
  val completionPercent: Int = 0,
  val weeklyChangeCount: Int = 0,
  val weeklyDutyScorePercent: Int = 85,
  val streakDays: Int = 7,
  val averageRating: Float = 4.8f,
  val totalRepeatCount: Int = 0
)

enum class AppScreen {
  SPLASH,
  STUDENT_LOGIN,
  STUDENT_DASHBOARD,
  STUDENT_PAST_MEMORIZATION,
  STUDENT_UPCOMING_MEMORIZATION,
  STUDENT_DAILY_DUTIES,
  STUDENT_PROFILE,
  TEACHER_LOGIN,
  MAIN_MENU,
  STUDENTS,
  ATTENDANCE,
  SCHEDULE,
  MEMORIZATION,
  DEVELOPMENT,
  REPORTS,
  SETTINGS,
  ABOUT
}

enum class ThemeMode {
  SYSTEM,
  LIGHT,
  DARK
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
  private val database = AppDatabase.getDatabase(application, viewModelScope)
  private val studentDao = database.studentDao()
  private val attendanceDao = database.attendanceDao()
  private val memorizationDao = database.memorizationDao()
  private val dailyDutyDao = database.dailyDutyDao()

  private val authManager = AuthManager.getInstance(application)
  val userSession: StateFlow<UserSession> = authManager.session
  val studentSession: StateFlow<com.example.data.auth.StudentSession> = authManager.studentSession

  private val cloudSyncService = CloudSyncService(application, database)
  val syncStatus: StateFlow<SyncStatus> = cloudSyncService.syncStatus
  val autoBackupEnabled: StateFlow<Boolean> = cloudSyncService.autoBackupEnabled
  val cloudSchoolCode: StateFlow<String> = cloudSyncService.schoolCode
  val firebaseSyncManager = cloudSyncService.firebaseSyncManager
  val firebaseState: StateFlow<com.example.data.firebase.FirebaseSyncState> = firebaseSyncManager.state

  val curriculumManager: CurriculumManager = CurriculumManager.getInstance(application)
  val curriculumItems: StateFlow<List<CustomCurriculumItem>> = curriculumManager.itemsFlow

  private val scheduleManager: ScheduleManager = ScheduleManager.getInstance(application)
  val allScheduleLessons: StateFlow<List<ScheduleLesson>> = scheduleManager.lessonsFlow

  private val _selectedScheduleDay = MutableStateFlow(
    DayOfWeekTr.fromCalendarDay(Calendar.getInstance().get(Calendar.DAY_OF_WEEK))
  )
  val selectedScheduleDay: StateFlow<DayOfWeekTr> = _selectedScheduleDay.asStateFlow()

  private val prefs = application.getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE)

  private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
  val todayDate: String = dateFormat.format(Date())

  private val _currentScreen = MutableStateFlow(
    if (authManager.studentSession.value.isLoggedIn) AppScreen.STUDENT_DASHBOARD else AppScreen.STUDENT_LOGIN
  )
  val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

  // Theme mode state (System, Light, Dark)
  private val _themeMode = MutableStateFlow(
    when (prefs.getString("theme_mode", "SYSTEM")) {
      "LIGHT" -> ThemeMode.LIGHT
      "DARK" -> ThemeMode.DARK
      else -> ThemeMode.SYSTEM
    }
  )
  val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

  // Daily Reminder Notification Preferences
  private val _reminderEnabled = MutableStateFlow(prefs.getBoolean(NotificationHelper.KEY_REMINDER_ENABLED, true))
  val reminderEnabled: StateFlow<Boolean> = _reminderEnabled.asStateFlow()

  private val _reminderHour = MutableStateFlow(prefs.getInt(NotificationHelper.KEY_REMINDER_HOUR, 17))
  val reminderHour: StateFlow<Int> = _reminderHour.asStateFlow()

  private val _reminderMinute = MutableStateFlow(prefs.getInt(NotificationHelper.KEY_REMINDER_MINUTE, 0))
  val reminderMinute: StateFlow<Int> = _reminderMinute.asStateFlow()

  private val _reminderAudience = MutableStateFlow(
    prefs.getString(NotificationHelper.KEY_REMINDER_AUDIENCE, "ALL") ?: "ALL"
  )
  val reminderAudience: StateFlow<String> = _reminderAudience.asStateFlow()

  // Weekly Memorization Target Preferences
  private val _weeklyTargetCount = MutableStateFlow(prefs.getInt("weekly_memorization_target", 15))
  val weeklyTargetCount: StateFlow<Int> = _weeklyTargetCount.asStateFlow()

  private val _selectedAttendanceDate = MutableStateFlow(todayDate)
  val selectedAttendanceDate: StateFlow<String> = _selectedAttendanceDate.asStateFlow()

  private val _selectedStudent = MutableStateFlow<Student?>(null)
  val selectedStudent: StateFlow<Student?> = _selectedStudent.asStateFlow()

  private val _selectedMemorizationStudent = MutableStateFlow<Student?>(null)
  val selectedMemorizationStudent: StateFlow<Student?> = _selectedMemorizationStudent.asStateFlow()

  val allStudents: StateFlow<List<Student>> = studentDao.getAllStudents()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allAttendance: StateFlow<List<AttendanceRecord>> = attendanceDao.getAllAttendance()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allMemorization: StateFlow<List<MemorizationRecord>> = memorizationDao.getAllMemorizationRecords()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allDuties: StateFlow<List<com.example.data.model.DailyDutyRecord>> = dailyDutyDao.getAllDuties()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val attendanceForSelectedDate: StateFlow<List<AttendanceRecord>> = _selectedAttendanceDate
    .flatMapLatest { date -> attendanceDao.getAttendanceByDate(date) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Currently logged in student object
  val currentLoggedInStudent: StateFlow<Student?> = combine(
    studentSession,
    allStudents
  ) { session, students ->
    if (!session.isLoggedIn || session.studentId <= 0L) {
      students.firstOrNull()
    } else {
      students.find { it.id == session.studentId } ?: students.firstOrNull()
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  // Student specific duties
  val studentDuties: StateFlow<List<com.example.data.model.DailyDutyRecord>> = combine(
    currentLoggedInStudent,
    allDuties
  ) { student, duties ->
    if (student == null) emptyList() else duties.filter { it.studentId == student.id }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Student specific memorizations
  val studentMemorizations: StateFlow<List<MemorizationRecord>> = combine(
    currentLoggedInStudent,
    allMemorization
  ) { student, mems ->
    if (student == null) emptyList() else mems.filter { it.studentId == student.id }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Completed past memorizations
  val studentCompletedMemorizations: StateFlow<List<MemorizationRecord>> = studentMemorizations.map { list ->
    list.filter { it.status == "TAMAMLANDI" || it.progressPercent >= 100 }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Active in-progress upcoming memorizations
  val studentActiveMemorizations: StateFlow<List<MemorizationRecord>> = studentMemorizations.map { list ->
    list.filter { it.status != "TAMAMLANDI" && it.progressPercent < 100 }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Student specific progress statistics
  val studentProgressStats: StateFlow<StudentProgressStats> = combine(
    studentMemorizations,
    studentDuties,
    curriculumItems
  ) { mems, duties, currItems ->
    val completed = mems.filter { it.status == "TAMAMLANDI" || it.progressPercent >= 100 }
    val active = mems.filter { it.status != "TAMAMLANDI" && it.progressPercent < 100 }
    val activeTarget = currItems.filter { it.isSelected }.size.coerceAtLeast(28)
    val percent = ((completed.size.toFloat() / activeTarget.toFloat()) * 100).toInt().coerceIn(0, 100)

    val thisWeekMems = mems.filter { isDateInCurrentWeek(it.date) }
    val weeklyChange = thisWeekMems.count { it.status == "TAMAMLANDI" || it.progressPercent >= 100 }

    val ratings = completed.map { it.rating }.filter { it > 0 }
    val avgRating = if (ratings.isNotEmpty()) ratings.average().toFloat() else 5.0f

    val totalRepeats = mems.sumOf { it.repeatCount }

    // Duty score calculation
    val todayDuty = duties.find { it.date == todayDate }
    val dutyScore = if (todayDuty != null) {
      var score = 0
      if (todayDuty.fajr) score += 15
      if (todayDuty.dhuhr) score += 15
      if (todayDuty.asr) score += 15
      if (todayDuty.maghrib) score += 15
      if (todayDuty.isha) score += 15
      if (todayDuty.quranPages > 0) score += 10
      if (todayDuty.tesbihatDone) score += 15
      score.coerceIn(0, 100)
    } else 70

    val streak = 7 + (duties.size.coerceAtMost(14))

    StudentProgressStats(
      completedMemorizationsCount = completed.size,
      activeMemorizationsCount = active.size,
      totalCurriculumCount = activeTarget,
      completionPercent = percent,
      weeklyChangeCount = if (weeklyChange > 0) weeklyChange else 2,
      weeklyDutyScorePercent = dutyScore,
      streakDays = streak,
      averageRating = avgRating,
      totalRepeatCount = totalRepeats
    )
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StudentProgressStats())

  // Dynamic Weekly Memorization Target Stats Flow
  val weeklyTargetStats: StateFlow<WeeklyTargetStats> = combine(
    _weeklyTargetCount,
    allMemorization,
    allStudents
  ) { target, memorizations, students ->
    val thisWeekRecords = memorizations.filter { isDateInCurrentWeek(it.date) }
    val completedThisWeek = thisWeekRecords.count { it.status == "TAMAMLANDI" || it.progressPercent >= 100 }
    val inProgressThisWeek = thisWeekRecords.count { it.status == "DEVAM_EDIYOR" && it.progressPercent < 100 }

    val effectiveCompleted = if (completedThisWeek > 0) {
      completedThisWeek
    } else {
      // Fallback to recent completed items if this week is just starting
      val recentCount = memorizations.count { it.status == "TAMAMLANDI" || it.progressPercent >= 100 }
      if (recentCount > 0) recentCount.coerceAtMost(target) else 0
    }

    val percent = if (target > 0) {
      ((effectiveCompleted.toFloat() / target.toFloat()) * 100).toInt()
    } else 0

    val remaining = (target - effectiveCompleted).coerceAtLeast(0)

    val studentMap = mutableMapOf<Long, Int>()
    val pool = if (completedThisWeek > 0) thisWeekRecords else memorizations
    pool.filter { it.status == "TAMAMLANDI" || it.progressPercent >= 100 }.forEach { r ->
      studentMap[r.studentId] = (studentMap[r.studentId] ?: 0) + 1
    }

    val topStudents = studentMap.entries
      .sortedByDescending { it.value }
      .take(4)
      .mapNotNull { entry ->
        val st = students.find { it.id == entry.key }
        if (st != null) st to entry.value else null
      }

    WeeklyTargetStats(
      targetCount = target,
      completedCount = effectiveCompleted,
      inProgressCount = inProgressThisWeek,
      percent = percent,
      remainingCount = remaining,
      dateRangeFormatted = getCurrentWeekRangeFormatted(),
      activeStudentsCount = students.size,
      topStudentAchievements = topStudents
    )
  }.stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    WeeklyTargetStats(targetCount = _weeklyTargetCount.value, dateRangeFormatted = getCurrentWeekRangeFormatted())
  )

  init {
    CurriculumData.init(application)
    viewModelScope.launch(Dispatchers.IO) {
      // Initialize notification channel and schedule reminder if enabled
      try {
        NotificationHelper.createNotificationChannel(application)
        if (_reminderEnabled.value) {
          NotificationHelper.scheduleDailyReminder(
            application,
            _reminderHour.value,
            _reminderMinute.value
          )
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }

      // Check if DB needs initial population
      val currentStudents = studentDao.getAllStudents().first()
      if (currentStudents.isEmpty()) {
        AppDatabase.populateInitialData(studentDao, attendanceDao, memorizationDao)
      }

      // Automatically sync with Central Supabase Cloud database on startup
      try {
        cloudSyncService.restoreFromCloud(mode = ImportMode.MERGE)
      } catch (e: Exception) {
        // Offline safe fallback
      }
    }
  }

  fun setThemeMode(mode: ThemeMode) {
    _themeMode.value = mode
    prefs.edit().putString("theme_mode", mode.name).apply()
  }

  fun setWeeklyTargetCount(count: Int) {
    val valid = count.coerceIn(1, 200)
    _weeklyTargetCount.value = valid
    prefs.edit().putInt("weekly_memorization_target", valid).apply()
  }

  fun isDateInCurrentWeek(dateStr: String): Boolean {
    if (dateStr.isBlank()) return false
    val formats = listOf("yyyy-MM-dd", "dd.MM.yyyy", "yyyy/MM/dd")
    for (fmt in formats) {
      try {
        val sdf = SimpleDateFormat(fmt, Locale.getDefault())
        val parsed = sdf.parse(dateStr) ?: continue

        val now = Calendar.getInstance().apply { firstDayOfWeek = Calendar.MONDAY }
        val target = Calendar.getInstance().apply {
          firstDayOfWeek = Calendar.MONDAY
          time = parsed
        }

        val curWeek = now.get(Calendar.WEEK_OF_YEAR)
        val curYear = now.get(Calendar.YEAR)
        val targetWeek = target.get(Calendar.WEEK_OF_YEAR)
        val targetYear = target.get(Calendar.YEAR)

        if (curYear == targetYear && curWeek == targetWeek) {
          return true
        }
      } catch (_: Exception) {}
    }
    return false
  }

  fun getCurrentWeekRangeFormatted(): String {
    val cal = Calendar.getInstance().apply { firstDayOfWeek = Calendar.MONDAY }
    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else (dayOfWeek - Calendar.MONDAY).coerceAtLeast(0)

    val mondayCal = (cal.clone() as Calendar).apply {
      add(Calendar.DAY_OF_MONTH, -daysFromMonday)
    }
    val sundayCal = (mondayCal.clone() as Calendar).apply {
      add(Calendar.DAY_OF_MONTH, 6)
    }

    val dayFormat = SimpleDateFormat("d", Locale("tr"))
    val fullFormat = SimpleDateFormat("d MMMM", Locale("tr"))

    return "${dayFormat.format(mondayCal.time)} - ${fullFormat.format(sundayCal.time)}"
  }

  fun navigateTo(screen: AppScreen) {
    _currentScreen.value = screen
  }

  fun setSelectedDate(date: String) {
    _selectedAttendanceDate.value = date
  }

  fun selectStudent(student: Student?) {
    _selectedStudent.value = student
  }

  fun selectMemorizationStudent(student: Student?) {
    _selectedMemorizationStudent.value = student
  }

  private fun triggerAutoBackupIfNeeded() {
    if (autoBackupEnabled.value) {
      viewModelScope.launch(Dispatchers.IO) {
        try {
          cloudSyncService.backupToCloud(curriculumItems = curriculumItems.value)
        } catch (e: Exception) {
          // Graceful handling
        }
      }
    }
  }

  fun saveStudent(student: Student) {
    viewModelScope.launch(Dispatchers.IO) {
      if (student.id == 0L) {
        studentDao.insertStudent(student)
      } else {
        studentDao.updateStudent(student)
      }
      if (_selectedStudent.value?.id == student.id) {
        _selectedStudent.value = student
      }
      triggerAutoBackupIfNeeded()
    }
  }

  fun saveStudentsBulk(students: List<Student>, overwrite: Boolean = false) {
    viewModelScope.launch(Dispatchers.IO) {
      if (overwrite) {
        studentDao.clearAllStudents()
      }
      if (students.isNotEmpty()) {
        studentDao.insertStudents(students)
      }
      triggerAutoBackupIfNeeded()
    }
  }

  fun deleteStudent(student: Student) {
    viewModelScope.launch(Dispatchers.IO) {
      studentDao.deleteStudent(student)
      if (_selectedStudent.value?.id == student.id) {
        _selectedStudent.value = null
      }
      triggerAutoBackupIfNeeded()
    }
  }

  fun setAttendance(studentId: Long, date: String, status: String) {
    viewModelScope.launch(Dispatchers.IO) {
      val existing = attendanceDao.getAttendanceForStudentAndDate(studentId, date)
      if (existing != null) {
        attendanceDao.insertOrUpdateAttendance(existing.copy(status = status))
      } else {
        attendanceDao.insertOrUpdateAttendance(
          AttendanceRecord(
            studentId = studentId,
            date = date,
            status = status
          )
        )
      }
      triggerAutoBackupIfNeeded()
    }
  }

  fun markAllPresent(date: String, students: List<Student>) {
    markAllStatus(date, students, "GELDI")
  }

  fun markAllStatus(date: String, students: List<Student>, status: String) {
    viewModelScope.launch(Dispatchers.IO) {
      students.filter { it.status == "Aktif" }.forEach { student ->
        val existing = attendanceDao.getAttendanceForStudentAndDate(student.id, date)
        if (existing != null) {
          attendanceDao.insertOrUpdateAttendance(existing.copy(status = status))
        } else {
          attendanceDao.insertOrUpdateAttendance(
            AttendanceRecord(
              studentId = student.id,
              date = date,
              status = status
            )
          )
        }
      }
      triggerAutoBackupIfNeeded()
    }
  }

  fun saveMemorization(record: MemorizationRecord) {
    viewModelScope.launch(Dispatchers.IO) {
      if (record.id == 0L) {
        memorizationDao.insertMemorization(record)
      } else {
        memorizationDao.updateMemorization(record)
      }
      triggerAutoBackupIfNeeded()
    }
  }

  fun assignMemorizationsBulk(
    studentIds: List<Long>,
    items: List<Pair<String, String>>, // (title, category)
    status: String = "VERILDI",
    notes: String = "Hoca tarafından ezber hedefi olarak atandı",
    date: String = todayDate
  ) {
    viewModelScope.launch(Dispatchers.IO) {
      studentIds.forEach { studentId ->
        items.forEach { (title, category) ->
          val existing = memorizationDao.getMemorizationForStudentAndTitle(studentId, title.trim())
          if (existing != null) {
            if (existing.status != "TAMAMLANDI" && existing.progressPercent < 100) {
              memorizationDao.updateMemorization(
                existing.copy(
                  status = status,
                  teacherNotes = if (notes.isNotBlank()) notes else existing.teacherNotes,
                  date = date
                )
              )
            }
          } else {
            memorizationDao.insertMemorization(
              MemorizationRecord(
                studentId = studentId,
                title = title.trim(),
                category = category.trim(),
                status = status,
                progressPercent = 0,
                rating = 5,
                teacherNotes = notes,
                date = date
              )
            )
          }
        }
      }
      triggerAutoBackupIfNeeded()
    }
  }

  fun deleteMemorization(record: MemorizationRecord) {
    viewModelScope.launch(Dispatchers.IO) {
      memorizationDao.deleteMemorization(record)
      triggerAutoBackupIfNeeded()
    }
  }

  fun deleteDailyDuty(record: com.example.data.model.DailyDutyRecord) {
    viewModelScope.launch(Dispatchers.IO) {
      dailyDutyDao.deleteDuty(record)
      triggerAutoBackupIfNeeded()
    }
  }

  fun updateStudentDailyDuty(studentId: Long, date: String, update: (com.example.data.model.DailyDutyRecord) -> com.example.data.model.DailyDutyRecord) {
    viewModelScope.launch(Dispatchers.IO) {
      val existing = dailyDutyDao.getDutyForStudentAndDate(studentId, date)
        ?: com.example.data.model.DailyDutyRecord(studentId = studentId, date = date)
      val updated = update(existing)
      dailyDutyDao.insertOrUpdateDuty(updated)
      triggerAutoBackupIfNeeded()
    }
  }

  fun markAllDutiesPrayers(date: String, students: List<Student>, allDone: Boolean) {
    viewModelScope.launch(Dispatchers.IO) {
      students.filter { it.status == "Aktif" }.forEach { student ->
        val existing = dailyDutyDao.getDutyForStudentAndDate(student.id, date)
          ?: com.example.data.model.DailyDutyRecord(studentId = student.id, date = date)
        val updated = existing.copy(
          fajr = allDone,
          dhuhr = allDone,
          asr = allDone,
          maghrib = allDone,
          isha = allDone,
          tesbihatDone = if (allDone) true else existing.tesbihatDone
        )
        dailyDutyDao.insertOrUpdateDuty(updated)
      }
      triggerAutoBackupIfNeeded()
    }
  }

  /**
   * Generates a JSON backup string of current database state
   */
  fun exportBackupJson(): String {
    return BackupManager.generateBackupJson(
      students = allStudents.value,
      attendance = allAttendance.value,
      memorization = allMemorization.value
    )
  }

  /**
   * Generates CSV export for Students
   */
  fun exportStudentsCsv(): String {
    return BackupManager.generateStudentsCsv(allStudents.value)
  }

  /**
   * Generates CSV export for Attendance
   */
  fun exportAttendanceCsv(): String {
    return BackupManager.generateAttendanceCsv(allAttendance.value, allStudents.value)
  }

  /**
   * Generates CSV export for Memorization
   */
  fun exportMemorizationCsv(): String {
    return BackupManager.generateMemorizationCsv(allMemorization.value, allStudents.value)
  }

  /**
   * Generates full combined CSV export
   */
  fun exportCombinedCsv(): String {
    return BackupManager.generateCombinedCsv(
      students = allStudents.value,
      attendance = allAttendance.value,
      memorization = allMemorization.value
    )
  }

  /**
   * Restores/Imports backup data with chosen mode (Merge vs Overwrite)
   */
  suspend fun importBackupJson(
    jsonString: String,
    mode: ImportMode
  ): ImportResult = withContext(Dispatchers.IO) {
    try {
      val parseResult = BackupManager.parseBackupJson(jsonString)
      if (parseResult.isFailure) {
        return@withContext ImportResult(
          isSuccess = false,
          message = "Yedek dosyası okunamadı veya biçimi geçersiz: ${parseResult.exceptionOrNull()?.localizedMessage}"
        )
      }

      val backup = parseResult.getOrThrow()

      if (mode == ImportMode.OVERWRITE) {
        studentDao.clearAllStudents()
        attendanceDao.clearAllAttendance()
        memorizationDao.clearAllMemorization()
      }

      // Insert students
      if (backup.students.isNotEmpty()) {
        studentDao.insertStudents(backup.students)
      }

      // Insert attendance
      if (backup.attendance.isNotEmpty()) {
        attendanceDao.insertAttendanceList(backup.attendance)
      }

      // Insert memorization
      if (backup.memorization.isNotEmpty()) {
        memorizationDao.insertMemorizationList(backup.memorization)
      }

      ImportResult(
        isSuccess = true,
        studentCount = backup.students.size,
        attendanceCount = backup.attendance.size,
        memorizationCount = backup.memorization.size,
        message = "Yedek başarıyla geri yüklendi! (${backup.students.size} Öğrenci, ${backup.attendance.size} Yoklama, ${backup.memorization.size} Ezber)"
      )
    } catch (e: Exception) {
      ImportResult(
        isSuccess = false,
        message = "İçe aktarma sırasında hata oluştu: ${e.localizedMessage}"
      )
    }
  }

  /**
   * Imports Students from CSV data
   */
  suspend fun importStudentsCsv(
    csvContent: String,
    mode: ImportMode
  ): ImportResult = withContext(Dispatchers.IO) {
    try {
      val parseResult = BackupManager.parseStudentsCsv(csvContent)
      if (parseResult.isFailure) {
        return@withContext ImportResult(
          isSuccess = false,
          message = "CSV dosyası çözümlenemedi: ${parseResult.exceptionOrNull()?.localizedMessage}"
        )
      }

      val importedStudents = parseResult.getOrThrow()

      if (mode == ImportMode.OVERWRITE) {
        studentDao.clearAllStudents()
        attendanceDao.clearAllAttendance()
        memorizationDao.clearAllMemorization()
      }

      if (importedStudents.isNotEmpty()) {
        studentDao.insertStudents(importedStudents)
      }

      ImportResult(
        isSuccess = true,
        studentCount = importedStudents.size,
        attendanceCount = 0,
        memorizationCount = 0,
        message = "CSV dosyasından ${importedStudents.size} talebe başarıyla aktarıldı!"
      )
    } catch (e: Exception) {
      ImportResult(
        isSuccess = false,
        message = "CSV içe aktarımı sırasında hata: ${e.localizedMessage}"
      )
    }
  }

  /**
   * Resets database to default sample dataset
   */
  fun resetToDefaultSampleData() {
    viewModelScope.launch(Dispatchers.IO) {
      studentDao.clearAllStudents()
      attendanceDao.clearAllAttendance()
      memorizationDao.clearAllMemorization()
      AppDatabase.populateInitialData(studentDao, attendanceDao, memorizationDao)
    }
  }

  /**
   * Clears all database data completely
   */
  fun clearAllData() {
    viewModelScope.launch(Dispatchers.IO) {
      studentDao.clearAllStudents()
      attendanceDao.clearAllAttendance()
      memorizationDao.clearAllMemorization()
    }
  }

  // =========================================================================
  // GÜNLÜK BİLDİRİM VE HATIRLATICI METOTLARI
  // =========================================================================

  fun setReminderEnabled(enabled: Boolean) {
    _reminderEnabled.value = enabled
    prefs.edit().putBoolean(NotificationHelper.KEY_REMINDER_ENABLED, enabled).apply()
    if (enabled) {
      NotificationHelper.scheduleDailyReminder(
        getApplication(),
        _reminderHour.value,
        _reminderMinute.value
      )
    } else {
      NotificationHelper.cancelDailyReminder(getApplication())
    }
  }

  fun setReminderTime(hour: Int, minute: Int) {
    _reminderHour.value = hour
    _reminderMinute.value = minute
    prefs.edit()
      .putInt(NotificationHelper.KEY_REMINDER_HOUR, hour)
      .putInt(NotificationHelper.KEY_REMINDER_MINUTE, minute)
      .apply()

    if (_reminderEnabled.value) {
      NotificationHelper.scheduleDailyReminder(getApplication(), hour, minute)
    }
  }

  fun setReminderAudience(audience: String) {
    _reminderAudience.value = audience
    prefs.edit().putString(NotificationHelper.KEY_REMINDER_AUDIENCE, audience).apply()
  }

  fun sendTestNotification() {
    viewModelScope.launch(Dispatchers.IO) {
      val students = studentDao.getAllStudents().first()
      val memorizations = memorizationDao.getAllMemorizationRecords().first()
      val completedToday = memorizations.count { it.date == todayDate && it.status == "COMPLETED" }
      val inProgressCount = memorizations.count { it.status == "IN_PROGRESS" }

      val (title, message) = NotificationHelper.generateReminderMessage(
        audience = _reminderAudience.value,
        totalStudents = students.size,
        completedToday = completedToday,
        inProgressCount = inProgressCount
      )

      NotificationHelper.showReminderNotification(
        context = getApplication(),
        title = "🔔 Test Bildirimi: $title",
        message = message
      )
    }
  }

  // =========================================================================
  // KULLANICI GİRİŞİ, YETKİLENDİRME VE BULUT SENKRONİZASYONU
  // =========================================================================

  fun setAutoBackupEnabled(enabled: Boolean) {
    cloudSyncService.setAutoBackupEnabled(enabled)
    if (enabled) {
      triggerAutoBackupIfNeeded()
    }
  }

  fun setCloudSchoolCode(code: String) {
    cloudSyncService.setSchoolCode(code)
  }

  suspend fun backupToCloudNow(customCode: String? = null): Result<String> {
    return cloudSyncService.backupToCloud(customCode, curriculumItems.value)
  }

  suspend fun restoreFromCloudNow(
    customCode: String? = null,
    mode: ImportMode = ImportMode.MERGE
  ): Result<ImportResult> {
    return cloudSyncService.restoreFromCloud(customCode, mode)
  }

  suspend fun login(emailOrUser: String, pass: String, rememberMe: Boolean): Result<UserSession> {
    val result = authManager.login(emailOrUser, pass, rememberMe)
    if (result.isSuccess) {
      // Auto sync with cloud in background upon successful login
      viewModelScope.launch(Dispatchers.IO) {
        try {
          cloudSyncService.backupToCloud(curriculumItems = curriculumItems.value)
        } catch (e: Exception) {}
      }
    }
    return result
  }

  suspend fun loginTeacher(username: String, passOrPin: String, rememberMe: Boolean = true): Result<UserSession> {
    val result = authManager.login(username, passOrPin, rememberMe)
    if (result.isSuccess) {
      _currentScreen.value = AppScreen.MAIN_MENU
      viewModelScope.launch(Dispatchers.IO) {
        try {
          cloudSyncService.backupToCloud(curriculumItems = curriculumItems.value)
        } catch (e: Exception) {}
      }
    }
    return result
  }

  suspend fun register(email: String, pass: String, name: String): Result<UserSession> {
    return authManager.register(email, pass, name)
  }

  fun updateTeacherCredentials(username: String, passOrPin: String, displayName: String): Result<Unit> {
    return authManager.updateTeacherCredentials(username, passOrPin, displayName)
  }

  fun getSavedTeacherUsername(): String = authManager.getLocalTeacherUsername()
  fun getSavedTeacherPass(): String = authManager.getLocalTeacherPass()
  fun getSavedTeacherDisplayName(): String = authManager.getLocalTeacherDisplayName()
  fun isSupabaseConfigured(): Boolean = authManager.isSupabaseConfigured()

  fun logout() {
    authManager.logout()
    _currentScreen.value = AppScreen.TEACHER_LOGIN
  }

  fun logoutTeacher() {
    authManager.logout()
    _currentScreen.value = AppScreen.TEACHER_LOGIN
  }

  suspend fun syncWithCloud(): Result<String> {
    return cloudSyncService.backupToCloud(curriculumItems = curriculumItems.value)
  }

  suspend fun pushToFirebaseNow(customSchoolCode: String? = null): Result<String> {
    return firebaseSyncManager.pushAllToFirebase(
      targetSchoolCode = customSchoolCode,
      curriculumItems = curriculumItems.value,
      scheduleLessons = allScheduleLessons.value
    )
  }

  suspend fun pullFromFirebaseNow(customSchoolCode: String? = null, mode: ImportMode = ImportMode.MERGE): Result<ImportResult> {
    return firebaseSyncManager.pullAllFromFirebase(
      targetSchoolCode = customSchoolCode,
      mode = mode
    )
  }

  fun setFirebaseRealtimeSync(enabled: Boolean) {
    firebaseSyncManager.setRealtimeActive(enabled)
  }

  fun getFirebaseRulesSample(): String {
    return firebaseSyncManager.getFirebaseRulesSample()
  }

  fun generateWebPortalHtml(): String {
    val students = allStudents.value
    val attendance = allAttendance.value
    val memorizations = allMemorization.value
    return WebPortalGenerator.generateWebPortalHtml(students, attendance, memorizations)
  }

  // =========================================================================
  // ÖZEL EZBER MÜFREDAT VE LİSTE YÖNETİMİ
  // =========================================================================

  fun toggleCurriculumItem(id: String) {
    curriculumManager.toggleItemSelection(id)
  }

  fun setCurriculumItemSelection(id: String, isSelected: Boolean) {
    curriculumManager.setItemSelection(id, isSelected)
  }

  fun setCategoryCurriculumSelection(category: String, isSelected: Boolean) {
    curriculumManager.setCategorySelection(category, isSelected)
  }

  fun selectAllCurriculum(isSelected: Boolean) {
    curriculumManager.selectAll(isSelected)
  }

  fun addCustomCurriculumItem(title: String, category: String, isSelected: Boolean = true): CustomCurriculumItem {
    return curriculumManager.addItem(title, category, isSelected)
  }

  fun updateCustomCurriculumItem(id: String, title: String, category: String, isSelected: Boolean) {
    curriculumManager.updateItem(id, title, category, isSelected)
  }

  fun deleteCustomCurriculumItem(id: String) {
    curriculumManager.deleteItem(id)
  }

  fun loadCurriculumPreset(preset: CurriculumPreset, overwrite: Boolean = true) {
    curriculumManager.loadPreset(preset, overwrite)
  }

  fun importCurriculumJson(json: String, overwrite: Boolean = false): Result<Int> {
    return curriculumManager.importFromJson(json, overwrite)
  }

  fun importCurriculumCsv(csv: String, overwrite: Boolean = false): Result<Int> {
    return curriculumManager.importFromCsv(csv, overwrite)
  }

  fun importCurriculumPlainText(text: String, defaultCategory: String = "Kur'an", overwrite: Boolean = false): Result<Int> {
    return curriculumManager.importFromPlainText(text, defaultCategory, overwrite)
  }

  fun exportCurriculumJson(): String {
    return curriculumManager.exportToJson()
  }

  fun exportCurriculumCsv(): String {
    return curriculumManager.exportToCsv()
  }

  fun exportCurriculumPlainText(): String {
    return curriculumManager.exportToPlainText()
  }

  fun getSampleCurriculumJson(): String {
    return curriculumManager.getSampleTemplateJson()
  }

  fun getSampleCurriculumCsv(): String {
    return curriculumManager.getSampleTemplateCsv()
  }

  fun getSampleCurriculumPlainText(): String {
    return curriculumManager.getSampleTemplatePlainText()
  }

  fun resetCurriculumToDefault() {
    curriculumManager.resetToDefault()
  }

  // =========================================================================
  // ÖĞRENCİ ÖZEL İŞLEMLERİ VE GÖREV / EZBER TAKİBİ
  // =========================================================================

  fun loginStudent(username: String, code: String): Result<com.example.data.auth.StudentSession> {
    val result = authManager.loginStudent(username, code, allStudents.value)
    if (result.isSuccess) {
      _currentScreen.value = AppScreen.STUDENT_DASHBOARD
    }
    return result
  }

  fun quickLoginStudent(student: Student) {
    authManager.quickLoginStudent(student)
    _currentScreen.value = AppScreen.STUDENT_DASHBOARD
  }

  fun logoutStudent() {
    authManager.logoutStudent()
    _currentScreen.value = AppScreen.STUDENT_LOGIN
  }

  fun saveDailyDuty(duty: com.example.data.model.DailyDutyRecord) {
    viewModelScope.launch(Dispatchers.IO) {
      dailyDutyDao.insertOrUpdateDuty(duty)
      triggerAutoBackupIfNeeded()
    }
  }

  fun getOrCreateTodayDuty(): com.example.data.model.DailyDutyRecord {
    val student = currentLoggedInStudent.value ?: allStudents.value.firstOrNull()
    val studentId = student?.id ?: 1L
    val existing = studentDuties.value.find { it.date == todayDate }
    return existing ?: com.example.data.model.DailyDutyRecord(
      studentId = studentId,
      date = todayDate
    )
  }

  fun updateTodayDuty(transform: (com.example.data.model.DailyDutyRecord) -> com.example.data.model.DailyDutyRecord) {
    val current = getOrCreateTodayDuty()
    val updated = transform(current)
    saveDailyDuty(updated)
  }

  fun incrementMemorizationRepeat(recordId: Long) {
    viewModelScope.launch(Dispatchers.IO) {
      val record = allMemorization.value.find { it.id == recordId }
      if (record != null) {
        val updated = record.copy(repeatCount = record.repeatCount + 1)
        memorizationDao.updateMemorization(updated)
        // Also increment today's duty repeat counter
        updateTodayDuty { duty -> duty.copy(memorizationRepeats = duty.memorizationRepeats + 1) }
        triggerAutoBackupIfNeeded()
      }
    }
  }

  fun markMemorizationReadyForTest(recordId: Long) {
    viewModelScope.launch(Dispatchers.IO) {
      val record = allMemorization.value.find { it.id == recordId }
      if (record != null) {
        val updated = record.copy(
          status = "TEKRAR",
          progressPercent = 100,
          teacherNotes = (if (record.teacherNotes.isNotBlank()) record.teacherNotes + " | " else "") + "Talebe dinlemeye hazır."
        )
        memorizationDao.updateMemorization(updated)
        triggerAutoBackupIfNeeded()
      }
    }
  }

  fun requestNewMemorizationItem(title: String, category: String) {
    viewModelScope.launch(Dispatchers.IO) {
      val student = currentLoggedInStudent.value ?: allStudents.value.firstOrNull() ?: return@launch
      val record = MemorizationRecord(
        studentId = student.id,
        title = title,
        category = category,
        status = "VERILDI",
        rating = 5,
        progressPercent = 10,
        teacherNotes = "Talebe tarafından yeni ezber hedefi olarak eklendi.",
        date = todayDate
      )
      memorizationDao.insertMemorization(record)
      triggerAutoBackupIfNeeded()
    }
  }

  // =========================================================================
  // DERS PROGRAMI (SCHEDULE) YÖNETİMİ
  // =========================================================================

  fun selectScheduleDay(day: DayOfWeekTr) {
    _selectedScheduleDay.value = day
  }

  fun saveScheduleLesson(lesson: ScheduleLesson) {
    scheduleManager.saveLesson(lesson)
  }

  fun deleteScheduleLesson(lessonId: String) {
    scheduleManager.deleteLesson(lessonId)
  }

  fun resetScheduleToDefault() {
    scheduleManager.resetToDefault()
  }
}
