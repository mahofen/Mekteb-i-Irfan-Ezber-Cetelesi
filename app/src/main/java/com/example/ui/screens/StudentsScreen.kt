package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceRecord
import com.example.data.model.MemorizationRecord
import com.example.data.model.Student
import com.example.data.util.BackupManager
import com.example.ui.components.AvatarColors
import com.example.ui.components.StudentAvatar
import com.example.ui.theme.*
import com.example.util.PdfExportHelper
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

// 5. Sınıftan Lise Son Sınıfa (12. Sınıf) kadar sınıf listesi
val STUDENT_GRADE_LEVELS = listOf(
  "5. Sınıf",
  "6. Sınıf",
  "7. Sınıf",
  "8. Sınıf",
  "9. Sınıf",
  "10. Sınıf",
  "11. Sınıf",
  "12. Sınıf"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentsScreen(
  students: List<Student>,
  attendanceList: List<AttendanceRecord>,
  memorizationList: List<MemorizationRecord>,
  onBackClick: () -> Unit,
  onSaveStudent: (Student) -> Unit,
  onDeleteStudent: (Student) -> Unit,
  onBulkSaveStudents: ((List<Student>, Boolean) -> Unit)? = null
) {
  val context = LocalContext.current
  var searchQuery by remember { mutableStateOf("") }
  var selectedGradeFilter by remember { mutableStateOf("Tüm Sınıflar") }
  var selectedTab by remember { mutableIntStateOf(0) } // 0: Talebe Listesi, 1: İletişim & Veli Listesi (PDF)
  var showGradeFilterDialog by remember { mutableStateOf(false) }
  var showPdfExportDialog by remember { mutableStateOf(false) }
  var showAddEditDialog by remember { mutableStateOf(false) }
  var showBulkImportDialog by remember { mutableStateOf(false) }
  var studentToEdit by remember { mutableStateOf<Student?>(null) }

  val filteredStudents = students.filter { student ->
    val matchesQuery = student.fullName.contains(searchQuery, ignoreCase = true) ||
        student.parentName.contains(searchQuery, ignoreCase = true) ||
        student.parentPhone.contains(searchQuery) ||
        student.phone.contains(searchQuery)
    val matchesGrade = selectedGradeFilter == "Tüm Sınıflar" ||
        student.grade.equals(selectedGradeFilter, ignoreCase = true) ||
        (selectedGradeFilter == "12. Sınıf" && student.grade.startsWith("12"))
    matchesQuery && matchesGrade
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = if (selectedTab == 1) "İletişim & Veli Rehberi" else "Öğrenciler",
              fontWeight = FontWeight.Bold,
              fontSize = 20.sp,
              color = Color.White
            )
            Text(
              text = "${students.size} Öğrenci Kayıtlı" +
                  if (selectedGradeFilter != "Tüm Sınıflar") " • $selectedGradeFilter (${filteredStudents.size})" else "",
              fontSize = 12.sp,
              color = Color.White.copy(alpha = 0.8f)
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onBackClick,
            modifier = Modifier.testTag("students_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Geri",
              tint = Color.White
            )
          }
        },
        actions = {
          // Top bar Bulk Import action icon
          IconButton(
            onClick = { showBulkImportDialog = true },
            modifier = Modifier.testTag("bulk_import_top_button")
          ) {
            Icon(
              imageVector = Icons.Default.GroupAdd,
              contentDescription = "Toplu Talebe Ekle",
              tint = Color.White
            )
          }

          // Top bar PDF Export action icon
          IconButton(
            onClick = { showPdfExportDialog = true },
            modifier = Modifier.testTag("pdf_export_top_button")
          ) {
            Icon(
              imageVector = Icons.Default.PictureAsPdf,
              contentDescription = "İletişim Listesini PDF Aktar",
              tint = Color.White
            )
          }

          // Top bar quick class filter icon
          IconButton(
            onClick = { showGradeFilterDialog = true },
            modifier = Modifier.testTag("grade_filter_top_button")
          ) {
            Icon(
              imageVector = Icons.Default.FilterList,
              contentDescription = "Sınıf Filtrele",
              tint = if (selectedGradeFilter != "Tüm Sınıflar") StatusExcusedAmber else Color.White
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = DeepBlueNavy
        )
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = {
          studentToEdit = null
          showAddEditDialog = true
        },
        containerColor = FeatureStudentsBlue,
        contentColor = Color.White,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
          .testTag("add_student_fab")
          .padding(bottom = 8.dp)
      ) {
        Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Yeni Öğrenci Ekle")
      }
    },
    containerColor = CanvasBackground
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // Tab Bar: Genel Talebe Listesi vs. İletişim & Veli Listesi (PDF)
      PrimaryTabRow(
        selectedTabIndex = selectedTab,
        containerColor = CanvasSurface,
        contentColor = FeatureStudentsBlue,
        indicator = {
          TabRowDefaults.PrimaryIndicator(
            modifier = Modifier.tabIndicatorOffset(selectedTab),
            color = FeatureStudentsBlue,
            height = 3.dp
          )
        },
        modifier = Modifier.fillMaxWidth()
      ) {
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.People,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
                tint = if (selectedTab == 0) FeatureStudentsBlue else TextSecondary
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Talebe Listesi",
                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                color = if (selectedTab == 0) FeatureStudentsBlue else TextSecondary,
                fontSize = 13.sp
              )
            }
          },
          modifier = Modifier.testTag("tab_student_list")
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Contacts,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
                tint = if (selectedTab == 1) FeatureStudentsBlue else TextSecondary
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "İletişim & Veli (PDF)",
                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                color = if (selectedTab == 1) FeatureStudentsBlue else TextSecondary,
                fontSize = 13.sp
              )
            }
          },
          modifier = Modifier.testTag("tab_contact_roster")
        )
      }

      // Search Bar and Class Filter Popup Button Row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Search Input
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = {
            Text(
              if (selectedTab == 1) "Öğrenci, veli veya tel ara..." else "Öğrenci adı ara...",
              color = TextMuted,
              fontSize = 14.sp
            )
          },
          leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Ara", tint = TextSecondary)
          },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(Icons.Default.Clear, contentDescription = "Temizle", tint = TextSecondary)
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(16.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedPlaceholderColor = TextMuted,
            unfocusedPlaceholderColor = TextMuted,
            focusedContainerColor = CanvasSurface,
            unfocusedContainerColor = CanvasSurface,
            focusedBorderColor = FeatureStudentsBlue,
            unfocusedBorderColor = BorderLight,
            cursorColor = FeatureStudentsBlue
          ),
          modifier = Modifier
            .weight(1f)
            .testTag("student_search_input")
        )

        // Class Filter Popup Trigger Button
        Surface(
          onClick = { showGradeFilterDialog = true },
          shape = RoundedCornerShape(16.dp),
          color = if (selectedGradeFilter != "Tüm Sınıflar") FeatureStudentsBlue else CanvasSurface,
          border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selectedGradeFilter != "Tüm Sınıflar") FeatureStudentsBlue else BorderLight
          ),
          shadowElevation = 1.dp,
          modifier = Modifier
            .height(56.dp)
            .testTag("student_grade_filter_button")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp)
          ) {
            Icon(
              imageVector = Icons.Default.School,
              contentDescription = "Sınıf Filtrele",
              tint = if (selectedGradeFilter != "Tüm Sınıflar") Color.White else FeatureStudentsBlue,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (selectedGradeFilter == "Tüm Sınıflar") "Sınıf ▾" else "$selectedGradeFilter ▾",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = if (selectedGradeFilter != "Tüm Sınıflar") Color.White else TextPrimary
            )
          }
        }
      }

      // Quick Bulk Import Banner in Tab 0
      if (selectedTab == 0) {
        Surface(
          onClick = { showBulkImportDialog = true },
          shape = RoundedCornerShape(12.dp),
          color = FeatureStudentsBlue.copy(alpha = 0.08f),
          border = androidx.compose.foundation.BorderStroke(1.dp, FeatureStudentsBlue.copy(alpha = 0.25f)),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("open_bulk_import_banner_button")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = CircleShape,
              color = FeatureStudentsBlue,
              modifier = Modifier.size(26.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.GroupAdd,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(15.dp)
                )
              }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "⚡ Otomatik / Toplu Talebe Ekle",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = FeatureStudentsBlue
              )
              Text(
                text = "Excel ve CSV dosyasından tek tıkla yükle",
                fontSize = 10.5.sp,
                color = TextSecondary
              )
            }
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
              contentDescription = null,
              tint = FeatureStudentsBlue,
              modifier = Modifier.size(13.dp)
            )
          }
        }
      }

      // Active Grade Filter Pill (if a specific grade is selected)
      if (selectedGradeFilter != "Tüm Sınıflar") {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            color = FeatureStudentsBlue.copy(alpha = 0.12f),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, FeatureStudentsBlue.copy(alpha = 0.25f))
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
              Icon(
                Icons.Default.School,
                contentDescription = null,
                tint = FeatureStudentsBlue,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Filtre: $selectedGradeFilter (${filteredStudents.size} talebe)",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = FeatureStudentsBlue
              )
              Spacer(modifier = Modifier.width(6.dp))
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Filtreyi Kaldır",
                tint = FeatureStudentsBlue,
                modifier = Modifier
                  .size(16.dp)
                  .clickable { selectedGradeFilter = "Tüm Sınıflar" }
              )
            }
          }
        }
      }

      // In Contact List tab: Show Hero PDF Export Card
      if (selectedTab == 1) {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = DeepBlueNavy),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.15f),
                modifier = Modifier.size(38.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = StatusExcusedAmber,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Öğrenci & Veli İletişim Rehberi",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
                Text(
                  text = "Öğrenci ad soyad, sınıf, veli ad soyad ve iletişim telefonlarını içerir.",
                  fontSize = 11.sp,
                  color = Color.White.copy(alpha = 0.8f),
                  lineHeight = 15.sp
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
              onClick = {
                PdfExportHelper.exportStudentContactListPdf(
                  context = context,
                  students = filteredStudents,
                  filterGradeTitle = selectedGradeFilter
                )
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = StatusExcusedAmber,
                contentColor = DeepBlueNavy
              ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("export_contact_list_pdf_button")
            ) {
              Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "📄 Listeyi PDF Olarak Dışa Aktar (${filteredStudents.size} Öğrenci)",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(2.dp))

      // Empty State or List
      if (filteredStudents.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = if (selectedTab == 1) Icons.Default.Contacts else Icons.Default.PeopleOutline,
              contentDescription = null,
              tint = TextMuted,
              modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = if (students.isEmpty()) "Henüz kayıtlı öğrenci yok" else "Aramaya uygun öğrenci bulunamadı",
              fontSize = 16.sp,
              fontWeight = FontWeight.Medium,
              color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (selectedGradeFilter != "Tüm Sınıflar") {
              TextButton(onClick = { selectedGradeFilter = "Tüm Sınıflar" }) {
                Text("Tüm Sınıfları Göster", fontWeight = FontWeight.Bold, color = FeatureStudentsBlue)
              }
            } else {
              Text(
                text = "Yeni öğrenci eklemek için sağ alttaki '+' butonuna dokunun.",
                fontSize = 13.sp,
                color = TextMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
              )
            }
          }
        }
      } else {
        if (selectedTab == 0) {
          // Tab 0: Regular Student List
          LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(filteredStudents, key = { it.id }) { student ->
              StudentSimpleRowItem(
                student = student,
                onClick = {
                  studentToEdit = student
                  showAddEditDialog = true
                }
              )
            }
          }
        } else {
          // Tab 1: Contact & Parent Roster (İletişim Listesi)
          LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            itemsIndexed(filteredStudents, key = { _, student -> student.id }) { index, student ->
              StudentContactRowItem(
                student = student,
                index = index,
                onEditClick = {
                  studentToEdit = student
                  showAddEditDialog = true
                }
              )
            }
          }
        }
      }
    }
  }

  // Sınıf Filtreleme Açılır Penceresi (Search Grade Filter Dialog)
  if (showGradeFilterDialog) {
    GradeFilterDialog(
      currentFilter = selectedGradeFilter,
      students = students,
      onDismiss = { showGradeFilterDialog = false },
      onSelectGrade = { grade ->
        selectedGradeFilter = grade
        showGradeFilterDialog = false
      }
    )
  }

  // PDF Export Options Dialog
  if (showPdfExportDialog) {
    PdfExportOptionsDialog(
      allStudents = students,
      filteredStudents = filteredStudents,
      currentGradeFilter = selectedGradeFilter,
      onDismiss = { showPdfExportDialog = false },
      onExport = { listToExport, gradeTitle ->
        showPdfExportDialog = false
        PdfExportHelper.exportStudentContactListPdf(
          context = context,
          students = listToExport,
          filterGradeTitle = gradeTitle
        )
      }
    )
  }

  // Add / Edit Student Dialog (Kayıt / Düzenleme Penceresi)
  if (showAddEditDialog) {
    AddEditStudentDialog(
      student = studentToEdit,
      onDismiss = { showAddEditDialog = false },
      onDelete = {
        if (studentToEdit != null) {
          onDeleteStudent(studentToEdit!!)
          showAddEditDialog = false
          Toast.makeText(context, "Öğrenci silindi", Toast.LENGTH_SHORT).show()
        }
      },
      onSave = { updatedStudent ->
        onSaveStudent(updatedStudent)
        showAddEditDialog = false
        Toast.makeText(
          context,
          if (studentToEdit == null) "Yeni öğrenci kaydedildi" else "Öğrenci güncellendi",
          Toast.LENGTH_SHORT
        ).show()
      }
    )
  }

  // Bulk Student Import Dialog (Toplu & Otomatik Talebe Ekleme Penceresi)
  if (showBulkImportDialog) {
    BulkStudentImportDialog(
      onDismiss = { showBulkImportDialog = false },
      onImport = { list, overwrite ->
        if (onBulkSaveStudents != null) {
          onBulkSaveStudents(list, overwrite)
        } else {
          list.forEach { onSaveStudent(it) }
        }
        showBulkImportDialog = false
        Toast.makeText(
          context,
          "Toplam ${list.size} talebe başarıyla sisteme aktarıldı!",
          Toast.LENGTH_LONG
        ).show()
      }
    )
  }
}

/**
 * Arama ve filtreleme için Sınıf Seçim Açılır Penceresi
 */
@Composable
private fun GradeFilterDialog(
  currentFilter: String,
  students: List<Student>,
  onDismiss: () -> Unit,
  onSelectGrade: (String) -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.School,
          contentDescription = null,
          tint = FeatureStudentsBlue,
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text("Sınıfa Göre Filtrele", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
          Text("Görüntülemek istediğiniz sınıfı seçin", fontSize = 12.sp, color = TextSecondary)
        }
      }
    },
    text = {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        // Tüm Sınıflar Seçeneği
        item {
          val isSelected = currentFilter == "Tüm Sınıflar"
          GradeFilterOptionRow(
            title = "Tüm Sınıflar",
            subtitle = "Bütün talebeleri listele",
            count = students.size,
            isSelected = isSelected,
            onClick = { onSelectGrade("Tüm Sınıflar") }
          )
          Spacer(modifier = Modifier.height(4.dp))
          Divider(color = BorderLight)
          Spacer(modifier = Modifier.height(4.dp))
        }

        // Ortaokul Başlığı (5 - 8)
        item {
          Text(
            text = "🏫 ORTAOKUL KADEMESİ",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
          )
        }

        items(STUDENT_GRADE_LEVELS.take(4)) { grade ->
          val count = students.count { it.grade.equals(grade, ignoreCase = true) }
          val isSelected = currentFilter == grade
          GradeFilterOptionRow(
            title = grade,
            subtitle = "Ortaokul",
            count = count,
            isSelected = isSelected,
            onClick = { onSelectGrade(grade) }
          )
        }

        // Lise Başlığı (9 - 12)
        item {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "🎓 LİSE KADEMESİ",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
          )
        }

        items(STUDENT_GRADE_LEVELS.drop(4)) { grade ->
          val count = students.count {
            it.grade.equals(grade, ignoreCase = true) ||
                (grade == "12. Sınıf" && it.grade.startsWith("12"))
          }
          val isSelected = currentFilter == grade
          GradeFilterOptionRow(
            title = if (grade == "12. Sınıf") "12. Sınıf (Lise Son)" else grade,
            subtitle = "Lise",
            count = count,
            isSelected = isSelected,
            onClick = { onSelectGrade(grade) }
          )
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Kapat", fontWeight = FontWeight.Bold, color = FeatureStudentsBlue)
      }
    },
    shape = RoundedCornerShape(20.dp),
    containerColor = CanvasSurface
  )
}

@Composable
private fun GradeFilterOptionRow(
  title: String,
  subtitle: String,
  count: Int,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(12.dp),
    color = if (isSelected) FeatureStudentsBlue.copy(alpha = 0.12f) else CanvasSurface,
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      if (isSelected) FeatureStudentsBlue else BorderLight
    ),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(
          selected = isSelected,
          onClick = onClick,
          colors = RadioButtonDefaults.colors(selectedColor = FeatureStudentsBlue)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
          Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) FeatureStudentsBlue else TextPrimary
          )
          Text(
            text = subtitle,
            fontSize = 11.sp,
            color = TextSecondary
          )
        }
      }

      Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) FeatureStudentsBlue else CanvasBackground
      ) {
        Text(
          text = "$count Talebe",
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold,
          color = if (isSelected) Color.White else TextSecondary,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
      }
    }
  }
}

/**
 * Sade ve temiz öğrenci liste elemanı (Ad-Soyad, Sınıf Rozeti ve Renkli Avatar)
 */
@Composable
private fun StudentSimpleRowItem(
  student: Student,
  onClick: () -> Unit
) {
  val studentGrade = if (student.grade.isNotBlank()) student.grade else "5. Sınıf"

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = CanvasSurface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag("student_item_${student.id}")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      StudentAvatar(
        name = student.fullName,
        colorIndex = student.avatarColorIndex,
        size = 44.dp,
        fontSize = 16
      )

      Spacer(modifier = Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = student.fullName,
          fontSize = 15.sp,
          fontWeight = FontWeight.SemiBold,
          color = TextPrimary
        )
        Spacer(modifier = Modifier.height(2.dp))
        // Sınıf Rozeti
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = FeatureStudentsBlue.copy(alpha = 0.08f),
          border = androidx.compose.foundation.BorderStroke(0.5.dp, FeatureStudentsBlue.copy(alpha = 0.2f))
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Icon(
              imageVector = Icons.Default.School,
              contentDescription = null,
              tint = FeatureStudentsBlue,
              modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = studentGrade,
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              color = FeatureStudentsBlue
            )
          }
        }
      }

      IconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Edit,
          contentDescription = "Düzenle",
          tint = TextSecondary,
          modifier = Modifier.size(18.dp)
        )
      }
    }
  }
}

/**
 * Kayıt ve Düzenleme Penceresi (Sınıf Seçimi Açılır Penceresi ile birlikte)
 */
@Composable
private fun AddEditStudentDialog(
  student: Student?,
  onDismiss: () -> Unit,
  onDelete: () -> Unit,
  onSave: (Student) -> Unit
) {
  var fullName by remember { mutableStateOf(student?.fullName ?: "") }
  var grade by remember { mutableStateOf(student?.grade?.takeIf { it.isNotBlank() } ?: "5. Sınıf") }
  var phone by remember { mutableStateOf(student?.phone ?: "") }
  var parentName by remember { mutableStateOf(student?.parentName ?: "") }
  var parentPhone by remember { mutableStateOf(student?.parentPhone ?: "") }
  var notes by remember { mutableStateOf(student?.notes ?: "") }
  var showGradePickerPopup by remember { mutableStateOf(false) }
  var showDeleteConfirm by remember { mutableStateOf(false) }

  val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
  val enrollmentDate = student?.enrollmentDate ?: dateFormat.format(Date())

  // Otomatik rastgele renk ataması (Yeni öğrenci ise rastgele seçilir)
  val assignedAvatarColorIndex = remember(student) {
    student?.avatarColorIndex ?: Random.nextInt(AvatarColors.size)
  }

  if (showDeleteConfirm) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirm = false },
      title = { Text("Öğrenciyi Sil", fontWeight = FontWeight.Bold) },
      text = { Text("\"$fullName\" isimli öğrenciyi ve tüm kayıtlarını silmek istediğinize emin misiniz?") },
      confirmButton = {
        Button(
          onClick = {
            showDeleteConfirm = false
            onDelete()
          },
          colors = ButtonDefaults.buttonColors(containerColor = StatusAbsentRed)
        ) {
          Text("Evet, Sil")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteConfirm = false }) {
          Text("Vazgeç")
        }
      }
    )
  }

  // Sınıf Seçimi Açılır Penceresi (Kayıt Formu İçin)
  if (showGradePickerPopup) {
    StudentGradePickerDialog(
      currentGrade = grade,
      onDismiss = { showGradePickerPopup = false },
      onGradeSelected = { selected ->
        grade = selected
        showGradePickerPopup = false
      }
    )
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = if (student == null) "Yeni Öğrenci Ekle" else "Öğrenci Bilgilerini Düzenle",
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp,
          color = TextPrimary
        )
      }
    },
    text = {
      LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        item {
          OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Adı Soyadı *") },
            placeholder = { Text("Örn: Mehmet Emin Yıldız") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("student_name_input")
          )
        }

        // Sınıf Seçim Alanı (Tıklanınca Açılır Pencere Açılır)
        item {
          Column {
            Text(
              text = "Sınıf / Düzey *",
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = TextSecondary,
              modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
            )
            Surface(
              onClick = { showGradePickerPopup = true },
              shape = RoundedCornerShape(12.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, FeatureStudentsBlue.copy(alpha = 0.5f)),
              color = FeatureStudentsBlue.copy(alpha = 0.05f),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("student_grade_picker_trigger")
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = FeatureStudentsBlue,
                    modifier = Modifier.size(22.dp)
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Column {
                    Text(
                      text = grade,
                      fontSize = 15.sp,
                      fontWeight = FontWeight.Bold,
                      color = TextPrimary
                    )
                    Text(
                      text = if (grade in STUDENT_GRADE_LEVELS.take(4)) "Ortaokul Kademesi" else "Lise Kademesi",
                      fontSize = 11.sp,
                      color = TextSecondary
                    )
                  }
                }

                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = FeatureStudentsBlue
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                  ) {
                    Text(
                      text = "Seç ▾",
                      fontSize = 12.sp,
                      color = Color.White,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }
              }
            }
          }
        }

        item {
          OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Öğrenci Telefonu") },
            placeholder = { Text("05xx xxx xx xx") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          )
        }

        item {
          OutlinedTextField(
            value = parentName,
            onValueChange = { parentName = it },
            label = { Text("Veli Adı Soyadı") },
            placeholder = { Text("Örn: Ahmet Yıldız (Babası)") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          )
        }

        item {
          OutlinedTextField(
            value = parentPhone,
            onValueChange = { parentPhone = it },
            label = { Text("Veli Telefonu / WhatsApp") },
            placeholder = { Text("05xx xxx xx xx") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          )
        }

        item {
          OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Hoca Notları / Açıklama") },
            placeholder = { Text("Öğrenci ile ilgili özel notlar...") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
          )
        }

        if (student != null) {
          item {
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(
              onClick = { showDeleteConfirm = true },
              colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusAbsentRed),
              border = androidx.compose.foundation.BorderStroke(1.dp, StatusAbsentRed.copy(alpha = 0.5f)),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Öğrenciyi Kayıtlardan Sil", fontWeight = FontWeight.SemiBold)
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (fullName.isNotBlank()) {
            onSave(
              Student(
                id = student?.id ?: 0L,
                fullName = fullName.trim(),
                grade = grade,
                phone = phone.trim(),
                parentName = parentName.trim(),
                parentPhone = parentPhone.trim(),
                enrollmentDate = enrollmentDate,
                status = "Aktif",
                notes = notes.trim(),
                avatarColorIndex = assignedAvatarColorIndex
              )
            )
          }
        },
        enabled = fullName.isNotBlank(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = FeatureStudentsBlue),
        modifier = Modifier.testTag("student_save_button")
      ) {
        Text("Kaydet", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("İptal", color = TextSecondary)
      }
    }
  )
}

/**
 * Kayıt ve düzenleme esnasında sınıf seçimi için açılır pencere (5. Sınıftan 12. Sınıfa kadar)
 */
@Composable
private fun StudentGradePickerDialog(
  currentGrade: String,
  onDismiss: () -> Unit,
  onGradeSelected: (String) -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.School,
          contentDescription = null,
          tint = FeatureStudentsBlue,
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text("Sınıf Düzeyi Seçin", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
          Text("5. Sınıftan 12. Sınıfa kadar (Ortaokul & Lise)", fontSize = 12.sp, color = TextSecondary)
        }
      }
    },
    text = {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        // Ortaokul Bölümü
        item {
          Surface(
            color = FeatureStudentsBlue.copy(alpha = 0.08f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "🏫 ORTAOKUL (5, 6, 7 ve 8. Sınıflar)",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = FeatureStudentsBlue,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
          }
        }

        items(STUDENT_GRADE_LEVELS.take(4)) { gradeLevel ->
          val isSelected = currentGrade.equals(gradeLevel, ignoreCase = true)
          GradeSelectionItemCard(
            gradeName = gradeLevel,
            category = "Ortaokul",
            isSelected = isSelected,
            onClick = { onGradeSelected(gradeLevel) }
          )
        }

        // Lise Bölümü
        item {
          Spacer(modifier = Modifier.height(6.dp))
          Surface(
            color = FeatureMemorizationPurple.copy(alpha = 0.08f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "🎓 LİSE (9, 10, 11 ve 12. Sınıflar)",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = FeatureMemorizationPurple,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
          }
        }

        items(STUDENT_GRADE_LEVELS.drop(4)) { gradeLevel ->
          val isSelected = currentGrade.equals(gradeLevel, ignoreCase = true) ||
              (gradeLevel == "12. Sınıf" && currentGrade.startsWith("12"))
          GradeSelectionItemCard(
            gradeName = if (gradeLevel == "12. Sınıf") "12. Sınıf (Lise Son)" else gradeLevel,
            category = "Lise",
            isSelected = isSelected,
            onClick = { onGradeSelected(gradeLevel) }
          )
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Kapat", fontWeight = FontWeight.Bold, color = FeatureStudentsBlue)
      }
    },
    shape = RoundedCornerShape(20.dp),
    containerColor = CanvasSurface
  )
}

@Composable
private fun GradeSelectionItemCard(
  gradeName: String,
  category: String,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(12.dp),
    color = if (isSelected) FeatureStudentsBlue.copy(alpha = 0.12f) else CanvasSurface,
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      if (isSelected) FeatureStudentsBlue else BorderLight
    ),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 11.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(
          selected = isSelected,
          onClick = onClick,
          colors = RadioButtonDefaults.colors(selectedColor = FeatureStudentsBlue)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
          Text(
            text = gradeName,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) FeatureStudentsBlue else TextPrimary
          )
          Text(
            text = category,
            fontSize = 11.sp,
            color = TextSecondary
          )
        }
      }

      if (isSelected) {
        Icon(
          imageVector = Icons.Default.CheckCircle,
          contentDescription = null,
          tint = FeatureStudentsBlue,
          modifier = Modifier.size(20.dp)
        )
      }
    }
  }
}

/**
 * Yalnızca Öğrenci Adı Soyadı, Sınıfı, Veli Adı Soyadı ve İletişim Numaraları içeren kart
 */
@Composable
private fun StudentContactRowItem(
  student: Student,
  index: Int,
  onEditClick: () -> Unit
) {
  val context = LocalContext.current
  val studentGrade = if (student.grade.isNotBlank()) student.grade else "5. Sınıf"

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = CanvasSurface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("student_contact_item_${student.id}")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp)
    ) {
      // Header row: Index, Student Name, Grade Badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Surface(
            shape = CircleShape,
            color = FeatureStudentsBlue.copy(alpha = 0.12f),
            modifier = Modifier.size(28.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(
                text = "${index + 1}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = FeatureStudentsBlue
              )
            }
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = student.fullName,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = FeatureStudentsBlue.copy(alpha = 0.1f),
          border = androidx.compose.foundation.BorderStroke(0.5.dp, FeatureStudentsBlue.copy(alpha = 0.3f))
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Icon(
              imageVector = Icons.Default.School,
              contentDescription = null,
              tint = FeatureStudentsBlue,
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = studentGrade,
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              color = FeatureStudentsBlue
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      HorizontalDivider(color = BorderLight.copy(alpha = 0.7f), thickness = 0.8.dp)
      Spacer(modifier = Modifier.height(10.dp))

      // Veli Bilgisi
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Icon(
          imageVector = Icons.Default.SupervisorAccount,
          contentDescription = "Veli",
          tint = DeepBlueNavy,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Veli: ",
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          color = TextSecondary
        )
        Text(
          text = if (student.parentName.isNotBlank()) student.parentName else "Belirtilmemiş",
          fontSize = 13.sp,
          fontWeight = FontWeight.Medium,
          color = TextPrimary
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      // İletişim Numaraları Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Veli Telefonu
        if (student.parentPhone.isNotBlank()) {
          Surface(
            onClick = {
              val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${student.parentPhone.replace(" ", "")}"))
              context.startActivity(intent)
            },
            shape = RoundedCornerShape(10.dp),
            color = StatusPresentGreen.copy(alpha = 0.08f),
            border = androidx.compose.foundation.BorderStroke(1.dp, StatusPresentGreen.copy(alpha = 0.25f)),
            modifier = Modifier.weight(1f)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = "Veli Ara",
                tint = StatusPresentGreen,
                modifier = Modifier.size(15.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Column {
                Text("Veli Tel (Arama Yap)", fontSize = 10.sp, color = TextSecondary)
                Text(
                  text = student.parentPhone,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = StatusPresentGreen
                )
              }
            }
          }
        }

        // Öğrenci Telefonu
        if (student.phone.isNotBlank()) {
          Surface(
            onClick = {
              val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${student.phone.replace(" ", "")}"))
              context.startActivity(intent)
            },
            shape = RoundedCornerShape(10.dp),
            color = CanvasBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
            modifier = Modifier.weight(1f)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.PhoneAndroid,
                contentDescription = "Öğrenci Ara",
                tint = TextSecondary,
                modifier = Modifier.size(15.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Column {
                Text("Öğrenci Tel", fontSize = 10.sp, color = TextSecondary)
                Text(
                  text = student.phone,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = TextPrimary
                )
              }
            }
          }
        }

        // Eğer telefon yoksa
        if (student.parentPhone.isBlank() && student.phone.isBlank()) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = CanvasBackground,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "İletişim numarası kaydedilmemiş",
              fontSize = 12.sp,
              color = TextMuted,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
          }
        }
      }
    }
  }
}

/**
 * PDF Olarak Dışa Aktarma Seçenekleri Penceresi
 */
@Composable
private fun PdfExportOptionsDialog(
  allStudents: List<Student>,
  filteredStudents: List<Student>,
  currentGradeFilter: String,
  onDismiss: () -> Unit,
  onExport: (List<Student>, String) -> Unit
) {
  var exportAll by remember { mutableStateOf(currentGradeFilter == "Tüm Sınıflar") }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Surface(
        shape = CircleShape,
        color = FeatureStudentsBlue.copy(alpha = 0.12f),
        modifier = Modifier.size(48.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Default.PictureAsPdf,
            contentDescription = null,
            tint = FeatureStudentsBlue,
            modifier = Modifier.size(26.dp)
          )
        }
      }
    },
    title = {
      Text(
        text = "İletişim Listesini PDF Aktar",
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        color = TextPrimary,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "Öğrenci adı, sınıfı, veli adı ve iletişim telefonlarını içeren A4 formatında liste PDF olarak hazırlanacaktır.",
          fontSize = 13.sp,
          color = TextSecondary,
          lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Option 1: Current filter (if specific grade filtered)
        if (currentGradeFilter != "Tüm Sınıflar") {
          Surface(
            onClick = { exportAll = false },
            shape = RoundedCornerShape(12.dp),
            color = if (!exportAll) FeatureStudentsBlue.copy(alpha = 0.12f) else CanvasSurface,
            border = androidx.compose.foundation.BorderStroke(
              1.dp,
              if (!exportAll) FeatureStudentsBlue else BorderLight
            ),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              RadioButton(
                selected = !exportAll,
                onClick = { exportAll = false },
                colors = RadioButtonDefaults.colors(selectedColor = FeatureStudentsBlue)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = "$currentGradeFilter Listesi",
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp,
                  color = if (!exportAll) FeatureStudentsBlue else TextPrimary
                )
                Text(
                  text = "${filteredStudents.size} Talebe Aktarılacak",
                  fontSize = 12.sp,
                  color = TextSecondary
                )
              }
            }
          }
        }

        // Option 2: All students
        Surface(
          onClick = { exportAll = true },
          shape = RoundedCornerShape(12.dp),
          color = if (exportAll) FeatureStudentsBlue.copy(alpha = 0.12f) else CanvasSurface,
          border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (exportAll) FeatureStudentsBlue else BorderLight
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
          ) {
            RadioButton(
              selected = exportAll,
              onClick = { exportAll = true },
              colors = RadioButtonDefaults.colors(selectedColor = FeatureStudentsBlue)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "Tüm Okul / Tüm Sınıflar",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (exportAll) FeatureStudentsBlue else TextPrimary
              )
              Text(
                text = "Toplam ${allStudents.size} Talebe Aktarılacak",
                fontSize = 12.sp,
                color = TextSecondary
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (exportAll) {
            onExport(allStudents, "Tüm Sınıflar")
          } else {
            onExport(filteredStudents, currentGradeFilter)
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = FeatureStudentsBlue),
        shape = RoundedCornerShape(10.dp)
      ) {
        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("PDF Oluştur & Paylaş", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("İptal", color = TextSecondary)
      }
    },
    shape = RoundedCornerShape(20.dp),
    containerColor = CanvasSurface
  )
}

/**
 * Toplu ve Otomatik Talebe Ekleme Penceresi (Desteklenen Excel / CSV Şablonu İndirme ve Dosya Yükleme)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BulkStudentImportDialog(
  onDismiss: () -> Unit,
  onImport: (List<Student>, Boolean) -> Unit
) {
  val context = LocalContext.current
  var loadedFileName by remember { mutableStateOf<String?>(null) }
  var parsedStudents by remember { mutableStateOf<List<Student>>(emptyList()) }
  var overwriteMode by remember { mutableStateOf(false) }

  // File Picker Launcher for CSV / TXT / Excel files
  val filePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    if (uri != null) {
      try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
        val stringBuilder = StringBuilder()
        var line: String? = reader.readLine()
        while (line != null) {
          stringBuilder.append(line).append("\n")
          line = reader.readLine()
        }
        reader.close()
        inputStream?.close()

        val fullText = stringBuilder.toString()
        val parsed = parsePastedStudentText(fullText)
        parsedStudents = parsed
        loadedFileName = uri.lastPathSegment?.substringAfterLast("/") ?: "Secilen_Dosya.csv"

        if (parsed.isNotEmpty()) {
          Toast.makeText(context, "✓ ${parsed.size} talebe başarıyla dosyadan okundu!", Toast.LENGTH_SHORT).show()
        } else {
          Toast.makeText(context, "Dosyada geçerli talebe satırı bulunamadı. Lütfen şablon formatına uygunluğunu kontrol edin.", Toast.LENGTH_LONG).show()
        }
      } catch (e: Exception) {
        Toast.makeText(context, "Dosya okunamadı: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
      }
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Surface(
        shape = CircleShape,
        color = FeatureStudentsBlue.copy(alpha = 0.12f),
        modifier = Modifier.size(48.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Default.GroupAdd,
            contentDescription = null,
            tint = FeatureStudentsBlue,
            modifier = Modifier.size(26.dp)
          )
        }
      }
    },
    title = {
      Text(
        text = "Otomatik / Toplu Talebe Ekle",
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        color = TextPrimary,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 490.dp)
      ) {
        LazyColumn(
          modifier = Modifier.weight(1f, fill = false),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Format Explanation Card
          item {
            Surface(
              color = Color(0xFFEFF6FF),
              shape = RoundedCornerShape(12.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = FeatureStudentsBlue,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "Desteklenen Excel Formatı (.xls / .csv)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = FeatureStudentsBlue
                  )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Excel dosyanızda şu sütunlar bulunmalıdır:\nAd Soyad | Sınıf | Telefon | Veli Adı | Veli Telefon | Notlar",
                  fontSize = 10.5.sp,
                  color = TextSecondary,
                  lineHeight = 15.sp
                )
              }
            }
          }

          // 1. Download Blank Excel Template Option
          item {
            Surface(
              color = FeatureStudentsBlue.copy(alpha = 0.06f),
              shape = RoundedCornerShape(12.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, FeatureStudentsBlue.copy(alpha = 0.25f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Surface(
                    color = FeatureStudentsBlue,
                    shape = CircleShape,
                    modifier = Modifier.size(24.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Text("1", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "Örnek Boş Excel Şablonu (.xls)",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = FeatureStudentsBlue
                  )
                }
                Text(
                  text = "Microsoft Excel (.xls) uyumlu boş şablon dosyasını indirin, Excel ile açıp talebelerinizi yazıp kaydedin.",
                  fontSize = 10.5.sp,
                  color = TextSecondary,
                  lineHeight = 14.sp
                )
                Button(
                  onClick = { downloadBlankExcelTemplate(context) },
                  colors = ButtonDefaults.buttonColors(containerColor = FeatureStudentsBlue),
                  shape = RoundedCornerShape(10.dp),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .testTag("download_excel_template_button")
                ) {
                  Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Boş Excel Şablonunu İndir (.xls)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }

          // 2. Upload / Choose Excel File Option
          item {
            Surface(
              color = FeatureAttendanceGreen.copy(alpha = 0.06f),
              shape = RoundedCornerShape(12.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, FeatureAttendanceGreen.copy(alpha = 0.25f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Surface(
                    color = FeatureAttendanceGreen,
                    shape = CircleShape,
                    modifier = Modifier.size(24.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Text("2", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "Excel / CSV Dosyasını Yükle",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = FeatureAttendanceGreen
                  )
                }
                Text(
                  text = "Doldurduğunuz Excel (.xls / .xlsx) veya CSV dosyasını seçerek talebeleri sisteme tek tıkla aktarın.",
                  fontSize = 10.5.sp,
                  color = TextSecondary,
                  lineHeight = 14.sp
                )
                OutlinedButton(
                  onClick = { filePickerLauncher.launch("*/*") },
                  colors = ButtonDefaults.outlinedButtonColors(contentColor = FeatureAttendanceGreen),
                  border = androidx.compose.foundation.BorderStroke(1.dp, FeatureAttendanceGreen),
                  shape = RoundedCornerShape(10.dp),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .testTag("choose_excel_file_button")
                ) {
                  Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = if (parsedStudents.isNotEmpty()) "📁 Farklı Dosya Seç (${parsedStudents.size} Talebe Yüklü)" else "📁 Excel / CSV Dosyası Seç",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }

          // Parsed Status Indicator & Preview
          if (parsedStudents.isNotEmpty()) {
            item {
              Surface(
                color = FeatureAttendanceGreen.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = FeatureAttendanceGreen,
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "✓ ${parsedStudents.size} Talebe Başarıyla Algılandı",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = FeatureAttendanceGreen
                  )
                }
              }
            }

            items(parsedStudents.take(5)) { st ->
              Surface(
                color = CanvasSurface,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(8.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  StudentAvatar(
                    name = st.fullName,
                    colorIndex = st.avatarColorIndex,
                    size = 28.dp,
                    fontSize = 11
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(st.fullName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("${st.grade} • Veli: ${st.parentName.ifBlank { "—" }} (${st.parentPhone.ifBlank { "—" }})", fontSize = 10.sp, color = TextSecondary)
                  }
                }
              }
            }

            if (parsedStudents.size > 5) {
              item {
                Text(
                  text = "... ve ${parsedStudents.size - 5} talebe daha",
                  fontSize = 11.sp,
                  color = TextMuted,
                  modifier = Modifier.padding(start = 6.dp)
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Mode switch: Merge vs Overwrite (Mevcut Talebeleri Sıfırla)
        Surface(
          color = CanvasBackground,
          shape = RoundedCornerShape(10.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { overwriteMode = !overwriteMode }
              .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Checkbox(
              checked = overwriteMode,
              onCheckedChange = { overwriteMode = it },
              colors = CheckboxDefaults.colors(checkedColor = StatusAbsentRed)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
              Text(
                text = "Mevcut Talebeleri Sıfırla (Temiz Kurulum)",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (overwriteMode) StatusAbsentRed else TextPrimary
              )
              Text(
                text = if (overwriteMode) "Mevcut tüm talebeler silinir ve sadece yeni yüklenen liste kaydedilir." else "Yeni talebeler mevcut sınıf listesinin üzerine eklenir.",
                fontSize = 10.sp,
                color = TextSecondary
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (parsedStudents.isNotEmpty()) {
            onImport(parsedStudents, overwriteMode)
          }
        },
        enabled = parsedStudents.isNotEmpty(),
        colors = ButtonDefaults.buttonColors(containerColor = FeatureStudentsBlue),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.testTag("confirm_bulk_import_button")
      ) {
        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = if (parsedStudents.isNotEmpty()) "Kaydet (${parsedStudents.size} Talebe)" else "Talebeleri Kaydet",
          fontWeight = FontWeight.Bold
        )
      }
    },
    shape = RoundedCornerShape(20.dp),
    containerColor = CanvasSurface
  )
}

/**
 * Downloads / shares a sample blank Excel template with .xls extension for Microsoft Excel & Google Sheets
 */
private fun downloadBlankExcelTemplate(context: Context) {
  try {
    val templateContent = buildString {
      append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
      append("<?mso-application progid=\"Excel.Sheet\"?>\n")
      append("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"\n")
      append(" xmlns:o=\"urn:schemas-microsoft-com:office:office\"\n")
      append(" xmlns:x=\"urn:schemas-microsoft-com:office:excel\"\n")
      append(" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\"\n")
      append(" xmlns:html=\"http://www.w3.org/TR/REC-html40\">\n")
      append(" <DocumentProperties xmlns=\"urn:schemas-microsoft-com:office:office\">\n")
      append("  <Author>Mekteb-i İrfan</Author>\n")
      append("  <Title>Örnek Talebe Listesi Şablonu</Title>\n")
      append(" </DocumentProperties>\n")
      append(" <Styles>\n")
      append("  <Style ss:ID=\"Default\" ss:Name=\"Normal\">\n")
      append("   <Alignment ss:Vertical=\"Center\"/>\n")
      append("   <Font ss:FontName=\"Calibri\" ss:Size=\"11\" ss:Color=\"#000000\"/>\n")
      append("  </Style>\n")
      append("  <Style ss:ID=\"HeaderStyle\">\n")
      append("   <Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\"/>\n")
      append("   <Borders>\n")
      append("    <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#CCCCCC\"/>\n")
      append("    <Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#CCCCCC\"/>\n")
      append("    <Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#CCCCCC\"/>\n")
      append("    <Border ss:Position=\"Top\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#CCCCCC\"/>\n")
      append("   </Borders>\n")
      append("   <Font ss:FontName=\"Calibri\" ss:Size=\"11\" ss:Color=\"#FFFFFF\" ss:Bold=\"1\"/>\n")
      append("   <Interior ss:Color=\"#1B365D\" ss:Pattern=\"Solid\"/>\n")
      append("  </Style>\n")
      append("  <Style ss:ID=\"DataStyle\">\n")
      append("   <Alignment ss:Vertical=\"Center\"/>\n")
      append("   <Borders>\n")
      append("    <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#E2E8F0\"/>\n")
      append("    <Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#E2E8F0\"/>\n")
      append("    <Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#E2E8F0\"/>\n")
      append("    <Border ss:Position=\"Top\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#E2E8F0\"/>\n")
      append("   </Borders>\n")
      append("   <Font ss:FontName=\"Calibri\" ss:Size=\"10\" ss:Color=\"#1E293B\"/>\n")
      append("  </Style>\n")
      append(" </Styles>\n")
      append(" <Worksheet ss:Name=\"Talebe Listesi\">\n")
      append("  <Table>\n")
      append("   <Column ss:Width=\"160\"/>\n")
      append("   <Column ss:Width=\"90\"/>\n")
      append("   <Column ss:Width=\"120\"/>\n")
      append("   <Column ss:Width=\"140\"/>\n")
      append("   <Column ss:Width=\"120\"/>\n")
      append("   <Column ss:Width=\"180\"/>\n")
      append("   <Row ss:Height=\"24\" ss:StyleID=\"HeaderStyle\">\n")
      append("    <Cell><Data ss:Type=\"String\">Ad Soyad</Data></Cell>\n")
      append("    <Cell><Data ss:Type=\"String\">Sınıf</Data></Cell>\n")
      append("    <Cell><Data ss:Type=\"String\">Telefon</Data></Cell>\n")
      append("    <Cell><Data ss:Type=\"String\">Veli Adı</Data></Cell>\n")
      append("    <Cell><Data ss:Type=\"String\">Veli Tel</Data></Cell>\n")
      append("    <Cell><Data ss:Type=\"String\">Notlar</Data></Cell>\n")
      append("   </Row>\n")
      append("   <Row ss:Height=\"20\" ss:StyleID=\"DataStyle\">\n")
      append("    <Cell><Data ss:Type=\"String\">Ahmet Yılmaz</Data></Cell>\n")
      append("    <Cell><Data ss:Type=\"String\">5. Sınıf</Data></Cell>\n")
      append("    <Cell><Data ss:Type=\"String\">0532 100 2030</Data></Cell>\n")
      append("    <Cell><Data ss:Type=\"String\">Mehmet Yılmaz</Data></Cell>\n")
      append("    <Cell><Data ss:Type=\"String\">0533 200 3040</Data></Cell>\n")
      append("    <Cell><Data ss:Type=\"String\">Hafızlık Başlangıç</Data></Cell>\n")
      append("   </Row>\n")
      append("   <Row ss:Height=\"20\" ss:StyleID=\"DataStyle\">\n")
      append("    <Cell><Data ss:Type=\"String\">Ömer Faruk Demir</Data></Cell>\n")
      append("    <Cell><Data ss:Type=\"String\">6. Sınıf</Data></Cell>\n")
      append("    <Cell><Data ss:Type=\"String\">0542 200 3040</Data></Cell>\n")
      append("    <Cell><Data ss:Type=\"String\">Mustafa Demir</Data></Cell>\n")
      append("    <Cell><Data ss:Type=\"String\">0543 300 4050</Data></Cell>\n")
      append("    <Cell><Data ss:Type=\"String\">Tecvid Talebesi</Data></Cell>\n")
      append("   </Row>\n")
      append("   <Row ss:Height=\"20\" ss:StyleID=\"DataStyle\">\n")
      append("    <Cell><Data ss:Type=\"String\">Yusuf Emre Kaya</Data></Cell>\n")
      append("    <Cell><Data ss:Type=\"String\">7. Sınıf</Data></Cell>\n")
      append("    <Cell><Data ss:Type=\"String\">0552 300 4050</Data></Cell>\n")
      append("    <Cell><Data ss:Type=\"String\">İbrahim Kaya</Data></Cell>\n")
      append("    <Cell><Data ss:Type=\"String\">0553 400 5060</Data></Cell>\n")
      append("    <Cell><Data ss:Type=\"String\">30. Cüz Ezberinde</Data></Cell>\n")
      append("   </Row>\n")
      append("  </Table>\n")
      append(" </Worksheet>\n")
      append("</Workbook>")
    }

    val exportDir = File(context.cacheDir, "exports")
    if (!exportDir.exists()) {
      exportDir.mkdirs()
    }
    val fileName = "Ornek_Talebe_Sablonu.xls"
    val file = File(exportDir, fileName)
    FileOutputStream(file).use { fos ->
      fos.write(templateContent.toByteArray(Charsets.UTF_8))
      fos.flush()
    }

    val fileUri: Uri = FileProvider.getUriForFile(
      context,
      "${context.packageName}.fileprovider",
      file
    )

    val intent = Intent(Intent.ACTION_SEND).apply {
      type = "application/vnd.ms-excel"
      putExtra(Intent.EXTRA_STREAM, fileUri)
      putExtra(Intent.EXTRA_SUBJECT, "Mekteb-i İrfan - Örnek Talebe Excel Şablonu (.xls)")
      putExtra(Intent.EXTRA_TEXT, "Mekteb-i İrfan Microsoft Excel (.xls) desteklenen boş talebe listesi şablonu. Bu dosyayı Excel ile açıp talebelerinizi doldurabilirsiniz.")
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      clipData = ClipData.newRawUri(fileName, fileUri)
    }

    val chooser = Intent.createChooser(intent, "Excel Şablonunu İndir / Paylaş (.xls)").apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(chooser)
    Toast.makeText(context, "Örnek Excel şablonu (.xls) hazırlandı!", Toast.LENGTH_SHORT).show()
  } catch (e: Exception) {
    Toast.makeText(context, "Şablon oluşturulamadı: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
  }
}

/**
 * Parses user text or loaded file from Excel / CSV / Plain Lines into Student list
 */
private fun parsePastedStudentText(rawText: String): List<Student> {
  if (rawText.isBlank()) return emptyList()

  val cleanText = rawText.removePrefix("\uFEFF").trim()
  val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

  // Support for Microsoft Excel XML Spreadsheet (.xls) format
  if (cleanText.contains("<Row", ignoreCase = true) || cleanText.contains("<ss:Row", ignoreCase = true)) {
    val rowRegex = "<(?:ss:)?Row[^>]*>(.*?)</(?:ss:)?Row>".toRegex(RegexOption.DOT_MATCHES_ALL)
    val dataRegex = "<(?:ss:)?Data[^>]*>(.*?)</(?:ss:)?Data>".toRegex(RegexOption.DOT_MATCHES_ALL)
    val xmlList = mutableListOf<Student>()

    for (rowMatch in rowRegex.findAll(cleanText)) {
      val rowXml = rowMatch.groupValues[1]
      val cells = dataRegex.findAll(rowXml).map { it.groupValues[1].trim() }.toList()
      if (cells.isEmpty()) continue

      val first = cells[0].lowercase(Locale("tr"))
      if (first.contains("ad") || first.contains("soyad") || first.contains("isim") || first.contains("talebe") || first.contains("öğrenci")) {
        continue
      }

      val fullName = cells.getOrNull(0)?.trim() ?: ""
      if (fullName.isBlank()) continue

      val rawGrade = cells.getOrNull(1)?.trim().takeIf { !it.isNullOrBlank() } ?: "5. Sınıf"
      val grade = when {
        rawGrade.contains("5") -> "5. Sınıf"
        rawGrade.contains("6") -> "6. Sınıf"
        rawGrade.contains("7") -> "7. Sınıf"
        rawGrade.contains("8") -> "8. Sınıf"
        rawGrade.contains("9") -> "9. Sınıf"
        rawGrade.contains("10") -> "10. Sınıf"
        rawGrade.contains("11") -> "11. Sınıf"
        rawGrade.contains("12") -> "12. Sınıf"
        else -> rawGrade
      }

      val phone = cells.getOrNull(2)?.trim() ?: ""
      val parentName = cells.getOrNull(3)?.trim() ?: ""
      val parentPhone = cells.getOrNull(4)?.trim() ?: ""
      val notes = cells.getOrNull(5)?.trim() ?: ""

      xmlList.add(
        Student(
          id = 0L,
          fullName = fullName,
          grade = grade,
          phone = phone,
          parentName = parentName,
          parentPhone = parentPhone,
          enrollmentDate = todayStr,
          status = "Aktif",
          notes = notes,
          avatarColorIndex = xmlList.size % 8
        )
      )
    }

    if (xmlList.isNotEmpty()) {
      return xmlList
    }
  }

  val lines = cleanText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
  if (lines.isEmpty()) return emptyList()

  val list = mutableListOf<Student>()

  for (line in lines) {
    if (line.startsWith("---") || line.startsWith("###")) continue

    // Determine delimiter for this line (Tab, semicolon, comma, pipe)
    val delimiter = when {
      line.contains('\t') -> '\t'
      line.contains(';') -> ';'
      line.contains('|') -> '|'
      line.contains(',') -> ','
      else -> null
    }

    val tokens: List<String> = if (delimiter != null) {
      line.split(delimiter).map { it.trim().trim('\"') }
    } else {
      listOf(line.trim())
    }

    if (tokens.isEmpty()) continue

    // Skip header line if detected
    val first = tokens[0].lowercase(Locale("tr"))
    if (first.contains("ad") || first.contains("soyad") || first.contains("isim") || first.contains("talebe") || first.contains("öğrenci")) {
      continue
    }

    var id = 0L
    var nameIdx = 0
    if (tokens[0].toLongOrNull() != null && tokens.size > 1) {
      id = tokens[0].toLong()
      nameIdx = 1
    }

    if (nameIdx >= tokens.size) continue
    val fullName = tokens.getOrNull(nameIdx)?.trim() ?: ""
    if (fullName.isBlank()) continue

    val rawGrade = tokens.getOrNull(nameIdx + 1)?.trim().takeIf { !it.isNullOrBlank() } ?: "5. Sınıf"
    val grade = when {
      rawGrade.contains("5") -> "5. Sınıf"
      rawGrade.contains("6") -> "6. Sınıf"
      rawGrade.contains("7") -> "7. Sınıf"
      rawGrade.contains("8") -> "8. Sınıf"
      rawGrade.contains("9") -> "9. Sınıf"
      rawGrade.contains("10") -> "10. Sınıf"
      rawGrade.contains("11") -> "11. Sınıf"
      rawGrade.contains("12") -> "12. Sınıf"
      else -> rawGrade
    }

    val phone = tokens.getOrNull(nameIdx + 2)?.trim() ?: ""
    val parentName = tokens.getOrNull(nameIdx + 3)?.trim() ?: ""
    val parentPhone = tokens.getOrNull(nameIdx + 4)?.trim() ?: ""
    val notes = tokens.getOrNull(nameIdx + 5)?.trim() ?: ""

    list.add(
      Student(
        id = id,
        fullName = fullName,
        grade = grade,
        phone = phone,
        parentName = parentName,
        parentPhone = parentPhone,
        enrollmentDate = todayStr,
        status = "Aktif",
        notes = notes,
        avatarColorIndex = list.size % 8
      )
    )
  }

  return list
}

