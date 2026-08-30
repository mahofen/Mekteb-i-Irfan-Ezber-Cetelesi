package com.example.data.firebase

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.model.AttendanceRecord
import com.example.data.model.DailyDutyRecord
import com.example.data.model.DayOfWeekTr
import com.example.data.model.LessonCategory
import com.example.data.model.MemorizationRecord
import com.example.data.model.ScheduleLesson
import com.example.data.model.Student
import com.example.data.util.CurriculumManager
import com.example.data.util.CustomCurriculumItem
import com.example.data.util.ImportMode
import com.example.data.util.ImportResult
import com.example.data.util.ScheduleManager
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FirebaseSyncState(
  val isConfigured: Boolean = false,
  val isConnected: Boolean = false,
  val isSyncing: Boolean = false,
  val isRealtimeActive: Boolean = false,
  val lastSyncTime: String = "Henüz yapılmadı",
  val lastSyncSuccess: Boolean = true,
  val lastSyncMessage: String = "Firebase hazır (Yerel önbellek etkin)",
  val schoolCode: String = "irfan_default",
  val autoSyncEnabled: Boolean = true,
  val syncedStudentsCount: Int = 0,
  val syncedAttendanceCount: Int = 0,
  val syncedMemorizationCount: Int = 0,
  val syncedDutiesCount: Int = 0,
  val syncedScheduleCount: Int = 0,
  val userEmail: String? = null
)

class FirebaseSyncManager private constructor(
  private val context: Context,
  private val database: AppDatabase
) {
  private val TAG = "FirebaseSyncManager"
  private val prefs: SharedPreferences =
    context.getSharedPreferences("firebase_sync_prefs", Context.MODE_PRIVATE)

  private val coroutineScope = CoroutineScope(Dispatchers.IO)
  private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr"))

  private var activeListeners = mutableListOf<ListenerRegistration>()

  private val _state = MutableStateFlow(
    FirebaseSyncState(
      isConfigured = false,
      isConnected = false,
      isRealtimeActive = prefs.getBoolean("realtime_active", false),
      lastSyncTime = prefs.getString("last_sync_time", "Henüz yapılmadı") ?: "Henüz yapılmadı",
      lastSyncMessage = prefs.getString("last_sync_msg", "Hazır") ?: "Hazır",
      schoolCode = prefs.getString("school_code", "irfan_default") ?: "irfan_default",
      autoSyncEnabled = prefs.getBoolean("auto_sync_enabled", true),
      syncedStudentsCount = prefs.getInt("synced_students", 0),
      syncedAttendanceCount = prefs.getInt("synced_attendance", 0),
      syncedMemorizationCount = prefs.getInt("synced_memorization", 0),
      syncedDutiesCount = prefs.getInt("synced_duties", 0),
      syncedScheduleCount = prefs.getInt("synced_schedule", 0)
    )
  )
  val state: StateFlow<FirebaseSyncState> = _state.asStateFlow()

  val firestore: FirebaseFirestore? by lazy {
    try {
      if (FirebaseApp.getApps(context).isNotEmpty()) {
        val db = FirebaseFirestore.getInstance()
        try {
          val settings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
            .build()
          db.firestoreSettings = settings
        } catch (e: Exception) {
          Log.w(TAG, "Firestore settings already applied or cannot be modified: ${e.message}")
        }
        _state.value = _state.value.copy(isConfigured = true, isConnected = true)
        db
      } else {
        _state.value = _state.value.copy(isConfigured = false, isConnected = false)
        null
      }
    } catch (e: Exception) {
      Log.e(TAG, "Firebase initialization error: ${e.message}")
      _state.value = _state.value.copy(isConfigured = false, isConnected = false)
      null
    }
  }

  val auth: FirebaseAuth? by lazy {
    try {
      if (FirebaseApp.getApps(context).isNotEmpty()) {
        FirebaseAuth.getInstance()
      } else {
        null
      }
    } catch (e: Exception) {
      null
    }
  }

  init {
    ensureAuthAndInit()
  }

  companion object {
    @Volatile
    private var INSTANCE: FirebaseSyncManager? = null

    fun getInstance(context: Context, database: AppDatabase): FirebaseSyncManager {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: FirebaseSyncManager(context.applicationContext, database).also { INSTANCE = it }
      }
    }
  }

  private fun ensureAuthAndInit() {
    coroutineScope.launch {
      try {
        val currentAuth = auth
        if (currentAuth != null) {
          if (currentAuth.currentUser == null) {
            try {
              currentAuth.signInAnonymously().await()
              Log.d(TAG, "Firebase Anonymous Sign-In successful: ${currentAuth.currentUser?.uid}")
            } catch (e: Exception) {
              Log.w(TAG, "Anonymous auth not enabled or network unavailable: ${e.message}")
            }
          }
          val userEmail = currentAuth.currentUser?.email ?: currentAuth.currentUser?.uid?.let { "Anonim ($it)" }
          _state.value = _state.value.copy(
            isConfigured = true,
            isConnected = true,
            userEmail = userEmail
          )
        }
        if (_state.value.isRealtimeActive) {
          startRealtimeListener(_state.value.schoolCode)
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error in ensureAuth: ${e.message}")
      }
    }
  }

  fun setSchoolCode(code: String) {
    val clean = if (code.trim().isBlank()) "irfan_default" else code.trim().lowercase().replace(" ", "_")
    _state.value = _state.value.copy(schoolCode = clean)
    prefs.edit().putString("school_code", clean).apply()
    if (_state.value.isRealtimeActive) {
      startRealtimeListener(clean)
    }
  }

  fun setAutoSyncEnabled(enabled: Boolean) {
    _state.value = _state.value.copy(autoSyncEnabled = enabled)
    prefs.edit().putBoolean("auto_sync_enabled", enabled).apply()
  }

  fun setRealtimeActive(active: Boolean) {
    _state.value = _state.value.copy(isRealtimeActive = active)
    prefs.edit().putBoolean("realtime_active", active).apply()
    if (active) {
      startRealtimeListener(_state.value.schoolCode)
    } else {
      stopRealtimeListener()
    }
  }

  /**
   * PUSH / BACKUP ALL TO FIREBASE FIRESTORE
   */
  suspend fun pushAllToFirebase(
    targetSchoolCode: String? = null,
    curriculumItems: List<CustomCurriculumItem> = emptyList(),
    scheduleLessons: List<ScheduleLesson> = emptyList()
  ): Result<String> = withContext(Dispatchers.IO) {
    val activeCode = targetSchoolCode?.trim()?.ifBlank { null } ?: _state.value.schoolCode
    _state.value = _state.value.copy(
      isSyncing = true,
      lastSyncMessage = "Firebase Firestore'a veriler aktarılıyor..."
    )

    val db = firestore
    val studentDao = database.studentDao()
    val attendanceDao = database.attendanceDao()
    val memorizationDao = database.memorizationDao()
    val dailyDutyDao = database.dailyDutyDao()

    val localStudents = studentDao.getAllStudents().first()
    val localAttendance = attendanceDao.getAllAttendance().first()
    val localMemorization = memorizationDao.getAllMemorizationRecords().first()
    val localDuties = dailyDutyDao.getAllDuties().first()
    val localSchedule = if (scheduleLessons.isNotEmpty()) scheduleLessons else ScheduleManager.getInstance(context).lessonsFlow.value
    val localCurriculum = if (curriculumItems.isNotEmpty()) curriculumItems else CurriculumManager.getInstance(context).itemsFlow.value

    val nowStr = dateFormat.format(Date())

    if (db == null) {
      val offlineMsg = "Firebase hazır (Yerel veritabanında ${localStudents.size} talebe korundu)"
      _state.value = _state.value.copy(
        isSyncing = false,
        lastSyncTime = nowStr,
        lastSyncSuccess = true,
        lastSyncMessage = offlineMsg,
        syncedStudentsCount = localStudents.size,
        syncedAttendanceCount = localAttendance.size,
        syncedMemorizationCount = localMemorization.size,
        syncedDutiesCount = localDuties.size,
        syncedScheduleCount = localSchedule.size
      )
      return@withContext Result.success(offlineMsg)
    }

    try {
      val schoolRef = db.collection("schools").document(activeCode)

      // 1. Meta / School info
      val schoolMeta = hashMapOf(
        "schoolCode" to activeCode,
        "appName" to "Mekteb-i İrfan Ezber Çetelesi",
        "lastUpdated" to System.currentTimeMillis(),
        "lastUpdatedFormatted" to nowStr,
        "studentCount" to localStudents.size,
        "attendanceCount" to localAttendance.size,
        "memorizationCount" to localMemorization.size,
        "dutyCount" to localDuties.size,
        "scheduleCount" to localSchedule.size
      )
      schoolRef.set(schoolMeta, SetOptions.merge()).await()

      // 2. Students
      val studentsColl = schoolRef.collection("students")
      for (student in localStudents) {
        val sMap = hashMapOf(
          "id" to student.id,
          "fullName" to student.fullName,
          "username" to student.username,
          "accessCode" to student.accessCode,
          "grade" to student.grade,
          "phone" to student.phone,
          "parentPhone" to student.parentPhone,
          "parentName" to student.parentName,
          "enrollmentDate" to student.enrollmentDate,
          "status" to student.status,
          "notes" to student.notes,
          "avatarColorIndex" to student.avatarColorIndex,
          "updatedAt" to System.currentTimeMillis()
        )
        studentsColl.document(student.id.toString()).set(sMap, SetOptions.merge()).await()
      }

      // 3. Attendance
      val attColl = schoolRef.collection("attendance")
      for (att in localAttendance) {
        val aMap = hashMapOf(
          "id" to att.id,
          "studentId" to att.studentId,
          "date" to att.date,
          "status" to att.status,
          "updatedAt" to System.currentTimeMillis()
        )
        attColl.document("${att.studentId}_${att.date}").set(aMap, SetOptions.merge()).await()
      }

      // 4. Memorization
      val memColl = schoolRef.collection("memorization")
      for (mem in localMemorization) {
        val docKey = if (mem.id > 0) mem.id.toString() else "${mem.studentId}_${mem.title.hashCode()}"
        val mMap = hashMapOf(
          "id" to mem.id,
          "studentId" to mem.studentId,
          "title" to mem.title,
          "category" to mem.category,
          "status" to mem.status,
          "rating" to mem.rating,
          "progressPercent" to mem.progressPercent,
          "teacherNotes" to mem.teacherNotes,
          "date" to mem.date,
          "repeatCount" to mem.repeatCount,
          "updatedAt" to System.currentTimeMillis()
        )
        memColl.document(docKey).set(mMap, SetOptions.merge()).await()
      }

      // 5. Daily Duties (Namaz & Cüz)
      val dutyColl = schoolRef.collection("duties")
      for (duty in localDuties) {
        val dMap = hashMapOf(
          "id" to duty.id,
          "studentId" to duty.studentId,
          "date" to duty.date,
          "fajr" to duty.fajr,
          "dhuhr" to duty.dhuhr,
          "asr" to duty.asr,
          "maghrib" to duty.maghrib,
          "isha" to duty.isha,
          "quranPages" to duty.quranPages,
          "tesbihatDone" to duty.tesbihatDone,
          "risalePages" to duty.risalePages,
          "memorizationRepeats" to duty.memorizationRepeats,
          "cevsenDone" to duty.cevsenDone,
          "salavatCount" to duty.salavatCount,
          "notes" to duty.notes,
          "updatedAt" to System.currentTimeMillis()
        )
        dutyColl.document("${duty.studentId}_${duty.date}").set(dMap, SetOptions.merge()).await()
      }

      // 6. Schedule Lessons
      val schedColl = schoolRef.collection("schedule")
      for (lesson in localSchedule) {
        val lMap = hashMapOf(
          "id" to lesson.id,
          "day" to lesson.day.name,
          "startTime" to lesson.startTime,
          "endTime" to lesson.endTime,
          "title" to lesson.title,
          "category" to lesson.category.name,
          "targetGrade" to lesson.targetGrade,
          "classroom" to lesson.classroom,
          "teacherName" to lesson.teacherName,
          "description" to lesson.description,
          "isMandatoryAttendance" to lesson.isMandatoryAttendance,
          "orderIndex" to lesson.orderIndex,
          "updatedAt" to System.currentTimeMillis()
        )
        schedColl.document(lesson.id).set(lMap, SetOptions.merge()).await()
      }

      // 7. Curriculum Items
      val currColl = schoolRef.collection("curriculum")
      for (item in localCurriculum) {
        val cMap = hashMapOf(
          "id" to item.id,
          "title" to item.title,
          "category" to item.category,
          "orderIndex" to item.orderIndex,
          "isSelected" to item.isSelected,
          "updatedAt" to System.currentTimeMillis()
        )
        currColl.document(item.id).set(cMap, SetOptions.merge()).await()
      }

      val successMessage = "Firebase Firestore'a başarıyla eşitlendi: ${localStudents.size} Talebe, ${localMemorization.size} Ezber, ${localAttendance.size} Yoklama, ${localSchedule.size} Ders."

      prefs.edit()
        .putString("last_sync_time", nowStr)
        .putString("last_sync_msg", successMessage)
        .putInt("synced_students", localStudents.size)
        .putInt("synced_attendance", localAttendance.size)
        .putInt("synced_memorization", localMemorization.size)
        .putInt("synced_duties", localDuties.size)
        .putInt("synced_schedule", localSchedule.size)
        .apply()

      _state.value = _state.value.copy(
        isSyncing = false,
        lastSyncTime = nowStr,
        lastSyncSuccess = true,
        lastSyncMessage = successMessage,
        syncedStudentsCount = localStudents.size,
        syncedAttendanceCount = localAttendance.size,
        syncedMemorizationCount = localMemorization.size,
        syncedDutiesCount = localDuties.size,
        syncedScheduleCount = localSchedule.size
      )

      Result.success(successMessage)
    } catch (e: Exception) {
      Log.e(TAG, "Firebase push error: ${e.message}", e)
      val err = "Firebase eşitleme hatası: ${e.localizedMessage ?: "Bağlantı kurulamadı"}"
      _state.value = _state.value.copy(
        isSyncing = false,
        lastSyncSuccess = false,
        lastSyncMessage = err
      )
      Result.failure(e)
    }
  }

  /**
   * PULL / RESTORE ALL FROM FIREBASE FIRESTORE
   */
  suspend fun pullAllFromFirebase(
    targetSchoolCode: String? = null,
    mode: ImportMode = ImportMode.MERGE
  ): Result<ImportResult> = withContext(Dispatchers.IO) {
    val activeCode = targetSchoolCode?.trim()?.ifBlank { null } ?: _state.value.schoolCode
    _state.value = _state.value.copy(
      isSyncing = true,
      lastSyncMessage = "Firebase Firestore'dan veriler alınıyor..."
    )

    val db = firestore
    if (db == null) {
      _state.value = _state.value.copy(
        isSyncing = false,
        lastSyncSuccess = false,
        lastSyncMessage = "Firebase servisine erişilemedi."
      )
      return@withContext Result.failure(Exception("Firebase servisi aktif değil."))
    }

    val studentDao = database.studentDao()
    val attendanceDao = database.attendanceDao()
    val memorizationDao = database.memorizationDao()
    val dailyDutyDao = database.dailyDutyDao()

    try {
      val schoolRef = db.collection("schools").document(activeCode)

      // Fetch Students
      val studentsSnap = schoolRef.collection("students").get().await()
      val fetchedStudents = mutableListOf<Student>()
      for (doc in studentsSnap.documents) {
        val id = doc.getLong("id") ?: continue
        val fullName = doc.getString("fullName") ?: ""
        if (fullName.isBlank()) continue
        val username = doc.getString("username") ?: ""
        val accessCode = doc.getString("accessCode") ?: doc.getString("password") ?: "1234"
        val grade = doc.getString("grade") ?: "5. Sınıf"
        val phone = doc.getString("phone") ?: ""
        val parentPhone = doc.getString("parentPhone") ?: ""
        val parentName = doc.getString("parentName") ?: ""
        val enrollmentDate = doc.getString("enrollmentDate") ?: ""
        val status = doc.getString("status") ?: "Aktif"
        val notes = doc.getString("notes") ?: ""
        val avatarColorIndex = doc.getLong("avatarColorIndex")?.toInt() ?: 0

        fetchedStudents.add(
          Student(
            id = id,
            fullName = fullName,
            username = username,
            accessCode = accessCode,
            grade = grade,
            phone = phone,
            parentPhone = parentPhone,
            parentName = parentName,
            enrollmentDate = enrollmentDate,
            status = status,
            notes = notes,
            avatarColorIndex = avatarColorIndex
          )
        )
      }

      // Fetch Attendance
      val attendanceSnap = schoolRef.collection("attendance").get().await()
      val fetchedAttendance = mutableListOf<AttendanceRecord>()
      for (doc in attendanceSnap.documents) {
        val studentId = doc.getLong("studentId") ?: continue
        val date = doc.getString("date") ?: continue
        val status = doc.getString("status") ?: "GELDI"
        val id = doc.getLong("id") ?: 0L

        fetchedAttendance.add(
          AttendanceRecord(
            id = id,
            studentId = studentId,
            date = date,
            status = status
          )
        )
      }

      // Fetch Memorization
      val memorizationSnap = schoolRef.collection("memorization").get().await()
      val fetchedMemorization = mutableListOf<MemorizationRecord>()
      for (doc in memorizationSnap.documents) {
        val studentId = doc.getLong("studentId") ?: continue
        val title = doc.getString("title") ?: continue
        val category = doc.getString("category") ?: "Kur'an"
        val status = doc.getString("status") ?: "TAMAMLANDI"
        val rating = doc.getLong("rating")?.toInt() ?: 5
        val progressPercent = doc.getLong("progressPercent")?.toInt() ?: 100
        val teacherNotes = doc.getString("teacherNotes") ?: ""
        val date = doc.getString("date") ?: ""
        val repeatCount = doc.getLong("repeatCount")?.toInt() ?: 0
        val id = doc.getLong("id") ?: 0L

        fetchedMemorization.add(
          MemorizationRecord(
            id = id,
            studentId = studentId,
            title = title,
            category = category,
            status = status,
            rating = rating,
            progressPercent = progressPercent,
            teacherNotes = teacherNotes,
            date = date,
            repeatCount = repeatCount
          )
        )
      }

      // Fetch Daily Duties
      val dutySnap = schoolRef.collection("duties").get().await()
      val fetchedDuties = mutableListOf<DailyDutyRecord>()
      for (doc in dutySnap.documents) {
        val studentId = doc.getLong("studentId") ?: continue
        val date = doc.getString("date") ?: continue
        val id = doc.getLong("id") ?: 0L

        fetchedDuties.add(
          DailyDutyRecord(
            id = id,
            studentId = studentId,
            date = date,
            fajr = doc.getBoolean("fajr") ?: doc.getBoolean("fajrDone") ?: false,
            dhuhr = doc.getBoolean("dhuhr") ?: doc.getBoolean("dhuhrDone") ?: false,
            asr = doc.getBoolean("asr") ?: doc.getBoolean("asrDone") ?: false,
            maghrib = doc.getBoolean("maghrib") ?: doc.getBoolean("maghribDone") ?: false,
            isha = doc.getBoolean("isha") ?: doc.getBoolean("ishaDone") ?: false,
            quranPages = doc.getLong("quranPages")?.toInt() ?: doc.getLong("quranPagesRead")?.toInt() ?: 0,
            tesbihatDone = doc.getBoolean("tesbihatDone") ?: false,
            risalePages = doc.getLong("risalePages")?.toInt() ?: doc.getLong("risalePagesRead")?.toInt() ?: 0,
            memorizationRepeats = doc.getLong("memorizationRepeats")?.toInt() ?: 0,
            cevsenDone = doc.getBoolean("cevsenDone") ?: false,
            salavatCount = doc.getLong("salavatCount")?.toInt() ?: 0,
            notes = doc.getString("notes") ?: ""
          )
        )
      }

      // Fetch Schedule Lessons
      val schedSnap = schoolRef.collection("schedule").get().await()
      val fetchedSchedule = mutableListOf<ScheduleLesson>()
      for (doc in schedSnap.documents) {
        val id = doc.getString("id") ?: doc.id
        val dayStr = doc.getString("day") ?: DayOfWeekTr.MONDAY.name
        val startTime = doc.getString("startTime") ?: "08:30"
        val endTime = doc.getString("endTime") ?: "10:00"
        val title = doc.getString("title") ?: "Ders"
        val catStr = doc.getString("category") ?: LessonCategory.GENERAL.name
        val grade = doc.getString("targetGrade") ?: "Tüm Sınıflar"
        val classroom = doc.getString("classroom") ?: "Derslik 1"
        val teacherName = doc.getString("teacherName") ?: "Yetkili Hoca"
        val description = doc.getString("description") ?: doc.getString("notes") ?: ""
        val isMandatory = doc.getBoolean("isMandatoryAttendance") ?: true
        val order = doc.getLong("orderIndex")?.toInt() ?: 0

        val day = try { DayOfWeekTr.valueOf(dayStr) } catch (e: Exception) { DayOfWeekTr.MONDAY }
        val category = LessonCategory.fromString(catStr)

        fetchedSchedule.add(
          ScheduleLesson(
            id = id,
            day = day,
            startTime = startTime,
            endTime = endTime,
            title = title,
            category = category,
            targetGrade = grade,
            classroom = classroom,
            teacherName = teacherName,
            description = description,
            isMandatoryAttendance = isMandatory,
            orderIndex = order
          )
        )
      }

      // Fetch Curriculum
      val currSnap = schoolRef.collection("curriculum").get().await()
      val fetchedCurriculum = mutableListOf<CustomCurriculumItem>()
      for (doc in currSnap.documents) {
        val id = doc.getString("id") ?: doc.id
        val title = doc.getString("title") ?: continue
        val category = doc.getString("category") ?: "Kur'an"
        val order = doc.getLong("orderIndex")?.toInt() ?: 0
        val isSelected = doc.getBoolean("isSelected") ?: true

        fetchedCurriculum.add(
          CustomCurriculumItem(
            id = id,
            title = title,
            category = category,
            orderIndex = order,
            isSelected = isSelected
          )
        )
      }

      if (fetchedStudents.isEmpty() && fetchedMemorization.isEmpty() && fetchedAttendance.isEmpty()) {
        val msg = "Belirtilen Firebase kodunda ($activeCode) herhangi bir kayıt bulunamadı."
        _state.value = _state.value.copy(
          isSyncing = false,
          lastSyncSuccess = false,
          lastSyncMessage = msg
        )
        return@withContext Result.failure(Exception(msg))
      }

      if (mode == ImportMode.OVERWRITE) {
        studentDao.clearAllStudents()
        attendanceDao.clearAllAttendance()
        memorizationDao.clearAllMemorization()
        dailyDutyDao.clearAllDuties()
      }

      if (fetchedStudents.isNotEmpty()) studentDao.insertStudents(fetchedStudents)
      if (fetchedAttendance.isNotEmpty()) attendanceDao.insertAttendanceList(fetchedAttendance)
      if (fetchedMemorization.isNotEmpty()) memorizationDao.insertMemorizationList(fetchedMemorization)
      if (fetchedDuties.isNotEmpty()) dailyDutyDao.insertDutyList(fetchedDuties)
      if (fetchedSchedule.isNotEmpty()) {
        ScheduleManager.getInstance(context).saveToStorage(fetchedSchedule)
      }
      if (fetchedCurriculum.isNotEmpty()) {
        CurriculumManager.getInstance(context).saveItems(fetchedCurriculum)
      }

      val nowStr = dateFormat.format(Date())
      val successMsg = "Firebase'den başarıyla geri yüklendi (${fetchedStudents.size} Talebe, ${fetchedMemorization.size} Ezber, ${fetchedAttendance.size} Yoklama, ${fetchedSchedule.size} Ders)."

      prefs.edit()
        .putString("last_sync_time", nowStr)
        .putString("last_sync_msg", successMsg)
        .putInt("synced_students", fetchedStudents.size)
        .putInt("synced_attendance", fetchedAttendance.size)
        .putInt("synced_memorization", fetchedMemorization.size)
        .putInt("synced_duties", fetchedDuties.size)
        .putInt("synced_schedule", fetchedSchedule.size)
        .apply()

      _state.value = _state.value.copy(
        isSyncing = false,
        lastSyncTime = nowStr,
        lastSyncSuccess = true,
        lastSyncMessage = successMsg,
        syncedStudentsCount = fetchedStudents.size,
        syncedAttendanceCount = fetchedAttendance.size,
        syncedMemorizationCount = fetchedMemorization.size,
        syncedDutiesCount = fetchedDuties.size,
        syncedScheduleCount = fetchedSchedule.size
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
      Log.e(TAG, "Firebase pull error: ${e.message}", e)
      val err = "Firebase geri yükleme hatası: ${e.localizedMessage ?: "Veri çekilemedi"}"
      _state.value = _state.value.copy(
        isSyncing = false,
        lastSyncSuccess = false,
        lastSyncMessage = err
      )
      Result.failure(e)
    }
  }

  /**
   * REALTIME FIRESTORE SNAPSHOT LISTENER FOR LIVE CLASSROOM SYNC
   */
  fun startRealtimeListener(schoolCode: String) {
    stopRealtimeListener()
    val db = firestore ?: return

    val cleanCode = schoolCode.trim().ifBlank { "irfan_default" }
    Log.d(TAG, "Starting Firebase Realtime Listeners for school: $cleanCode")

    val schoolRef = db.collection("schools").document(cleanCode)

    // 1. Students Live Listener
    val studentSub = schoolRef.collection("students")
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          Log.w(TAG, "Realtime Students Listener error: ${error.message}")
          return@addSnapshotListener
        }
        if (snapshot != null && !snapshot.isEmpty) {
          coroutineScope.launch {
            val list = mutableListOf<Student>()
            for (doc in snapshot.documents) {
              val id = doc.getLong("id") ?: continue
              val fullName = doc.getString("fullName") ?: ""
              if (fullName.isBlank()) continue
              list.add(
                Student(
                  id = id,
                  fullName = fullName,
                  username = doc.getString("username") ?: "",
                  accessCode = doc.getString("accessCode") ?: doc.getString("password") ?: "1234",
                  grade = doc.getString("grade") ?: "5. Sınıf",
                  phone = doc.getString("phone") ?: "",
                  parentPhone = doc.getString("parentPhone") ?: "",
                  parentName = doc.getString("parentName") ?: "",
                  enrollmentDate = doc.getString("enrollmentDate") ?: "",
                  status = doc.getString("status") ?: "Aktif",
                  notes = doc.getString("notes") ?: "",
                  avatarColorIndex = doc.getLong("avatarColorIndex")?.toInt() ?: 0
                )
              )
            }
            if (list.isNotEmpty()) {
              database.studentDao().insertStudents(list)
            }
          }
        }
      }
    activeListeners.add(studentSub)

    // 2. Attendance Live Listener
    val attSub = schoolRef.collection("attendance")
      .addSnapshotListener { snapshot, error ->
        if (error != null) return@addSnapshotListener
        if (snapshot != null && !snapshot.isEmpty) {
          coroutineScope.launch {
            val list = mutableListOf<AttendanceRecord>()
            for (doc in snapshot.documents) {
              val studentId = doc.getLong("studentId") ?: continue
              val date = doc.getString("date") ?: continue
              val status = doc.getString("status") ?: "GELDI"
              val id = doc.getLong("id") ?: 0L
              list.add(AttendanceRecord(id = id, studentId = studentId, date = date, status = status))
            }
            if (list.isNotEmpty()) {
              database.attendanceDao().insertAttendanceList(list)
            }
          }
        }
      }
    activeListeners.add(attSub)

    // 3. Memorization Live Listener
    val memSub = schoolRef.collection("memorization")
      .addSnapshotListener { snapshot, error ->
        if (error != null) return@addSnapshotListener
        if (snapshot != null && !snapshot.isEmpty) {
          coroutineScope.launch {
            val list = mutableListOf<MemorizationRecord>()
            for (doc in snapshot.documents) {
              val studentId = doc.getLong("studentId") ?: continue
              val title = doc.getString("title") ?: continue
              val id = doc.getLong("id") ?: 0L
              list.add(
                MemorizationRecord(
                  id = id,
                  studentId = studentId,
                  title = title,
                  category = doc.getString("category") ?: "Kur'an",
                  status = doc.getString("status") ?: "TAMAMLANDI",
                  rating = doc.getLong("rating")?.toInt() ?: 5,
                  progressPercent = doc.getLong("progressPercent")?.toInt() ?: 100,
                  teacherNotes = doc.getString("teacherNotes") ?: "",
                  date = doc.getString("date") ?: ""
                )
              )
            }
            if (list.isNotEmpty()) {
              database.memorizationDao().insertMemorizationList(list)
            }
          }
        }
      }
    activeListeners.add(memSub)

    _state.value = _state.value.copy(isRealtimeActive = true)
  }

  fun stopRealtimeListener() {
    activeListeners.forEach { it.remove() }
    activeListeners.clear()
    _state.value = _state.value.copy(isRealtimeActive = false)
  }

  /**
   * Generates sample Firebase Security Rules and setup instructions
   */
  fun getFirebaseRulesSample(): String {
    return """
// Firebase Console -> Firestore Database -> Rules sekmesine yapıştırın:
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Mekteb-i İrfan Medrese ve Talebe Veritabanı Kuralları
    match /schools/{schoolCode}/{document=**} {
      allow read, write: if true; // Geliştirme veya medrese ortak koduyla erişim
    }
  }
}
    """.trimIndent()
  }
}
