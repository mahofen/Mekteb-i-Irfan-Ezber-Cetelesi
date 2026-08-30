package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.data.model.MemorizationRecord
import com.example.data.model.Student
import com.example.data.util.CustomCurriculumItem
import com.example.ui.components.CurriculumImportDialog
import com.example.ui.components.CurriculumSelectionDialog
import com.example.ui.components.GoldSubtleBorderGradient
import com.example.ui.components.StudentAvatar
import com.example.ui.components.StudentBottomSheetPicker
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModel
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemorizationScreen(
  students: List<Student>,
  memorizationList: List<MemorizationRecord>,
  todayDate: String,
  onBackClick: () -> Unit,
  onSaveMemorization: (MemorizationRecord) -> Unit,
  onDeleteMemorization: (MemorizationRecord) -> Unit,
  viewModel: AppViewModel? = null
) {
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current

  // Selected Student State
  var selectedStudentId by remember {
    mutableStateOf(students.firstOrNull()?.id ?: 0L)
  }

  // Ensure valid student selected
  LaunchedEffect(students) {
    if (students.isNotEmpty() && (selectedStudentId == 0L || students.none { it.id == selectedStudentId })) {
      selectedStudentId = students.first().id
    }
  }

  val activeStudent = remember(students, selectedStudentId) {
    students.find { it.id == selectedStudentId } ?: students.firstOrNull()
  }

  val currentStudentIndex = remember(students, selectedStudentId) {
    students.indexOfFirst { it.id == selectedStudentId }
  }

  // Dynamic Curriculum list from ViewModel
  val customCurriculumList by (viewModel?.curriculumItems?.collectAsState()
    ?: remember { mutableStateOf(emptyList<CustomCurriculumItem>()) })

  // Active items (isSelected == true)
  val activeCurriculumItems = remember(customCurriculumList) {
    customCurriculumList.filter { it.isSelected }.sortedBy { it.orderIndex }
  }

  // Dynamic categories
  val activeCategories = remember(activeCurriculumItems) {
    val cats = activeCurriculumItems.map { it.category.trim() }.distinct()
    if (cats.isEmpty()) listOf("Kur'an-ı Kerim", "Namaz Tesbihatı", "Risale-i Nur", "Hadis-i Şerif")
    else cats
  }

  var selectedCategoryIndex by remember { mutableIntStateOf(0) }
  val activeCategoryName = remember(activeCategories, selectedCategoryIndex) {
    if (selectedCategoryIndex in activeCategories.indices) activeCategories[selectedCategoryIndex]
    else activeCategories.firstOrNull() ?: "Genel"
  }

  // Filters & Dialog states
  var selectedStatusFilter by remember { mutableStateOf("Tümü") } // "Tümü", "Sırada", "Tekrar", "Tamamlananlar"
  var searchQuery by remember { mutableStateOf("") }
  var isMultiSelectMode by remember { mutableStateOf(false) }
  var selectedTitlesForBulk by remember { mutableStateOf(setOf<String>()) }
  var showStudentDropdownDialog by remember { mutableStateOf(false) }
  var showCurriculumSelectionDialog by remember { mutableStateOf(false) }
  var showCurriculumImportDialog by remember { mutableStateOf(false) }
  var showOverallProgressDialog by remember { mutableStateOf(false) }
  var showAssignNewTargetDialog by remember { mutableStateOf(false) }
  var showItemDetailDialog by remember { mutableStateOf<MemorizationItemDetailState?>(null) }

  // Student's memorization records
  val studentRecords = remember(memorizationList, selectedStudentId) {
    memorizationList.filter { it.studentId == selectedStudentId }
  }

  // Helper matching functions
  fun getItemRecord(title: String, cat: String): MemorizationRecord? {
    return studentRecords.find { record ->
      record.title.equals(title.trim(), ignoreCase = true) &&
          (record.category.equals(cat.trim(), ignoreCase = true) ||
              normalizeCategoryKey(record.category) == normalizeCategoryKey(cat))
    }
  }

  fun isItemCompleted(title: String, cat: String): Boolean {
    val rec = getItemRecord(title, cat)
    return rec != null && (rec.status == "TAMAMLANDI" || rec.progressPercent >= 100)
  }

  val currentCategoryItems = remember(activeCurriculumItems, activeCategoryName, studentRecords) {
    val fromCurriculum = activeCurriculumItems.filter { it.category.equals(activeCategoryName, ignoreCase = true) }
    // Also include custom memorizations added by teacher for this category not in curriculum
    val extraStudentRecords = studentRecords.filter { record ->
      record.category.equals(activeCategoryName, ignoreCase = true) &&
          fromCurriculum.none { it.title.equals(record.title, ignoreCase = true) }
    }.map { rec ->
      CustomCurriculumItem(
        id = UUID.randomUUID().toString(),
        title = rec.title,
        category = rec.category,
        orderIndex = 999,
        isSelected = true
      )
    }
    fromCurriculum + extraStudentRecords
  }

  val categoryColor = getCategoryThemeColor(activeCategoryName)

  // Counts
  val totalActiveCurriculumCount = activeCurriculumItems.size
  val totalOverallCompletedCount = activeCurriculumItems.count { isItemCompleted(it.title, it.category) }
  val overallProgressPercent = if (totalActiveCurriculumCount > 0) {
    (totalOverallCompletedCount * 100) / totalActiveCurriculumCount
  } else 0

  // Filtered items
  val filteredCategoryItems = currentCategoryItems.mapIndexed { index, item ->
    val completed = isItemCompleted(item.title, activeCategoryName)
    val record = getItemRecord(item.title, activeCategoryName)
    CurriculumItemUiModel(
      index = index + 1,
      title = item.title,
      category = activeCategoryName,
      isCompleted = completed,
      record = record,
      originalItem = item
    )
  }.filter { item ->
    val matchesSearch = searchQuery.isBlank() || item.title.contains(searchQuery, ignoreCase = true)
    val matchesStatus = when (selectedStatusFilter) {
      "Tamamlananlar" -> item.isCompleted
      "Sırada" -> !item.isCompleted && (item.record?.status != "TEKRAR")
      "Tekrar" -> item.record?.status == "TEKRAR"
      else -> true
    }
    matchesSearch && matchesStatus
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Ezber Yönetimi & Takip",
              fontWeight = FontWeight.Bold,
              fontSize = 19.sp,
              color = Color.White
            )
            Text(
              text = "$totalActiveCurriculumCount Aktif Ezber • %$overallProgressPercent Tamamlandı",
              fontSize = 11.5.sp,
              color = Color.White.copy(alpha = 0.85f)
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onBackClick,
            modifier = Modifier.testTag("memorization_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Geri",
              tint = Color.White
            )
          }
        },
        actions = {
          // Toplu Ezber Ata Modu
          IconButton(
            onClick = {
              isMultiSelectMode = !isMultiSelectMode
              if (!isMultiSelectMode) selectedTitlesForBulk = emptySet()
            },
            modifier = Modifier.testTag("memorization_bulk_assign_toggle_button")
          ) {
            Icon(
              imageVector = if (isMultiSelectMode) Icons.Default.Close else Icons.Default.PlaylistAddCheck,
              contentDescription = if (isMultiSelectMode) "Seçim Modunu Kapat" else "Toplu Ezber Ata",
              tint = if (isMultiSelectMode) StatusExcusedAmber else Color.White
            )
          }

          // Müfredatı Yönet Butonu
          IconButton(
            onClick = { showCurriculumSelectionDialog = true },
            modifier = Modifier.testTag("memorization_curriculum_manage_button")
          ) {
            Icon(
              imageVector = Icons.Default.ChecklistRtl,
              contentDescription = "Ezber Listesini Yönet",
              tint = Color.White
            )
          }

          // Yeni Liste Yükle
          IconButton(
            onClick = { showCurriculumImportDialog = true },
            modifier = Modifier.testTag("memorization_curriculum_import_button")
          ) {
            Icon(
              imageVector = Icons.Default.CloudUpload,
              contentDescription = "Yeni Ezber Listesi Yükle",
              tint = Color.White
            )
          }

          // Genel Gelişim Özeti
          IconButton(
            onClick = { showOverallProgressDialog = true },
            modifier = Modifier.testTag("overall_progress_button")
          ) {
            Icon(
              imageVector = Icons.Default.Analytics,
              contentDescription = "Genel Gelişim Özeti",
              tint = Color.White
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBlueNavy)
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showAssignNewTargetDialog = true },
        containerColor = FeatureAttendanceGreen,
        contentColor = Color.White,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.testTag("assign_new_memorization_fab")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.Add, contentDescription = "Yeni Ezber Ata")
          Spacer(modifier = Modifier.width(6.dp))
          Text("Ezber Ata", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
      }
    },
    containerColor = CanvasBackground
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // ----------------------------------------------------
      // 1. TALEBE SEÇİCİ DROPDOWN STRIP
      // ----------------------------------------------------
      if (students.isEmpty()) {
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = CanvasSurface),
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ) {
          Text(
            text = "Kayıtlı öğrenci bulunamadı. Lütfen önce Öğrenciler menüsünden öğrenci ekleyin.",
            color = TextSecondary,
            fontSize = 13.5.sp,
            modifier = Modifier.padding(16.dp)
          )
        }
      } else if (activeStudent != null) {
        Surface(
          onClick = { showStudentDropdownDialog = true },
          shape = RoundedCornerShape(14.dp),
          color = CanvasSurface,
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
          shadowElevation = 1.5.dp,
          modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 4.dp)
            .testTag("student_dropdown_trigger")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            IconButton(
              onClick = {
                if (currentStudentIndex > 0) {
                  selectedStudentId = students[currentStudentIndex - 1].id
                } else if (students.isNotEmpty()) {
                  selectedStudentId = students.last().id
                }
              },
              modifier = Modifier.size(30.dp)
            ) {
              Icon(Icons.Default.ChevronLeft, contentDescription = "Önceki Talebe", tint = FeatureStudentsBlue)
            }

            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp)
            ) {
              StudentAvatar(
                name = activeStudent.fullName,
                colorIndex = activeStudent.avatarColorIndex,
                size = 36.dp,
                fontSize = 13
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = activeStudent.fullName,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = FeatureStudentsBlue,
                    modifier = Modifier.size(18.dp)
                  )
                }
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Surface(
                    color = FeatureStudentsBlue.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(4.dp)
                  ) {
                    Text(
                      text = activeStudent.grade,
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      color = FeatureStudentsBlue,
                      modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                  }
                  Text(
                    text = "$totalOverallCompletedCount/$totalActiveCurriculumCount Ezber (%$overallProgressPercent)",
                    fontSize = 11.sp,
                    color = TextSecondary
                  )
                }
              }
            }

            IconButton(
              onClick = {
                if (currentStudentIndex >= 0 && currentStudentIndex < students.size - 1) {
                  selectedStudentId = students[currentStudentIndex + 1].id
                } else if (students.isNotEmpty()) {
                  selectedStudentId = students.first().id
                }
              },
              modifier = Modifier.size(30.dp)
            ) {
              Icon(Icons.Default.ChevronRight, contentDescription = "Sonraki Talebe", tint = FeatureStudentsBlue)
            }
          }
        }
      }

      // ----------------------------------------------------
      // 2. DİNAMİK KATEGORİ SEKMELERİ
      // ----------------------------------------------------
      if (activeCategories.size <= 3) {
        PrimaryTabRow(
          selectedTabIndex = selectedCategoryIndex,
          containerColor = CanvasSurface,
          contentColor = categoryColor,
          indicator = {
            TabRowDefaults.PrimaryIndicator(
              modifier = Modifier.tabIndicatorOffset(selectedCategoryIndex),
              color = categoryColor,
              height = 3.dp
            )
          },
          modifier = Modifier.fillMaxWidth()
        ) {
          activeCategories.forEachIndexed { index, catName ->
            val isSelected = selectedCategoryIndex == index
            val catIcon = getCategoryIcon(catName)
            val catCount = activeCurriculumItems.count { it.category.equals(catName, ignoreCase = true) }
            val catColor = getCategoryThemeColor(catName)

            Tab(
              selected = isSelected,
              onClick = { selectedCategoryIndex = index },
              text = {
                Text(
                  text = "$catIcon $catName ($catCount)",
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp,
                  color = if (isSelected) catColor else TextSecondary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              },
              modifier = Modifier.testTag("tab_cat_$index")
            )
          }
        }
      } else {
        ScrollableTabRow(
          selectedTabIndex = selectedCategoryIndex,
          containerColor = CanvasSurface,
          contentColor = categoryColor,
          edgePadding = 12.dp,
          indicator = { tabPositions ->
            if (selectedCategoryIndex < tabPositions.size) {
              TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedCategoryIndex]),
                color = categoryColor,
                height = 3.dp
              )
            }
          },
          modifier = Modifier.fillMaxWidth()
        ) {
          activeCategories.forEachIndexed { index, catName ->
            val isSelected = selectedCategoryIndex == index
            val catIcon = getCategoryIcon(catName)
            val catCount = activeCurriculumItems.count { it.category.equals(catName, ignoreCase = true) }
            val catColor = getCategoryThemeColor(catName)

            Tab(
              selected = isSelected,
              onClick = { selectedCategoryIndex = index },
              text = {
                Text(
                  text = "$catIcon $catName ($catCount)",
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp,
                  color = if (isSelected) catColor else TextSecondary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              },
              modifier = Modifier.testTag("tab_cat_$index")
            )
          }
        }
      }

      // ----------------------------------------------------
      // 3. ARAMA VE DURUM FİLTRELERİ
      // ----------------------------------------------------
      Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
        // Arama Çubuğu
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Ezber ara (Sure, Aşır, Hadis...)", fontSize = 12.sp) },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(17.dp)) },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(Icons.Default.Clear, contentDescription = "Temizle", modifier = Modifier.size(15.dp))
              }
            }
          },
          shape = RoundedCornerShape(10.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = CanvasSurface,
            unfocusedContainerColor = CanvasSurface
          ),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 4 Durum Hap Filtresi: Tümü, Sırada, Tekrar, Tamamlananlar
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          FilterStatusChip("Tümü", selectedStatusFilter == "Tümü", DeepBlueNavy, { selectedStatusFilter = "Tümü" }, Modifier.weight(1f))
          FilterStatusChip("Sırada", selectedStatusFilter == "Sırada", FeatureStudentsBlue, { selectedStatusFilter = "Sırada" }, Modifier.weight(1f))
          FilterStatusChip("Tekrar", selectedStatusFilter == "Tekrar", StatusExcusedAmber, { selectedStatusFilter = "Tekrar" }, Modifier.weight(1f))
          FilterStatusChip("Kabul", selectedStatusFilter == "Tamamlananlar", FeatureAttendanceGreen, { selectedStatusFilter = "Tamamlananlar" }, Modifier.weight(1f))
        }

        // TOPLU EZBER ATAMA ÇUBUĞU (BULK ASSIGN BANNER)
        if (isMultiSelectMode) {
          Spacer(modifier = Modifier.height(6.dp))
          Surface(
            color = DeepBlueNavy,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.Checklist, contentDescription = null, tint = StatusExcusedAmber, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "${selectedTitlesForBulk.size} Ezber Seçildi",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp
                  )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                  TextButton(
                    onClick = {
                      val allTitles = filteredCategoryItems.map { it.title }.toSet()
                      selectedTitlesForBulk = if (selectedTitlesForBulk.size == allTitles.size) emptySet() else allTitles
                    },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                  ) {
                    Text(
                      text = if (selectedTitlesForBulk.size == filteredCategoryItems.size && filteredCategoryItems.isNotEmpty()) "Temizle" else "Tümünü Seç",
                      color = StatusExcusedAmber,
                      fontSize = 11.5.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }

                  IconButton(
                    onClick = {
                      isMultiSelectMode = false
                      selectedTitlesForBulk = emptySet()
                    },
                    modifier = Modifier.size(26.dp)
                  ) {
                    Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                  }
                }
              }

              Spacer(modifier = Modifier.height(6.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Button(
                  onClick = {
                    if (activeStudent != null && selectedTitlesForBulk.isNotEmpty()) {
                      val itemsToAssign = filteredCategoryItems
                        .filter { selectedTitlesForBulk.contains(it.title) }
                        .map { it.title to it.category }

                      viewModel?.assignMemorizationsBulk(
                        studentIds = listOf(activeStudent.id),
                        items = itemsToAssign,
                        status = "VERILDI",
                        notes = "Hoca tarafından ezber hedefi olarak atandı"
                      )
                      Toast.makeText(context, "${selectedTitlesForBulk.size} ezber ${activeStudent.fullName} talebesine görev olarak atandı! 🎯", Toast.LENGTH_SHORT).show()
                      isMultiSelectMode = false
                      selectedTitlesForBulk = emptySet()
                    }
                  },
                  enabled = activeStudent != null && selectedTitlesForBulk.isNotEmpty(),
                  colors = ButtonDefaults.buttonColors(containerColor = FeatureAttendanceGreen),
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                  modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .testTag("assign_selected_to_active_student_button")
                ) {
                  Icon(Icons.Default.TaskAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Bu Talebeye Ata 🎯", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                  onClick = {
                    if (students.isNotEmpty() && selectedTitlesForBulk.isNotEmpty()) {
                      val itemsToAssign = filteredCategoryItems
                        .filter { selectedTitlesForBulk.contains(it.title) }
                        .map { it.title to it.category }

                      viewModel?.assignMemorizationsBulk(
                        studentIds = students.map { it.id },
                        items = itemsToAssign,
                        status = "VERILDI",
                        notes = "Hoca tarafından tüm sınıfa ezber hedefi olarak atandı"
                      )
                      Toast.makeText(context, "${selectedTitlesForBulk.size} ezber tüm sınıfa (${students.size} talebe) görev olarak atandı! 🎯", Toast.LENGTH_SHORT).show()
                      isMultiSelectMode = false
                      selectedTitlesForBulk = emptySet()
                    }
                  },
                  enabled = students.isNotEmpty() && selectedTitlesForBulk.isNotEmpty(),
                  colors = ButtonDefaults.buttonColors(containerColor = FeatureStudentsBlue),
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                  modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .testTag("assign_selected_to_all_class_button")
                ) {
                  Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Tüm Sınıfa Ata 👥", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }

      // ----------------------------------------------------
      // 4. EZBER MADDELERİ LİSTESİ
      // ----------------------------------------------------
      if (filteredCategoryItems.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = Icons.Default.MenuBook,
              contentDescription = null,
              tint = TextMuted,
              modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = if (searchQuery.isNotBlank()) "Aramaya uygun ezber bulunamadı"
              else if (selectedStatusFilter == "Tamamlananlar") "Bu kategoride henüz tamamlanan ezber yok"
              else "Bu kategoride gösterilecek ezber maddesi bulunmuyor",
              fontSize = 13.sp,
              color = TextSecondary,
              textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
              onClick = { showAssignNewTargetDialog = true },
              colors = ButtonDefaults.buttonColors(containerColor = FeatureAttendanceGreen),
              shape = RoundedCornerShape(10.dp)
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Yeni Ezber Tanımla", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      } else {
        LazyColumn(
          contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 80.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.fillMaxSize()
        ) {
          items(filteredCategoryItems, key = { "${it.category}_${it.index}_${it.title}" }) { item ->
            val isSelectedForBulk = selectedTitlesForBulk.contains(item.title)
            AdminMemorizationCard(
              item = item,
              categoryColor = categoryColor,
              isMultiSelectMode = isMultiSelectMode,
              isSelectedForBulk = isSelectedForBulk,
              onToggleSelectForBulk = {
                selectedTitlesForBulk = if (isSelectedForBulk) {
                  selectedTitlesForBulk - item.title
                } else {
                  selectedTitlesForBulk + item.title
                }
              },
              onAssignAsTask = {
                if (activeStudent != null) {
                  haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                  viewModel?.assignMemorizationsBulk(
                    studentIds = listOf(activeStudent.id),
                    items = listOf(item.title to item.category),
                    status = "VERILDI",
                    notes = "Hoca tarafından ezber hedefi olarak atandı"
                  )
                  Toast.makeText(context, "${item.title} talebeye görev olarak atandı 🎯", Toast.LENGTH_SHORT).show()
                }
              },
              onToggleComplete = {
                if (activeStudent != null) {
                  haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                  if (item.isCompleted) {
                    val existing = item.record
                    if (existing != null) onDeleteMemorization(existing)
                    Toast.makeText(context, "${item.title} geri alındı", Toast.LENGTH_SHORT).show()
                  } else {
                    val newRecord = (item.record ?: MemorizationRecord(
                      studentId = activeStudent.id,
                      title = item.title,
                      category = item.category,
                      status = "TAMAMLANDI",
                      rating = 5,
                      progressPercent = 100,
                      teacherNotes = "Kabul edildi",
                      date = todayDate
                    )).copy(
                      studentId = activeStudent.id,
                      title = item.title,
                      category = item.category,
                      status = "TAMAMLANDI",
                      rating = 5,
                      progressPercent = 100,
                      date = todayDate
                    )
                    onSaveMemorization(newRecord)
                    Toast.makeText(context, "${item.title} kabul edildi ✅", Toast.LENGTH_SHORT).show()
                  }
                }
              },
              onRequestRepeat = {
                if (activeStudent != null) {
                  haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                  val updated = (item.record ?: MemorizationRecord(
                    studentId = activeStudent.id,
                    title = item.title,
                    category = item.category,
                    date = todayDate
                  )).copy(
                    studentId = activeStudent.id,
                    title = item.title,
                    category = item.category,
                    status = "TEKRAR",
                    progressPercent = 50,
                    repeatCount = (item.record?.repeatCount ?: 0) + 1,
                    date = todayDate
                  )
                  onSaveMemorization(updated)
                  Toast.makeText(context, "${item.title} tekrar listesine alındı 🔄", Toast.LENGTH_SHORT).show()
                }
              },
              onOpenDetails = {
                showItemDetailDialog = MemorizationItemDetailState(
                  itemTitle = item.title,
                  category = item.category,
                  isCompleted = item.isCompleted,
                  existingRecord = item.record
                )
              }
            )
          }
        }
      }
    }
  }

  // ----------------------------------------------------
  // 5. ÖĞRENCİ SEÇİM BOTTOM SHEET
  // ----------------------------------------------------
  if (showStudentDropdownDialog) {
    StudentBottomSheetPicker(
      students = students,
      memorizationList = memorizationList,
      selectedStudentId = selectedStudentId,
      title = "Ezber Yönetimi İçin Talebe Seçin",
      onDismiss = { showStudentDropdownDialog = false },
      onStudentSelected = { student ->
        selectedStudentId = student.id
        showStudentDropdownDialog = false
      }
    )
  }

  // ----------------------------------------------------
  // 6. DETAYLI DİNLETME VE NOTLANDIRMA MODALI
  // ----------------------------------------------------
  showItemDetailDialog?.let { detailState ->
    if (activeStudent != null) {
      AdminMemorizationGradingDialog(
        studentName = activeStudent.fullName,
        itemTitle = detailState.itemTitle,
        category = detailState.category,
        existingRecord = detailState.existingRecord,
        todayDate = todayDate,
        onDismiss = { showItemDetailDialog = null },
        onSave = { updatedRecord ->
          onSaveMemorization(
            updatedRecord.copy(
              studentId = activeStudent.id,
              title = detailState.itemTitle,
              category = detailState.category
            )
          )
          showItemDetailDialog = null
          Toast.makeText(context, "Ezber değerlendirmesi kaydedildi ✨", Toast.LENGTH_SHORT).show()
        },
        onDelete = {
          if (detailState.existingRecord != null) {
            onDeleteMemorization(detailState.existingRecord)
          }
          showItemDetailDialog = null
          Toast.makeText(context, "Ezber kaydı silindi", Toast.LENGTH_SHORT).show()
        }
      )
    }
  }

  // ----------------------------------------------------
  // 7. YENİ EZBER HEDEFİ TANIMLA DİALOGU
  // ----------------------------------------------------
  if (showAssignNewTargetDialog && activeStudent != null) {
    AssignNewMemorizationDialog(
      activeStudent = activeStudent,
      allStudents = students,
      availableCategories = activeCategories,
      onDismiss = { showAssignNewTargetDialog = false },
      onAssign = { studentId, title, category, targetPercent, status, notes ->
        val record = MemorizationRecord(
          studentId = studentId,
          title = title,
          category = category,
          status = status,
          progressPercent = targetPercent,
          rating = 5,
          teacherNotes = notes,
          date = todayDate
        )
        onSaveMemorization(record)
        showAssignNewTargetDialog = false
        Toast.makeText(context, "$title ezberi tanımlandı 🎯", Toast.LENGTH_SHORT).show()
      }
    )
  }

  // ----------------------------------------------------
  // 8. GENEL GELİŞİM ÖZETİ DİALOGU
  // ----------------------------------------------------
  if (showOverallProgressDialog && activeStudent != null) {
    OverallStudentMemorizationDialog(
      student = activeStudent,
      activeCategories = activeCategories,
      allActiveItems = activeCurriculumItems,
      studentRecords = studentRecords,
      onDismiss = { showOverallProgressDialog = false }
    )
  }

  // ----------------------------------------------------
  // 9. MÜFREDAT YÖNETİMİ VE İÇE AKTARMA
  // ----------------------------------------------------
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

  if (showCurriculumImportDialog && viewModel != null) {
    CurriculumImportDialog(
      viewModel = viewModel,
      onDismiss = { showCurriculumImportDialog = false },
      onCompleted = { showCurriculumImportDialog = false }
    )
  }
}

// =========================================================================
// YÖNETİCİ EZBER KARTI (ADMIN MEMORIZATION CARD)
// =========================================================================
@Composable
private fun AdminMemorizationCard(
  item: CurriculumItemUiModel,
  categoryColor: Color,
  isMultiSelectMode: Boolean = false,
  isSelectedForBulk: Boolean = false,
  onToggleSelectForBulk: () -> Unit = {},
  onAssignAsTask: () -> Unit = {},
  onToggleComplete: () -> Unit,
  onRequestRepeat: () -> Unit,
  onOpenDetails: () -> Unit
) {
  val isCompleted = item.isCompleted
  val status = item.record?.status ?: "SIRADA"
  val isRepeat = status == "TEKRAR"
  val isAssigned = status == "VERILDI" || status == "DEVAM_EDIYOR" || item.record != null
  val percent = item.record?.progressPercent ?: if (isCompleted) 100 else 0
  val rating = item.record?.rating ?: 5
  val repeats = item.record?.repeatCount ?: 0

  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isMultiSelectMode && isSelectedForBulk) DeepBlueNavy.copy(alpha = 0.08f) else CanvasSurface
    ),
    border = androidx.compose.foundation.BorderStroke(
      width = if (isMultiSelectMode && isSelectedForBulk) 1.5.dp else 1.dp,
      color = if (isMultiSelectMode && isSelectedForBulk) FeatureStudentsBlue
              else if (isCompleted) FeatureAttendanceGreen.copy(alpha = 0.4f)
              else if (isRepeat) StatusExcusedAmber.copy(alpha = 0.4f)
              else if (isAssigned && !isCompleted) FeatureMemorizationPurple.copy(alpha = 0.4f)
              else BorderLight
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("memorization_item_${item.index}")
      .then(
        if (isMultiSelectMode) {
          Modifier.clickable { onToggleSelectForBulk() }
        } else {
          Modifier
        }
      )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        // Sol: Sıra No / Checkbox ve Başlık
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          if (isMultiSelectMode) {
            Checkbox(
              checked = isSelectedForBulk,
              onCheckedChange = { onToggleSelectForBulk() },
              colors = CheckboxDefaults.colors(
                checkedColor = FeatureStudentsBlue,
                uncheckedColor = TextMuted
              ),
              modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
          } else {
            Surface(
              shape = CircleShape,
              color = if (isCompleted) FeatureAttendanceGreen
                      else if (isRepeat) StatusExcusedAmber
                      else if (isAssigned) FeatureMemorizationPurple.copy(alpha = 0.2f)
                      else categoryColor.copy(alpha = 0.12f),
              modifier = Modifier.size(28.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(
                  text = "${item.index}",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isCompleted || isRepeat) Color.White
                          else if (isAssigned) FeatureMemorizationPurple
                          else categoryColor
                )
              }
            }
            Spacer(modifier = Modifier.width(8.dp))
          }

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = item.title,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              // Durum Etiketi
              if (isCompleted) {
                Text(text = "Kabul Edildi ✅", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = FeatureAttendanceGreen)
              } else if (isRepeat) {
                Text(text = "Tekrar İstendi 🔄", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = StatusExcusedAmber)
              } else if (isAssigned) {
                Text(text = "🎯 Hedef Atandı", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = FeatureMemorizationPurple)
              } else {
                Text(text = "Sırada", fontSize = 10.5.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
              }

              if (percent > 0 && !isCompleted) {
                Text(text = "• %$percent", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = DeepBlueNavy)
              }

              if (repeats > 0) {
                Text(text = "• $repeats Dinlendi", fontSize = 10.sp, color = TextMuted)
              }

              if (isCompleted && rating > 0) {
                Text(text = "• ${"★".repeat(rating)}", fontSize = 10.sp, color = GoldStar)
              }
            }
          }
        }

        // Sağ: Hızlı Aksiyonlar (Ata, Tekrar İste, Tamamla, Dinle & Notlandır)
        if (!isMultiSelectMode) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
          ) {
            // Hızlı Ata Butonu (Eğer tamamlanmamışsa)
            if (!isCompleted) {
              IconButton(
                onClick = onAssignAsTask,
                modifier = Modifier.size(30.dp)
              ) {
                Icon(
                  imageVector = if (isAssigned) Icons.Default.TaskAlt else Icons.Default.AddCircleOutline,
                  contentDescription = "Ezber Görevi Ata",
                  tint = if (isAssigned) FeatureMemorizationPurple else TextSecondary,
                  modifier = Modifier.size(18.dp)
                )
              }
            }

            // Tekrar İste Butonu
            IconButton(
              onClick = onRequestRepeat,
              modifier = Modifier.size(30.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Tekrar İste",
                tint = if (isRepeat) StatusExcusedAmber else TextSecondary,
                modifier = Modifier.size(17.dp)
              )
            }

            // Hızlı Kabul / Tamamla Butonu
            Surface(
              onClick = onToggleComplete,
              shape = RoundedCornerShape(8.dp),
              color = if (isCompleted) FeatureAttendanceGreen else CanvasBackground,
              border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isCompleted) FeatureAttendanceGreen else BorderLight
              ),
              modifier = Modifier
                .height(30.dp)
                .padding(horizontal = 2.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 6.dp)
              ) {
                Icon(
                  imageVector = if (isCompleted) Icons.Default.Check else Icons.Default.CheckCircleOutline,
                  contentDescription = null,
                  tint = if (isCompleted) Color.White else TextSecondary,
                  modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                  text = if (isCompleted) "Kabul" else "Onayla",
                  fontSize = 10.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isCompleted) Color.White else TextPrimary
                )
              }
            }

            // Dinletme & Notlandırma Butonu
            IconButton(
              onClick = onOpenDetails,
              modifier = Modifier.size(30.dp)
            ) {
              Icon(
                imageVector = Icons.Default.RateReview,
                contentDescription = "Dinle & Notlandır",
                tint = DeepBlueNavy,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }
      }

      // Varsa İlerleme Çubuğu veya Hoca Notu
      if (!isCompleted && percent > 0) {
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
          progress = { percent / 100f },
          color = categoryColor,
          trackColor = CanvasBackground,
          modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
        )
      }

      if (item.record?.teacherNotes?.isNotBlank() == true) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "💬 ${item.record.teacherNotes}",
          fontSize = 10.5.sp,
          color = TextSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}

// =========================================================================
// KAPSAMLI DİNLETME VE NOTLANDIRMA DİYALOĞU (ADMIN LIVE EVALUATION DIALOG)
// =========================================================================
@Composable
private fun AdminMemorizationGradingDialog(
  studentName: String,
  itemTitle: String,
  category: String,
  existingRecord: MemorizationRecord?,
  todayDate: String,
  onDismiss: () -> Unit,
  onSave: (MemorizationRecord) -> Unit,
  onDelete: () -> Unit
) {
  var status by remember {
    mutableStateOf(existingRecord?.status ?: if ((existingRecord?.progressPercent ?: 0) >= 100) "TAMAMLANDI" else "DEVAM_EDIYOR")
  }
  var progressPercent by remember {
    mutableIntStateOf(existingRecord?.progressPercent ?: if (status == "TAMAMLANDI") 100 else 50)
  }
  var rating by remember { mutableIntStateOf(existingRecord?.rating ?: 5) }
  var repeatCount by remember { mutableIntStateOf(existingRecord?.repeatCount ?: 0) }
  var notes by remember { mutableStateOf(existingRecord?.teacherNotes ?: "") }

  val ratingDescription = when (rating) {
    5 -> "Fevkalade / Mükemmel 🌟"
    4 -> "Çok İyi / Akıcı ✨"
    3 -> "İyi / Kabul Edilebilir 👍"
    2 -> "Orta / Geliştirilmeli ⚠️"
    else -> "Zayıf / Tekrar Edilmeli 🔄"
  }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = CanvasSurface),
      border = androidx.compose.foundation.BorderStroke(1.2.dp, BorderLight),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp)
      ) {
        // Başlık
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = DeepBlueNavy, modifier = Modifier.size(24.dp))
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = itemTitle,
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp,
              color = TextPrimary
            )
            Text(
              text = "$studentName • $category",
              fontSize = 11.5.sp,
              color = TextSecondary
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 1. Durum Seçimi: [ Tamamlandı ] [ Devam Ediyor ] [ Tekrar ]
        Text("Ezber Durumu:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          FilterStatusChip("✅ Tamamlandı", status == "TAMAMLANDI", FeatureAttendanceGreen, {
            status = "TAMAMLANDI"
            progressPercent = 100
          }, Modifier.weight(1f))
          FilterStatusChip("⏳ Devam", status == "DEVAM_EDIYOR", FeatureStudentsBlue, {
            status = "DEVAM_EDIYOR"
          }, Modifier.weight(1f))
          FilterStatusChip("🔄 Tekrar", status == "TEKRAR", StatusExcusedAmber, {
            status = "TEKRAR"
          }, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Yıldız Puanı (1..5)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Okuma Kalitesi:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
          Text(text = ratingDescription, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = DarkSlateNavy)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Center
        ) {
          for (star in 1..5) {
            IconButton(
              onClick = { rating = star },
              modifier = Modifier.size(36.dp)
            ) {
              Icon(
                imageVector = if (star <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = "$star Yıldız",
                tint = if (star <= rating) GoldStar else TextMuted,
                modifier = Modifier.size(26.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. İlerleme Yüzdesi Slider & Hızlı Seçim Butonları
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Ezber İlerlemesi:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
          Text("%$progressPercent", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FeatureAttendanceGreen)
        }
        Slider(
          value = progressPercent.toFloat(),
          onValueChange = {
            progressPercent = it.toInt()
            if (progressPercent == 100) status = "TAMAMLANDI"
          },
          valueRange = 0f..100f,
          steps = 3,
          colors = SliderDefaults.colors(
            thumbColor = FeatureAttendanceGreen,
            activeTrackColor = FeatureAttendanceGreen
          )
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          listOf(25, 50, 75, 100).forEach { pct ->
            Surface(
              onClick = {
                progressPercent = pct
                if (pct == 100) status = "TAMAMLANDI"
              },
              shape = RoundedCornerShape(6.dp),
              color = if (progressPercent == pct) FeatureAttendanceGreen.copy(alpha = 0.15f) else CanvasBackground,
              border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
              modifier = Modifier.weight(1f)
            ) {
              Text(
                text = "%$pct",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 4.dp),
                color = if (progressPercent == pct) FeatureAttendanceGreen else TextSecondary
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Tekrar / Dinleme Sayacı
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Dinleme / Tekrar Sayısı:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = { if (repeatCount > 0) repeatCount-- },
              modifier = Modifier.size(28.dp)
            ) {
              Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Text("$repeatCount Kez", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DeepBlueNavy)
            IconButton(
              onClick = { repeatCount++ },
              modifier = Modifier.size(28.dp)
            ) {
              Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = FeatureAttendanceGreen)
            }
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 5. Hoca Geri Bildirim Notu
        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Hoca Notu / Değerlendirme", fontSize = 11.sp) },
          placeholder = { Text("Mahreç, tecvid veya tebrik...", fontSize = 12.sp) },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth(),
          maxLines = 2
        )

        // Hızlı Şablonlar
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          val templates = listOf(
            "Tecvid ve mahreçler fevkalade 🌟",
            "Akıcı okundu, kabul edildi ✨",
            "1. sayfada duraklara dikkat ⚠️",
            "Yarın baştan dinlenecek 🔄"
          )
          items(templates) { t ->
            Surface(
              color = CanvasBackground,
              shape = RoundedCornerShape(6.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
              modifier = Modifier.clickable { notes = t }
            ) {
              Text(
                text = t,
                fontSize = 10.sp,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Butonlar (Sil / İptal / Kaydet)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (existingRecord != null) {
            TextButton(onClick = onDelete) {
              Text("Kaydı Sil", color = StatusAbsentRed, fontSize = 12.sp)
            }
          } else {
            Spacer(modifier = Modifier.width(10.dp))
          }

          Row {
            TextButton(onClick = onDismiss) {
              Text("İptal", color = TextSecondary)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Button(
              onClick = {
                val record = (existingRecord ?: MemorizationRecord(
                  title = itemTitle,
                  category = category,
                  date = todayDate
                )).copy(
                  title = itemTitle,
                  category = category,
                  status = status,
                  progressPercent = progressPercent,
                  rating = rating,
                  repeatCount = repeatCount,
                  teacherNotes = notes.trim(),
                  date = todayDate
                )
                onSave(record)
              },
              colors = ButtonDefaults.buttonColors(containerColor = FeatureAttendanceGreen),
              shape = RoundedCornerShape(10.dp)
            ) {
              Text("Kaydet", fontWeight = FontWeight.Bold, color = Color.White)
            }
          }
        }
      }
    }
  }
}

// =========================================================================
// YENİ EZBER / HEDEF ATA DİYALOĞU (ASSIGN NEW MEMORIZATION DIALOG)
// =========================================================================
@Composable
private fun AssignNewMemorizationDialog(
  activeStudent: Student,
  allStudents: List<Student>,
  availableCategories: List<String>,
  onDismiss: () -> Unit,
  onAssign: (studentId: Long, title: String, category: String, targetPercent: Int, status: String, notes: String) -> Unit
) {
  var selectedStudent by remember { mutableStateOf(activeStudent) }
  var selectedCategory by remember { mutableStateOf(availableCategories.firstOrNull() ?: "Kur'an-ı Kerim") }
  var titleText by remember { mutableStateOf("") }
  var targetPercent by remember { mutableIntStateOf(0) }
  var notesText by remember { mutableStateOf("") }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = CanvasSurface),
      border = androidx.compose.foundation.BorderStroke(1.2.dp, BorderLight),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.AddCircle, contentDescription = null, tint = FeatureAttendanceGreen, modifier = Modifier.size(24.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Yeni Ezber Hedefi Ata", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Talebe Seçimi
        Text("Talebe:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
        Spacer(modifier = Modifier.height(3.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(allStudents) { st ->
            val isSel = st.id == selectedStudent.id
            Surface(
              onClick = { selectedStudent = st },
              shape = RoundedCornerShape(8.dp),
              color = if (isSel) FeatureStudentsBlue else CanvasBackground,
              border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) FeatureStudentsBlue else BorderLight)
            ) {
              Text(
                text = st.fullName,
                fontSize = 11.5.sp,
                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                color = if (isSel) Color.White else TextPrimary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Kategori Seçimi
        Text("Kategori:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
        Spacer(modifier = Modifier.height(3.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(availableCategories) { cat ->
            val isSel = cat == selectedCategory
            Surface(
              onClick = { selectedCategory = cat },
              shape = RoundedCornerShape(8.dp),
              color = if (isSel) getCategoryThemeColor(cat) else CanvasBackground,
              border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) getCategoryThemeColor(cat) else BorderLight)
            ) {
              Text(
                text = cat,
                fontSize = 11.5.sp,
                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                color = if (isSel) Color.White else TextPrimary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Ezber Başlığı / Metni
        OutlinedTextField(
          value = titleText,
          onValueChange = { titleText = it },
          label = { Text("Ezber Başlığı / Sure / Aşır", fontSize = 11.sp) },
          placeholder = { Text("Örn: Mülk Suresi 1-15 veya Sabah Tesbihatı", fontSize = 12.sp) },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )

        // Hızlı Başlık Önerileri
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          val presets = listOf("Nebe Suresi", "Mülk Suresi", "Yasin 1. Sayfa", "Haşr Son 3 Ayet", "Sabah Tesbihatı", "40 Hadis 1-5")
          items(presets) { preset ->
            Surface(
              color = CanvasBackground,
              shape = RoundedCornerShape(6.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
              modifier = Modifier.clickable { titleText = preset }
            ) {
              Text(
                text = preset,
                fontSize = 10.sp,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Hoca Talimatı / Notu
        OutlinedTextField(
          value = notesText,
          onValueChange = { notesText = it },
          label = { Text("Hoca Talimatı / Açıklama", fontSize = 11.sp) },
          placeholder = { Text("Örn: Cuma gününe kadar dinlenecek", fontSize = 12.sp) },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth(),
          maxLines = 2
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          TextButton(onClick = onDismiss) {
            Text("İptal", color = TextSecondary)
          }
          Spacer(modifier = Modifier.width(6.dp))
          Button(
            onClick = {
              if (titleText.isNotBlank()) {
                onAssign(
                  selectedStudent.id,
                  titleText.trim(),
                  selectedCategory,
                  targetPercent,
                  "VERILDI",
                  notesText.trim()
                )
              }
            },
            enabled = titleText.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = FeatureAttendanceGreen),
            shape = RoundedCornerShape(10.dp)
          ) {
            Text("Ezberi Ata", fontWeight = FontWeight.Bold, color = Color.White)
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------------------
// FİLTRE HAP ÇİPİ (FILTER STATUS CHIP)
// -------------------------------------------------------------------------
@Composable
private fun FilterStatusChip(
  title: String,
  isSelected: Boolean,
  activeColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(8.dp),
    color = if (isSelected) activeColor else CanvasSurface,
    border = androidx.compose.foundation.BorderStroke(
      width = 1.dp,
      color = if (isSelected) activeColor else BorderLight
    ),
    modifier = modifier.height(32.dp)
  ) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
      Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        color = if (isSelected) Color.White else TextPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

// -------------------------------------------------------------------------
// GENEL ÖĞRENCİ EZBER GELİŞİM ÖZETİ DİALOGU
// -------------------------------------------------------------------------
@Composable
private fun OverallStudentMemorizationDialog(
  student: Student,
  activeCategories: List<String>,
  allActiveItems: List<CustomCurriculumItem>,
  studentRecords: List<MemorizationRecord>,
  onDismiss: () -> Unit
) {
  fun isCompleted(title: String, cat: String): Boolean {
    return studentRecords.any { record ->
      record.title.equals(title.trim(), ignoreCase = true) &&
          (record.category.equals(cat.trim(), ignoreCase = true) ||
              normalizeCategoryKey(record.category) == normalizeCategoryKey(cat)) &&
          (record.status == "TAMAMLANDI" || record.progressPercent >= 100)
    }
  }

  val totalCompleted = allActiveItems.count { isCompleted(it.title, it.category) }
  val totalAll = allActiveItems.size
  val overallPct = if (totalAll > 0) (totalCompleted * 100) / totalAll else 0

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Analytics,
          contentDescription = null,
          tint = DeepBlueNavy,
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Müfredat Gelişim Özeti", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          StudentAvatar(name = student.fullName, colorIndex = student.avatarColorIndex, size = 36.dp, fontSize = 13)
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(student.fullName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("${student.grade} • Toplam %$overallPct Tamamlandı", fontSize = 12.sp, color = TextSecondary)
          }
        }

        HorizontalDivider(color = BorderLight)

        activeCategories.forEach { catName ->
          val catItems = allActiveItems.filter { it.category.equals(catName, ignoreCase = true) }
          val catCompleted = catItems.count { isCompleted(it.title, catName) }
          val catTotal = catItems.size
          val catColor = getCategoryThemeColor(catName)
          val catIcon = getCategoryIcon(catName)

          CategoryProgressRow(
            icon = catIcon,
            title = catName,
            completed = catCompleted,
            total = catTotal,
            color = catColor
          )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Surface(
          color = DeepBlueNavy.copy(alpha = 0.08f),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Genel Toplam:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DeepBlueNavy)
            Text(
              text = "$totalCompleted / $totalAll (%$overallPct)",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = DeepBlueNavy
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        colors = ButtonDefaults.buttonColors(containerColor = DeepBlueNavy),
        shape = RoundedCornerShape(10.dp)
      ) {
        Text("Kapat", fontWeight = FontWeight.Bold)
      }
    },
    shape = RoundedCornerShape(20.dp),
    containerColor = CanvasSurface
  )
}

@Composable
private fun CategoryProgressRow(
  icon: String,
  title: String,
  completed: Int,
  total: Int,
  color: Color
) {
  val pct = if (total > 0) (completed * 100) / total else 0
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = "$icon $title",
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextPrimary
      )
      Text(
        text = "$completed / $total (%$pct)",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = color
      )
    }

    Spacer(modifier = Modifier.height(4.dp))

    LinearProgressIndicator(
      progress = { if (total > 0) completed.toFloat() / total else 0f },
      color = color,
      trackColor = SurfaceVariantColor,
      modifier = Modifier
        .fillMaxWidth()
        .height(6.dp)
        .clip(RoundedCornerShape(3.dp))
    )
  }
}

// Data models
private data class CurriculumItemUiModel(
  val index: Int,
  val title: String,
  val category: String,
  val isCompleted: Boolean,
  val record: MemorizationRecord?,
  val originalItem: CustomCurriculumItem? = null
)

private data class MemorizationItemDetailState(
  val itemTitle: String,
  val category: String,
  val isCompleted: Boolean,
  val existingRecord: MemorizationRecord?
)

private fun normalizeCategoryKey(cat: String): String {
  val lower = cat.lowercase(Locale.ROOT)
  return when {
    lower.contains("kuran") || lower.contains("kur'an") || lower.contains("sure") || lower.contains("cüz") -> "kuran"
    lower.contains("risale") || lower.contains("sozler") || lower.contains("nur") -> "risale"
    lower.contains("tesbihat") || lower.contains("dua") -> "tesbihat"
    lower.contains("hadis") -> "hadis"
    lower.contains("elif") || lower.contains("elifba") || lower.contains("tecvid") -> "elifba"
    else -> lower.trim()
  }
}

private fun getCategoryThemeColor(category: String): Color {
  val lower = category.lowercase(Locale.ROOT)
  return when {
    lower.contains("kuran") || lower.contains("kur'an") || lower.contains("sure") -> FeatureAttendanceGreen
    lower.contains("risale") || lower.contains("sozler") -> FeatureReportsOrange
    lower.contains("tesbihat") || lower.contains("dua") -> FeatureMemorizationPurple
    lower.contains("hadis") -> FeatureDevelopmentTeal
    lower.contains("elif") || lower.contains("tecvid") -> FeatureStudentsBlue
    else -> DeepBlueNavy
  }
}

private fun getCategoryIcon(category: String): String {
  val lower = category.lowercase(Locale.ROOT)
  return when {
    lower.contains("kuran") || lower.contains("kur'an") || lower.contains("sure") -> "📖"
    lower.contains("risale") || lower.contains("sozler") -> "📚"
    lower.contains("tesbihat") || lower.contains("dua") -> "📿"
    lower.contains("hadis") -> "📜"
    lower.contains("elif") || lower.contains("tecvid") -> "🌟"
    else -> "🏷️"
  }
}
