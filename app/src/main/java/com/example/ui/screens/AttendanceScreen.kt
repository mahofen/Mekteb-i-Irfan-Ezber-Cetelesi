package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.data.model.DailyDutyRecord
import com.example.data.model.Student
import com.example.ui.components.GoldSubtleBorderGradient
import com.example.ui.components.SeljukStarBox
import com.example.ui.components.StudentAvatar
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * 🕌 GÜNLÜK GÖREVLER (VAZİFE & VİRD ÇETELESİ) YÖNETİMİ
 * Hocalar için öğrencilerin 5 vakit namaz, Kur'an tilaveti, Risale okumaları,
 * tesbihat, cevşen, salavat ve hoca değerlendirme notlarının tek merkezden
 * yönetildiği özel vazife çetelesi ekranı.
 *
 * (Yoklama alma işlemleri "Ders Programı & Yoklama" ekranından yapılmaktadır.)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
  students: List<Student>,
  allAttendanceList: List<AttendanceRecord> = emptyList(),
  attendanceForSelectedDate: List<AttendanceRecord> = emptyList(),
  selectedDate: String,
  todayDate: String,
  allDuties: List<DailyDutyRecord> = emptyList(),
  onBackClick: () -> Unit,
  onDateChange: (String) -> Unit,
  onSetAttendance: (studentId: Long, date: String, status: String) -> Unit = { _, _, _ -> },
  onMarkAllPresent: (date: String, students: List<Student>) -> Unit = { _, _ -> },
  onMarkAllStatus: (date: String, students: List<Student>, status: String) -> Unit = { _, _, _ -> },
  onSaveDuty: (DailyDutyRecord) -> Unit = {},
  onUpdateDuty: (Long, String, (DailyDutyRecord) -> DailyDutyRecord) -> Unit = { _, _, _ -> },
  onMarkAllPrayers: (String, List<Student>, Boolean) -> Unit = { _, _, _ -> },
  onNavigateSchedule: (() -> Unit)? = null,
  viewModel: AppViewModel? = null
) {
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current
  val activeStudents = remember(students) { students.filter { it.status == "Aktif" } }

  var showCalendarDialog by remember { mutableStateOf(false) }
  var noteEditingStudentDuty by remember { mutableStateOf<Pair<Student, DailyDutyRecord>?>(null) }
  var searchQuery by remember { mutableStateOf("") }
  var dutyFilter by remember { mutableStateOf("Tümü") } // "Tümü", "5 Vakit Tam", "Eksik Vakit", "Kur'an", "Risale"

  val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
  val displayFormat = SimpleDateFormat("d MMMM yyyy, EEEE", Locale("tr"))

  val formattedDisplayDate = try {
    val parsed = dateFormat.parse(selectedDate)
    if (parsed != null) displayFormat.format(parsed) else selectedDate
  } catch (e: Exception) {
    selectedDate
  }

  // Duties for selected date
  val dutiesForSelectedDate = remember(allDuties, selectedDate) {
    allDuties.filter { it.date == selectedDate }
  }

  // Class duty metrics calculation
  val totalQuranPagesToday = dutiesForSelectedDate.sumOf { it.quranPages }
  val totalRisalePagesToday = dutiesForSelectedDate.sumOf { it.risalePages }
  val totalSalavatToday = dutiesForSelectedDate.sumOf { it.salavatCount }
  val totalFullPrayersCount = dutiesForSelectedDate.count {
    it.fajr && it.dhuhr && it.asr && it.maghrib && it.isha
  }

  // Filter students based on search query and duty status
  val filteredStudents = remember(students, searchQuery, dutyFilter, dutiesForSelectedDate) {
    var list = if (searchQuery.isBlank()) students
    else students.filter {
      it.fullName.contains(searchQuery, ignoreCase = true) ||
        it.grade.contains(searchQuery, ignoreCase = true)
    }

    when (dutyFilter) {
      "5 Vakit Tam" -> {
        list = list.filter { st ->
          val d = dutiesForSelectedDate.find { it.studentId == st.id }
          d != null && d.fajr && d.dhuhr && d.asr && d.maghrib && d.isha
        }
      }
      "Eksik Vakit" -> {
        list = list.filter { st ->
          val d = dutiesForSelectedDate.find { it.studentId == st.id }
          d == null || !(d.fajr && d.dhuhr && d.asr && d.maghrib && d.isha)
        }
      }
      "Kur'an" -> {
        list = list.filter { st ->
          val d = dutiesForSelectedDate.find { it.studentId == st.id }
          d != null && d.quranPages > 0
        }
      }
      "Risale" -> {
        list = list.filter { st ->
          val d = dutiesForSelectedDate.find { it.studentId == st.id }
          d != null && d.risalePages > 0
        }
      }
    }
    list
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Günlük Görevler",
              fontWeight = FontWeight.Bold,
              fontSize = 19.sp,
              color = Color.White
            )
            Text(
              text = "${students.size} Talebe • $totalFullPrayersCount Tam 5 Vakit • $totalQuranPagesToday Syf Kur'an",
              fontSize = 11.5.sp,
              color = Color.White.copy(alpha = 0.85f)
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onBackClick,
            modifier = Modifier.testTag("attendance_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Geri",
              tint = Color.White
            )
          }
        },
        actions = {
          // Yoklama Al (Ders Programı & Yoklama) Shortcut Action
          if (onNavigateSchedule != null) {
            Surface(
              onClick = onNavigateSchedule,
              color = Color.White.copy(alpha = 0.15f),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.padding(end = 6.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.FactCheck,
                  contentDescription = "Yoklama Ekranı",
                  tint = TezhipGoldLight,
                  modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Yoklama Al",
                  fontSize = 11.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
              }
            }
          }

          // Takvimden Tarih Seç
          IconButton(
            onClick = { showCalendarDialog = true },
            modifier = Modifier.testTag("open_calendar_top_button")
          ) {
            Icon(
              imageVector = Icons.Default.CalendarMonth,
              contentDescription = "Takvimden Tarih Seç",
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
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // -----------------------------------------------------------------
      // 1. YOKLAMA BİLGİLENDİRME & HIZLI GEÇİŞ ŞERİDİ
      // -----------------------------------------------------------------
      if (onNavigateSchedule != null) {
        Surface(
          color = DeepBlueNavy.copy(alpha = 0.08f),
          border = androidx.compose.foundation.BorderStroke(1.dp, DeepBlueNavy.copy(alpha = 0.15f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = DeepBlueNavy,
                modifier = Modifier.size(15.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Yoklama işlemleri 'Ders Programı & Yoklama' ekranından alınır.",
                fontSize = 11.5.sp,
                color = DeepBlueNavy,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }

            TextButton(
              onClick = onNavigateSchedule,
              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
              modifier = Modifier.height(26.dp)
            ) {
              Text(
                text = "Yoklama Ekranına Geç",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = FeatureAttendanceGreen
              )
              Spacer(modifier = Modifier.width(3.dp))
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = FeatureAttendanceGreen,
                modifier = Modifier.size(12.dp)
              )
            }
          }
        }
      }

      // -----------------------------------------------------------------
      // 2. TARİH SEÇİCİ & HIZLI GEÇİŞ ÇUBUĞU
      // -----------------------------------------------------------------
      Surface(
        color = CanvasSurface,
        shape = RoundedCornerShape(0.dp, 0.dp, 16.dp, 16.dp),
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            // Önceki Gün
            IconButton(
              onClick = {
                try {
                  val cal = Calendar.getInstance()
                  val parsed = dateFormat.parse(selectedDate)
                  if (parsed != null) {
                    cal.time = parsed
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                    onDateChange(dateFormat.format(cal.time))
                  }
                } catch (e: Exception) {}
              },
              modifier = Modifier
                .size(34.dp)
                .testTag("attendance_prev_day")
            ) {
              Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "Önceki Gün",
                tint = TextPrimary
              )
            }

            // Seçili Tarih Başlığı (Tıklanarak Takvim Açılır)
            Surface(
              onClick = { showCalendarDialog = true },
              shape = RoundedCornerShape(10.dp),
              color = CanvasBackground,
              border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
              modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .testTag("attendance_date_display")
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 6.dp, horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Event,
                  contentDescription = null,
                  tint = FeatureAttendanceGreen,
                  modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = formattedDisplayDate,
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.5.sp,
                  color = TextPrimary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                if (selectedDate == todayDate) {
                  Spacer(modifier = Modifier.width(6.dp))
                  Surface(
                    color = TezhipGold.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                  ) {
                    Text(
                      text = "BUGÜN",
                      fontSize = 9.sp,
                      fontWeight = FontWeight.Bold,
                      color = DarkSlateNavy,
                      modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                  }
                }
              }
            }

            // Sonraki Gün
            IconButton(
              onClick = {
                try {
                  val cal = Calendar.getInstance()
                  val parsed = dateFormat.parse(selectedDate)
                  if (parsed != null) {
                    cal.time = parsed
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    onDateChange(dateFormat.format(cal.time))
                  }
                } catch (e: Exception) {}
              },
              modifier = Modifier
                .size(34.dp)
                .testTag("attendance_next_day")
            ) {
              Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Sonraki Gün",
                tint = TextPrimary
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      // -----------------------------------------------------------------
      // GÜNLÜK VAZİFE VE VİRD ÇETELESİ İÇERİĞİ
      // -----------------------------------------------------------------
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // 1. Sınıf Özeti Bento Paneli
        item {
          Surface(
            color = CanvasSurface,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  SeljukStarBox(size = 28.dp, backgroundColor = FeatureAttendanceGreen) {
                    Icon(
                      imageVector = Icons.Default.SelfImprovement,
                      contentDescription = null,
                      tint = Color.White,
                      modifier = Modifier.size(15.dp)
                    )
                  }
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "Sınıf Günlük Vird Tablosu",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary
                  )
                }

                Surface(
                  color = FeatureAttendanceGreen.copy(alpha = 0.12f),
                  shape = RoundedCornerShape(6.dp)
                ) {
                  Text(
                    text = "$totalFullPrayersCount / ${students.size} Tam Vakit",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = FeatureAttendanceGreen,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              // 4 Bento Kartı (Namaz, Kur'an, Risale, Salavat)
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                DutyStatBentoCard("5 Vakit Namaz", "$totalFullPrayersCount Talebe", FeatureAttendanceGreen, Modifier.weight(1f))
                DutyStatBentoCard("Kur'an-ı Kerim", "$totalQuranPagesToday Syf", DeepBlueNavy, Modifier.weight(1f))
                DutyStatBentoCard("Risale-i Nur", "$totalRisalePagesToday Syf", FeatureMemorizationPurple, Modifier.weight(1f))
                DutyStatBentoCard("Salavat-ı Şerife", "$totalSalavatToday", TezhipGoldDark, Modifier.weight(1f))
              }
            }
          }
        }

        // 2. Toplu Hızlı Vird & Sayfa Ekleme Butonları
        item {
          Surface(
            color = CanvasSurface,
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
            ) {
              Text(
                text = "⚡ Toplu Sınıf Vazife İşlemleri:",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
              )
              Spacer(modifier = Modifier.height(6.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                // Tüm Sınıf 5 Vakit
                Button(
                  onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onMarkAllPrayers(selectedDate, activeStudents, true)
                    Toast.makeText(context, "Tüm aktif talebeler için 5 vakit namaz tamamlandı! 🌿", Toast.LENGTH_SHORT).show()
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = FeatureAttendanceGreen),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.weight(1.3f),
                  contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                  Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(13.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Tüm Sınıf 5 Vakit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Tüm Sınıfa +1 Sayfa Kur'an
                OutlinedButton(
                  onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    activeStudents.forEach { st ->
                      onUpdateDuty(st.id, selectedDate) { it.copy(quranPages = it.quranPages + 1) }
                    }
                    Toast.makeText(context, "Tüm öğrencilere +1 sayfa Kur'an eklendi", Toast.LENGTH_SHORT).show()
                  },
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.weight(1f),
                  contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                  Text("+1 Syf Kur'an", fontSize = 11.sp, color = DeepBlueNavy, fontWeight = FontWeight.Bold)
                }

                // Tüm Sınıfa +5 Sayfa Risale
                OutlinedButton(
                  onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    activeStudents.forEach { st ->
                      onUpdateDuty(st.id, selectedDate) { it.copy(risalePages = it.risalePages + 5) }
                    }
                    Toast.makeText(context, "Tüm öğrencilere +5 sayfa Risale eklendi", Toast.LENGTH_SHORT).show()
                  },
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.weight(1f),
                  contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                  Text("+5 Syf Risale", fontSize = 11.sp, color = FeatureMemorizationPurple, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }

        // 3. Durum Filtre Çipleri
        item {
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            val filterOptions = listOf(
              "Tümü" to "Tümü (${students.size})",
              "5 Vakit Tam" to "5 Vakit Tam 🕌 ($totalFullPrayersCount)",
              "Eksik Vakit" to "Eksik Vakit ⏳",
              "Kur'an" to "Kur'an Okuyanlar 📖",
              "Risale" to "Risale Okuyanlar 📚"
            )
            items(filterOptions) { (key, label) ->
              val isSelected = dutyFilter == key
              FilterChip(
                selected = isSelected,
                onClick = { dutyFilter = key },
                label = {
                  Text(
                    text = label,
                    fontSize = 11.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                  )
                },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = FeatureAttendanceGreen,
                  selectedLabelColor = Color.White,
                  containerColor = CanvasSurface,
                  labelColor = TextPrimary
                ),
                border = FilterChipDefaults.filterChipBorder(
                  enabled = true,
                  selected = isSelected,
                  borderColor = if (isSelected) FeatureAttendanceGreen else BorderLight
                ),
                shape = RoundedCornerShape(10.dp)
              )
            }
          }
        }

        // 4. Arama Çubuğu
        item {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Talebe ara (isim veya sınıf)...", fontSize = 12.5.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
              if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { searchQuery = "" }) {
                  Icon(Icons.Default.Clear, contentDescription = "Temizle", modifier = Modifier.size(16.dp))
                }
              }
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = CanvasSurface,
              unfocusedContainerColor = CanvasSurface,
              focusedBorderColor = FeatureAttendanceGreen,
              unfocusedBorderColor = BorderLight
            ),
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("duty_search_input")
          )
        }

        // 5. Öğrenci Başına Detaylı Vazife ve Namaz Düzenleme Kartları
        if (filteredStudents.isEmpty()) {
          item {
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = CanvasSurface,
              border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Icon(
                  imageVector = Icons.Default.SearchOff,
                  contentDescription = null,
                  tint = TextSecondary,
                  modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = "Bu filtreye uygun talebe bulunamadı.",
                  fontSize = 13.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary
                )
              }
            }
          }
        } else {
          items(filteredStudents, key = { it.id }) { student ->
            val currentDuty = dutiesForSelectedDate.find { it.studentId == student.id }
              ?: DailyDutyRecord(studentId = student.id, date = selectedDate)

            StudentDutyAdminCard(
              student = student,
              duty = currentDuty,
              selectedDate = selectedDate,
              onTogglePrayer = { prayerKey, newVal ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val updated = when (prayerKey) {
                  "fajr" -> currentDuty.copy(fajr = newVal)
                  "dhuhr" -> currentDuty.copy(dhuhr = newVal)
                  "asr" -> currentDuty.copy(asr = newVal)
                  "maghrib" -> currentDuty.copy(maghrib = newVal)
                  "isha" -> currentDuty.copy(isha = newVal)
                  else -> currentDuty
                }
                onSaveDuty(updated)
              },
              onToggleTesbihat = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onSaveDuty(currentDuty.copy(tesbihatDone = !currentDuty.tesbihatDone))
              },
              onToggleCevsen = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onSaveDuty(currentDuty.copy(cevsenDone = !currentDuty.cevsenDone))
              },
              onAdjustQuran = { delta ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val newPages = (currentDuty.quranPages + delta).coerceAtLeast(0)
                onSaveDuty(currentDuty.copy(quranPages = newPages))
              },
              onAdjustRisale = { delta ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val newPages = (currentDuty.risalePages + delta).coerceAtLeast(0)
                onSaveDuty(currentDuty.copy(risalePages = newPages))
              },
              onAdjustSalavat = { delta ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val newCount = (currentDuty.salavatCount + delta).coerceAtLeast(0)
                onSaveDuty(currentDuty.copy(salavatCount = newCount))
              },
              onOpenNoteDialog = {
                noteEditingStudentDuty = Pair(student, currentDuty)
              }
            )
          }
        }
      }
    }
  }

  // -----------------------------------------------------------------
  // HOCA ÖZEL NOTU / VAZİFE GERİ BİLDİRİM DIALOGU
  // -----------------------------------------------------------------
  noteEditingStudentDuty?.let { (student, duty) ->
    var tempNote by remember(duty) { mutableStateOf(duty.notes) }
    var tempQuranPages by remember(duty) { mutableIntStateOf(duty.quranPages) }
    var tempRisalePages by remember(duty) { mutableIntStateOf(duty.risalePages) }
    var tempSalavat by remember(duty) { mutableIntStateOf(duty.salavatCount) }

    Dialog(onDismissRequest = { noteEditingStudentDuty = null }) {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CanvasSurface),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, GoldSubtleBorderGradient),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            StudentAvatar(name = student.fullName, colorIndex = student.avatarColorIndex, size = 36.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "${student.fullName} - Vazife Düzenle",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextPrimary
              )
              Text(
                text = "$selectedDate tarihli vird detayları",
                fontSize = 11.5.sp,
                color = TextSecondary
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Kur'an & Risale Sayfaları Girişi
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            OutlinedTextField(
              value = if (tempQuranPages > 0) tempQuranPages.toString() else "",
              onValueChange = { tempQuranPages = it.toIntOrNull() ?: 0 },
              label = { Text("Kur'an (Sayfa)", fontSize = 11.sp) },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(10.dp),
              singleLine = true
            )

            OutlinedTextField(
              value = if (tempRisalePages > 0) tempRisalePages.toString() else "",
              onValueChange = { tempRisalePages = it.toIntOrNull() ?: 0 },
              label = { Text("Risale (Sayfa)", fontSize = 11.sp) },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(10.dp),
              singleLine = true
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          OutlinedTextField(
            value = if (tempSalavat > 0) tempSalavat.toString() else "",
            onValueChange = { tempSalavat = it.toIntOrNull() ?: 0 },
            label = { Text("Salavat-ı Şerife Adedi", fontSize = 11.sp) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            singleLine = true
          )

          Spacer(modifier = Modifier.height(10.dp))

          // Hoca Değerlendirme & Teşvik Notu
          OutlinedTextField(
            value = tempNote,
            onValueChange = { tempNote = it },
            label = { Text("Hoca Notu / Tebrik & İkaz", fontSize = 11.sp) },
            placeholder = { Text("Örn: Sabah namazı camide kılındı, tebrikler!", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            minLines = 2,
            maxLines = 3
          )

          // Hızlı Şablonlar
          Spacer(modifier = Modifier.height(8.dp))
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val presets = listOf(
              "Maşallah, virdlerini eksiksiz tamamladı 🌟",
              "Kur'an tilaveti çok akıcıydı 👏",
              "Sabah namazı cemaatle kılındı 🕌",
              "Yarın tilavet sayfaları artırılacak 📖"
            )
            items(presets) { preset ->
              Surface(
                color = FeatureAttendanceGreen.copy(alpha = 0.1f),
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, FeatureAttendanceGreen.copy(alpha = 0.3f)),
                modifier = Modifier.clickable { tempNote = preset }
              ) {
                Text(
                  text = preset,
                  fontSize = 10.5.sp,
                  color = DarkSlateNavy,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
          ) {
            TextButton(onClick = { noteEditingStudentDuty = null }) {
              Text("İptal", color = TextSecondary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = {
                val updatedDuty = duty.copy(
                  quranPages = tempQuranPages,
                  risalePages = tempRisalePages,
                  salavatCount = tempSalavat,
                  notes = tempNote
                )
                onSaveDuty(updatedDuty)
                noteEditingStudentDuty = null
                Toast.makeText(context, "${student.fullName} vazife notu kaydedildi", Toast.LENGTH_SHORT).show()
              },
              colors = ButtonDefaults.buttonColors(containerColor = FeatureAttendanceGreen),
              shape = RoundedCornerShape(10.dp)
            ) {
              Text("Kaydet", color = Color.White, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }

  // -----------------------------------------------------------------
  // TAKVİM SEÇİCİ DİALOGU
  // -----------------------------------------------------------------
  if (showCalendarDialog) {
    AttendanceCalendarDialog(
      selectedDate = selectedDate,
      attendanceDates = allDuties.map { it.date }.toSet(),
      onDateSelected = {
        onDateChange(it)
        showCalendarDialog = false
      },
      onDismiss = { showCalendarDialog = false }
    )
  }
}

// =========================================================================
// BENTO STAT KARTI
// =========================================================================
@Composable
private fun DutyStatBentoCard(
  title: String,
  value: String,
  color: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    color = color.copy(alpha = 0.08f),
    shape = RoundedCornerShape(10.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f)),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = title,
        fontSize = 9.5.sp,
        color = TextSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = value,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

// =========================================================================
// ÖĞRENCİ VAZİFE YÖNETİCİ KARTI (STUDENT DUTY ADMIN CARD)
// =========================================================================
@Composable
private fun StudentDutyAdminCard(
  student: Student,
  duty: DailyDutyRecord,
  selectedDate: String,
  onTogglePrayer: (String, Boolean) -> Unit,
  onToggleTesbihat: () -> Unit,
  onToggleCevsen: () -> Unit,
  onAdjustQuran: (Int) -> Unit,
  onAdjustRisale: (Int) -> Unit,
  onAdjustSalavat: (Int) -> Unit,
  onOpenNoteDialog: () -> Unit
) {
  // Duty completion score calculation
  var score = 0
  if (duty.fajr) score += 15
  if (duty.dhuhr) score += 15
  if (duty.asr) score += 15
  if (duty.maghrib) score += 15
  if (duty.isha) score += 15
  if (duty.tesbihatDone) score += 10
  if (duty.quranPages > 0) score += 10
  if (duty.risalePages > 0) score += 5
  score = score.coerceIn(0, 100)

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = CanvasSurface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("student_duty_card_${student.id}")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)
    ) {
      // 1. Talebe Başlığı & Puan Rozeti
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          StudentAvatar(
            name = student.fullName,
            colorIndex = student.avatarColorIndex,
            size = 36.dp
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = student.fullName,
              fontWeight = FontWeight.Bold,
              fontSize = 14.5.sp,
              color = TextPrimary
            )
            Text(
              text = student.grade,
              fontSize = 11.sp,
              color = TextSecondary
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          // Puan Rozeti
          Surface(
            color = if (score >= 80) FeatureAttendanceGreen.copy(alpha = 0.15f)
                    else if (score >= 40) TezhipGold.copy(alpha = 0.15f)
                    else StatusAbsentRed.copy(alpha = 0.12f),
            shape = RoundedCornerShape(8.dp)
          ) {
            Text(
              text = "%$score Vird",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = if (score >= 80) FeatureAttendanceGreen
                      else if (score >= 40) DarkSlateNavy
                      else StatusAbsentRed,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
            )
          }

          Spacer(modifier = Modifier.width(6.dp))

          // Hoca Notu Butonu
          IconButton(
            onClick = onOpenNoteDialog,
            modifier = Modifier.size(28.dp)
          ) {
            Icon(
              imageVector = if (duty.notes.isNotEmpty()) Icons.Default.Comment else Icons.Default.EditNote,
              contentDescription = "Hoca Notu",
              tint = if (duty.notes.isNotEmpty()) TezhipGold else TextSecondary,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // 2. 5 VAKİT NAMAZ BUTONLARI (Interactive Prayer Selector Strip)
      Text(
        text = "5 Vakit Namaz Takibi",
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary
      )
      Spacer(modifier = Modifier.height(4.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        PrayerTogglePill("Sabah", "S", duty.fajr, { onTogglePrayer("fajr", !duty.fajr) }, Modifier.weight(1f))
        PrayerTogglePill("Öğle", "Ö", duty.dhuhr, { onTogglePrayer("dhuhr", !duty.dhuhr) }, Modifier.weight(1f))
        PrayerTogglePill("İkindi", "İ", duty.asr, { onTogglePrayer("asr", !duty.asr) }, Modifier.weight(1f))
        PrayerTogglePill("Akşam", "A", duty.maghrib, { onTogglePrayer("maghrib", !duty.maghrib) }, Modifier.weight(1f))
        PrayerTogglePill("Yatsı", "Y", duty.isha, { onTogglePrayer("isha", !duty.isha) }, Modifier.weight(1f))
      }

      Spacer(modifier = Modifier.height(8.dp))

      // 3. VİRD, TİLAVET VE RİSALE SAYAÇLARI
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Tesbihat Rozet Toggle
        Surface(
          onClick = onToggleTesbihat,
          shape = RoundedCornerShape(8.dp),
          color = if (duty.tesbihatDone) FeatureAttendanceGreen.copy(alpha = 0.15f) else CanvasBackground,
          border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (duty.tesbihatDone) FeatureAttendanceGreen else BorderLight
          ),
          modifier = Modifier.weight(1f)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Icon(
              imageVector = if (duty.tesbihatDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
              contentDescription = null,
              tint = if (duty.tesbihatDone) FeatureAttendanceGreen else TextSecondary,
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Tesbihat",
              fontSize = 11.sp,
              fontWeight = if (duty.tesbihatDone) FontWeight.Bold else FontWeight.Medium,
              color = if (duty.tesbihatDone) FeatureAttendanceGreen else TextSecondary
            )
          }
        }

        // Cevşen Rozet Toggle
        Surface(
          onClick = onToggleCevsen,
          shape = RoundedCornerShape(8.dp),
          color = if (duty.cevsenDone) TezhipGold.copy(alpha = 0.2f) else CanvasBackground,
          border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (duty.cevsenDone) TezhipGold else BorderLight
          ),
          modifier = Modifier.weight(1f)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Icon(
              imageVector = if (duty.cevsenDone) Icons.Default.AutoAwesome else Icons.Default.RadioButtonUnchecked,
              contentDescription = null,
              tint = if (duty.cevsenDone) DarkSlateNavy else TextSecondary,
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Cevşen",
              fontSize = 11.sp,
              fontWeight = if (duty.cevsenDone) FontWeight.Bold else FontWeight.Medium,
              color = if (duty.cevsenDone) DarkSlateNavy else TextSecondary
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Kur'an Tilavet Sayacı & Risale Sayacı
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        // Kur'an Stepper
        DutyStepperCompact(
          label = "Kur'an",
          valueText = "${duty.quranPages} Syf",
          color = FeatureAttendanceGreen,
          onMinus = { onAdjustQuran(-1) },
          onPlus = { onAdjustQuran(1) },
          modifier = Modifier.weight(1f)
        )

        // Risale Stepper
        DutyStepperCompact(
          label = "Risale",
          valueText = "${duty.risalePages} Syf",
          color = FeatureMemorizationPurple,
          onMinus = { onAdjustRisale(-5) },
          onPlus = { onAdjustRisale(5) },
          modifier = Modifier.weight(1f)
        )

        // Salavat Stepper
        DutyStepperCompact(
          label = "Salavat",
          valueText = "${duty.salavatCount}",
          color = TezhipGold,
          onMinus = { onAdjustSalavat(-100) },
          onPlus = { onAdjustSalavat(100) },
          modifier = Modifier.weight(1f)
        )
      }

      // Varsa Hoca Notu Şeridi
      if (duty.notes.isNotBlank()) {
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
          color = TezhipGold.copy(alpha = 0.08f),
          shape = RoundedCornerShape(8.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, TezhipGold.copy(alpha = 0.25f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Notes, contentDescription = null, tint = TezhipGold, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = duty.notes,
              fontSize = 11.sp,
              color = DarkSlateNavy,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------------------
// PRAYER TOGGLE PILL (NAMAZ HAP BUTONU)
// -------------------------------------------------------------------------
@Composable
private fun PrayerTogglePill(
  title: String,
  shortLetter: String,
  isChecked: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val bgColor by animateColorAsState(
    targetValue = if (isChecked) FeatureAttendanceGreen else CanvasBackground,
    label = "prayer_bg"
  )
  val textColor by animateColorAsState(
    targetValue = if (isChecked) Color.White else TextPrimary,
    label = "prayer_txt"
  )

  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(8.dp),
    color = bgColor,
    border = androidx.compose.foundation.BorderStroke(
      width = 1.dp,
      color = if (isChecked) TezhipGold else BorderLight
    ),
    shadowElevation = if (isChecked) 1.dp else 0.dp,
    modifier = modifier.height(34.dp)
  ) {
    Box(contentAlignment = Alignment.Center) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        if (isChecked) {
          Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(12.dp)
          )
          Spacer(modifier = Modifier.width(2.dp))
        }
        Text(
          text = title,
          fontSize = 10.5.sp,
          fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium,
          color = textColor
        )
      }
    }
  }
}

// -------------------------------------------------------------------------
// COMPACT DUTY STEPPER (KÜÇÜK SAYAÇ BUTONU)
// -------------------------------------------------------------------------
@Composable
private fun DutyStepperCompact(
  label: String,
  valueText: String,
  color: Color,
  onMinus: () -> Unit,
  onPlus: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    color = CanvasBackground,
    shape = RoundedCornerShape(8.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
    modifier = modifier
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      IconButton(
        onClick = onMinus,
        modifier = Modifier.size(22.dp)
      ) {
        Text("-", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
      }

      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 9.sp, color = TextMuted)
        Text(text = valueText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
      }

      IconButton(
        onClick = onPlus,
        modifier = Modifier.size(22.dp)
      ) {
        Text("+", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
      }
    }
  }
}

// -------------------------------------------------------------------------
// AY TAKVİM DİALOGU
// -------------------------------------------------------------------------
@Composable
fun AttendanceCalendarDialog(
  selectedDate: String,
  attendanceDates: Set<String>,
  onDateSelected: (String) -> Unit,
  onDismiss: () -> Unit
) {
  val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
  val cal = Calendar.getInstance().apply {
    try {
      val parsed = dateFormat.parse(selectedDate)
      if (parsed != null) time = parsed
    } catch (e: Exception) {}
  }

  var currentYear by remember { mutableIntStateOf(cal.get(Calendar.YEAR)) }
  var currentMonth by remember { mutableIntStateOf(cal.get(Calendar.MONTH)) }

  val monthFormat = SimpleDateFormat("MMMM yyyy", Locale("tr"))
  val monthCal = Calendar.getInstance().apply {
    set(Calendar.YEAR, currentYear)
    set(Calendar.MONTH, currentMonth)
    set(Calendar.DAY_OF_MONTH, 1)
  }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = CanvasSurface),
      border = androidx.compose.foundation.BorderStroke(1.2.dp, GoldSubtleBorderGradient),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
      ) {
        // Month Navigation
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(onClick = {
            if (currentMonth == 0) {
              currentMonth = 11
              currentYear--
            } else {
              currentMonth--
            }
          }) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Önceki Ay")
          }

          Text(
            text = monthFormat.format(monthCal.time),
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = TextPrimary
          )

          IconButton(onClick = {
            if (currentMonth == 11) {
              currentMonth = 0
              currentYear++
            } else {
              currentMonth++
            }
          }) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Sonraki Ay")
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Days Header (Pzt, Sal, Çar, Per, Cum, Cmt, Paz)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceAround
        ) {
          listOf("Pt", "Sa", "Ça", "Pe", "Cu", "Ct", "Pz").forEach { day ->
            Text(
              text = day,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = TextSecondary,
              textAlign = TextAlign.Center,
              modifier = Modifier.width(32.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Calendar Days Grid
        val firstDayOfWeek = (monthCal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Monday = 0
        val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val totalCells = ((firstDayOfWeek + daysInMonth + 6) / 7) * 7

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          for (row in 0 until totalCells / 7) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceAround
            ) {
              for (col in 0 until 7) {
                val cellIndex = row * 7 + col
                val dayNumber = cellIndex - firstDayOfWeek + 1

                if (dayNumber in 1..daysInMonth) {
                  val dayDateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d", currentYear, currentMonth + 1, dayNumber)
                  val isSelected = dayDateStr == selectedDate
                  val hasData = attendanceDates.contains(dayDateStr)

                  Surface(
                    onClick = { onDateSelected(dayDateStr) },
                    shape = CircleShape,
                    color = if (isSelected) FeatureAttendanceGreen else if (hasData) FeatureAttendanceGreen.copy(alpha = 0.12f) else Color.Transparent,
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, TezhipGold) else null,
                    modifier = Modifier.size(32.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Text(
                        text = dayNumber.toString(),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else TextPrimary
                      )
                    }
                  }
                } else {
                  Spacer(modifier = Modifier.size(32.dp))
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          TextButton(onClick = onDismiss) {
            Text("Kapat", color = TextSecondary)
          }
        }
      }
    }
  }
}
