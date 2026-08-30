package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CurriculumData
import com.example.data.model.MemorizationRecord
import com.example.data.model.Student
import com.example.data.util.CurriculumManager
import com.example.data.util.CustomCurriculumItem
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevelopmentScreen(
  students: List<Student>,
  memorizationList: List<MemorizationRecord>,
  todayDate: String,
  onBackClick: () -> Unit,
  viewModel: AppViewModel? = null
) {
  val context = LocalContext.current
  val curriculumManager = remember { CurriculumManager.getInstance(context) }
  val currentCurriculum: List<CustomCurriculumItem> by if (viewModel != null) {
    viewModel.curriculumItems.collectAsState()
  } else {
    curriculumManager.itemsFlow.collectAsState()
  }

  // Active (selected) curriculum items
  val activeCurriculumItems: List<CustomCurriculumItem> = remember(currentCurriculum) {
    val selectedOnly = currentCurriculum.filter { it.isSelected }
    if (selectedOnly.isNotEmpty()) selectedOnly else currentCurriculum
  }
  val activeTargetCount: Int = activeCurriculumItems.size.coerceAtLeast(1)

  var selectedTab by remember { mutableStateOf(0) } // 0: Bireysel Gelişim, 1: Sınıf Karşılaştırması
  var selectedStudentId by remember { mutableStateOf<Long?>(null) }
  var showStudentPickerSheet by remember { mutableStateOf(false) }

  val activeStudents = students
  val currentStudent = activeStudents.firstOrNull { it.id == selectedStudentId }
    ?: activeStudents.firstOrNull()

  // Overall Statistics across all students based on dynamic curriculum target
  val totalCurriculumTarget = activeTargetCount * (if (activeStudents.isNotEmpty()) activeStudents.size else 1)
  val totalCompletedRecords = memorizationList.count { it.status == "TAMAMLANDI" || it.progressPercent >= 100 }
  val overallClassProgressPercent = if (totalCurriculumTarget > 0) {
    ((totalCompletedRecords.toFloat() / totalCurriculumTarget) * 100).toInt().coerceIn(0, 100)
  } else 0

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Gelişim Takibi",
              fontWeight = FontWeight.Bold,
              fontSize = 20.sp,
              color = Color.White
            )
            Text(
              text = "Ezber Grafikleri ve İlerleme Analizi ($activeTargetCount Madde)",
              fontSize = 12.sp,
              color = Color.White.copy(alpha = 0.8f)
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onBackClick,
            modifier = Modifier.testTag("development_back_button")
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
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // Main View Mode Tabs (2 Tabs: Bireysel Gelişim & Sınıf Karşılaştırması)
      TabRow(
        selectedTabIndex = selectedTab,
        containerColor = CanvasSurface,
        contentColor = FeatureDevelopmentTeal,
        divider = { HorizontalDivider(color = BorderLight) }
      ) {
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Bireysel Gelişim", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
          }
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Sınıf Karşılaştırması", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
          }
        )
      }

      // Active Curriculum Info Strip
      Surface(
        color = FeatureDevelopmentTeal.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = null,
              tint = FeatureDevelopmentTeal,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            val isStandard108 = activeTargetCount == CurriculumData.ALL_CURRICULUM_COUNT
            Text(
              text = if (isStandard108) {
                "Aktif Müfredat: Standart Liste ($activeTargetCount Madde)"
              } else {
                "Aktif Müfredat: Özel / Seçili Ezber Listesi ($activeTargetCount Madde)"
              },
              fontSize = 11.5.sp,
              fontWeight = FontWeight.SemiBold,
              color = DeepBlueNavy
            )
          }

          val categoryCount = activeCurriculumItems.map { it.category.trim() }.distinct().size
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = FeatureDevelopmentTeal.copy(alpha = 0.18f)
          ) {
            Text(
              text = "$categoryCount Kategori",
              fontSize = 10.5.sp,
              fontWeight = FontWeight.Bold,
              color = DeepBlueNavy,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }
      }

      if (activeStudents.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = Icons.Default.TrendingUp,
              contentDescription = null,
              tint = TextMuted,
              modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "Gelişim grafikleri için henüz kayıtlı öğrenci yok",
              fontSize = 15.sp,
              fontWeight = FontWeight.Medium,
              color = TextSecondary,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
          }
        }
      } else {
        when (selectedTab) {
          0 -> {
            // === TAB 0: Bireysel Gelişim ve Müfredat İlerlemesi ===
            LazyColumn(
              modifier = Modifier.fillMaxSize(),
              contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 48.dp),
              verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
              // Student Selector Interactive Card (Opens Bottom Sheet)
              item {
                Surface(
                  onClick = { showStudentPickerSheet = true },
                  shape = RoundedCornerShape(16.dp),
                  color = CanvasSurface,
                  border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                  shadowElevation = 2.dp,
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("development_student_selector_card")
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.weight(1f)
                    ) {
                      if (currentStudent != null) {
                        StudentAvatar(
                          name = currentStudent.fullName,
                          colorIndex = currentStudent.avatarColorIndex,
                          size = 40.dp,
                          fontSize = 14
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                          Text(
                            text = "Gelişimi İncelenen Talebe",
                            fontSize = 11.sp,
                            color = TextSecondary
                          )
                          Text(
                            text = currentStudent.fullName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                          )
                          if (currentStudent.grade.isNotBlank()) {
                            Text(
                              text = currentStudent.grade,
                              fontSize = 11.sp,
                              color = FeatureDevelopmentTeal,
                              fontWeight = FontWeight.Medium
                            )
                          }
                        }
                      } else {
                        Text("Talebe seçiniz...", color = TextSecondary)
                      }
                    }

                    Surface(
                      color = FeatureDevelopmentTeal.copy(alpha = 0.12f),
                      shape = RoundedCornerShape(10.dp)
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Text(
                          text = "Talebeleri Gör",
                          fontSize = 12.sp,
                          fontWeight = FontWeight.SemiBold,
                          color = FeatureDevelopmentTeal
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                          imageVector = Icons.Default.KeyboardArrowDown,
                          contentDescription = "Aşağı Kaydır",
                          tint = FeatureDevelopmentTeal,
                          modifier = Modifier.size(18.dp)
                        )
                      }
                    }
                  }
                }
              }

              if (currentStudent != null) {
                val studentRecords = memorizationList.filter { it.studentId == currentStudent.id }
                val summaries = calculateStudentCategorySummaries(studentRecords, activeCurriculumItems)
                val completedCount = studentRecords.count { it.status == "TAMAMLANDI" || it.progressPercent >= 100 }
                val overallStudentPercent = if (activeTargetCount > 0) {
                  ((completedCount.toFloat() / activeTargetCount) * 100).toInt().coerceIn(0, 100)
                } else 0

                // Student Profile & Progress Summary Header Card
                item {
                  Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CanvasSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                      ) {
                        StudentAvatar(
                          name = currentStudent.fullName,
                          colorIndex = currentStudent.avatarColorIndex,
                          size = 52.dp,
                          fontSize = 18
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                          Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                              text = currentStudent.fullName,
                              fontSize = 17.sp,
                              fontWeight = FontWeight.Bold,
                              color = TextPrimary
                            )
                            if (currentStudent.grade.isNotBlank()) {
                              Spacer(modifier = Modifier.width(6.dp))
                              Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = FeatureDevelopmentTeal.copy(alpha = 0.12f)
                              ) {
                                Text(
                                  text = currentStudent.grade,
                                  fontSize = 11.sp,
                                  fontWeight = FontWeight.SemiBold,
                                  color = FeatureDevelopmentTeal,
                                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                              }
                            }
                          }
                          Spacer(modifier = Modifier.height(2.dp))
                          Text(
                            text = "$completedCount / $activeTargetCount Müfredat Ezberi Tamamlandı",
                            fontSize = 12.sp,
                            color = TextSecondary
                          )
                        }

                        // Big Percentage Pill
                        Surface(
                          color = FeatureDevelopmentTeal,
                          shape = RoundedCornerShape(14.dp)
                        ) {
                          Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                          ) {
                            Text(
                              text = "%$overallStudentPercent",
                              color = Color.White,
                              fontWeight = FontWeight.ExtraBold,
                              fontSize = 16.sp
                            )
                            Text(
                              text = "Genel Başarı",
                              color = Color.White.copy(alpha = 0.85f),
                              fontSize = 9.sp
                            )
                          }
                        }
                      }

                      Spacer(modifier = Modifier.height(14.dp))
                      HorizontalDivider(color = BorderLight)
                      Spacer(modifier = Modifier.height(12.dp))

                      // Category Mini KPI Cards
                      LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                      ) {
                        items(summaries) { summary ->
                          Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = summary.color.copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, summary.color.copy(alpha = 0.2f)),
                            modifier = Modifier.widthIn(min = 100.dp)
                          ) {
                            Column(
                              modifier = Modifier.padding(10.dp),
                              horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                              Icon(summary.icon, contentDescription = null, tint = summary.color, modifier = Modifier.size(18.dp))
                              Spacer(modifier = Modifier.height(4.dp))
                              Text(
                                text = "%${summary.averagePercent}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = summary.color
                              )
                              Text(
                                text = summary.categoryName,
                                fontSize = 10.5.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                              )
                              Text(
                                text = "${summary.completedCount}/${summary.officialTargetCount}",
                                fontSize = 9.5.sp,
                                color = TextMuted
                              )
                            }
                          }
                        }
                      }
                    }
                  }
                }

                // 1. Müfredat Bitiş Tahmini & Gelecek Projeksiyon Grafiği (Forecast & Pace)
                item {
                  CurriculumCompletionProjectionCard(
                    student = currentStudent,
                    records = studentRecords,
                    curriculumItems = activeCurriculumItems
                  )
                }

                // 2. Ezber Hızı & Son 1 Aylık Gelişim Grafiği
                item {
                  StudentMemorizationPaceChart(
                    student = currentStudent,
                    records = studentRecords
                  )
                }

                // 3. Haftalık Çalışma Hızı & Teslim Yoğunluğu Grafiği (Study Velocity)
                item {
                  StudyVelocityAndActivityChart(
                    student = currentStudent,
                    records = studentRecords
                  )
                }

                // Student Notes if available
                if (currentStudent.notes.isNotEmpty()) {
                  item {
                    Card(
                      shape = RoundedCornerShape(16.dp),
                      colors = CardDefaults.cardColors(containerColor = CanvasSurface),
                      border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Icon(
                          imageVector = Icons.Default.EditNote,
                          contentDescription = null,
                          tint = FeatureDevelopmentTeal,
                          modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                          Text("Hoca Gelişim Notu", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                          Text(currentStudent.notes, fontSize = 13.sp, color = TextSecondary)
                        }
                      }
                    }
                  }
                }
              }
            }
          }

          1 -> {
            // === TAB 1: Sınıf Karşılaştırması & Genel Analiz ===
            LazyColumn(
              modifier = Modifier.fillMaxSize(),
              contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 48.dp),
              verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
              // Overview Class Statistics Card
              item {
                Card(
                  shape = RoundedCornerShape(20.dp),
                  colors = CardDefaults.cardColors(containerColor = CanvasSurface),
                  elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                  border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                      text = "🏫 Sınıf Geneli Müfredat İlerlemesi",
                      fontSize = 16.sp,
                      fontWeight = FontWeight.Bold,
                      color = TextPrimary
                    )
                    Text(
                      text = "${activeStudents.size} talebenin $activeTargetCount maddelik aktif müfredattaki ortalama başarısı",
                      fontSize = 12.sp,
                      color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                      Surface(
                        color = FeatureDevelopmentTeal.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                      ) {
                        Column(
                          modifier = Modifier.padding(10.dp),
                          horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                          Text("Ortalama İlerleme", fontSize = 11.sp, color = TextSecondary)
                          Text("%$overallClassProgressPercent", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = FeatureDevelopmentTeal)
                        }
                      }

                      Surface(
                        color = FeatureAttendanceGreen.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                      ) {
                        Column(
                          modifier = Modifier.padding(10.dp),
                          horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                          Text("Tamamlanan", fontSize = 11.sp, color = TextSecondary)
                          Text("$totalCompletedRecords", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = FeatureAttendanceGreen)
                        }
                      }

                      Surface(
                        color = FeatureMemorizationPurple.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                      ) {
                        Column(
                          modifier = Modifier.padding(10.dp),
                          horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                          Text("Aktif Talebe", fontSize = 11.sp, color = TextSecondary)
                          Text("${activeStudents.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = FeatureMemorizationPurple)
                        }
                      }
                    }
                  }
                }
              }

              // Comparative Multi-Bar Chart for all students
              item {
                ClassComparisonBarChart(
                  students = activeStudents,
                  records = memorizationList,
                  curriculumItems = activeCurriculumItems
                )
              }
            }
          }
        }
      }
    }
  }

  // Student Selection Bottom Sheet
  if (showStudentPickerSheet) {
    StudentBottomSheetPicker(
      students = activeStudents,
      selectedStudentId = currentStudent?.id,
      memorizationList = memorizationList,
      title = "Gelişim Takibi İçin Talebe Seçin",
      onDismiss = { showStudentPickerSheet = false },
      onStudentSelected = { student ->
        selectedStudentId = student.id
        showStudentPickerSheet = false
      }
    )
  }
}
