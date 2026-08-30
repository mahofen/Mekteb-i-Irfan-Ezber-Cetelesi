package com.example.data.cloud

import android.content.Context
import android.content.SharedPreferences
import com.example.data.db.AppDatabase
import com.example.data.firebase.FirebaseSyncManager
import com.example.data.model.AttendanceRecord
import com.example.data.model.MemorizationRecord
import com.example.data.model.Student
import com.example.data.supabase.SupabaseConfig
import com.example.data.supabase.SupabaseSyncEngine
import com.example.data.util.CustomCurriculumItem
import com.example.data.util.ImportMode
import com.example.data.util.ImportResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class SyncStatus(
  val isSyncing: Boolean = false,
  val lastSyncTime: String = "Henüz yapılmadı",
  val lastSyncSuccess: Boolean = true,
  val lastSyncMessage: String = "Hazır",
  val syncedStudentsCount: Int = 0,
  val syncedAttendanceCount: Int = 0,
  val syncedMemorizationCount: Int = 0
)

class CloudSyncService(
  private val context: Context,
  private val database: AppDatabase
) {
  private val prefs: SharedPreferences =
    context.getSharedPreferences("cloud_sync_prefs", Context.MODE_PRIVATE)

  val firebaseSyncManager = FirebaseSyncManager.getInstance(context, database)
  private val supabaseEngine = SupabaseSyncEngine(context, database)
  private val supabaseConfig = SupabaseConfig.getInstance(context)

  private val _autoBackupEnabled = MutableStateFlow(
    prefs.getBoolean("auto_backup_enabled", true)
  )
  val autoBackupEnabled: StateFlow<Boolean> = _autoBackupEnabled.asStateFlow()

  private val _schoolCode = MutableStateFlow(
    prefs.getString("school_code", "irfan_default") ?: "irfan_default"
  )
  val schoolCode: StateFlow<String> = _schoolCode.asStateFlow()

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

  fun setAutoBackupEnabled(enabled: Boolean) {
    _autoBackupEnabled.value = enabled
    firebaseSyncManager.setAutoSyncEnabled(enabled)
    prefs.edit().putBoolean("auto_backup_enabled", enabled).apply()
  }

  fun setSchoolCode(code: String) {
    val clean = if (code.trim().isBlank()) "irfan_default" else code.trim().lowercase().replace(" ", "_")
    _schoolCode.value = clean
    firebaseSyncManager.setSchoolCode(clean)
    supabaseConfig.schoolCode = clean
    prefs.edit().putString("school_code", clean).apply()
  }

  /**
   * Performs full backup / push to Firebase Firestore (with Supabase fallback).
   */
  suspend fun backupToCloud(
    targetSchoolCode: String? = null,
    curriculumItems: List<CustomCurriculumItem> = emptyList()
  ): Result<String> = withContext(Dispatchers.IO) {
    val activeCode = targetSchoolCode?.trim()?.ifBlank { null } ?: _schoolCode.value
    _syncStatus.value = _syncStatus.value.copy(
      isSyncing = true,
      lastSyncMessage = "Veriler Firebase Firestore'a aktarılıyor..."
    )

    // 1. Try Firebase Firestore Sync
    val fbRes = firebaseSyncManager.pushAllToFirebase(activeCode, curriculumItems)
    val fbState = firebaseSyncManager.state.value

    _syncStatus.value = SyncStatus(
      isSyncing = false,
      lastSyncTime = fbState.lastSyncTime,
      lastSyncSuccess = fbRes.isSuccess,
      lastSyncMessage = fbState.lastSyncMessage,
      syncedStudentsCount = fbState.syncedStudentsCount,
      syncedAttendanceCount = fbState.syncedAttendanceCount,
      syncedMemorizationCount = fbState.syncedMemorizationCount
    )

    prefs.edit()
      .putString("last_sync_time", fbState.lastSyncTime)
      .putString("last_sync_msg", fbState.lastSyncMessage)
      .putInt("last_synced_students", fbState.syncedStudentsCount)
      .putInt("last_synced_attendance", fbState.syncedAttendanceCount)
      .putInt("last_synced_memorization", fbState.syncedMemorizationCount)
      .apply()

    if (fbRes.isSuccess) {
      return@withContext fbRes
    }

    // 2. Try Supabase Sync Engine if configured
    if (supabaseConfig.isConfigured) {
      val supabaseRes = supabaseEngine.pushToSupabase(activeCode, curriculumItems)
      if (supabaseRes.isSuccess) {
        val sStatus = supabaseEngine.syncStatus.value
        _syncStatus.value = sStatus
        return@withContext supabaseRes
      }
    }

    return@withContext fbRes
  }

  /**
   * Restores all data from Firebase Firestore (with Supabase fallback).
   */
  suspend fun restoreFromCloud(
    targetSchoolCode: String? = null,
    mode: ImportMode = ImportMode.MERGE
  ): Result<ImportResult> = withContext(Dispatchers.IO) {
    val activeCode = targetSchoolCode?.trim()?.ifBlank { null } ?: _schoolCode.value
    _syncStatus.value = _syncStatus.value.copy(
      isSyncing = true,
      lastSyncMessage = "Firebase'den veriler indiriliyor..."
    )

    // 1. Try Firebase pull
    val fbRes = firebaseSyncManager.pullAllFromFirebase(activeCode, mode)
    val fbState = firebaseSyncManager.state.value

    _syncStatus.value = SyncStatus(
      isSyncing = false,
      lastSyncTime = fbState.lastSyncTime,
      lastSyncSuccess = fbRes.isSuccess,
      lastSyncMessage = fbState.lastSyncMessage,
      syncedStudentsCount = fbState.syncedStudentsCount,
      syncedAttendanceCount = fbState.syncedAttendanceCount,
      syncedMemorizationCount = fbState.syncedMemorizationCount
    )

    if (fbRes.isSuccess) {
      return@withContext fbRes
    }

    // 2. Try Supabase pull if configured
    if (supabaseConfig.isConfigured) {
      val supabaseRes = supabaseEngine.pullFromSupabase(activeCode, mode)
      if (supabaseRes.isSuccess) {
        val sStatus = supabaseEngine.syncStatus.value
        _syncStatus.value = sStatus
        return@withContext supabaseRes
      }
    }

    return@withContext fbRes
  }

  suspend fun syncAll(
    schoolCode: String = _schoolCode.value,
    curriculumItems: List<CustomCurriculumItem> = emptyList()
  ): Result<String> = backupToCloud(schoolCode, curriculumItems)
}

