package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CurriculumData
import com.example.data.model.MemorizationRecord
import com.example.data.model.Student
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveListeningDialog(
  students: List<Student>,
  initialStudent: Student?,
  allMemorization: List<MemorizationRecord>,
  todayDate: String,
  onDismiss: () -> Unit,
  onSaveRecord: (MemorizationRecord) -> Unit
) {
  val haptic = LocalHapticFeedback.current
  val coroutineScope = rememberCoroutineScope()

  var selectedStudentIndex by remember {
    val idx = students.indexOfFirst { it.id == initialStudent?.id }
    mutableIntStateOf(if (idx >= 0) idx else 0)
  }

  val currentStudent = students.getOrNull(selectedStudentIndex) ?: return

  // Filter category
  var selectedCategory by remember { mutableStateOf("Kur'an") }
  val availableItems = remember(selectedCategory) {
    when (selectedCategory) {
      "Kur'an" -> CurriculumData.KURAN_ITEMS
      "Risale" -> CurriculumData.RISALE_ITEMS
      else -> CurriculumData.TESBIHAT_ITEMS
    }
  }

  var selectedItemTitle by remember {
    mutableStateOf(availableItems.firstOrNull() ?: "Fatiha Suresi")
  }

  // Update selectedItemTitle when category changes
  LaunchedEffect(selectedCategory) {
    if (!availableItems.contains(selectedItemTitle)) {
      selectedItemTitle = availableItems.firstOrNull() ?: ""
    }
  }

  var teacherNote by remember { mutableStateOf("") }
  var showSuccessAnimation by remember { mutableStateOf(false) }
  var lastSavedMessage by remember { mutableStateOf("") }

  // Check if current item is already completed by student
  val studentMems = remember(currentStudent, allMemorization) {
    allMemorization.filter { it.studentId == currentStudent.id }
  }
  val isCurrentItemCompleted = studentMems.any {
    it.title.equals(selectedItemTitle.trim(), ignoreCase = true) &&
        (it.status == "TAMAMLANDI" || it.progressPercent >= 100)
  }

  fun handleGradeSelection(status: String, rating: Int, progressPercent: Int, label: String) {
    // 1. Haptic vibration feedback
    try {
      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    } catch (e: Exception) {}

    // 2. Save record to DB
    val newRecord = MemorizationRecord(
      studentId = currentStudent.id,
      title = selectedItemTitle,
      category = selectedCategory,
      status = status,
      rating = rating,
      progressPercent = progressPercent,
      teacherNotes = teacherNote.trim(),
      date = todayDate
    )
    onSaveRecord(newRecord)

    // 3. Trigger visual success animation
    lastSavedMessage = "${currentStudent.fullName} • $selectedItemTitle ($label)"
    showSuccessAnimation = true
    teacherNote = ""

    coroutineScope.launch {
      delay(900)
      showSuccessAnimation = false
      // Automatically advance to next student if more students exist
      if (selectedStudentIndex < students.size - 1) {
        selectedStudentIndex++
      }
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color(0xE60A192F))
        .systemBarsPadding()
        .padding(16.dp),
      contentAlignment = Alignment.Center
    ) {
      Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CanvasSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldStar.copy(alpha = 0.4f)),
        modifier = Modifier
          .fillMaxWidth()
          .fillMaxHeight(0.92f)
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
        ) {
          // Top Header Bar
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(38.dp)
                  .clip(CircleShape)
                  .background(FeatureMemorizationPurple.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Mic,
                  contentDescription = null,
                  tint = FeatureMemorizationPurple,
                  modifier = Modifier.size(22.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "🎙️ Canlı Ezber Dinleme",
                  fontSize = 17.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = TextPrimary
                )
                Text(
                  text = "Hızlı not verme & sıradaki talebe akışı",
                  fontSize = 11.5.sp,
                  color = TextSecondary
                )
              }
            }

            IconButton(
              onClick = onDismiss,
              modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0x15000000))
                .testTag("close_live_listening_button")
            ) {
              Icon(Icons.Default.Close, contentDescription = "Kapat", tint = TextPrimary, modifier = Modifier.size(18.dp))
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // -------------------------------------------------------------
          // STUDENT NAVIGATION BAR (CURRENT / PREV / NEXT)
          // -------------------------------------------------------------
          Surface(
            shape = RoundedCornerShape(18.dp),
            color = DeepBlueNavy,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              IconButton(
                onClick = {
                  if (selectedStudentIndex > 0) {
                    selectedStudentIndex--
                  } else {
                    selectedStudentIndex = students.size - 1
                  }
                },
                modifier = Modifier.testTag("live_prev_student")
              ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Önceki", tint = Color.White)
              }

              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center
              ) {
                StudentAvatar(
                  name = currentStudent.fullName,
                  colorIndex = currentStudent.avatarColorIndex,
                  size = 42.dp,
                  fontSize = 15
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.Start) {
                  Text(
                    text = currentStudent.fullName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                  )
                  Text(
                    text = "${currentStudent.grade} • Sıra: ${selectedStudentIndex + 1}/${students.size}",
                    fontSize = 11.5.sp,
                    color = Color(0xFF93C5FD)
                  )
                }
              }

              IconButton(
                onClick = {
                  if (selectedStudentIndex < students.size - 1) {
                    selectedStudentIndex++
                  } else {
                    selectedStudentIndex = 0
                  }
                },
                modifier = Modifier.testTag("live_next_student")
              ) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Sonraki", tint = Color.White)
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // -------------------------------------------------------------
          // CATEGORY SELECTOR CHIPS
          // -------------------------------------------------------------
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            listOf("Kur'an" to "📖 Kur'an", "Risale" to "📚 Risale", "Tesbihat" to "📿 Tesbihat").forEach { (catKey, catLabel) ->
              val isSelected = selectedCategory == catKey
              Surface(
                onClick = { selectedCategory = catKey },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) FeatureMemorizationPurple else CanvasBackground,
                border = androidx.compose.foundation.BorderStroke(
                  1.dp,
                  if (isSelected) FeatureMemorizationPurple else BorderLight
                ),
                modifier = Modifier.weight(1f)
              ) {
                Text(
                  text = catLabel,
                  fontSize = 12.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSelected) Color.White else TextPrimary,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.padding(vertical = 8.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // -------------------------------------------------------------
          // SURAH / ITEM HORIZONTAL SELECTOR
          // -------------------------------------------------------------
          Text(
            text = "Dinlenen Sure / Bölüm:",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
          )

          Spacer(modifier = Modifier.height(4.dp))

          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            items(availableItems) { item ->
              val isSelected = item == selectedItemTitle
              val isCompletedByStudent = studentMems.any {
                it.title.equals(item.trim(), ignoreCase = true) && (it.status == "TAMAMLANDI" || it.progressPercent >= 100)
              }

              Surface(
                onClick = { selectedItemTitle = item },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) FeatureStudentsBlue else if (isCompletedByStudent) StatusPresentGreen.copy(alpha = 0.12f) else CanvasBackground,
                border = androidx.compose.foundation.BorderStroke(
                  1.dp,
                  if (isSelected) FeatureStudentsBlue else if (isCompletedByStudent) StatusPresentGreen else BorderLight
                ),
                modifier = Modifier.height(34.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                  if (isCompletedByStudent) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = if (isSelected) Color.White else StatusPresentGreen, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                  }
                  Text(
                    text = item,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) Color.White else TextPrimary,
                    maxLines = 1
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Quick Note Input
          OutlinedTextField(
            value = teacherNote,
            onValueChange = { teacherNote = it },
            placeholder = { Text("Hoca Notu (Örn: Tecvid hatasız, akıcı)", fontSize = 12.sp, color = TextSecondary) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("live_teacher_note_input"),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary,
              focusedPlaceholderColor = TextMuted,
              unfocusedPlaceholderColor = TextMuted,
              focusedContainerColor = CanvasSurface,
              unfocusedContainerColor = CanvasSurface,
              unfocusedBorderColor = BorderLight,
              focusedBorderColor = FeatureStudentsBlue,
              cursorColor = FeatureStudentsBlue
            )
          )

          Spacer(modifier = Modifier.weight(1f))

          // -------------------------------------------------------------
          // SUCCESS CONFIRMATION ANIMATION OVERLAY
          // -------------------------------------------------------------
          AnimatedVisibility(
            visible = showSuccessAnimation,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f)
          ) {
            Surface(
              color = StatusPresentGreen,
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(
                    text = "Ezber Başarıyla Kaydedildi! ✓",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                  )
                  Text(
                    text = lastSavedMessage,
                    fontSize = 11.5.sp,
                    color = Color.White.copy(alpha = 0.9f)
                  )
                }
              }
            }
          }

          // -------------------------------------------------------------
          // RAPID EVALUATION BIG ACTION BUTTONS
          // -------------------------------------------------------------
          Text(
            text = "Hızlı Notlandırma (Tek Dokunuş):",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )

          Spacer(modifier = Modifier.height(8.dp))

          // 1. TAM GEÇTİ (100 PUAN)
          Button(
            onClick = {
              handleGradeSelection(
                status = "TAMAMLANDI",
                rating = 5,
                progressPercent = 100,
                label = "100 Puan • Hatasız Geçti"
              )
            },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = StatusPresentGreen),
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("live_btn_pass_full")
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
              Spacer(modifier = Modifier.width(8.dp))
              Text("🟢 Tam Geçti (100 Puan • Hatasız)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // 2. TAKILMALI GEÇTİ (85 PUAN)
          Button(
            onClick = {
              handleGradeSelection(
                status = "TAMAMLANDI",
                rating = 4,
                progressPercent = 100,
                label = "85 Puan • Takılmalı Geçti"
              )
            },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("live_btn_pass_minor")
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Icon(Icons.Default.StarHalf, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
              Spacer(modifier = Modifier.width(8.dp))
              Text("🟡 1-2 Takılma İle Geçti (85 Puan)", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // 3. TEKRAR DİNLENECEK (YARIN)
          Button(
            onClick = {
              handleGradeSelection(
                status = "TEKRAR",
                rating = 3,
                progressPercent = 60,
                label = "Tekrar Dinlenecek"
              )
            },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = StatusAbsentRed),
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("live_btn_repeat")
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
              Spacer(modifier = Modifier.width(8.dp))
              Text("🔴 Tekrar Dinlenecek (Yarın)", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
          }
        }
      }
    }
  }
}
