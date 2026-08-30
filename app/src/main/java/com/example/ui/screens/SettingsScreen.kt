package com.example.ui.screens

import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.auth.UserSession
import com.example.data.cloud.SyncStatus
import com.example.data.model.AttendanceRecord
import com.example.data.model.MemorizationRecord
import com.example.data.model.Student
import com.example.data.util.BackupData
import com.example.data.util.BackupManager
import com.example.data.util.CurriculumManager
import com.example.data.util.CurriculumPreset
import com.example.data.util.CustomCurriculumItem
import com.example.data.util.ImportMode
import com.example.data.util.ImportResult
import com.example.ui.components.AppLogoEmblem
import com.example.ui.components.CurriculumImportDialog
import com.example.ui.components.CurriculumSelectionDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.ThemeMode
import com.example.util.NotificationHelper
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  themeMode: ThemeMode,
  students: List<Student>,
  attendanceList: List<AttendanceRecord>,
  memorizationList: List<MemorizationRecord>,
  userSession: UserSession = UserSession(isLoggedIn = true, email = "admin@irfan.org", displayName = "Eğitmen"),
  syncStatus: SyncStatus = SyncStatus(),
  reminderEnabled: Boolean = true,
  reminderHour: Int = 17,
  reminderMinute: Int = 0,
  reminderAudience: String = "ALL",
  autoBackupEnabled: Boolean = true,
  cloudSchoolCode: String = "irfan_default",
  onAutoBackupEnabledChange: (Boolean) -> Unit = {},
  onCloudSchoolCodeChange: (String) -> Unit = {},
  onBackupToCloudNow: suspend (String?) -> Result<String> = { Result.success("Yedeklendi") },
  onRestoreFromCloudNow: suspend (String?, ImportMode) -> Result<ImportResult> = { _, _ -> Result.success(ImportResult(true)) },
  onThemeModeChange: (ThemeMode) -> Unit,
  onReminderEnabledChange: (Boolean) -> Unit = {},
  onReminderTimeChange: (Int, Int) -> Unit = { _, _ -> },
  onReminderAudienceChange: (String) -> Unit = {},
  onSendTestNotification: () -> Unit = {},
  onSyncWithCloud: suspend () -> Result<String> = { Result.success("Eşitlendi") },
  onExportWebPortal: () -> String = { "" },
  onLogout: () -> Unit = {},
  onBackClick: () -> Unit,
  onExportBackupJson: () -> String,
  onExportStudentsCsv: () -> String = { BackupManager.generateStudentsCsv(students) },
  onExportAttendanceCsv: () -> String = { BackupManager.generateAttendanceCsv(attendanceList, students) },
  onExportMemorizationCsv: () -> String = { BackupManager.generateMemorizationCsv(memorizationList, students) },
  onExportCombinedCsv: () -> String = { BackupManager.generateCombinedCsv(students, attendanceList, memorizationList) },
  onImportBackup: suspend (String, ImportMode) -> ImportResult,
  onImportStudentsCsv: suspend (String, ImportMode) -> ImportResult = { _, _ -> ImportResult(false, message = "Desteklenmiyor") },
  onResetToSampleData: () -> Unit,
  onClearAllData: () -> Unit,
  onNavigateAbout: () -> Unit = {},
  viewModel: AppViewModel? = null
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  // Curriculum Management Dialog States
  var showCurriculumSelectionDialog by remember { mutableStateOf(false) }
  var showCurriculumImportDialog by remember { mutableStateOf(false) }

  // Selected format tab for Backup & Export/Import: 0: JSON (Tam Yedek), 1: CSV (Excel / Tablo)
  var selectedFormatTab by remember { mutableStateOf(0) }

  // Dialog & pending import states
  var showPasteDialog by remember { mutableStateOf(false) }
  var pasteDialogInitialFormat by remember { mutableStateOf("JSON") }

  var showPreviewJsonImportDialog by remember { mutableStateOf(false) }
  var pendingImportJson by remember { mutableStateOf("") }
  var pendingParsedBackup by remember { mutableStateOf<BackupData?>(null) }

  var showPreviewCsvImportDialog by remember { mutableStateOf(false) }
  var pendingImportCsv by remember { mutableStateOf("") }
  var pendingParsedCsvStudents by remember { mutableStateOf<List<Student>>(emptyList()) }

  var selectedImportMode by remember { mutableStateOf(ImportMode.MERGE) }
  var isImporting by remember { mutableStateOf(false) }

  var showResetConfirmDialog by remember { mutableStateOf(false) }
  var showClearConfirmDialog by remember { mutableStateOf(false) }
  var showSuccessDialog by remember { mutableStateOf(false) }
  var successMessage by remember { mutableStateOf("") }
  var showTeacherCredentialsDialog by remember { mutableStateOf(false) }
  var showFirebaseRulesDialog by remember { mutableStateOf(false) }

  val fbState by (viewModel?.firebaseState?.collectAsState() ?: remember { mutableStateOf(null) })

  // Notification Permission State for Android 13+
  var hasNotificationPermissionState by remember {
    mutableStateOf(NotificationHelper.hasNotificationPermission(context))
  }
  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    hasNotificationPermissionState = isGranted
    if (isGranted) {
      Toast.makeText(context, "Bildirim izni başarıyla verildi.", Toast.LENGTH_SHORT).show()
    } else {
      Toast.makeText(context, "Bildirim izni verilmedi. Hatırlatıcılar için izin gereklidir.", Toast.LENGTH_LONG).show()
    }
  }

  // File Picker for reading JSON or CSV files
  val filePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    if (uri != null) {
      try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val stringBuilder = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
          stringBuilder.append(line).append("\n")
        }
        reader.close()
        inputStream?.close()

        val fileContent = stringBuilder.toString().trim()

        // Auto-detect JSON or CSV based on content or active tab
        if (fileContent.startsWith("{") || selectedFormatTab == 0) {
          val parseResult = BackupManager.parseBackupJson(fileContent)
          if (parseResult.isSuccess) {
            pendingImportJson = fileContent
            pendingParsedBackup = parseResult.getOrNull()
            showPreviewJsonImportDialog = true
          } else {
            // Try fallback to CSV
            val csvResult = BackupManager.parseStudentsCsv(fileContent)
            if (csvResult.isSuccess) {
              pendingImportCsv = fileContent
              pendingParsedCsvStudents = csvResult.getOrNull() ?: emptyList()
              showPreviewCsvImportDialog = true
            } else {
              Toast.makeText(
                context,
                "Seçilen dosya geçerli bir JSON veya CSV dosyası değil!",
                Toast.LENGTH_LONG
              ).show()
            }
          }
        } else {
          // CSV Mode
          val csvResult = BackupManager.parseStudentsCsv(fileContent)
          if (csvResult.isSuccess) {
            pendingImportCsv = fileContent
            pendingParsedCsvStudents = csvResult.getOrNull() ?: emptyList()
            showPreviewCsvImportDialog = true
          } else {
            Toast.makeText(
              context,
              "CSV dosyası çözümlenemedi: ${csvResult.exceptionOrNull()?.localizedMessage}",
              Toast.LENGTH_LONG
            ).show()
          }
        }
      } catch (e: Exception) {
        Toast.makeText(
          context,
          "Dosya okunurken hata oluştu: ${e.localizedMessage}",
          Toast.LENGTH_LONG
        ).show()
      }
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Ayarlar & Veri Yönetimi",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
            Text(
              text = "Tema, JSON & CSV Yedekleme ve Aktarım",
              fontSize = 11.5.sp,
              color = Color(0xFFA7F3D0)
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onBackClick,
            modifier = Modifier.testTag("settings_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Geri",
              tint = Color.White
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = DeepBlueNavy
        )
      )
    },
    containerColor = CanvasBackground
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {

      // -------------------------------------------------------------
      // 🔔 GÜNLÜK EZBER VE TEKRAR HATIRLATICILARI (YEREL BİLDİRİMLER)
      // -------------------------------------------------------------
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = CanvasSurface),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            // Header with Switch
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
              ) {
                Box(
                  modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(FeatureMemorizationPurple.copy(alpha = 0.12f)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = FeatureMemorizationPurple,
                    modifier = Modifier.size(22.dp)
                  )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                  Text(
                    text = "🔔 Günlük Ezber & Tekrar Hatırlatıcısı",
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                  )
                  Text(
                    text = if (reminderEnabled) "Her gün ${String.format("%02d:%02d", reminderHour, reminderMinute)} vaktinde zarif hatırlatıcı gönderilir" else "Hatırlatıcılar şu anda kapalı",
                    fontSize = 11.5.sp,
                    color = TextSecondary
                  )
                }
              }

              Switch(
                checked = reminderEnabled,
                onCheckedChange = { isChecked ->
                  if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermissionState) {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                  }
                  onReminderEnabledChange(isChecked)
                },
                colors = SwitchDefaults.colors(
                  checkedThumbColor = Color.White,
                  checkedTrackColor = FeatureAttendanceGreen,
                  uncheckedThumbColor = TextMuted,
                  uncheckedTrackColor = SurfaceVariantColor
                )
              )
            }

            // Permission Warning if disabled on Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermissionState) {
              Spacer(modifier = Modifier.height(12.dp))
              Surface(
                color = Color(0xFFFEF2F2),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(
                      imageVector = Icons.Default.WarningAmber,
                      contentDescription = null,
                      tint = Color(0xFFDC2626),
                      modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = "Cihazınızda bildirim izni henüz verilmedi.",
                      fontSize = 12.sp,
                      color = Color(0xFF991B1B)
                    )
                  }
                  Button(
                    onClick = {
                      notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                  ) {
                    Text("İzin Ver", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                  }
                }
              }
            }

            if (reminderEnabled) {
              Spacer(modifier = Modifier.height(14.dp))
              Divider(color = BorderLight.copy(alpha = 0.5f))
              Spacer(modifier = Modifier.height(12.dp))

              // Hatırlatıcı Hedef Kitlesi / Modu
              Text(
                text = "👥 Hatırlatıcı Mesaj Tonu & Modu",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
              )
              Spacer(modifier = Modifier.height(8.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                val audienceOptions = listOf(
                  Triple("ALL", "🕌 Genel", "Dengeli"),
                  Triple("TEACHER", "📚 Hoca", "Ders Takibi"),
                  Triple("STUDENT", "🌿 Talebe", "15 dk Tekrar")
                )
                audienceOptions.forEach { (key, label, desc) ->
                  val isSelected = reminderAudience == key
                  Surface(
                    color = if (isSelected) FeatureMemorizationPurple.copy(alpha = 0.12f) else SurfaceVariantColor,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(
                      width = if (isSelected) 1.5.dp else 1.dp,
                      color = if (isSelected) FeatureMemorizationPurple else BorderLight
                    ),
                    modifier = Modifier
                      .weight(1f)
                      .clickable { onReminderAudienceChange(key) }
                  ) {
                    Column(
                      modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                      horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                      Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) FeatureMemorizationPurple else TextPrimary
                      )
                      Text(
                        text = desc,
                        fontSize = 10.sp,
                        color = if (isSelected) FeatureMemorizationPurple.copy(alpha = 0.8f) else TextSecondary
                      )
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(14.dp))

              // Hatırlatıcı Saati Seçimi
              Text(
                text = "⏰ Günlük Hatırlatma Saati",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
              )
              Spacer(modifier = Modifier.height(8.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                val timePresets = listOf(
                  Pair(9, 0) to "🌅 09:00",
                  Pair(16, 30) to "☀️ 16:30",
                  Pair(20, 0) to "🌙 20:00"
                )

                timePresets.forEach { (timePair, label) ->
                  val isSelected = reminderHour == timePair.first && reminderMinute == timePair.second
                  Surface(
                    color = if (isSelected) FeatureAttendanceGreen.copy(alpha = 0.15f) else SurfaceVariantColor,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(
                      width = if (isSelected) 1.5.dp else 1.dp,
                      color = if (isSelected) FeatureAttendanceGreen else BorderLight
                    ),
                    modifier = Modifier
                      .weight(1f)
                      .clickable { onReminderTimeChange(timePair.first, timePair.second) }
                  ) {
                    Box(
                      modifier = Modifier.padding(vertical = 9.dp),
                      contentAlignment = Alignment.Center
                    ) {
                      Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) FeatureAttendanceGreen else TextPrimary
                      )
                    }
                  }
                }

                // Custom Time Button
                val isCustomTime = timePresets.none { it.first.first == reminderHour && it.first.second == reminderMinute }
                Surface(
                  color = if (isCustomTime) FeatureAttendanceGreen.copy(alpha = 0.15f) else SurfaceVariantColor,
                  shape = RoundedCornerShape(10.dp),
                  border = androidx.compose.foundation.BorderStroke(
                    width = if (isCustomTime) 1.5.dp else 1.dp,
                    color = if (isCustomTime) FeatureAttendanceGreen else BorderLight
                  ),
                  modifier = Modifier
                    .weight(1f)
                    .clickable {
                      TimePickerDialog(
                        context,
                        { _, selectedHour, selectedMinute ->
                          onReminderTimeChange(selectedHour, selectedMinute)
                        },
                        reminderHour,
                        reminderMinute,
                        true
                      ).show()
                    }
                ) {
                  Row(
                    modifier = Modifier.padding(vertical = 9.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(
                      imageVector = Icons.Default.Schedule,
                      contentDescription = null,
                      tint = if (isCustomTime) FeatureAttendanceGreen else TextSecondary,
                      modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                      text = if (isCustomTime) String.format("%02d:%02d", reminderHour, reminderMinute) else "Özel",
                      fontSize = 12.sp,
                      fontWeight = if (isCustomTime) FontWeight.Bold else FontWeight.Medium,
                      color = if (isCustomTime) FeatureAttendanceGreen else TextPrimary
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(14.dp))

              // Test Notification Button
              OutlinedButton(
                onClick = {
                  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermissionState) {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                  } else {
                    onSendTestNotification()
                    Toast.makeText(context, "Örnek ezber hatırlatma bildirimi gönderildi!", Toast.LENGTH_SHORT).show()
                  }
                },
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, FeatureMemorizationPurple.copy(alpha = 0.6f)),
                colors = ButtonDefaults.outlinedButtonColors(
                  contentColor = FeatureMemorizationPurple
                ),
                modifier = Modifier.fillMaxWidth()
              ) {
                Icon(
                  imageVector = Icons.Default.NotificationsNone,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "🔔 Örnek Bildirim Gönder (Hemen Test Et)",
                  fontSize = 12.5.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }
      }

      // -------------------------------------------------------------
      // 📖 EZBER MÜFREDATI & ÖZEL LİSTE YÖNETİMİ (CSV / JSON / METİN)
      // -------------------------------------------------------------
      item {
        Card(
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = CanvasSurface),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              Box(
                modifier = Modifier
                  .size(42.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(FeatureMemorizationPurple),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.MenuBook,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(24.dp)
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "📖 Ezber Müfredatı & Liste Yönetimi",
                  fontSize = 15.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary
                )
                Text(
                  text = "Kendi CSV/JSON ezber listenizi yükleyin veya hazır paketleri seçin",
                  fontSize = 12.sp,
                  color = TextSecondary
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Curriculum Summary & Status pill
            val currentCurriculumItems = viewModel?.curriculumItems?.collectAsState()?.value
              ?: remember { CurriculumManager.getInstance(context).itemsFlow.value }
            val activeItemCount = currentCurriculumItems.count { it.isSelected }
            val totalItemCount = currentCurriculumItems.size
            val activeCategories = currentCurriculumItems.filter { it.isSelected }.map { it.category }.distinct()

            Surface(
              color = FeatureMemorizationPurple.copy(alpha = 0.08f),
              shape = RoundedCornerShape(14.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, FeatureMemorizationPurple.copy(alpha = 0.2f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Default.ChecklistRtl,
                      contentDescription = null,
                      tint = FeatureMemorizationPurple,
                      modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "Aktif Ezber Müfredatı:",
                      fontWeight = FontWeight.Bold,
                      fontSize = 13.sp,
                      color = DeepBlueNavy
                    )
                  }
                  Surface(
                    color = FeatureAttendanceGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                  ) {
                    Text(
                      text = "$activeItemCount / $totalItemCount Ezber Aktif",
                      fontWeight = FontWeight.Bold,
                      fontSize = 11.5.sp,
                      color = FeatureAttendanceGreen,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                  }
                }

                if (activeCategories.isNotEmpty()) {
                  Spacer(modifier = Modifier.height(8.dp))
                  LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(activeCategories) { cat ->
                      val catCount = currentCurriculumItems.count { it.category == cat && it.isSelected }
                      Surface(
                        color = CanvasSurface,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
                      ) {
                        Text(
                          text = "$cat ($catCount)",
                          fontSize = 11.sp,
                          color = TextSecondary,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                      }
                    }
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              // 1. "Ezber Listesi Yükle" button
              Button(
                onClick = { showCurriculumImportDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = FeatureAttendanceGreen),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                modifier = Modifier
                  .weight(1f)
                  .height(52.dp)
                  .testTag("settings_curriculum_upload_button")
              ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ezber Listesi", fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
              }

              // 2. "Ezberleri Seç & Düzenle" button
              Button(
                onClick = { showCurriculumSelectionDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = DeepBlueNavy),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                modifier = Modifier
                  .weight(1f)
                  .height(52.dp)
                  .testTag("settings_curriculum_select_button")
              ) {
                Icon(Icons.Default.Checklist, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ezberleri Seç", fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Info Note
            Text(
              text = "💡 CSV, JSON veya düz metin ezber listesi yüklenebilir",
              fontSize = 11.5.sp,
              color = TextMuted,
              modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Alt Satır: Varsayılanları Sıfırla Butonu
            OutlinedButton(
              onClick = {
                viewModel?.resetCurriculumToDefault()
                Toast.makeText(context, "Müfredat varsayılan 108 ezber paketine sıfırlandı.", Toast.LENGTH_SHORT).show()
              },
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
              border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .testTag("settings_curriculum_reset_button")
            ) {
              Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextSecondary)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Varsayılanları Sıfırla (108 Ezber)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
            }
          }
        }
      }

      // -------------------------------------------------------------
      // ☁️ MERKEZİ SUPABASE BULUT VERİTABANI & CANLI SENKRONİZASYON
      // -------------------------------------------------------------
      item {
        var isSyncingLocal by remember { mutableStateOf(false) }
        var schoolCodeInput by remember(cloudSchoolCode) { mutableStateOf(cloudSchoolCode) }
        var showSchoolCodeDialog by remember { mutableStateOf(false) }

        Card(
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = CanvasSurface),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF97316).copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              Box(
                modifier = Modifier
                  .size(42.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(Color(0xFFEA580C)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.CloudSync,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(24.dp)
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "🔥 Firebase Firestore & Bulut Senkronizasyonu",
                  fontSize = 15.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary
                )
                Text(
                  text = "Gerçek zamanlı ortak veritabanı, çevrimdışı önbellek ve çoklu cihaz senkronizasyonu",
                  fontSize = 11.5.sp,
                  color = TextSecondary
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sync Status Pill
            Surface(
              color = if (syncStatus.lastSyncSuccess) FeatureAttendanceGreen.copy(alpha = 0.08f) else Color(0xFFFEF2F2),
              shape = RoundedCornerShape(12.dp),
              border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (syncStatus.lastSyncSuccess) FeatureAttendanceGreen.copy(alpha = 0.25f) else Color(0xFFFECACA)
              ),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                      modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (syncStatus.isSyncing) GoldStar else if (syncStatus.lastSyncSuccess) FeatureAttendanceGreen else Color(0xFFDC2626))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = if (syncStatus.isSyncing) "Eşitleniyor..." else if (syncStatus.lastSyncSuccess) "Firebase & Bulut Bağlantısı Canlı" else "Bağlantı Bekliyor",
                      fontWeight = FontWeight.Bold,
                      fontSize = 12.5.sp,
                      color = if (syncStatus.lastSyncSuccess) FeatureAttendanceGreen else Color(0xFFDC2626)
                    )
                  }
                  Text(
                    text = "Son Eşitleme: ${syncStatus.lastSyncTime}",
                    fontSize = 11.sp,
                    color = TextSecondary
                  )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = syncStatus.lastSyncMessage,
                  fontSize = 11.5.sp,
                  color = TextSecondary,
                  lineHeight = 15.sp
                )

                if (fbState != null) {
                  Spacer(modifier = Modifier.height(6.dp))
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = if (fbState?.isRealtimeActive == true) "🟢 Canlı Dinleyici (Snapshot) Aktif" else "⚪ Canlı Dinleyici Pasif",
                      fontSize = 11.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = if (fbState?.isRealtimeActive == true) FeatureAttendanceGreen else TextSecondary
                    )
                    if (fbState?.userEmail != null) {
                      Text(
                        text = "Oturum: ${fbState?.userEmail}",
                        fontSize = 10.5.sp,
                        color = TextSecondary
                      )
                    }
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // School / Branch Code Row
            Surface(
              color = SurfaceVariantColor.copy(alpha = 0.6f),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text("Kurum / Sınıf Kodu (Firebase Koleksiyon Anahtarı)", fontSize = 11.sp, color = TextSecondary)
                  Text(
                    text = cloudSchoolCode.ifBlank { "irfan_default" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepBlueNavy
                  )
                }
                OutlinedButton(
                  onClick = { showSchoolCodeDialog = true },
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                  modifier = Modifier.height(32.dp)
                ) {
                  Text("Kodu Değiştir", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Realtime Live Listener Toggle
            if (viewModel != null) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "🔄 Canlı Dinleyici (Firestore Snapshot)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                  )
                  Text(
                    text = "Diğer cihazlardan yapılan yoklama ve ezber girişlerini ekranınıza canlı düşürür",
                    fontSize = 11.sp,
                    color = TextSecondary
                  )
                }
                Switch(
                  checked = fbState?.isRealtimeActive ?: false,
                  onCheckedChange = { viewModel.setFirebaseRealtimeSync(it) },
                  colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFFEA580C)
                  )
                )
              }

              Spacer(modifier = Modifier.height(10.dp))
            }

            // Auto-backup toggle
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "⚡ Otomatik Anlık Bulut Senkronizasyonu",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = TextPrimary
                )
                Text(
                  text = "Yoklama veya ezber girildiğinde Firebase'e anında yazılır",
                  fontSize = 11.sp,
                  color = TextSecondary
                )
              }
              Switch(
                checked = autoBackupEnabled,
                onCheckedChange = { onAutoBackupEnabledChange(it) },
                colors = SwitchDefaults.colors(
                  checkedThumbColor = Color.White,
                  checkedTrackColor = Color(0xFFEA580C)
                )
              )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Button(
                onClick = {
                  coroutineScope.launch {
                    isSyncingLocal = true
                    val res = onBackupToCloudNow(cloudSchoolCode)
                    isSyncingLocal = false
                    if (res.isSuccess) {
                      Toast.makeText(context, "Firebase Firestore'a eşitlendi! 🔥", Toast.LENGTH_SHORT).show()
                    } else {
                      Toast.makeText(context, res.exceptionOrNull()?.localizedMessage ?: "Eşitleme hatası", Toast.LENGTH_LONG).show()
                    }
                  }
                },
                enabled = !isSyncingLocal && !syncStatus.isSyncing,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .weight(1.2f)
                  .height(48.dp)
              ) {
                if (isSyncingLocal) {
                  CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Eşitleniyor...", fontSize = 12.sp)
                } else {
                  Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Firebase'e Eşitle", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                }
              }

              OutlinedButton(
                onClick = {
                  coroutineScope.launch {
                    isSyncingLocal = true
                    val res = onRestoreFromCloudNow(cloudSchoolCode, ImportMode.MERGE)
                    isSyncingLocal = false
                    if (res.isSuccess) {
                      Toast.makeText(context, "Firebase'den veriler güncellendi! ✅", Toast.LENGTH_SHORT).show()
                    } else {
                      Toast.makeText(context, res.exceptionOrNull()?.localizedMessage ?: "Veri çekilemedi", Toast.LENGTH_LONG).show()
                    }
                  }
                },
                enabled = !isSyncingLocal && !syncStatus.isSyncing,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .weight(1f)
                  .height(48.dp)
              ) {
                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Geri Yükle", fontSize = 11.5.sp)
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Firebase Rules Guide Button
            OutlinedButton(
              onClick = { showFirebaseRulesDialog = true },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFEA580C))
              Spacer(modifier = Modifier.width(6.dp))
              Text("📋 Firestore Güvenlik Kuralları & Kurulum Rehberi", fontSize = 11.5.sp, color = TextPrimary)
            }
          }
        }

        // Change School Code Dialog
        if (showSchoolCodeDialog) {
          AlertDialog(
            onDismissRequest = { showSchoolCodeDialog = false },
            title = { Text("Kurum / Sınıf Kodu Belirle", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                  text = "Aynı kodu kullanan tüm eğitmenler ve talebeler aynı Firebase Firestore koleksiyonunu paylaşır:",
                  fontSize = 12.sp,
                  color = TextSecondary
                )
                OutlinedTextField(
                  value = schoolCodeInput,
                  onValueChange = { schoolCodeInput = it },
                  placeholder = { Text("Örn: irfan_ankara_1") },
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth()
                )
              }
            },
            confirmButton = {
              Button(
                onClick = {
                  onCloudSchoolCodeChange(schoolCodeInput.trim())
                  showSchoolCodeDialog = false
                  Toast.makeText(context, "Kurum kodu güncellendi: ${schoolCodeInput.trim()}", Toast.LENGTH_SHORT).show()
                }
              ) {
                Text("Kaydet")
              }
            },
            dismissButton = {
              TextButton(onClick = { showSchoolCodeDialog = false }) {
                Text("İptal")
              }
            }
          )
        }

        // Firebase Security Rules & Setup Guide Dialog
        if (showFirebaseRulesDialog) {
          val rulesText = viewModel?.getFirebaseRulesSample() ?: """
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /schools/{schoolCode}/{document=**} {
      allow read, write: if true;
    }
  }
}
          """.trimIndent()

          AlertDialog(
            onDismissRequest = { showFirebaseRulesDialog = false },
            title = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Firebase Firestore Kurulumu", fontWeight = FontWeight.Bold, fontSize = 16.sp)
              }
            },
            text = {
              Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                  text = "1. Firebase Console (console.firebase.google.com) üzerinden Firestore Database oluşturun.\n2. Rules (Kurallar) sekmesine aşağıdaki güvenlik kuralını yapıştırıp Publish edin:",
                  fontSize = 12.sp,
                  color = TextSecondary
                )

                Surface(
                  color = Color(0xFF1E293B),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text(
                    text = rulesText,
                    fontSize = 11.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = Color(0xFF38BDF8),
                    modifier = Modifier.padding(10.dp)
                  )
                }

                Button(
                  onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Firestore Rules", rulesText)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Güvenlik kuralları panoya kopyalandı! 📋", Toast.LENGTH_SHORT).show()
                  },
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(8.dp)
                ) {
                  Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Kuralları Kopyala", fontSize = 12.sp)
                }

                Text(
                  text = "3. Uygulama otomatik olarak çevrimdışı önbellek (Offline Persistence) ve gerçek zamanlı eşitleme ile çalışır.",
                  fontSize = 11.5.sp,
                  color = TextSecondary
                )
              }
            },
            confirmButton = {
              Button(onClick = { showFirebaseRulesDialog = false }) {
                Text("Tamam")
              }
            }
          )
        }
      }

      // -------------------------------------------------------------
      // 2. CİHAZLAR ARASI VERİ AKTARIMI VE YEDEKLEME MERKEZİ (JSON & CSV)
      // -------------------------------------------------------------
      item {
        Card(
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = CanvasSurface),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              Box(
                modifier = Modifier
                  .size(42.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(DeepBlueNavy),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.SyncAlt,
                  contentDescription = null,
                  tint = GoldStar,
                  modifier = Modifier.size(24.dp)
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "📱 Veri Aktarımı & Yedekleme",
                  fontSize = 16.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = TextPrimary
                )
                Text(
                  text = "JSON & CSV formatında dışa aktarın ve geri yükleyin",
                  fontSize = 12.sp,
                  color = TextSecondary
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Current Stats Badge
            Surface(
              color = SurfaceVariantColor.copy(alpha = 0.7f),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceAround
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text("${students.size}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FeatureStudentsBlue)
                  Text("Talebe", fontSize = 11.sp, color = TextSecondary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text("${attendanceList.size}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FeatureAttendanceGreen)
                  Text("Yoklama", fontSize = 11.sp, color = TextSecondary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text("${memorizationList.size}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FeatureMemorizationPurple)
                  Text("Ezber", fontSize = 11.sp, color = TextSecondary)
                }
              }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Format Selection Segmented Tabs (JSON vs CSV)
            Surface(
              color = SurfaceVariantColor,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(4.dp)
              ) {
                // Tab 0: JSON
                Surface(
                  color = if (selectedFormatTab == 0) DeepBlueNavy else Color.Transparent,
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier
                    .weight(1f)
                    .clickable { selectedFormatTab = 0 }
                ) {
                  Row(
                    modifier = Modifier.padding(vertical = 9.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(
                      imageVector = Icons.Default.Code,
                      contentDescription = null,
                      tint = if (selectedFormatTab == 0) GoldStar else TextSecondary,
                      modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "JSON (Tam Yedek)",
                      fontSize = 12.5.sp,
                      fontWeight = if (selectedFormatTab == 0) FontWeight.Bold else FontWeight.Medium,
                      color = if (selectedFormatTab == 0) Color.White else TextSecondary
                    )
                  }
                }

                // Tab 1: CSV
                Surface(
                  color = if (selectedFormatTab == 1) FeatureAttendanceGreen else Color.Transparent,
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier
                    .weight(1f)
                    .clickable { selectedFormatTab = 1 }
                ) {
                  Row(
                    modifier = Modifier.padding(vertical = 9.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(
                      imageVector = Icons.Default.TableChart,
                      contentDescription = null,
                      tint = if (selectedFormatTab == 1) Color.White else TextSecondary,
                      modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "CSV (Excel Tablosu)",
                      fontSize = 12.5.sp,
                      fontWeight = if (selectedFormatTab == 1) FontWeight.Bold else FontWeight.Medium,
                      color = if (selectedFormatTab == 1) Color.White else TextSecondary
                    )
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // =========================================================
            // FORMAT CONTENT: JSON MODE (TAB 0)
            // =========================================================
            if (selectedFormatTab == 0) {
              Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Info banner
                Surface(
                  color = FeatureStudentsBlue.copy(alpha = 0.08f),
                  shape = RoundedCornerShape(12.dp),
                  border = androidx.compose.foundation.BorderStroke(1.dp, FeatureStudentsBlue.copy(alpha = 0.2f)),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = FeatureStudentsBlue, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = "JSON formatı tüm talebeleri, yoklamaları ve ezber puanlarını eksiksiz paketler. Birebir telefon aktarımı için en güvenilir formattır.",
                      fontSize = 11.5.sp,
                      color = TextSecondary,
                      lineHeight = 15.sp
                    )
                  }
                }

                // EXPORT JSON
                Text(
                  text = "📤 JSON Olarak Dışa Aktar / Paylaş",
                  fontSize = 13.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = FeatureAttendanceGreen
                )

                Button(
                  onClick = {
                    val jsonStr = onExportBackupJson()
                    val fileName = BackupManager.getBackupFileName("json")
                    BackupManager.shareBackupData(context, jsonStr, fileName, "application/json")
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = FeatureAttendanceGreen),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("export_backup_button")
                ) {
                  Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text("Tam JSON Yedek Dosyasını Paylaş (.json Dosyası)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                OutlinedButton(
                  onClick = {
                    val jsonStr = onExportBackupJson()
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("MektebiIrfan_Yedek", jsonStr)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "JSON yedeği panoya kopyalandı! 📋", Toast.LENGTH_SHORT).show()
                  },
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("JSON Metnini Panoya Kopyala", fontSize = 12.5.sp)
                }

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = BorderLight)
                Spacer(modifier = Modifier.height(4.dp))

                // IMPORT JSON
                Text(
                  text = "📥 JSON Yedeği İçe Aktar / Geri Yükle",
                  fontSize = 13.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = FeatureStudentsBlue
                )

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Button(
                    onClick = { filePickerLauncher.launch("*/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = FeatureStudentsBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                      .weight(1.2f)
                      .testTag("import_file_button")
                  ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(".JSON Dosyası Seç", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                  }

                  OutlinedButton(
                    onClick = {
                      pasteDialogInitialFormat = "JSON"
                      showPasteDialog = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                      .weight(1f)
                      .testTag("paste_import_button")
                  ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Metin Yapıştır", fontSize = 12.sp)
                  }
                }
              }
            }

            // =========================================================
            // FORMAT CONTENT: CSV MODE (TAB 1)
            // =========================================================
            if (selectedFormatTab == 1) {
              Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Info banner for CSV
                Surface(
                  color = FeatureAttendanceGreen.copy(alpha = 0.08f),
                  shape = RoundedCornerShape(12.dp),
                  border = androidx.compose.foundation.BorderStroke(1.dp, FeatureAttendanceGreen.copy(alpha = 0.25f)),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(Icons.Default.TableChart, contentDescription = null, tint = FeatureAttendanceGreen, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = "CSV formatı Microsoft Excel ve Google E-Tablolar ile tam uyumludur. Talebe listelerini tablo olarak dışa aktarabilir veya Excel'den toplu talebe aktarabilirsiniz.",
                      fontSize = 11.5.sp,
                      color = TextSecondary,
                      lineHeight = 15.sp
                    )
                  }
                }

                // EXPORT CSV OPTIONS
                Text(
                  text = "📤 Excel / CSV Formatında Dışa Aktar",
                  fontSize = 13.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = FeatureAttendanceGreen
                )

                // 4 CSV Export Buttons
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  // 1. Students CSV
                  OutlinedButton(
                    onClick = {
                      val csvStr = onExportStudentsCsv()
                      val fileName = "mektebi_irfan_talebeler_${students.size}kisi.csv"
                      BackupManager.shareBackupData(context, csvStr, fileName, "text/csv")
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FeatureStudentsBlue),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("👨‍🎓 Talebe Listesi CSV İndir / Paylaş (${students.size} Kişi)", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                  }

                  // 2. Memorization CSV
                  OutlinedButton(
                    onClick = {
                      val csvStr = onExportMemorizationCsv()
                      val fileName = "mektebi_irfan_ezber_kayitlari_${memorizationList.size}adet.csv"
                      BackupManager.shareBackupData(context, csvStr, fileName, "text/csv")
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FeatureMemorizationPurple),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("📖 Ezber Kayıtları CSV İndir / Paylaş (${memorizationList.size} Kayıt)", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                  }

                  // 3. Attendance CSV
                  OutlinedButton(
                    onClick = {
                      val csvStr = onExportAttendanceCsv()
                      val fileName = "mektebi_irfan_yoklama_kayitlari_${attendanceList.size}adet.csv"
                      BackupManager.shareBackupData(context, csvStr, fileName, "text/csv")
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FeatureAttendanceGreen),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("📋 Yoklama Kayıtları CSV İndir / Paylaş (${attendanceList.size} Kayıt)", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                  }

                  // 4. Complete Combined CSV
                  Button(
                    onClick = {
                      val csvStr = onExportCombinedCsv()
                      val fileName = "mektebi_irfan_tam_rapor.csv"
                      BackupManager.shareBackupData(context, csvStr, fileName, "text/csv")
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FeatureDevelopmentTeal),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Icon(Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("📦 Tüm Tabloları Tek Birleşik CSV Olarak Paylaş", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                  }
                }

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = BorderLight)
                Spacer(modifier = Modifier.height(4.dp))

                // IMPORT CSV
                Text(
                  text = "📥 Excel / CSV Öğrenci Listesi İçe Aktar",
                  fontSize = 13.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = FeatureStudentsBlue
                )

                Text(
                  text = "Excel'de hazırladığınız veya başka programdan aldığınız öğrenci listesini (.csv) seçip doğrudan veritabanına aktarabilirsiniz:",
                  fontSize = 12.sp,
                  color = TextSecondary
                )

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Button(
                    onClick = { filePickerLauncher.launch("*/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = FeatureAttendanceGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1.2f)
                  ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(".CSV Dosyası Seç", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                  }

                  OutlinedButton(
                    onClick = {
                      pasteDialogInitialFormat = "CSV"
                      showPasteDialog = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                  ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("CSV Yapıştır", fontSize = 12.sp)
                  }
                }
              }
            }
          }
        }
      }

      // -------------------------------------------------------------
      // 🔐 EĞİTMEN VE YÖNETİCİ GİRİŞ KODU YÖNETİMİ
      // -------------------------------------------------------------
      item {
        Card(
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = CanvasSurface),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, DeepBlueNavy.copy(alpha = 0.3f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              Box(
                modifier = Modifier
                  .size(42.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(DeepBlueNavy),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.LockPerson,
                  contentDescription = null,
                  tint = TezhipGoldLight,
                  modifier = Modifier.size(24.dp)
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "🔐 Eğitmen & Giriş Kodu Yönetimi",
                  fontSize = 15.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary
                )
                Text(
                  text = "Kullanıcı adı ve 4-6 haneli yönetici giriş PIN kodunuzu güncelleyin",
                  fontSize = 11.5.sp,
                  color = TextSecondary
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Current Teacher Status Card
            Surface(
              color = SurfaceVariantColor.copy(alpha = 0.5f),
              shape = RoundedCornerShape(14.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "Aktif Oturum: ${userSession.displayName.ifBlank { "Yetkili Hoca" }}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = DeepBlueNavy
                  )
                  Surface(
                    color = FeatureAttendanceGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                  ) {
                    Text(
                      text = userSession.role,
                      fontSize = 10.5.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = FeatureAttendanceGreen,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }
                Text(
                  text = "Kullanıcı Adı: ${viewModel?.getSavedTeacherUsername() ?: "admin"} • E-posta: ${userSession.email}",
                  fontSize = 11.5.sp,
                  color = TextSecondary
                )
                Text(
                  text = "Kayıtlı Giriş Kodu: ${"•".repeat(viewModel?.getSavedTeacherPass()?.length ?: 4)}",
                  fontSize = 11.5.sp,
                  fontWeight = FontWeight.Medium,
                  color = TezhipGoldDark
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Button(
                onClick = { showTeacherCredentialsDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = DeepBlueNavy),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
              ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Giriş Kodunu Değiştir", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }

              OutlinedButton(
                onClick = onLogout,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
                modifier = Modifier.weight(1f)
              ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Paneli Kilitle", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      // -------------------------------------------------------------
      // 3. TEHLİKELİ BÖLGE / VERİ SIFIRLAMA (RESET DATA)
      // -------------------------------------------------------------
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Text(
              text = "⚠️ Veritabanı Sıfırlama Seçenekleri",
              fontSize = 14.5.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF991B1B)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Örnek kayıtları yeniden yükleyebilir veya tüm verileri temizleyebilirsiniz.",
              fontSize = 12.sp,
              color = Color(0xFFB91C1C)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedButton(
                onClick = { showResetConfirmDialog = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF991B1B)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
              ) {
                Text("Örnek Verileri Yükle", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
              }

              OutlinedButton(
                onClick = { showClearConfirmDialog = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
              ) {
                Text("Tüm Verileri Sil", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  }

  // -------------------------------------------------------------
  // DIALOG 1: PASTE TEXT DIALOG (SUPPORTS JSON & CSV TABS)
  // -------------------------------------------------------------
  if (showPasteDialog) {
    var rawText by remember { mutableStateOf("") }
    var pasteTab by remember { mutableStateOf(if (pasteDialogInitialFormat == "CSV") 1 else 0) }

    AlertDialog(
      onDismissRequest = { showPasteDialog = false },
      title = {
        Text("📋 Yedek Metnini Yapıştır", fontWeight = FontWeight.Bold, fontSize = 16.sp)
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          // Tab Switch for Paste Type
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            FilterChip(
              selected = pasteTab == 0,
              onClick = { pasteTab = 0 },
              label = { Text("JSON Metni", fontSize = 12.sp) }
            )
            FilterChip(
              selected = pasteTab == 1,
              onClick = { pasteTab = 1 },
              label = { Text("CSV / Excel Metni", fontSize = 12.sp) }
            )
          }

          Text(
            text = if (pasteTab == 0) {
              "Diğer cihazdan kopyaladığınız JSON yedek metnini aşağıdaki kutucuğa yapıştırın:"
            } else {
              "Excel'den veya tablodan kopyaladığınız öğrenci satırlarını (Ad, Sınıf, Telefon...) yapıştırın:"
            },
            fontSize = 12.sp,
            color = TextSecondary
          )

          OutlinedTextField(
            value = rawText,
            onValueChange = { rawText = it },
            placeholder = {
              Text(
                if (pasteTab == 0) "{\"appName\": \"Mekteb-i İrfan\", ...}"
                else "Ad Soyad, Sınıf, Telefon\nAhmet Yılmaz, 5. Sınıf, 05551234567",
                fontSize = 11.5.sp
              )
            },
            maxLines = 8,
            minLines = 4,
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary,
              focusedPlaceholderColor = TextMuted,
              unfocusedPlaceholderColor = TextMuted,
              focusedContainerColor = CanvasSurface,
              unfocusedContainerColor = CanvasSurface,
              cursorColor = FeatureStudentsBlue
            ),
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (rawText.isBlank()) {
              Toast.makeText(context, "Lütfen metin girin", Toast.LENGTH_SHORT).show()
              return@Button
            }

            if (pasteTab == 0) {
              // JSON Parse
              val parseResult = BackupManager.parseBackupJson(rawText)
              if (parseResult.isSuccess) {
                pendingImportJson = rawText
                pendingParsedBackup = parseResult.getOrNull()
                showPasteDialog = false
                showPreviewJsonImportDialog = true
              } else {
                Toast.makeText(context, "Geçersiz JSON formatı! Lütfen kontrol edin.", Toast.LENGTH_LONG).show()
              }
            } else {
              // CSV Parse
              val csvResult = BackupManager.parseStudentsCsv(rawText)
              if (csvResult.isSuccess) {
                pendingImportCsv = rawText
                pendingParsedCsvStudents = csvResult.getOrNull() ?: emptyList()
                showPasteDialog = false
                showPreviewCsvImportDialog = true
              } else {
                Toast.makeText(context, "CSV formatı okunamadı: ${csvResult.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG).show()
              }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = FeatureStudentsBlue)
        ) {
          Text("Devam Et")
        }
      },
      dismissButton = {
        TextButton(onClick = { showPasteDialog = false }) {
          Text("İptal")
        }
      }
    )
  }

  // -------------------------------------------------------------
  // DIALOG 2: PREVIEW & CONFIRM JSON IMPORT DIALOG
  // -------------------------------------------------------------
  if (showPreviewJsonImportDialog && pendingParsedBackup != null) {
    val backup = pendingParsedBackup!!
    AlertDialog(
      onDismissRequest = {
        if (!isImporting) showPreviewJsonImportDialog = false
      },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.CloudDownload, contentDescription = null, tint = FeatureStudentsBlue)
          Spacer(modifier = Modifier.width(8.dp))
          Text("JSON Yedeği Onayı", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Yedek dosyasında tespit edilen kayıtlar:",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
          )

          Surface(
            color = SurfaceVariantColor.copy(alpha = 0.5f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text("📅 Yedek Tarihi: ${backup.exportDate}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
              Text("👥 Öğrenci Sayısı: ${backup.students.size} Kişi", fontSize = 12.sp, color = FeatureStudentsBlue, fontWeight = FontWeight.Bold)
              Text("📋 Yoklama Kayıtları: ${backup.attendance.size} Adet", fontSize = 12.sp, color = FeatureAttendanceGreen, fontWeight = FontWeight.Bold)
              Text("📖 Ezber Kayıtları: ${backup.memorization.size} Adet", fontSize = 12.sp, color = FeatureMemorizationPurple, fontWeight = FontWeight.Bold)
            }
          }

          Spacer(modifier = Modifier.height(4.dp))

          Text("Yükleme Modunu Seçin:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

          // Radio option: MERGE
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .clickable { selectedImportMode = ImportMode.MERGE }
              .padding(vertical = 4.dp)
          ) {
            RadioButton(
              selected = selectedImportMode == ImportMode.MERGE,
              onClick = { selectedImportMode = ImportMode.MERGE }
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
              Text("Mevcut Verilere Ekle (Birleştir)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
              Text("Mevcut öğrencileri silmez, yenileri ekler", fontSize = 11.sp, color = TextSecondary)
            }
          }

          // Radio option: OVERWRITE
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .clickable { selectedImportMode = ImportMode.OVERWRITE }
              .padding(vertical = 4.dp)
          ) {
            RadioButton(
              selected = selectedImportMode == ImportMode.OVERWRITE,
              onClick = { selectedImportMode = ImportMode.OVERWRITE }
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
              Text("Tam Üzerine Yaz (Temizle & Geri Yükle)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
              Text("Tüm mevcut verileri silip sadece bu yedeği yükler", fontSize = 11.sp, color = Color(0xFFDC2626))
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            isImporting = true
            coroutineScope.launch {
              val result = onImportBackup(pendingImportJson, selectedImportMode)
              isImporting = false
              showPreviewJsonImportDialog = false
              if (result.isSuccess) {
                successMessage = result.message
                showSuccessDialog = true
              } else {
                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
              }
            }
          },
          enabled = !isImporting,
          colors = ButtonDefaults.buttonColors(containerColor = FeatureStudentsBlue)
        ) {
          if (isImporting) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Yükleniyor...")
          } else {
            Text("Yedekten Geri Yükle")
          }
        }
      },
      dismissButton = {
        TextButton(
          onClick = { showPreviewJsonImportDialog = false },
          enabled = !isImporting
        ) {
          Text("Vazgeç")
        }
      }
    )
  }

  // -------------------------------------------------------------
  // DIALOG 3: PREVIEW & CONFIRM CSV STUDENTS IMPORT DIALOG
  // -------------------------------------------------------------
  if (showPreviewCsvImportDialog && pendingParsedCsvStudents.isNotEmpty()) {
    AlertDialog(
      onDismissRequest = {
        if (!isImporting) showPreviewCsvImportDialog = false
      },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.TableChart, contentDescription = null, tint = FeatureAttendanceGreen)
          Spacer(modifier = Modifier.width(8.dp))
          Text("CSV Öğrenci Listesi Onayı", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "CSV tablosunda toplam ${pendingParsedCsvStudents.size} talebe bulundu:",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
          )

          // Preview first 3 students
          Surface(
            color = SurfaceVariantColor.copy(alpha = 0.5f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text("Önizleme (İlk Talebeler):", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
              pendingParsedCsvStudents.take(3).forEachIndexed { idx, s ->
                Text(
                  text = "${idx + 1}. ${s.fullName} (${s.grade})",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Medium,
                  color = DeepBlueNavy
                )
              }
              if (pendingParsedCsvStudents.size > 3) {
                Text("... ve ${pendingParsedCsvStudents.size - 3} talebe daha", fontSize = 11.sp, color = TextMuted)
              }
            }
          }

          Spacer(modifier = Modifier.height(4.dp))

          Text("Yükleme Modunu Seçin:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

          // Radio option: MERGE
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .clickable { selectedImportMode = ImportMode.MERGE }
              .padding(vertical = 4.dp)
          ) {
            RadioButton(
              selected = selectedImportMode == ImportMode.MERGE,
              onClick = { selectedImportMode = ImportMode.MERGE }
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
              Text("Mevcut Talebelere Ekle", fontSize = 13.sp, fontWeight = FontWeight.Bold)
              Text("Mevcut listeyi korur, bu talebeleri listeye ilave eder", fontSize = 11.sp, color = TextSecondary)
            }
          }

          // Radio option: OVERWRITE
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .clickable { selectedImportMode = ImportMode.OVERWRITE }
              .padding(vertical = 4.dp)
          ) {
            RadioButton(
              selected = selectedImportMode == ImportMode.OVERWRITE,
              onClick = { selectedImportMode = ImportMode.OVERWRITE }
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
              Text("Öğrenci Listesini Sıfırla & Yükle", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
              Text("Mevcut öğrencileri silip yalnızca CSV'dekileri ekler", fontSize = 11.sp, color = Color(0xFFDC2626))
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            isImporting = true
            coroutineScope.launch {
              val result = onImportStudentsCsv(pendingImportCsv, selectedImportMode)
              isImporting = false
              showPreviewCsvImportDialog = false
              if (result.isSuccess) {
                successMessage = result.message
                showSuccessDialog = true
              } else {
                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
              }
            }
          },
          enabled = !isImporting,
          colors = ButtonDefaults.buttonColors(containerColor = FeatureAttendanceGreen)
        ) {
          if (isImporting) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Aktarılıyor...")
          } else {
            Text("Talebeleri Aktar")
          }
        }
      },
      dismissButton = {
        TextButton(
          onClick = { showPreviewCsvImportDialog = false },
          enabled = !isImporting
        ) {
          Text("Vazgeç")
        }
      }
    )
  }

  // -------------------------------------------------------------
  // DIALOG 4: SUCCESS DIALOG
  // -------------------------------------------------------------
  if (showSuccessDialog) {
    AlertDialog(
      onDismissRequest = { showSuccessDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.CheckCircle, contentDescription = null, tint = FeatureAttendanceGreen)
          Spacer(modifier = Modifier.width(8.dp))
          Text("İşlem Başarılı", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
      },
      text = {
        Text(successMessage, fontSize = 13.sp, color = TextPrimary)
      },
      confirmButton = {
        Button(
          onClick = { showSuccessDialog = false },
          colors = ButtonDefaults.buttonColors(containerColor = FeatureAttendanceGreen)
        ) {
          Text("Tamam")
        }
      }
    )
  }

  // -------------------------------------------------------------
  // DIALOG 5: RESET TO DEFAULT CONFIRMATION
  // -------------------------------------------------------------
  if (showResetConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showResetConfirmDialog = false },
      title = { Text("Örnek Verileri Yükle", fontWeight = FontWeight.Bold) },
      text = {
        Text("Mevcut veriler silinip orijinal örnek talebeler ve ezberler geri yüklenecektir. Devam etmek istiyor musunuz?")
      },
      confirmButton = {
        Button(
          onClick = {
            onResetToSampleData()
            showResetConfirmDialog = false
            Toast.makeText(context, "Örnek veriler başarıyla yüklendi! 👍", Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991B1B))
        ) {
          Text("Evet, Yükle")
        }
      },
      dismissButton = {
        TextButton(onClick = { showResetConfirmDialog = false }) {
          Text("İptal")
        }
      }
    )
  }

  // -------------------------------------------------------------
  // DIALOG 6: CLEAR ALL CONFIRMATION
  // -------------------------------------------------------------
  if (showClearConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showClearConfirmDialog = false },
      title = { Text("Tüm Verileri Sil?", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626)) },
      text = {
        Text("Tüm öğrenci, yoklama ve ezber kayıtları kalıcı olarak silinecektir. Bu işlem geri alınamaz!")
      },
      confirmButton = {
        Button(
          onClick = {
            onClearAllData()
            showClearConfirmDialog = false
            Toast.makeText(context, "Tüm veritabanı temizlendi.", Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
        ) {
          Text("Tümünü Sil")
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearConfirmDialog = false }) {
          Text("Vazgeç")
        }
      }
    )
  }

  // DIALOG: CURRICULUM SELECTION & EDITING
  if (showCurriculumSelectionDialog && viewModel != null) {
    CurriculumSelectionDialog(
      viewModel = viewModel,
      onDismiss = { showCurriculumSelectionDialog = false },
      onOpenImport = {
        showCurriculumSelectionDialog = false
        showCurriculumImportDialog = true
      }
    )
  }

  // DIALOG: CURRICULUM IMPORT (CSV, JSON, PRESETS, TEXT)
  if (showCurriculumImportDialog && viewModel != null) {
    CurriculumImportDialog(
      viewModel = viewModel,
      onDismiss = { showCurriculumImportDialog = false },
      onCompleted = { showCurriculumImportDialog = false }
    )
  }

  // DIALOG: TEACHER CREDENTIALS MANAGEMENT
  if (showTeacherCredentialsDialog && viewModel != null) {
    var editUsername by remember { mutableStateOf(viewModel.getSavedTeacherUsername()) }
    var editDisplayName by remember { mutableStateOf(viewModel.getSavedTeacherDisplayName()) }
    var editPin by remember { mutableStateOf("") }
    var credError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
      onDismissRequest = { showTeacherCredentialsDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.LockPerson, contentDescription = null, tint = DeepBlueNavy)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Eğitmen Giriş Bilgilerini Güncelle", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          if (credError != null) {
            Text(credError!!, color = Color(0xFFDC2626), fontSize = 12.sp)
          }

          Text(
            text = "Eğitmen panelinize girişte kullanacağınız kullanıcı adı ve en az 4 haneli PIN kodunu belirleyin:",
            fontSize = 12.5.sp,
            color = TextSecondary
          )

          OutlinedTextField(
            value = editDisplayName,
            onValueChange = { editDisplayName = it },
            label = { Text("Eğitmen Adı / Unvanı") },
            placeholder = { Text("Örn: Ahmet Hoca") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = editUsername,
            onValueChange = { editUsername = it },
            label = { Text("Kullanıcı Adı") },
            placeholder = { Text("Örn: admin veya hoca") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = editPin,
            onValueChange = { if (it.length <= 12) editPin = it },
            label = { Text("Yeni Giriş PIN Kodu") },
            placeholder = { Text("En az 4 karakter (örn: 1234)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (editUsername.isBlank() || editPin.length < 4) {
              credError = "Lütfen geçerli bir kullanıcı adı ve en az 4 haneli PIN giriniz."
              return@Button
            }
            val res = viewModel.updateTeacherCredentials(editUsername, editPin, editDisplayName)
            if (res.isSuccess) {
              showTeacherCredentialsDialog = false
              Toast.makeText(context, "Eğitmen giriş kodu başarıyla güncellendi! ✅", Toast.LENGTH_SHORT).show()
            } else {
              credError = res.exceptionOrNull()?.message ?: "Hata oluştu."
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = DeepBlueNavy)
        ) {
          Text("Kaydet")
        }
      },
      dismissButton = {
        TextButton(onClick = { showTeacherCredentialsDialog = false }) {
          Text("İptal")
        }
      }
    )
  }
}
