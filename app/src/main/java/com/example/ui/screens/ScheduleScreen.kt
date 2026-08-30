package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AttendanceRecord
import com.example.data.model.DayOfWeekTr
import com.example.data.model.LessonCategory
import com.example.data.model.ScheduleLesson
import com.example.data.model.Student
import com.example.ui.components.GoldSubtleBorderGradient
import com.example.ui.components.SeljukStarBox
import com.example.ui.components.StudentAvatar
import com.example.ui.components.TezhipCard
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
  lessons: List<ScheduleLesson>,
  students: List<Student>,
  attendanceForSelectedDate: List<AttendanceRecord>,
  allAttendanceList: List<AttendanceRecord> = emptyList(),
  selectedDay: DayOfWeekTr,
  selectedDate: String,
  todayDate: String,
  onDaySelected: (DayOfWeekTr) -> Unit,
  onBackClick: () -> Unit,
  onSetAttendance: (studentId: Long, date: String, status: String) -> Unit,
  onMarkAllStatus: (date: String, students: List<Student>, status: String) -> Unit,
  onSaveLesson: (ScheduleLesson) -> Unit,
  onDeleteLesson: (String) -> Unit,
  onResetToDefault: () -> Unit,
  onDateChange: ((String) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current

  // Filter & Search states
  var selectedGradeFilter by remember { mutableStateOf("Tümü") }
  var selectedCategoryFilter by remember { mutableStateOf<LessonCategory?>(null) }
  var searchQuery by remember { mutableStateOf("") }
  var showAddEditDialog by remember { mutableStateOf(false) }
  var showCalendarDialog by remember { mutableStateOf(false) }
  var lessonToEdit by remember { mutableStateOf<ScheduleLesson?>(null) }
  var showResetConfirmation by remember { mutableStateOf(false) }
  var expandedLessonId by remember { mutableStateOf<String?>(null) }

  // Current system day & time
  val calendar = Calendar.getInstance()
  val todayDayOfWeek = DayOfWeekTr.fromCalendarDay(calendar.get(Calendar.DAY_OF_WEEK))
  val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
  val currentMinute = calendar.get(Calendar.MINUTE)
  val currentTimeMinutes = currentHour * 60 + currentMinute

  // Active day lessons
  val dayLessons = remember(lessons, selectedDay, selectedGradeFilter, selectedCategoryFilter, searchQuery) {
    lessons
      .filter { it.day == selectedDay }
      .filter {
        selectedGradeFilter == "Tümü" ||
          it.targetGrade.contains(selectedGradeFilter, ignoreCase = true) ||
          it.targetGrade.equals("Tüm Sınıflar", ignoreCase = true)
      }
      .filter { selectedCategoryFilter == null || it.category == selectedCategoryFilter }
      .filter {
        searchQuery.isBlank() ||
          it.title.contains(searchQuery, ignoreCase = true) ||
          it.teacherName.contains(searchQuery, ignoreCase = true) ||
          it.classroom.contains(searchQuery, ignoreCase = true) ||
          it.targetGrade.contains(searchQuery, ignoreCase = true)
      }
      .sortedBy { it.startTime }
  }

  // Calculate overall day attendance stats
  val activeStudents = remember(students) { students.filter { it.status == "Aktif" } }
  val presentCount = attendanceForSelectedDate.count { it.status == "GELDI" }
  val absentCount = attendanceForSelectedDate.count { it.status == "GELMEDI" }
  val excusedCount = attendanceForSelectedDate.count { it.status == "IZINLI" }
  val lateCount = attendanceForSelectedDate.count { it.status == "GEC" }
  val totalActive = activeStudents.size
  val dayAttendancePercent = if (totalActive > 0) {
    ((presentCount.toFloat() / totalActive.toFloat()) * 100).toInt().coerceIn(0, 100)
  } else 0

  Scaffold(
    containerColor = CanvasBackground,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "Ders Programı & Yoklama",
                fontWeight = FontWeight.Black,
                fontSize = 18.5.sp,
                color = Color.White
              )
            }
            Text(
              text = "${selectedDay.fullName} • ${dayLessons.size} Ders • %$dayAttendancePercent Günlük Katılım",
              fontSize = 11.5.sp,
              color = TezhipGoldLight.copy(alpha = 0.95f)
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onBackClick,
            modifier = Modifier.testTag("schedule_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Geri",
              tint = Color.White
            )
          }
        },
        actions = {
          // Calendar Date Picker
          if (onDateChange != null) {
            IconButton(
              onClick = { showCalendarDialog = true },
              modifier = Modifier.testTag("schedule_calendar_button")
            ) {
              Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = "Tarih Seç",
                tint = Color.White
              )
            }
          }

          // Reset to default schedule action
          IconButton(
            onClick = { showResetConfirmation = true },
            modifier = Modifier.testTag("schedule_reset_button")
          ) {
            Icon(
              imageVector = Icons.Default.Restore,
              contentDescription = "Varsayılana Dön",
              tint = TezhipGoldLight
            )
          }

          // Add new lesson
          IconButton(
            onClick = {
              lessonToEdit = null
              showAddEditDialog = true
            },
            modifier = Modifier.testTag("schedule_add_lesson_button")
          ) {
            Icon(
              imageVector = Icons.Default.AddCircle,
              contentDescription = "Yeni Ders Ekle",
              tint = TezhipGoldLight
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = DeepBlueNavy,
          titleContentColor = Color.White
        )
      )
    },
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = {
          lessonToEdit = null
          showAddEditDialog = true
        },
        containerColor = DeepBlueNavy,
        contentColor = TezhipGoldLight,
        shape = RoundedCornerShape(16.dp),
        elevation = FloatingActionButtonDefaults.elevation(6.dp),
        modifier = Modifier.testTag("schedule_fab_add_lesson")
      ) {
        Icon(
          imageVector = Icons.Default.Add,
          contentDescription = "Yeni Ders",
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Yeni Ders Ekle",
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp
        )
      }
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 88.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // -------------------------------------------------------------
      // 1. GÜNLER SEÇİM SEKMELERİ (PAZARTESİ - PAZAR)
      // -------------------------------------------------------------
      item {
        WeekDaySelectorBar(
          selectedDay = selectedDay,
          todayDay = todayDayOfWeek,
          lessons = lessons,
          onDaySelected = { day ->
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onDaySelected(day)
          }
        )
      }

      // -------------------------------------------------------------
      // 2. GÜNLÜK ÖZET BİLGİ KARTI & AKTİF DERS BİLGİSİ
      // -------------------------------------------------------------
      item {
        DailySummaryCard(
          selectedDay = selectedDay,
          isToday = selectedDay == todayDayOfWeek,
          lessonCount = dayLessons.size,
          presentCount = presentCount,
          totalStudents = totalActive,
          attendancePercent = dayAttendancePercent,
          selectedDate = selectedDate,
          todayDate = todayDate,
          onMarkAllStatus = { status ->
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onMarkAllStatus(selectedDate, activeStudents, status)
            val label = when (status) {
              "GELDI" -> "GELDİ"
              "IZINLI" -> "İZİNLİ"
              "GELMEDI" -> "GELMEDİ"
              else -> status
            }
            Toast.makeText(context, "Tüm aktif talebeler '$label' olarak kaydedildi! 🌿", Toast.LENGTH_SHORT).show()
          }
        )
      }

      // -------------------------------------------------------------
      // 3. FİLTRELEME & ARAMA ÇUBUĞU
      // -------------------------------------------------------------
      item {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Arama Kutusu
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Ders, eğitmen veya derslik ara...", fontSize = 13.sp) },
            leadingIcon = {
              Icon(Icons.Default.Search, contentDescription = null, tint = DeepBlueNavy, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
              if (searchQuery.isNotBlank()) {
                IconButton(onClick = { searchQuery = "" }) {
                  Icon(Icons.Default.Clear, contentDescription = "Temizle", modifier = Modifier.size(16.dp))
                }
              }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = CanvasSurface,
              unfocusedContainerColor = CanvasSurface,
              focusedBorderColor = DeepBlueNavy,
              unfocusedBorderColor = BorderLight
            ),
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("schedule_search_input")
          )

          // Sınıf Filtre Çipleri
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            val gradeOptions = listOf("Tümü", "5. Sınıf", "6. Sınıf", "7. Sınıf", "8. Sınıf", "Hafızlık Grubu")
            items(gradeOptions) { grade ->
              val isSelected = selectedGradeFilter == grade
              FilterChip(
                selected = isSelected,
                onClick = { selectedGradeFilter = grade },
                label = { Text(grade, fontSize = 11.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = DeepBlueNavy,
                  selectedLabelColor = Color.White,
                  containerColor = CanvasSurface,
                  labelColor = TextPrimary
                ),
                border = FilterChipDefaults.filterChipBorder(
                  enabled = true,
                  selected = isSelected,
                  borderColor = if (isSelected) DeepBlueNavy else BorderLight
                ),
                shape = RoundedCornerShape(10.dp)
              )
            }
          }
        }
      }

      // -------------------------------------------------------------
      // 4. DERS LİSTESİ BAŞLIĞI
      // -------------------------------------------------------------
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            SeljukStarBox(size = 24.dp, backgroundColor = DeepBlueNavy) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                tint = TezhipGoldLight,
                modifier = Modifier.size(13.dp)
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "${selectedDay.fullName} Çizelgesi",
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp,
              color = DeepBlueNavy
            )
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = DeepBlueNavy.copy(alpha = 0.1f)
            ) {
              Text(
                text = "${dayLessons.size} Ders",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = DeepBlueNavy,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
              )
            }
          }

          if (dayLessons.isNotEmpty()) {
            Text(
              text = "Tıkla & Yoklama Al",
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium,
              color = TextSecondary
            )
          }
        }
      }

      // -------------------------------------------------------------
      // 5. DERS KARTLARI (TIMELINE & INTERACTIVE ATTENDANCE)
      // -------------------------------------------------------------
      if (dayLessons.isEmpty()) {
        item {
          TezhipCard(
            shape = RoundedCornerShape(16.dp),
            containerColor = CanvasSurface,
            elevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center
            ) {
              SeljukStarBox(size = 48.dp, backgroundColor = DeepBlueNavy.copy(alpha = 0.1f)) {
                Icon(
                  imageVector = Icons.Default.EventBusy,
                  contentDescription = null,
                  tint = DeepBlueNavy,
                  modifier = Modifier.size(24.dp)
                )
              }
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = "Bu gün için kayıtlı ders bulunamadı",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextPrimary
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Filtreleri temizleyebilir veya yeni bir ders ekleyebilirsiniz.",
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
              )
              Spacer(modifier = Modifier.height(14.dp))
              Button(
                onClick = {
                  lessonToEdit = null
                  showAddEditDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = DeepBlueNavy),
                shape = RoundedCornerShape(10.dp)
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ders Ekle", fontSize = 13.sp)
              }
            }
          }
        }
      } else {
        items(dayLessons, key = { it.id }) { lesson ->
          // Calculate matching students for this lesson
          val targetStudents = remember(students, lesson.targetGrade) {
            if (lesson.targetGrade.equals("Tüm Sınıflar", ignoreCase = true) || lesson.targetGrade.isBlank()) {
              students.filter { it.status == "Aktif" }
            } else {
              students.filter { it.status == "Aktif" && (it.grade.contains(lesson.targetGrade, ignoreCase = true) || lesson.targetGrade.contains(it.grade, ignoreCase = true)) }
            }
          }

          // Calculate time status
          val lessonStartMins = parseTimeToMinutes(lesson.startTime)
          val lessonEndMins = parseTimeToMinutes(lesson.endTime)
          val isToday = selectedDay == todayDayOfWeek

          val timeStatus = when {
            !isToday -> LessonTimeStatus.UPCOMING
            currentTimeMinutes in lessonStartMins..lessonEndMins -> LessonTimeStatus.ONGOING
            currentTimeMinutes > lessonEndMins -> LessonTimeStatus.FINISHED
            else -> LessonTimeStatus.UPCOMING
          }

          val isExpanded = expandedLessonId == lesson.id

          LessonTimelineCard(
            lesson = lesson,
            targetStudents = targetStudents,
            attendanceRecords = attendanceForSelectedDate,
            selectedDate = selectedDate,
            timeStatus = timeStatus,
            isExpanded = isExpanded,
            onToggleExpand = {
              expandedLessonId = if (isExpanded) null else lesson.id
            },
            onSetAttendance = onSetAttendance,
            onMarkClassPresent = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              onMarkAllStatus(selectedDate, targetStudents, "GELDI")
              Toast.makeText(context, "${lesson.title} için ${targetStudents.size} talebe 'GELDİ' yazıldı! 🌿", Toast.LENGTH_SHORT).show()
            },
            onEdit = {
              lessonToEdit = lesson
              showAddEditDialog = true
            },
            onDelete = {
              onDeleteLesson(lesson.id)
              Toast.makeText(context, "Ders programdan kaldırıldı.", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.testTag("schedule_lesson_card_${lesson.id}")
          )
        }
      }
    }
  }

  // -------------------------------------------------------------
  // DİALOG: YENİ DERS EKLE / DÜZENLE
  // -------------------------------------------------------------
  if (showAddEditDialog) {
    AddEditLessonDialog(
      lessonToEdit = lessonToEdit,
      defaultDay = selectedDay,
      onDismiss = {
        showAddEditDialog = false
        lessonToEdit = null
      },
      onSave = { savedLesson ->
        onSaveLesson(savedLesson)
        showAddEditDialog = false
        lessonToEdit = null
        Toast.makeText(context, "Ders başarıyla kaydedildi! 🌿", Toast.LENGTH_SHORT).show()
      }
    )
  }

  // -------------------------------------------------------------
  // DİALOG: VARSAYILANA DÖN ONAYI
  // -------------------------------------------------------------
  if (showResetConfirmation) {
    AlertDialog(
      onDismissRequest = { showResetConfirmation = false },
      title = {
        Text("Ders Programını Sıfırla", fontWeight = FontWeight.Bold)
      },
      text = {
        Text("Ders programı klasik Mekteb-i İrfan haftalık ders ve etüt şablonuna geri döndürülecektir. Devam etmek istiyor musunuz?")
      },
      confirmButton = {
        Button(
          onClick = {
            onResetToDefault()
            showResetConfirmation = false
            Toast.makeText(context, "Ders programı varsayılan şablona sıfırlandı.", Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = FeatureReportsOrange)
        ) {
          Text("Evet, Sıfırla")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showResetConfirmation = false }) {
          Text("İptal")
        }
      }
    )
  }

  // -------------------------------------------------------------
  // DİALOG: TAKVİMDEN TARİH SEÇ
  // -------------------------------------------------------------
  if (showCalendarDialog && onDateChange != null) {
    AttendanceCalendarDialog(
      selectedDate = selectedDate,
      attendanceDates = allAttendanceList.map { it.date }.toSet(),
      onDateSelected = {
        onDateChange(it)
        showCalendarDialog = false
      },
      onDismiss = { showCalendarDialog = false }
    )
  }
}

enum class LessonTimeStatus {
  ONGOING,
  UPCOMING,
  FINISHED
}

// -------------------------------------------------------------
// 1. HAFTALIK GÜN SEÇİCİ BAR (PZT - PAZ)
// -------------------------------------------------------------
@Composable
private fun WeekDaySelectorBar(
  selectedDay: DayOfWeekTr,
  todayDay: DayOfWeekTr,
  lessons: List<ScheduleLesson>,
  onDaySelected: (DayOfWeekTr) -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = CanvasSurface,
    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
    shadowElevation = 2.dp,
    modifier = modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(6.dp),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      DayOfWeekTr.values().forEach { day ->
        val isSelected = day == selectedDay
        val isToday = day == todayDay
        val lessonCountOnDay = lessons.count { it.day == day }

        val bgColor by animateColorAsState(
          targetValue = when {
            isSelected -> DeepBlueNavy
            isToday -> TezhipGoldLight.copy(alpha = 0.6f)
            else -> Color.Transparent
          },
          label = "dayBgColor"
        )

        val textColor = when {
          isSelected -> TezhipGoldLight
          isToday -> TezhipGoldDark
          else -> TextPrimary
        }

        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable { onDaySelected(day) }
            .padding(vertical = 8.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            if (isToday) {
              Box(
                modifier = Modifier
                  .size(5.dp)
                  .clip(CircleShape)
                  .background(if (isSelected) TezhipGoldLight else FeatureAttendanceGreen)
              )
              Spacer(modifier = Modifier.height(2.dp))
            }

            Text(
              text = day.shortName,
              fontSize = 13.sp,
              fontWeight = if (isSelected || isToday) FontWeight.Black else FontWeight.Medium,
              color = textColor
            )

            Text(
              text = "$lessonCountOnDay Ders",
              fontSize = 9.5.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
              color = if (isSelected) Color.White.copy(alpha = 0.8f) else TextSecondary
            )
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// 2. GÜNLÜK ÖZET BİLGİ KARTI
// -------------------------------------------------------------
@Composable
private fun DailySummaryCard(
  selectedDay: DayOfWeekTr,
  isToday: Boolean,
  lessonCount: Int,
  presentCount: Int,
  totalStudents: Int,
  attendancePercent: Int,
  selectedDate: String,
  todayDate: String,
  onMarkAllStatus: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  TezhipCard(
    shape = RoundedCornerShape(18.dp),
    containerColor = CanvasSurface,
    elevation = 3.dp,
    showCornerOrnaments = true,
    modifier = modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          SeljukStarBox(
            size = 36.dp,
            backgroundColor = if (isToday) FeatureAttendanceGreen else DeepBlueNavy
          ) {
            Icon(
              imageVector = if (isToday) Icons.Default.Today else Icons.Default.CalendarMonth,
              contentDescription = null,
              tint = TezhipGoldLight,
              modifier = Modifier.size(18.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "${selectedDay.fullName} Programı & Yoklama",
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = DeepBlueNavy
              )
              if (isToday) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = FeatureAttendanceGreen.copy(alpha = 0.15f)
                ) {
                  Text(
                    text = "BUGÜN",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Black,
                    color = FeatureAttendanceGreen,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }
            }
            Text(
              text = "Tarih: $selectedDate • $lessonCount Ders Planlandı",
              fontSize = 11.5.sp,
              color = TextSecondary
            )
          }
        }

        // Katılım Yüzdesi Rozeti
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = when {
            attendancePercent >= 80 -> FeatureAttendanceGreen.copy(alpha = 0.12f)
            attendancePercent >= 50 -> StatusExcusedAmber.copy(alpha = 0.12f)
            else -> DeepBlueNavy.copy(alpha = 0.08f)
          },
          border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (attendancePercent >= 80) FeatureAttendanceGreen.copy(alpha = 0.3f) else BorderLight
          )
        ) {
          Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text(
              text = "%$attendancePercent",
              fontSize = 14.sp,
              fontWeight = FontWeight.Black,
              color = if (attendancePercent >= 80) FeatureAttendanceGreen else DeepBlueNavy
            )
            Text(
              text = "$presentCount/$totalStudents Derste",
              fontSize = 9.5.sp,
              color = TextSecondary
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // İlerleme Çubuğu
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(6.dp)
          .clip(RoundedCornerShape(100.dp))
          .background(BorderLight.copy(alpha = 0.4f))
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth((attendancePercent / 100f).coerceIn(0.02f, 1f))
            .fillMaxHeight()
            .clip(RoundedCornerShape(100.dp))
            .background(
              Brush.horizontalGradient(
                listOf(FeatureAttendanceGreen, TezhipGold)
              )
            )
        )
      }

      // Hızlı Toplu Yoklama Butonları
      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Button(
          onClick = { onMarkAllStatus("GELDI") },
          colors = ButtonDefaults.buttonColors(containerColor = FeatureAttendanceGreen),
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
          modifier = Modifier
            .weight(1.3f)
            .height(32.dp)
        ) {
          Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(13.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Tüm Sınıf Geldi", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
          onClick = { onMarkAllStatus("IZINLI") },
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
          modifier = Modifier
            .weight(1f)
            .height(32.dp)
        ) {
          Text("İzinli Yaz", fontSize = 11.sp, color = DarkSlateNavy, fontWeight = FontWeight.SemiBold)
        }

        OutlinedButton(
          onClick = { onMarkAllStatus("GELMEDI") },
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
          modifier = Modifier
            .weight(1f)
            .height(32.dp)
        ) {
          Text("Gelmedi", fontSize = 11.sp, color = StatusAbsentRed, fontWeight = FontWeight.SemiBold)
        }
      }
    }
  }
}

// -------------------------------------------------------------
// 3. DERS ZAMAN ÇİZELGESİ KARTI & İNTERAKTİF YOKLAMA PANELİ
// -------------------------------------------------------------
@Composable
private fun LessonTimelineCard(
  lesson: ScheduleLesson,
  targetStudents: List<Student>,
  attendanceRecords: List<AttendanceRecord>,
  selectedDate: String,
  timeStatus: LessonTimeStatus,
  isExpanded: Boolean,
  onToggleExpand: () -> Unit,
  onSetAttendance: (studentId: Long, date: String, status: String) -> Unit,
  onMarkClassPresent: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier
) {
  val haptic = LocalHapticFeedback.current
  val categoryColor = Color(lesson.category.badgeColorHex)

  // Students attendance stats for this specific lesson
  val studentAttendanceMap = remember(targetStudents, attendanceRecords) {
    targetStudents.associateWith { student ->
      attendanceRecords.find { it.studentId == student.id }?.status ?: "YOK"
    }
  }

  val presentInLesson = studentAttendanceMap.values.count { it == "GELDI" }
  val absentInLesson = studentAttendanceMap.values.count { it == "GELMEDI" }
  val excusedInLesson = studentAttendanceMap.values.count { it == "IZINLI" }
  val lateInLesson = studentAttendanceMap.values.count { it == "GEC" }
  val totalInClass = targetStudents.size

  val attendanceRate = if (totalInClass > 0) {
    ((presentInLesson.toFloat() / totalInClass.toFloat()) * 100).toInt()
  } else 0

  Surface(
    shape = RoundedCornerShape(18.dp),
    color = CanvasSurface,
    border = androidx.compose.foundation.BorderStroke(
      1.2.dp,
      when (timeStatus) {
        LessonTimeStatus.ONGOING -> FeatureAttendanceGreen
        else -> BorderLight
      }
    ),
    shadowElevation = if (timeStatus == LessonTimeStatus.ONGOING) 4.dp else 2.dp,
    modifier = modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp)
    ) {
      // 1. ÜST ŞERİT: SAAT ROZETİ, KATEGORİ & DURUM ETİKETİ
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Saat Aralığı Rozeti
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = DeepBlueNavy,
          border = androidx.compose.foundation.BorderStroke(1.dp, TezhipGoldLight.copy(alpha = 0.4f))
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Schedule,
              contentDescription = null,
              tint = TezhipGoldLight,
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = "${lesson.startTime} - ${lesson.endTime}",
              fontSize = 11.5.sp,
              fontWeight = FontWeight.Black,
              color = TezhipGoldLight
            )
          }
        }

        // Kategori & Durum Etiketi
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = categoryColor.copy(alpha = 0.12f),
            border = androidx.compose.foundation.BorderStroke(1.dp, categoryColor.copy(alpha = 0.3f))
          ) {
            Text(
              text = lesson.category.title,
              fontSize = 10.5.sp,
              fontWeight = FontWeight.Bold,
              color = categoryColor,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
            )
          }

          if (timeStatus == LessonTimeStatus.ONGOING) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = FeatureAttendanceGreen,
              shadowElevation = 2.dp
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "ŞU AN DERSTE",
                  fontSize = 9.5.sp,
                  fontWeight = FontWeight.Black,
                  color = Color.White
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // 2. DERS BAŞLIĞI & AÇIKLAMA
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = lesson.title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = TextPrimary
          )
          if (lesson.description.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = lesson.description,
              fontSize = 11.5.sp,
              color = TextSecondary,
              maxLines = if (isExpanded) 4 else 2,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        // Düzenle & Sil Butonları
        Row {
          IconButton(
            onClick = onEdit,
            modifier = Modifier.size(32.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Edit,
              contentDescription = "Düzenle",
              tint = DeepBlueNavy,
              modifier = Modifier.size(16.dp)
            )
          }
          IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp)
          ) {
            Icon(
              imageVector = Icons.Default.DeleteOutline,
              contentDescription = "Sil",
              tint = FeatureReportsOrange,
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // 3. SINIF, DERSLİK & EĞİTMEN BİLGİLERİ
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Hedef Sınıf
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = DeepBlueNavy.copy(alpha = 0.08f)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
          ) {
            Icon(Icons.Default.Group, contentDescription = null, tint = DeepBlueNavy, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = lesson.targetGrade, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DeepBlueNavy)
          }
        }

        // Derslik
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = CanvasBackground
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
          ) {
            Icon(Icons.Default.MeetingRoom, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = lesson.classroom, fontSize = 11.sp, color = TextSecondary)
          }
        }

        // Eğitmen
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = CanvasBackground
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
          ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = lesson.teacherName, fontSize = 11.sp, color = TextSecondary)
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // 4. CANLI YOKLAMA ÖZET ŞERİDİ
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = CanvasBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onToggleExpand() }
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.FactCheck,
                contentDescription = null,
                tint = FeatureAttendanceGreen,
                modifier = Modifier.size(15.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Yoklama Durumu:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "$presentInLesson/$totalInClass Talebe Derste (%$attendanceRate)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = if (attendanceRate >= 80) FeatureAttendanceGreen else TextPrimary
              )
            }

            Icon(
              imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
              contentDescription = if (isExpanded) "Kapat" else "Genişlet",
              tint = DeepBlueNavy,
              modifier = Modifier.size(18.dp)
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          // Mini Renkli Durum Sayacı
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            AttendancePillBadge(label = "Geldi", count = presentInLesson, color = FeatureAttendanceGreen, modifier = Modifier.weight(1f))
            AttendancePillBadge(label = "Gelmedi", count = absentInLesson, color = FeatureReportsOrange, modifier = Modifier.weight(1f))
            AttendancePillBadge(label = "İzinli", count = excusedInLesson, color = StatusExcusedAmber, modifier = Modifier.weight(1f))
            AttendancePillBadge(label = "Geç", count = lateInLesson, color = FeatureDevelopmentTeal, modifier = Modifier.weight(1f))
          }
        }
      }

      // 5. GENİŞLETİLEBİLİR İNTERAKTİF TALEBE YOKLAMA LİSTESİ
      AnimatedVisibility(visible = isExpanded) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Talebe Listesi & Hızlı Giriş",
              fontSize = 13.sp,
              fontWeight = FontWeight.Black,
              color = DeepBlueNavy
            )

            Button(
              onClick = onMarkClassPresent,
              colors = ButtonDefaults.buttonColors(containerColor = FeatureAttendanceGreen),
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
              modifier = Modifier.height(28.dp)
            ) {
              Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(12.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Tümünü Geldi Yap", fontSize = 11.sp)
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          if (targetStudents.isEmpty()) {
            Text(
              text = "Bu derse atanmış aktif talebe bulunamadı.",
              fontSize = 12.sp,
              color = TextSecondary,
              modifier = Modifier.padding(vertical = 8.dp)
            )
          } else {
            Column(
              verticalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              targetStudents.forEach { student ->
                val currentStatus = studentAttendanceMap[student] ?: "YOK"
                LessonStudentAttendanceRow(
                  student = student,
                  currentStatus = currentStatus,
                  onStatusChange = { newStatus ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSetAttendance(student.id, selectedDate, newStatus)
                  }
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AttendancePillBadge(
  label: String,
  count: Int,
  color: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(6.dp),
    color = color.copy(alpha = 0.1f),
    border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.25f)),
    modifier = modifier
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 3.dp),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(5.dp)
          .clip(CircleShape)
          .background(color)
      )
      Spacer(modifier = Modifier.width(4.dp))
      Text(
        text = "$count $label",
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = color
      )
    }
  }
}

// -------------------------------------------------------------
// 4. İNTERAKTİF TALEBE YOKLAMA SATIRI (TEK TIKLA DURUM SEÇİMİ)
// -------------------------------------------------------------
@Composable
private fun LessonStudentAttendanceRow(
  student: Student,
  currentStatus: String,
  onStatusChange: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = CanvasBackground,
    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight.copy(alpha = 0.7f)),
    modifier = modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f)
      ) {
        StudentAvatar(
          name = student.fullName,
          colorIndex = student.avatarColorIndex,
          size = 28.dp,
          fontSize = 11
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
            text = student.fullName,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = student.grade,
            fontSize = 10.5.sp,
            color = TextSecondary
          )
        }
      }

      // 4 Butonlu Hızlı Seçici: GELDI, GELMEDI, IZINLI, GEC
      Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        QuickAttendanceButton(
          label = "G",
          fullName = "Geldi",
          isSelected = currentStatus == "GELDI",
          activeColor = FeatureAttendanceGreen,
          onClick = { onStatusChange("GELDI") }
        )
        QuickAttendanceButton(
          label = "M",
          fullName = "Gelmedi",
          isSelected = currentStatus == "GELMEDI",
          activeColor = FeatureReportsOrange,
          onClick = { onStatusChange("GELMEDI") }
        )
        QuickAttendanceButton(
          label = "İ",
          fullName = "İzinli",
          isSelected = currentStatus == "IZINLI",
          activeColor = StatusExcusedAmber,
          onClick = { onStatusChange("IZINLI") }
        )
        QuickAttendanceButton(
          label = "Geç",
          fullName = "Geç",
          isSelected = currentStatus == "GEC",
          activeColor = FeatureDevelopmentTeal,
          onClick = { onStatusChange("GEC") }
        )
      }
    }
  }
}

@Composable
private fun QuickAttendanceButton(
  label: String,
  fullName: String,
  isSelected: Boolean,
  activeColor: Color,
  onClick: () -> Unit
) {
  val bgColor by animateColorAsState(
    targetValue = if (isSelected) activeColor else Color.Transparent,
    label = "quickAttBg"
  )
  val textColor = if (isSelected) Color.White else activeColor

  Box(
    modifier = Modifier
      .size(28.dp)
      .clip(RoundedCornerShape(6.dp))
      .background(bgColor)
      .border(1.dp, if (isSelected) activeColor else activeColor.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
      .clickable { onClick() },
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = label,
      fontSize = 10.5.sp,
      fontWeight = FontWeight.Black,
      color = textColor
    )
  }
}

// -------------------------------------------------------------
// 5. DİALOG: YENİ DERS EKLE / DÜZENLE
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditLessonDialog(
  lessonToEdit: ScheduleLesson?,
  defaultDay: DayOfWeekTr,
  onDismiss: () -> Unit,
  onSave: (ScheduleLesson) -> Unit
) {
  var day by remember { mutableStateOf(lessonToEdit?.day ?: defaultDay) }
  var startTime by remember { mutableStateOf(lessonToEdit?.startTime ?: "08:30") }
  var endTime by remember { mutableStateOf(lessonToEdit?.endTime ?: "10:00") }
  var title by remember { mutableStateOf(lessonToEdit?.title ?: "") }
  var category by remember { mutableStateOf(lessonToEdit?.category ?: LessonCategory.MEMORIZATION) }
  var targetGrade by remember { mutableStateOf(lessonToEdit?.targetGrade ?: "Tüm Sınıflar") }
  var classroom by remember { mutableStateOf(lessonToEdit?.classroom ?: "Derslik 1 (Mescid)") }
  var teacherName by remember { mutableStateOf(lessonToEdit?.teacherName ?: "Yetkili Hoca") }
  var description by remember { mutableStateOf(lessonToEdit?.description ?: "") }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = CanvasSurface,
      border = androidx.compose.foundation.BorderStroke(1.5.dp, BorderLight),
      shadowElevation = 8.dp,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 4.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Başlık
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            SeljukStarBox(size = 32.dp, backgroundColor = DeepBlueNavy) {
              Icon(
                imageVector = if (lessonToEdit == null) Icons.Default.AddCircle else Icons.Default.Edit,
                contentDescription = null,
                tint = TezhipGoldLight,
                modifier = Modifier.size(16.dp)
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (lessonToEdit == null) "Yeni Ders Ekle" else "Dersi Düzenle",
              fontSize = 16.sp,
              fontWeight = FontWeight.Black,
              color = DeepBlueNavy
            )
          }

          IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Kapat", tint = TextSecondary)
          }
        }

        Divider(color = BorderLight)

        // Gün Seçimi
        Text("Gün:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          items(DayOfWeekTr.values()) { d ->
            val isSelected = d == day
            FilterChip(
              selected = isSelected,
              onClick = { day = d },
              label = { Text(d.shortName, fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = DeepBlueNavy,
                selectedLabelColor = Color.White
              ),
              shape = RoundedCornerShape(8.dp)
            )
          }
        }

        // Ders Başlığı
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Ders Başlığı", fontSize = 12.sp) },
          placeholder = { Text("Örn: Has Hafızlık Dinleme", fontSize = 12.sp) },
          singleLine = true,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        )

        // Saatler
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = startTime,
            onValueChange = { startTime = it },
            label = { Text("Başlangıç", fontSize = 11.sp) },
            placeholder = { Text("08:30", fontSize = 11.sp) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = endTime,
            onValueChange = { endTime = it },
            label = { Text("Bitiş", fontSize = 11.sp) },
            placeholder = { Text("10:00", fontSize = 11.sp) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
          )
        }

        // Kategori & Sınıf
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = targetGrade,
            onValueChange = { targetGrade = it },
            label = { Text("Sınıf / Grup", fontSize = 11.sp) },
            placeholder = { Text("Tüm Sınıflar, 6. Sınıf...", fontSize = 11.sp) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = classroom,
            onValueChange = { classroom = it },
            label = { Text("Derslik / Mekan", fontSize = 11.sp) },
            placeholder = { Text("Derslik 1", fontSize = 11.sp) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
          )
        }

        // Eğitmen & Açıklama
        OutlinedTextField(
          value = teacherName,
          onValueChange = { teacherName = it },
          label = { Text("Eğitmen Adı", fontSize = 12.sp) },
          singleLine = true,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Açıklama / Konu Notu", fontSize = 12.sp) },
          placeholder = { Text("Ders içeriği ve hedefler...", fontSize = 12.sp) },
          shape = RoundedCornerShape(10.dp),
          maxLines = 2,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Kaydet Butonları
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = onDismiss,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
          ) {
            Text("İptal", fontSize = 13.sp)
          }

          Button(
            onClick = {
              if (title.isBlank()) {
                title = "Mekteb-i İrfan Dersi"
              }
              val lesson = ScheduleLesson(
                id = lessonToEdit?.id ?: UUID.randomUUID().toString(),
                day = day,
                startTime = startTime.ifBlank { "08:30" },
                endTime = endTime.ifBlank { "10:00" },
                title = title.trim(),
                category = category,
                targetGrade = targetGrade.ifBlank { "Tüm Sınıflar" }.trim(),
                classroom = classroom.ifBlank { "Derslik 1" }.trim(),
                teacherName = teacherName.ifBlank { "Yetkili Hoca" }.trim(),
                description = description.trim(),
                isMandatoryAttendance = true,
                orderIndex = lessonToEdit?.orderIndex ?: 0
              )
              onSave(lesson)
            },
            colors = ButtonDefaults.buttonColors(containerColor = DeepBlueNavy),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Kaydet", fontSize = 13.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

private fun parseTimeToMinutes(timeStr: String): Int {
  return try {
    val parts = timeStr.trim().split(":")
    val h = parts[0].toInt()
    val m = parts.getOrNull(1)?.toInt() ?: 0
    h * 60 + m
  } catch (e: Exception) {
    0
  }
}
