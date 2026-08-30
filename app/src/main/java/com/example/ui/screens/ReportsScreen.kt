package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceRecord
import com.example.data.model.BadgeHelper
import com.example.data.model.CurriculumData
import com.example.data.model.MemorizationRecord
import com.example.data.model.Student
import com.example.data.util.CurriculumManager
import com.example.data.util.CustomCurriculumItem
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.PdfExportHelper
import com.example.ui.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
  students: List<Student>,
  attendanceList: List<AttendanceRecord>,
  memorizationList: List<MemorizationRecord>,
  todayDate: String,
  onBackClick: () -> Unit,
  viewModel: AppViewModel? = null
) {
  val context = LocalContext.current
  var selectedStudentId by remember { mutableStateOf<Long?>(null) }
  var studentDropdownExpanded by remember { mutableStateOf(false) }
  var studentSearchQuery by remember { mutableStateOf("") }

  val curriculumFlow = remember(viewModel, context) {
    viewModel?.curriculumItems ?: CurriculumManager.getInstance(context).itemsFlow
  }
  val currentCurriculumItems by curriculumFlow.collectAsState()
  val activeCurriculum = remember(currentCurriculumItems) {
    val sel = currentCurriculumItems.filter { it.isSelected }
    if (sel.isNotEmpty()) sel else currentCurriculumItems
  }
  val totalCurriculumTargetCount = activeCurriculum.size.coerceAtLeast(1)

  // Student ranking data for individual reports
  val studentRankings = students.map { student ->
    val studentMems = memorizationList.filter { it.studentId == student.id }
    val completedCount = studentMems.count { it.status == "TAMAMLANDI" || it.progressPercent >= 100 }
    val inProgressCount = studentMems.count { it.status == "DEVAM_EDIYOR" || (it.progressPercent in 1..99) }
    val avgRating = if (studentMems.isNotEmpty()) studentMems.map { it.rating }.average() else 5.0
    val studentAtt = attendanceList.filter { it.studentId == student.id }
    val attRate = if (studentAtt.isNotEmpty()) (studentAtt.count { it.status == "GELDI" } * 100) / studentAtt.size else 100

    val summaries = calculateStudentCategorySummaries(studentMems, currentCurriculumItems)

    StudentReportData(
      student = student,
      completedCount = completedCount,
      inProgressCount = inProgressCount,
      avgRating = avgRating,
      attendanceRate = attRate,
      categorySummaries = summaries,
      memorizations = studentMems
    )
  }.sortedByDescending { it.completedCount * 100 + it.inProgressCount * 10 + it.attendanceRate }

  val selectedStudentData = studentRankings.firstOrNull { it.student.id == selectedStudentId }
    ?: studentRankings.firstOrNull()

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "Talebe Karnesi & Bireysel Rapor",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color.White
          )
        },
        navigationIcon = {
          IconButton(
            onClick = onBackClick,
            modifier = Modifier.testTag("reports_back_button")
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
      if (selectedStudentData == null) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
          contentAlignment = Alignment.Center
        ) {
          Text("Kayıtlı öğrenci bulunamadı", color = TextSecondary)
        }
      } else {
          val student = selectedStudentData.student
          val studentRankIndex = studentRankings.indexOfFirst { it.student.id == student.id } + 1
          val studentMems = selectedStudentData.memorizations

          // Dynamic Category Summaries
          val dynamicSummaries = remember(studentMems, currentCurriculumItems) {
            calculateStudentCategorySummaries(studentMems, currentCurriculumItems)
          }

          // Calculate Dynamic Curriculum counts
          val totalCurriculumDone = activeCurriculum.count { curItem ->
            studentMems.any { m -> m.title.equals(curItem.title.trim(), ignoreCase = true) && (m.status == "TAMAMLANDI" || m.progressPercent >= 100) }
          }
          val totalCurriculumPct = ((totalCurriculumDone.toFloat() / totalCurriculumTargetCount) * 100).toInt().coerceIn(0, 100)

          LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            // Student Selector Dropdown Menu (Aşağı Açılır Pencere)
            item {
              val currentStudentIndex = students.indexOfFirst { it.id == student.id }.coerceAtLeast(0)
              val filteredStudents = if (studentSearchQuery.isBlank()) {
                students
              } else {
                students.filter {
                  it.fullName.contains(studentSearchQuery, ignoreCase = true) ||
                  it.grade.contains(studentSearchQuery, ignoreCase = true)
                }
              }

              Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "Karne Talebe Seçimi:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                  )
                  Text(
                    text = "${currentStudentIndex + 1} / ${students.size} Talebe",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted
                  )
                }

                // Selector trigger Card (Aşağıdan Kayar Pencere Açıcı)
                Card(
                  shape = RoundedCornerShape(16.dp),
                  colors = CardDefaults.cardColors(containerColor = CanvasSurface),
                  border = androidx.compose.foundation.BorderStroke(1.5.dp, BorderLight),
                  elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable { studentDropdownExpanded = true }
                    .testTag("student_dropdown_trigger")
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    // Student Initials Avatar
                    Box(
                      modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                          Brush.linearGradient(
                            listOf(DeepBlueNavy, Color(0xFF1E293B))
                          )
                        ),
                      contentAlignment = Alignment.Center
                    ) {
                      Text(
                        text = student.fullName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase(Locale("tr")),
                        fontWeight = FontWeight.Bold,
                        color = GoldStar,
                        fontSize = 14.sp
                      )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                          text = student.fullName,
                          fontSize = 15.sp,
                          fontWeight = FontWeight.Bold,
                          color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                          color = FeatureAttendanceGreen.copy(alpha = 0.12f),
                          shape = RoundedCornerShape(6.dp)
                        ) {
                          Text(
                            text = "#$studentRankIndex",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = FeatureAttendanceGreen,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                          )
                        }
                      }
                      Text(
                        text = "${if (student.grade.isNotBlank()) student.grade else "Sınıf Belirtilmedi"} • ${student.status}",
                        fontSize = 11.5.sp,
                        color = TextSecondary
                      )
                    }

                    // Change / Dropdown icon indicator
                    Surface(
                      color = DeepBlueNavy.copy(alpha = 0.1f),
                      shape = RoundedCornerShape(10.dp),
                      modifier = Modifier.size(36.dp)
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        Icon(
                          imageVector = Icons.Default.KeyboardArrowDown,
                          contentDescription = "Öğrenci Listesini Aç",
                          tint = DeepBlueNavy,
                          modifier = Modifier.size(20.dp)
                        )
                      }
                    }
                  }
                }
              }
            }

            // PDF Export Action Header Card
            item {
              Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DeepBlueNavy),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
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
                      color = GoldStar.copy(alpha = 0.2f),
                      shape = RoundedCornerShape(10.dp),
                      modifier = Modifier.size(38.dp)
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        Icon(
                          imageVector = Icons.Default.PictureAsPdf,
                          contentDescription = "PDF",
                          tint = GoldStar,
                          modifier = Modifier.size(22.dp)
                        )
                      }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = "Resmi A4 Karne Çıktısı (PDF)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                      )
                      Text(
                        text = "Veliye vermek veya yazdırmak için A4 karne belgesi oluşturun.",
                        fontSize = 11.sp,
                        color = Color(0xFFCBD5E1)
                      )
                    }
                  }

                  Spacer(modifier = Modifier.height(12.dp))

                  // WhatsApp Direct Share Button
                  Button(
                    onClick = {
                      PdfExportHelper.shareReportCardViaWhatsApp(
                        context = context,
                        student = student,
                        memorizations = memorizationList,
                        attendanceList = attendanceList,
                        rankIndex = studentRankIndex,
                        totalStudents = students.size
                      )
                    },
                    colors = ButtonDefaults.buttonColors(
                      containerColor = Color(0xFF25D366), // WhatsApp Green
                      contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("whatsapp_share_report_button")
                  ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "📱 WhatsApp ile Veliye Karne Gönder (${student.fullName.split(" ").firstOrNull() ?: "Talebe"})",
                      fontWeight = FontWeight.Bold,
                      fontSize = 12.5.sp
                    )
                  }

                  Spacer(modifier = Modifier.height(8.dp))

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    // Export Selected Student Report Card PDF
                    Button(
                      onClick = {
                        PdfExportHelper.exportStudentReportCardPdf(
                          context = context,
                          student = student,
                          memorizations = memorizationList,
                          attendanceList = attendanceList,
                          rankIndex = studentRankIndex,
                          totalStudents = students.size
                        )
                      },
                      colors = ButtonDefaults.buttonColors(
                        containerColor = GoldStar,
                        contentColor = DeepBlueNavy
                      ),
                      shape = RoundedCornerShape(10.dp),
                      modifier = Modifier
                        .weight(1f)
                        .testTag("export_single_student_pdf_button")
                    ) {
                      Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text("${student.fullName.split(" ").firstOrNull() ?: "Talebe"} PDF İndir", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    // Export All Students Report Cards Multi-Page PDF
                    OutlinedButton(
                      onClick = {
                        PdfExportHelper.exportAllStudentsReportCardsPdf(
                          context = context,
                          students = students,
                          memorizations = memorizationList,
                          attendanceList = attendanceList
                        )
                      },
                      colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                      ),
                      border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                      shape = RoundedCornerShape(10.dp),
                      modifier = Modifier
                        .weight(1f)
                        .testTag("export_all_students_pdf_button")
                    ) {
                      Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text("Tüm Sınıf (${students.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                  }
                }
              }
            }

            // 1. Student Report Card Container (Karne Tasarımı)
            item {
              Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = CanvasSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldStar.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                ) {
                  // Report Card Header Banner
                  Row(
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    StudentAvatar(
                      name = student.fullName,
                      colorIndex = student.avatarColorIndex,
                      size = 54.dp,
                      fontSize = 18
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                      ) {
                        Text(
                          text = "MEKTEB-İ İRFAN",
                          fontSize = 11.sp,
                          fontWeight = FontWeight.Bold,
                          color = GoldStar,
                          letterSpacing = 1.sp
                        )
                        Surface(
                          color = DeepBlueNavy.copy(alpha = 0.08f),
                          shape = RoundedCornerShape(6.dp)
                        ) {
                          Text(
                            text = "Sıralama: $studentRankIndex / ${students.size}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepBlueNavy,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                          )
                        }
                      }
                      Text(
                        text = student.fullName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                      )
                      Text(
                        text = "${if (student.grade.isNotBlank()) student.grade else "Talebe"} • Resmi Gelişim & Ezber Karnesi",
                        fontSize = 12.sp,
                        color = TextSecondary
                      )
                    }
                  }

                  Spacer(modifier = Modifier.height(14.dp))
                  Divider(color = BorderLight)
                  Spacer(modifier = Modifier.height(12.dp))

                  // Aktif Ezber Müfredatı Overall Progress Highlight
                  Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Text(
                          text = "Aktif Ezber Müfredatı",
                          fontSize = 13.sp,
                          fontWeight = FontWeight.Bold,
                          color = TextPrimary
                        )
                        Text(
                          text = "$totalCurriculumDone / $totalCurriculumTargetCount (%$totalCurriculumPct)",
                          fontSize = 12.sp,
                          fontWeight = FontWeight.Bold,
                          color = FeatureAttendanceGreen
                        )
                      }
                      Spacer(modifier = Modifier.height(8.dp))
                      LinearProgressIndicator(
                        progress = { (totalCurriculumPct.coerceIn(0, 100) / 100f) },
                        modifier = Modifier
                          .fillMaxWidth()
                          .height(8.dp)
                          .clip(RoundedCornerShape(4.dp)),
                        color = FeatureAttendanceGreen,
                        trackColor = Color(0xFFE2E8F0)
                      )
                    }
                  }

                  Spacer(modifier = Modifier.height(12.dp))

                  // Stats 2x2 Grid
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                  ) {
                    ReportStatBox(
                      title = "Tamamlanan Ezber",
                      value = "${selectedStudentData.completedCount} Adet",
                      color = FeatureAttendanceGreen,
                      modifier = Modifier.weight(1f)
                    )
                    ReportStatBox(
                      title = "Devamlılık Oranı",
                      value = "%${selectedStudentData.attendanceRate}",
                      color = FeatureStudentsBlue,
                      modifier = Modifier.weight(1f)
                    )
                  }

                  Spacer(modifier = Modifier.height(10.dp))

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                  ) {
                    ReportStatBox(
                      title = "Ortalama Başarı",
                      value = String.format(Locale.US, "%.1f / 5.0 ⭐", selectedStudentData.avgRating),
                      color = Color(0xFFD97706),
                      modifier = Modifier.weight(1f)
                    )
                    ReportStatBox(
                      title = "Durum / Kayıt",
                      value = student.status,
                      color = FeatureMemorizationPurple,
                      modifier = Modifier.weight(1f)
                    )
                  }

                  Spacer(modifier = Modifier.height(10.dp))

                  // Talebe Motivasyon & Teşvik Rozetleri (Karne Rozet Gösterimi)
                  val studentBadges = remember(student, memorizationList, attendanceList) {
                    BadgeHelper.calculateBadges(student, memorizationList, attendanceList)
                  }

                  KarneBadgesShowcaseSection(
                    badges = studentBadges,
                    modifier = Modifier.fillMaxWidth()
                  )

                  if (student.notes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                      color = Color(0xFFFEFCE8),
                      shape = RoundedCornerShape(12.dp),
                      border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFEF08A)),
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Icon(Icons.Default.EditNote, contentDescription = null, tint = Color(0xFFCA8A04), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                          Text("Eğitmen Değerlendirme Notu:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF854D0E))
                          Text(student.notes, fontSize = 12.sp, color = Color(0xFF451A03), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        }
                      }
                    }
                  }

                  Spacer(modifier = Modifier.height(18.dp))

                  // Primary PDF Export Button
                  Button(
                    onClick = {
                      PdfExportHelper.exportStudentReportCardPdf(
                        context = context,
                        student = student,
                        memorizations = memorizationList,
                        attendanceList = attendanceList,
                        rankIndex = studentRankIndex,
                        totalStudents = students.size
                      )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepBlueNavy),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("export_karne_pdf_main_button")
                  ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF Karne", modifier = Modifier.size(18.dp), tint = GoldStar)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Karneyi PDF Olarak İndir / Çıktı Al", fontWeight = FontWeight.Bold, color = Color.White)
                  }

                  Spacer(modifier = Modifier.height(8.dp))

                  // Multi-Page A4 Preview Dialog Button
                  var showMultiPagePreview by remember { mutableStateOf(false) }
                  OutlinedButton(
                    onClick = { showMultiPagePreview = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FeatureStudentsBlue),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FeatureStudentsBlue.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("open_multipage_preview_button")
                  ) {
                    Icon(Icons.Default.AutoStories, contentDescription = "Önizle", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Çok Sayfalı A4 Karne Önizlemesi", fontWeight = FontWeight.Bold)
                  }

                  if (showMultiPagePreview) {
                    MultiPageReportCardPreviewDialog(
                      student = student,
                      studentData = selectedStudentData,
                      memorizations = selectedStudentData.memorizations,
                      onDismiss = { showMultiPagePreview = false },
                      onExportPdf = {
                        showMultiPagePreview = false
                        PdfExportHelper.exportStudentReportCardPdf(
                          context = context,
                          student = student,
                          memorizations = memorizationList,
                          attendanceList = attendanceList,
                          rankIndex = studentRankIndex,
                          totalStudents = students.size
                        )
                      }
                    )
                  }

                  Spacer(modifier = Modifier.height(8.dp))

                  // Share Text Report Button
                  OutlinedButton(
                    onClick = {
                      shareStudentReportCard(context, selectedStudentData)
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FeatureReportsOrange),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FeatureReportsOrange.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("share_report_button")
                  ) {
                    Icon(Icons.Default.Share, contentDescription = "Paylaş", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Veliyle Metin Olarak Paylaş (WhatsApp / SMS)", fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }
        }
      }
    }

    // Student Selection Bottom Sheet for Reports Screen
    if (studentDropdownExpanded) {
      StudentBottomSheetPicker(
        students = students,
        selectedStudentId = selectedStudentData?.student?.id,
        memorizationList = memorizationList,
        title = "Karne Görüntülemek İçin Talebe Seçin",
        onDismiss = { studentDropdownExpanded = false },
        onStudentSelected = { student ->
          selectedStudentId = student.id
          studentDropdownExpanded = false
        }
      )
    }
  }

@Composable
private fun CategoryMiniChip(
  label: String,
  percent: Int,
  color: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    color = color.copy(alpha = 0.08f),
    shape = RoundedCornerShape(8.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f)),
    modifier = modifier
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
      Text(text = "%$percent", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
    }
  }
}

@Composable
private fun StatCardItem(
  title: String,
  value: String,
  subtitle: String,
  color: Color,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = CanvasSurface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(12.dp)
    ) {
      Box(
        modifier = Modifier
          .size(32.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
      }
      Spacer(modifier = Modifier.height(8.dp))
      Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
      Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color)
      Text(text = subtitle, fontSize = 10.sp, color = TextMuted)
    }
  }
}

@Composable
private fun ReportStatBox(
  title: String,
  value: String,
  color: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    color = color.copy(alpha = 0.08f),
    shape = RoundedCornerShape(12.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f)),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(10.dp)
    ) {
      Text(text = title, fontSize = 11.sp, color = TextSecondary)
      Spacer(modifier = Modifier.height(2.dp))
      Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color)
    }
  }
}

data class StudentReportData(
  val student: Student,
  val completedCount: Int,
  val inProgressCount: Int,
  val avgRating: Double,
  val attendanceRate: Int,
  val categorySummaries: List<CategoryProgressSummary>,
  val memorizations: List<MemorizationRecord>
)

private fun shareGeneralSummary(
  context: Context,
  totalStudents: Int,
  completedCount: Int,
  attendanceRate: Int,
  rankings: List<StudentReportData>
) {
  val date = SimpleDateFormat("dd.MM.yyyy", Locale("tr")).format(Date())
  val sb = StringBuilder()
  sb.append("📋 MEKTEB-İ İRFAN EZBER & GELİŞİM ÖZETİ\n")
  sb.append("Tarih: $date\n")
  sb.append("------------------------------------\n")
  sb.append("👥 Toplam Talebe: $totalStudents\n")
  sb.append("📖 Tamamlanan Ezber: $completedCount adet\n")
  sb.append("✅ Sınıf Devamlılık Oranı: %$attendanceRate\n\n")
  sb.append("🏆 EN ÇOK EZBER YAPANLAR:\n")
  rankings.take(5).forEachIndexed { index, data ->
    val catInfo = data.categorySummaries.joinToString(", ") { "${it.categoryName}: %${it.averagePercent}" }
    sb.append("${index + 1}. ${data.student.fullName} - ${data.completedCount} Tamamlanan ($catInfo)\n")
  }
  sb.append("\nMekteb-i İrfan Talebe Takip Sistemi")

  val intent = Intent(Intent.ACTION_SEND).apply {
    type = "text/plain"
    putExtra(Intent.EXTRA_SUBJECT, "Mekteb-i İrfan Ezber Özeti")
    putExtra(Intent.EXTRA_TEXT, sb.toString())
  }
  context.startActivity(Intent.createChooser(intent, "Raporu Paylaş"))
}

private fun shareStudentReportCard(
  context: Context,
  data: StudentReportData
) {
  val date = SimpleDateFormat("dd.MM.yyyy", Locale("tr")).format(Date())
  val sb = StringBuilder()
  sb.append("🌟 MEKTEB-İ İRFAN TALEBE KARNESİ 🌟\n")
  sb.append("Öğrenci: ${data.student.fullName} (${if (data.student.grade.isNotBlank()) data.student.grade else "Talebe"})\n")
  sb.append("Tarih: $date\n")
  sb.append("------------------------------------\n")
  sb.append("📊 Devamlılık Oranı: %${data.attendanceRate}\n")
  sb.append("📖 Tamamlanan Ezber: ${data.completedCount} Adet\n")
  sb.append("⭐ Başarı Puanı: ${String.format(Locale.US, "%.1f", data.avgRating)} / 5.0\n\n")
  if (data.categorySummaries.isNotEmpty()) {
    sb.append("📊 KATEGORİ İLERLEME ORANLARI:\n")
    data.categorySummaries.forEach { cat ->
      sb.append("• ${cat.categoryName}: %${cat.averagePercent} (${cat.completedCount}/${cat.officialTargetCount} Ezber)\n")
    }
    sb.append("\n")
  }
  sb.append("📌 EZBER DURUMLARI:\n")
  if (data.memorizations.isEmpty()) {
    sb.append("- Devam eden ezber kaydı bulunmamaktadır.\n")
  } else {
    data.memorizations.forEach { mem ->
      val statusText = when (mem.status) {
        "TAMAMLANDI" -> "Tamamlandı ✅"
        "DEVAM_EDIYOR" -> "Devam Ediyor (%${mem.progressPercent})"
        "TEKRAR" -> "Tekrar Edilecek"
        else -> mem.status
      }
      sb.append("• ${mem.title} (${mem.category}): $statusText\n")
      if (mem.teacherNotes.isNotEmpty()) {
        sb.append("  Hoca Notu: ${mem.teacherNotes}\n")
      }
    }
  }
  if (data.student.notes.isNotEmpty()) {
    sb.append("\n📝 Eğitmen Görüşü: ${data.student.notes}\n")
  }
  sb.append("\nMekteb-i İrfan Talebe Takip Sistemi")

  val intent = Intent(Intent.ACTION_SEND).apply {
    type = "text/plain"
    putExtra(Intent.EXTRA_SUBJECT, "${data.student.fullName} - Ezber Karnesi")
    putExtra(Intent.EXTRA_TEXT, sb.toString())
  }
  context.startActivity(Intent.createChooser(intent, "Karneyi Paylaş"))
}

/**
 * Öğrenci Karnesindeki Tüm Ezber Maddelerini Sayfa Doldukça Sonraki Sayfalara Aktaran Tablo Tasarımı
 */
@Composable
private fun PaginatedStudentMemorizationTableCard(
  student: Student,
  memorizations: List<MemorizationRecord>,
  modifier: Modifier = Modifier
) {
  var selectedFilter by remember { mutableStateOf("TÜMÜ") } // TÜMÜ, TAMAMLANDI, DEVAM_EDIYOR
  var currentPage by remember { mutableIntStateOf(0) }
  val pageSize = 6 // Bir sayfada gösterilecek standart ezber maddesi sayısı

  val filteredRecords = remember(memorizations, selectedFilter) {
    when (selectedFilter) {
      "TAMAMLANDI" -> memorizations.filter { it.status == "TAMAMLANDI" }
      "DEVAM_EDIYOR" -> memorizations.filter { it.status != "TAMAMLANDI" }
      else -> memorizations
    }
  }

  val totalPages = maxOf(1, (filteredRecords.size + pageSize - 1) / pageSize)
  val safeCurrentPage = currentPage.coerceIn(0, totalPages - 1)

  // Current page items
  val pageItems = remember(filteredRecords, safeCurrentPage, pageSize) {
    val start = safeCurrentPage * pageSize
    val end = minOf(start + pageSize, filteredRecords.size)
    if (start < filteredRecords.size) filteredRecords.subList(start, end) else emptyList()
  }

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = CanvasSurface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(16.dp)
    ) {
      // Title Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = CircleShape,
            color = FeatureMemorizationPurple.copy(alpha = 0.12f),
            modifier = Modifier.size(36.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                tint = FeatureMemorizationPurple,
                modifier = Modifier.size(20.dp)
              )
            }
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "Talebe Ezber Çizelgesi (Karne Dökümü)",
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Text(
              text = "Sayfa doldukça otomatik sonraki sayfalara aktarılır",
              fontSize = 11.sp,
              color = TextSecondary
            )
          }
        }

        // Total Count Badge
        Surface(
          color = FeatureMemorizationPurple.copy(alpha = 0.12f),
          shape = RoundedCornerShape(8.dp)
        ) {
          Text(
            text = "${memorizations.size} Kayıt",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = FeatureMemorizationPurple,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Filter Chips Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        val completedCount = memorizations.count { it.status == "TAMAMLANDI" }
        val inProgressCount = memorizations.size - completedCount

        FilterChip(
          selected = selectedFilter == "TÜMÜ",
          onClick = {
            selectedFilter = "TÜMÜ"
            currentPage = 0
          },
          label = { Text("Tümü (${memorizations.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = DeepBlueNavy,
            selectedLabelColor = Color.White
          ),
          shape = RoundedCornerShape(10.dp)
        )

        FilterChip(
          selected = selectedFilter == "TAMAMLANDI",
          onClick = {
            selectedFilter = "TAMAMLANDI"
            currentPage = 0
          },
          label = { Text("Tamamlandı ($completedCount)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = FeatureAttendanceGreen,
            selectedLabelColor = Color.White
          ),
          shape = RoundedCornerShape(10.dp)
        )

        FilterChip(
          selected = selectedFilter == "DEVAM_EDIYOR",
          onClick = {
            selectedFilter = "DEVAM_EDIYOR"
            currentPage = 0
          },
          label = { Text("Devam ($inProgressCount)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = StatusExcusedAmber,
            selectedLabelColor = Color.White
          ),
          shape = RoundedCornerShape(10.dp)
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      if (filteredRecords.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "Bu filtreye ait ezber kaydı bulunamadı.",
            fontSize = 12.5.sp,
            color = TextMuted
          )
        }
      } else {
        // Table Header
        Surface(
          color = DeepBlueNavy,
          shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("#", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.width(22.dp))
            Text("Ezber / Sure", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1.3f))
            Text("Kategori", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(0.9f))
            Text("Durum", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(0.9f))
            Text("Puan", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.width(42.dp))
          }
        }

        // Table Rows (Current Page Items)
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .border(
              width = 1.dp,
              color = BorderLight,
              shape = RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp)
            )
            .clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
        ) {
          pageItems.forEachIndexed { idx, record ->
            val globalIndex = safeCurrentPage * pageSize + idx + 1
            val rowBg = if (idx % 2 == 0) CanvasSurface else Color(0xFFF8FAFC)

            Surface(
              color = rowBg,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                // Index
                Text(
                  text = "$globalIndex",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = TextSecondary,
                  modifier = Modifier.width(22.dp)
                )

                // Title
                Column(modifier = Modifier.weight(1.3f)) {
                  Text(
                    text = record.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                  )
                  if (record.teacherNotes.isNotBlank()) {
                    Text(
                      text = record.teacherNotes,
                      fontSize = 9.5.sp,
                      color = TextMuted,
                      maxLines = 1,
                      overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                  }
                }

                // Category Badge
                val catColor = getCategoryColor(record.category)
                Surface(
                  color = catColor.copy(alpha = 0.12f),
                  shape = RoundedCornerShape(4.dp),
                  modifier = Modifier.weight(0.9f)
                ) {
                  Text(
                    text = record.category,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = catColor,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                  )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Status Badge
                val isDone = record.status == "TAMAMLANDI"
                Surface(
                  color = if (isDone) FeatureAttendanceGreen.copy(alpha = 0.12f) else StatusExcusedAmber.copy(alpha = 0.12f),
                  shape = RoundedCornerShape(4.dp),
                  modifier = Modifier.weight(0.9f)
                ) {
                  Text(
                    text = if (isDone) "✓ Tamam" else "%${record.progressPercent}",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDone) FeatureAttendanceGreen else StatusExcusedAmber,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                  )
                }

                // Rating
                Row(
                  modifier = Modifier.width(42.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "★",
                    fontSize = 11.sp,
                    color = GoldStar
                  )
                  Text(
                    text = "${record.rating}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                  )
                }
              }
            }

            if (idx < pageItems.size - 1) {
              HorizontalDivider(color = BorderLight, thickness = 0.5.dp)
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Pagination Navigation Bar (Sayfa Seçici)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Prev Button
          OutlinedButton(
            onClick = { if (safeCurrentPage > 0) currentPage = safeCurrentPage - 1 },
            enabled = safeCurrentPage > 0,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.height(32.dp)
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Önceki", fontSize = 11.sp)
          }

          // Page indicators
          Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            for (p in 0 until totalPages) {
              val isSelected = p == safeCurrentPage
              Surface(
                onClick = { currentPage = p },
                shape = RoundedCornerShape(6.dp),
                color = if (isSelected) DeepBlueNavy else CanvasBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) DeepBlueNavy else BorderLight),
                modifier = Modifier.size(28.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(
                    text = "${p + 1}",
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else TextPrimary
                  )
                }
              }
            }
          }

          // Next Button
          OutlinedButton(
            onClick = { if (safeCurrentPage < totalPages - 1) currentPage = safeCurrentPage + 1 },
            enabled = safeCurrentPage < totalPages - 1,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.height(32.dp)
          ) {
            Text("Sonraki", fontSize = 11.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(13.dp))
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Page info text
        Text(
          text = "Sayfa ${safeCurrentPage + 1} / $totalPages • Gösterilen: ${pageItems.size} (Toplam ${filteredRecords.size} Ezber)",
          fontSize = 10.5.sp,
          color = TextSecondary,
          textAlign = androidx.compose.ui.text.style.TextAlign.Center,
          modifier = Modifier.fillMaxWidth()
        )
      }
    }
  }
}

/**
 * A4 Çok Sayfalı Resmi Karne Önizleme Penceresi
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MultiPageReportCardPreviewDialog(
  student: Student,
  studentData: StudentReportData,
  memorizations: List<MemorizationRecord>,
  onDismiss: () -> Unit,
  onExportPdf: () -> Unit
) {
  var selectedPreviewPage by remember { mutableIntStateOf(0) }
  val page1ItemsLimit = 11
  val remainingItemsPageLimit = 18

  val remainingList = if (memorizations.size > page1ItemsLimit) memorizations.subList(page1ItemsLimit, memorizations.size) else emptyList()
  val extraPagesCount = if (remainingList.isEmpty()) 0 else (remainingList.size + remainingItemsPageLimit - 1) / remainingItemsPageLimit
  val totalPdfPages = 1 + extraPagesCount

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "📄 Çok Sayfalı A4 Karne Önizleme",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = DeepBlueNavy
          )
          Text(
            text = "${student.fullName} • Toplam $totalPdfPages Sayfa",
            fontSize = 11.sp,
            color = TextSecondary
          )
        }
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 500.dp)
      ) {
        // Page Tabs
        PrimaryTabRow(
          selectedTabIndex = selectedPreviewPage,
          containerColor = CanvasBackground,
          contentColor = DeepBlueNavy,
          modifier = Modifier.fillMaxWidth()
        ) {
          for (p in 0 until totalPdfPages) {
            Tab(
              selected = selectedPreviewPage == p,
              onClick = { selectedPreviewPage = p },
              text = {
                Text(
                  text = if (p == 0) "1. Sayfa (Ana Karne)" else "${p + 1}. Sayfa (Ezber Tablosu)",
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp
                )
              }
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // A4 Visual Sheet Container
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = CanvasSurface),
          border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldStar),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
        ) {
          LazyColumn(
            modifier = Modifier
              .fillMaxSize()
              .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            if (selectedPreviewPage == 0) {
              // Page 1 Content
              item {
                // Header Banner
                Surface(
                  color = DeepBlueNavy,
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                  ) {
                    Text("MEKTEB-İ İRFAN", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GoldStar)
                    Text("TALEBE EZBER & GELİŞİM KARNESİ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("RESMİ DÖNEM DEĞERLENDİRME BELGESİ", fontSize = 8.5.sp, color = Color.White.copy(alpha = 0.8f))
                  }
                }
              }

              item {
                // Student Profile Card
                Surface(
                  color = Color(0xFFF8FAFC),
                  shape = RoundedCornerShape(8.dp),
                  border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    StudentAvatar(
                      name = student.fullName,
                      colorIndex = student.avatarColorIndex,
                      size = 36.dp,
                      fontSize = 13
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                      Text(student.fullName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                      Text("${student.grade.ifBlank { "Talebe" }} • Veli: ${student.parentName.ifBlank { "—" }}", fontSize = 10.5.sp, color = TextSecondary)
                    }
                  }
                }
              }

              item {
                // KPI Stats Grid
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  ReportStatBox("Tamamlanan", "${studentData.completedCount} Ezber", FeatureAttendanceGreen, Modifier.weight(1f))
                  ReportStatBox("Devamlılık", "%${studentData.attendanceRate}", FeatureStudentsBlue, Modifier.weight(1f))
                  ReportStatBox("Ortalama", String.format(Locale.US, "%.1f ⭐", studentData.avgRating), GoldStar, Modifier.weight(1f))
                }
              }

              item {
                // First 11 items
                Text("📌 İlk Sayfa Ezber Kayıtları (1 - ${minOf(page1ItemsLimit, memorizations.size)}):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DeepBlueNavy)
              }

              val p1List = memorizations.take(page1ItemsLimit)
              items(p1List) { mem ->
                Surface(
                  color = CanvasBackground,
                  shape = RoundedCornerShape(6.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Text(mem.title, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(if (mem.status == "TAMAMLANDI") "✓ Tamamlandı" else "%${mem.progressPercent}", fontSize = 10.sp, color = if (mem.status == "TAMAMLANDI") FeatureAttendanceGreen else StatusExcusedAmber, fontWeight = FontWeight.Bold)
                  }
                }
              }

              if (memorizations.size > page1ItemsLimit) {
                item {
                  Surface(
                    color = FeatureMemorizationPurple.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Text(
                      text = "➡️ Kalan ${memorizations.size - page1ItemsLimit} ezber maddesi Sayfa 2'de devam etmektedir.",
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      color = FeatureMemorizationPurple,
                      modifier = Modifier.padding(6.dp)
                    )
                  }
                }
              }
            } else {
              // Page 2+ Content (Continuation table)
              val startIdx = page1ItemsLimit + (selectedPreviewPage - 1) * remainingItemsPageLimit
              val endIdx = minOf(startIdx + remainingItemsPageLimit, memorizations.size)
              val pExtraList = if (startIdx < memorizations.size) memorizations.subList(startIdx, endIdx) else emptyList()

              item {
                Surface(
                  color = DeepBlueNavy,
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text("EZBER LİSTESİ DEVAMI", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldStar)
                    Text("Sayfa ${selectedPreviewPage + 1} / $totalPdfPages", fontSize = 10.sp, color = Color.White)
                  }
                }
              }

              item {
                Text(
                  text = "Öğrenci: ${student.fullName} • Ezber Maddeleri (${startIdx + 1} - $endIdx):",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary
                )
              }

              items(pExtraList) { mem ->
                Surface(
                  color = CanvasBackground,
                  shape = RoundedCornerShape(6.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Text(mem.title, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(if (mem.status == "TAMAMLANDI") "✓ Tamamlandı" else "%${mem.progressPercent}", fontSize = 10.sp, color = if (mem.status == "TAMAMLANDI") FeatureAttendanceGreen else StatusExcusedAmber, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onExportPdf,
        colors = ButtonDefaults.buttonColors(containerColor = DeepBlueNavy),
        shape = RoundedCornerShape(10.dp)
      ) {
        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = GoldStar, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Tüm Sayfaları PDF İndir ($totalPdfPages Sayfa)", fontWeight = FontWeight.Bold, color = Color.White)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Kapat", color = TextSecondary)
      }
    },
    shape = RoundedCornerShape(20.dp),
    containerColor = CanvasSurface
  )
}

private fun getCategoryColor(category: String): Color {
  return when {
    category.contains("Kur'an", ignoreCase = true) || category.contains("Kuran", ignoreCase = true) -> FeatureAttendanceGreen
    category.contains("Risale", ignoreCase = true) -> FeatureMemorizationPurple
    category.contains("Dua", ignoreCase = true) || category.contains("Sure", ignoreCase = true) -> FeatureStudentsBlue
    category.contains("Hadis", ignoreCase = true) -> Color(0xFFD97706)
    category.contains("İlmihal", ignoreCase = true) -> Color(0xFF0284C7)
    else -> Color(0xFF64748B)
  }
}

