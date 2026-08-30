package com.example.ui.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MemorizationRecord
import com.example.data.model.Student
import com.example.ui.theme.*

/**
 * Modern, aşağıdan kayarak açılan (Modal Bottom Sheet) öğrenci seçim penceresi.
 * Ezber Takip, Gelişim Takip ve Raporlar ekranlarında ortak kullanılır.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentBottomSheetPicker(
  students: List<Student>,
  selectedStudentId: Long?,
  memorizationList: List<MemorizationRecord> = emptyList(),
  title: String = "Talebe Seçin",
  onDismiss: () -> Unit,
  onStudentSelected: (Student) -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var searchQuery by remember { mutableStateOf("") }
  var selectedGradeFilter by remember { mutableStateOf("Tümü") }

  val gradeList = remember(students) {
    listOf("Tümü") + students.map { it.grade }.filter { it.isNotBlank() }.distinct().sorted()
  }

  val filteredStudents = remember(students, searchQuery, selectedGradeFilter) {
    students.filter { student ->
      val matchesSearch = searchQuery.isBlank() ||
        student.fullName.contains(searchQuery, ignoreCase = true) ||
        student.grade.contains(searchQuery, ignoreCase = true)

      val matchesGrade = selectedGradeFilter == "Tümü" || student.grade == selectedGradeFilter
      matchesSearch && matchesGrade
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = CanvasSurface,
    dragHandle = {
      BottomSheetDefaults.DragHandle(
        color = BorderLight,
        height = 4.dp,
        width = 40.dp
      )
    },
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 32.dp)
    ) {
      // Başlık ve Kapat Butonu
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          Text(
            text = "${students.size} kayıtlı talebe arasından seçim yapın",
            fontSize = 12.sp,
            color = TextSecondary
          )
        }

        IconButton(
          onClick = onDismiss,
          modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(SurfaceVariantColor)
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Kapat",
            tint = TextSecondary,
            modifier = Modifier.size(18.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Arama Çubuğu
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Talebe adı veya sınıfı ile ara...", fontSize = 13.sp) },
        leadingIcon = {
          Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = FeatureStudentsBlue,
            modifier = Modifier.size(20.dp)
          )
        },
        trailingIcon = {
          if (searchQuery.isNotBlank()) {
            IconButton(onClick = { searchQuery = "" }) {
              Icon(Icons.Default.Clear, contentDescription = "Temizle", modifier = Modifier.size(16.dp))
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = TextPrimary,
          unfocusedTextColor = TextPrimary,
          focusedPlaceholderColor = TextMuted,
          unfocusedPlaceholderColor = TextMuted,
          focusedBorderColor = FeatureStudentsBlue,
          unfocusedBorderColor = BorderLight,
          focusedContainerColor = CanvasBackground,
          unfocusedContainerColor = CanvasBackground,
          cursorColor = FeatureStudentsBlue
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("student_picker_search_input")
      )

      // Sınıf Filtre Çipleri
      if (gradeList.size > 1) {
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          items(gradeList) { grade ->
            val isSelected = selectedGradeFilter == grade
            FilterChip(
              selected = isSelected,
              onClick = { selectedGradeFilter = grade },
              label = {
                Text(
                  text = grade,
                  fontSize = 12.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = FeatureStudentsBlue,
                selectedLabelColor = Color.White,
                containerColor = CanvasBackground
              ),
              shape = RoundedCornerShape(10.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Öğrenci Listesi
      if (filteredStudents.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = Icons.Default.PersonSearch,
              contentDescription = null,
              tint = TextMuted,
              modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Aramaya uygun talebe bulunamadı",
              color = TextSecondary,
              fontSize = 13.5.sp
            )
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(filteredStudents, key = { it.id }) { student ->
            val isSelected = student.id == selectedStudentId
            val completedCount = memorizationList.count {
              it.studentId == student.id && (it.status == "TAMAMLANDI" || it.progressPercent >= 100)
            }

            Surface(
              onClick = {
                onStudentSelected(student)
                onDismiss()
              },
              shape = RoundedCornerShape(14.dp),
              color = if (isSelected) FeatureStudentsBlue.copy(alpha = 0.10f) else CanvasBackground,
              border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isSelected) FeatureStudentsBlue else BorderLight
              ),
              shadowElevation = if (isSelected) 1.dp else 0.dp,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("student_picker_item_${student.id}")
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 14.dp, vertical = 10.dp),
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
                    size = 38.dp,
                    fontSize = 14
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Column {
                    Text(
                      text = student.fullName,
                      fontSize = 14.5.sp,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                      color = if (isSelected) FeatureStudentsBlue else TextPrimary
                    )
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      if (student.grade.isNotBlank()) {
                        Text(
                          text = student.grade,
                          fontSize = 11.5.sp,
                          color = FeatureStudentsBlue,
                          fontWeight = FontWeight.Medium
                        )
                      }
                      if (memorizationList.isNotEmpty()) {
                        Text(
                          text = "•",
                          fontSize = 10.sp,
                          color = TextMuted
                        )
                        Text(
                          text = "$completedCount/108 Tamamlandı",
                          fontSize = 11.sp,
                          color = if (completedCount > 0) FeatureAttendanceGreen else TextSecondary,
                          fontWeight = if (completedCount > 0) FontWeight.SemiBold else FontWeight.Normal
                        )
                      }
                    }
                  }
                }

                if (isSelected) {
                  Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Seçili",
                    tint = FeatureStudentsBlue,
                    modifier = Modifier.size(22.dp)
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
