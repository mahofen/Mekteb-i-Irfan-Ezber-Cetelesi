package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.auth.UserSession
import com.example.data.model.AttendanceRecord
import com.example.data.model.MemorizationRecord
import com.example.data.model.Student
import com.example.ui.components.GoldSubtleBorderGradient
import com.example.ui.components.SeljukStarBox
import com.example.ui.components.StudentAvatar
import com.example.ui.components.TezhipCard
import com.example.ui.theme.*

/**
 * 🕌 GELENEKSEL TEZHİP & SELÇUKLU ZARAFETİ ANA MENÜSÜ
 * Altın varak yaldızlar, Selçuklu 8 köşeli yıldızları,
 * Lapis Lazuli laciverti ve aharli parşömen zemin işçiliği.
 */
@Composable
fun MainMenuScreen(
  students: List<Student>,
  attendanceList: List<AttendanceRecord>,
  memorizationList: List<MemorizationRecord>,
  todayDate: String,
  allDuties: List<com.example.data.model.DailyDutyRecord> = emptyList(),
  onNavigateStudents: () -> Unit,
  onNavigateAttendance: () -> Unit,
  onNavigateSchedule: () -> Unit = {},
  onNavigateMemorization: () -> Unit,
  onNavigateDevelopment: () -> Unit,
  onNavigateReports: () -> Unit,
  onNavigateSettings: () -> Unit,
  onNavigateAbout: () -> Unit,
  onNavigateStudentPortal: (() -> Unit)? = null,
  userSession: UserSession? = null,
  onLogoutTeacher: (() -> Unit)? = null
) {
  val context = LocalContext.current
  val totalStudents = students.size
  val todayAttendance = attendanceList.filter { it.date == todayDate }
  val presentCount = todayAttendance.count { it.status == "GELDI" }
  val inProgressMemorization = memorizationList.count { it.status == "DEVAM_EDIYOR" }
  val completedMemorization = memorizationList.count { it.status == "TAMAMLANDI" }

  val todayDuties = remember(allDuties, todayDate) { allDuties.filter { it.date == todayDate } }
  val fullPrayersCount = remember(todayDuties) {
    todayDuties.count { it.fajr && it.dhuhr && it.asr && it.maghrib && it.isha }
  }
  val dutyCompletionPercent = if (totalStudents > 0) {
    ((fullPrayersCount.toFloat() / totalStudents.toFloat()) * 100).toInt()
  } else 0

  val completionPercent = if (memorizationList.isNotEmpty()) {
    ((completedMemorization.toFloat() / memorizationList.size.toFloat()) * 100).toInt()
  } else 0

  val attendanceRate = if (totalStudents > 0) {
    ((presentCount.toFloat() / totalStudents.toFloat()) * 100).toInt()
  } else 100

  Scaffold(
    containerColor = CanvasBackground
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 36.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // -------------------------------------------------------------
      // 1. TEZHİP & HAT SANATI BAŞLIĞI (Brand & Selçuklu Varak)
      // -------------------------------------------------------------
      item {
        val displayDate = remember(todayDate) {
          try {
            val inFmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val outFmt = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale("tr"))
            val parsed = inFmt.parse(todayDate)
            if (parsed != null) outFmt.format(parsed) else todayDate
          } catch (e: Exception) {
            todayDate
          }
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            // Selçuklu 8 Köşeli Varak Yıldızı Logo İkonu
            SeljukStarBox(
              size = 44.dp,
              backgroundColor = DeepBlueNavy
            ) {
              Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = TezhipGoldLight,
                modifier = Modifier.size(20.dp)
              )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = "MEKTEB-İ İRFAN",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Black,
                  color = TezhipGoldDark,
                  letterSpacing = 1.3.sp
                )
              }
              Text(
                text = "Ezber Çetelesi",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = DeepBlueNavy,
                letterSpacing = (-0.5).sp
              )
            }
          }

          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Tarih Rozeti (Gün Ay Yıl formatında)
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = CanvasSurface,
              border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
              shadowElevation = 2.dp
            ) {
              Text(
                text = displayDate,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
              )
            }

            // Hakkında Butonu
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = CanvasSurface,
              border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
              shadowElevation = 2.dp,
              onClick = onNavigateAbout,
              modifier = Modifier
                .size(36.dp)
                .testTag("main_menu_about_button")
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Info,
                  contentDescription = "Hakkında",
                  tint = DeepBlueNavy,
                  modifier = Modifier.size(18.dp)
                )
              }
            }

            // Ayarlar Butonu
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = CanvasSurface,
              border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
              shadowElevation = 2.dp,
              onClick = onNavigateSettings,
              modifier = Modifier
                .size(36.dp)
                .testTag("main_menu_settings_button")
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Settings,
                  contentDescription = "Ayarlar",
                  tint = DeepBlueNavy,
                  modifier = Modifier.size(18.dp)
                )
              }
            }

            // Çıkış / Kilitle Butonu
            if (onLogoutTeacher != null) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = CanvasSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                shadowElevation = 2.dp,
                onClick = onLogoutTeacher,
                modifier = Modifier
                  .size(36.dp)
                  .testTag("main_menu_logout_button")
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Paneli Kilitle / Çıkış Yap",
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(18.dp)
                  )
                }
              }
            }
          }
        }
      }

      // Aktif Eğitmen Durum Rozeti
      if (userSession != null && userSession.isLoggedIn) {
        item {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            color = DeepBlueNavy.copy(alpha = 0.07f),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DeepBlueNavy.copy(alpha = 0.18f))
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(DeepBlueNavy),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                  )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text(
                    text = "Giriş Yapan: ${userSession.displayName.ifBlank { "Yetkili Hoca" }}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepBlueNavy
                  )
                  Text(
                    text = "Yetki: ${userSession.role}",
                    fontSize = 10.5.sp,
                    color = TextSecondary
                  )
                }
              }

              if (onLogoutTeacher != null) {
                TextButton(
                  onClick = onLogoutTeacher,
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                  modifier = Modifier.height(28.dp)
                ) {
                  Text("Paneli Kilitle", fontSize = 11.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }

      // Talebe Portalı Geçiş Bannerı
      if (onNavigateStudentPortal != null) {
        item {
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .clickable { onNavigateStudentPortal() },
            color = EmeraldGreen.copy(alpha = 0.12f),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, EmeraldGreen.copy(alpha = 0.4f))
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                SeljukStarBox(
                  size = 36.dp,
                  backgroundColor = EmeraldGreen
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = "Talebe Portalı",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                  )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                  Text(
                    text = "Talebe Portalı & Öğrenci Girişi",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldGreen
                  )
                  Text(
                    text = "Talebenin ezber, görev ve gelişim takip ekranına geç",
                    fontSize = 11.5.sp,
                    color = TextSecondary
                  )
                }
              }

              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Geçiş",
                tint = EmeraldGreen,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }
      }

      // -------------------------------------------------------------
      // 1.5. SELÇUKLU TEZHİP GENİŞ KART: DERS PROGRAMI & ÇİZELGE
      // -------------------------------------------------------------
      item {
        TezhipCard(
          shape = RoundedCornerShape(20.dp),
          containerColor = CanvasSurface,
          elevation = 3.dp,
          showCornerOrnaments = true,
          onClick = onNavigateSchedule,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("menu_card_schedule")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically
            ) {
              SeljukStarBox(
                size = 40.dp,
                backgroundColor = DeepBlueNavy
              ) {
                Icon(
                  imageVector = Icons.Default.CalendarMonth,
                  contentDescription = null,
                  tint = TezhipGoldLight,
                  modifier = Modifier.size(20.dp)
                )
              }

              Spacer(modifier = Modifier.width(14.dp))

              Column {
                Text(
                  text = "Ders Programı & Yoklama",
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Bold,
                  color = DeepBlueNavy
                )
                Text(
                  text = "Günlük ders saatleri, sınıflar & canlı yoklama",
                  fontSize = 11.5.sp,
                  color = TextSecondary
                )
              }
            }

            Surface(
              color = DeepBlueNavy.copy(alpha = 0.10f),
              shape = RoundedCornerShape(12.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, DeepBlueNavy.copy(alpha = 0.3f))
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                  contentDescription = null,
                  tint = DeepBlueNavy,
                  modifier = Modifier.size(16.dp)
                )
              }
            }
          }
        }
      }

      // -------------------------------------------------------------
      // 2. SELÇUKLU TEZHİP ÇİFT KART 1: EZBER & TALEBELER (EŞİT BOYUT)
      // -------------------------------------------------------------
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // Kart: Ezber (Mor / Altın Tezhip Varak)
          SeljukTezhipMenuCard(
            title = "Ezber",
            metric = if (completedMemorization > 0) "$completedMemorization" else "$inProgressMemorization",
            metricUnit = if (completedMemorization > 0) "Teslim" else "Aktif Ders",
            accentColor = FeatureMemorizationPurple,
            icon = Icons.AutoMirrored.Filled.MenuBook,
            testTag = "menu_card_memorization",
            onClick = onNavigateMemorization,
            modifier = Modifier.weight(1f)
          ) {
            Column(modifier = Modifier.fillMaxWidth()) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Tamamlanma",
                  fontSize = 10.5.sp,
                  fontWeight = FontWeight.Medium,
                  color = TextSecondary
                )
                Text(
                  text = "%$completionPercent",
                  fontSize = 11.5.sp,
                  fontWeight = FontWeight.Black,
                  color = FeatureMemorizationPurple
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(6.dp)
                  .clip(RoundedCornerShape(100.dp))
                  .background(FeatureMemorizationPurple.copy(alpha = 0.15f))
              ) {
                Box(
                  modifier = Modifier
                    .fillMaxWidth((completionPercent / 100f).coerceIn(0.05f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(100.dp))
                    .background(FeatureMemorizationPurple)
                )
              }
            }
          }

          // Kart: Talebeler (Lapis Lazuli & Altın)
          SeljukTezhipMenuCard(
            title = "Talebeler",
            metric = "$totalStudents",
            metricUnit = "Kayıtlı",
            accentColor = DeepBlueNavy,
            icon = Icons.Default.People,
            testTag = "menu_card_students",
            onClick = onNavigateStudents,
            modifier = Modifier.weight(1f)
          ) {
            Column(modifier = Modifier.fillMaxWidth()) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Aktif Sınıf",
                  fontSize = 10.5.sp,
                  fontWeight = FontWeight.Medium,
                  color = TextSecondary
                )
                Text(
                  text = "$totalStudents Talebe",
                  fontSize = 11.5.sp,
                  fontWeight = FontWeight.Black,
                  color = DeepBlueNavy
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(6.dp)
                  .clip(RoundedCornerShape(100.dp))
                  .background(DeepBlueNavy.copy(alpha = 0.15f))
              ) {
                Box(
                  modifier = Modifier
                    .fillMaxWidth(if (totalStudents > 0) 1f else 0.05f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(100.dp))
                    .background(DeepBlueNavy)
                )
              }
            }
          }
        }
      }

      // -------------------------------------------------------------
      // 3. SELÇUKLU TEZHİP ÇİFT KART 2: YOKLAMA & GELİŞİM & HIZ (EŞİT BOYUT)
      // -------------------------------------------------------------
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // Kart: Günlük Görevler (Zümrüt Yeşili & Altın)
          SeljukTezhipMenuCard(
            title = "Günlük Görev",
            metric = "$fullPrayersCount",
            metricUnit = "/ $totalStudents 5 Vakit",
            accentColor = FeatureAttendanceGreen,
            icon = Icons.Default.TaskAlt,
            testTag = "menu_card_attendance",
            onClick = onNavigateAttendance,
            modifier = Modifier.weight(1f)
          ) {
            Column(modifier = Modifier.fillMaxWidth()) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "5 Vakit İntizamı",
                  fontSize = 10.5.sp,
                  fontWeight = FontWeight.Medium,
                  color = TextSecondary
                )
                Text(
                  text = "%$dutyCompletionPercent",
                  fontSize = 11.5.sp,
                  fontWeight = FontWeight.Black,
                  color = FeatureAttendanceGreen
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(6.dp)
                  .clip(RoundedCornerShape(100.dp))
                  .background(FeatureAttendanceGreen.copy(alpha = 0.15f))
              ) {
                Box(
                  modifier = Modifier
                    .fillMaxWidth((dutyCompletionPercent / 100f).coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(100.dp))
                    .background(FeatureAttendanceGreen)
                )
              }
            }
          }

          // Kart: Gelişim Analizi (Firuze Çini & Altın)
          SeljukTezhipMenuCard(
            title = "Gelişim & Hız",
            metric = "",
            metricUnit = "",
            accentColor = FeatureDevelopmentTeal,
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            testTag = "menu_card_development",
            onClick = onNavigateDevelopment,
            modifier = Modifier.weight(1f)
          ) {
            Column(modifier = Modifier.fillMaxWidth()) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(28.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
              ) {
                val sparkHeights = listOf(10.dp, 16.dp, 12.dp, 22.dp, 18.dp, 26.dp)
                sparkHeights.forEachIndexed { idx, h ->
                  Box(
                    modifier = Modifier
                      .weight(1f)
                      .height(h)
                      .clip(RoundedCornerShape(100.dp))
                      .background(
                        if (idx == sparkHeights.lastIndex) {
                          FeatureDevelopmentTeal
                        } else {
                          FeatureDevelopmentTeal.copy(alpha = 0.3f)
                        }
                      )
                  )
                }
              }
            }
          }
        }
      }

      // -------------------------------------------------------------
      // 4. SELÇUKLU TEZHİP GENİŞ KART: TALEBE KARNE
      // -------------------------------------------------------------
      item {
        TezhipCard(
          shape = RoundedCornerShape(20.dp),
          containerColor = CanvasSurface,
          elevation = 3.dp,
          showCornerOrnaments = true,
          onClick = onNavigateReports,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("menu_card_reports")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically
            ) {
              SeljukStarBox(
                size = 40.dp,
                backgroundColor = FeatureReportsOrange
              ) {
                Icon(
                  imageVector = Icons.Default.Assessment,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(20.dp)
                )
              }

              Spacer(modifier = Modifier.width(14.dp))

              Text(
                text = "Talebe Karne",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
              )
            }

            Surface(
              color = FeatureReportsOrange.copy(alpha = 0.12f),
              shape = RoundedCornerShape(12.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, FeatureReportsOrange.copy(alpha = 0.4f))
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                  contentDescription = null,
                  tint = FeatureReportsOrange,
                  modifier = Modifier.size(16.dp)
                )
              }
            }
          }
        }
      }
    }
  }
}

/**
 * Geleneksel Tezhip & Selçuklu Motifli Menü Kartı (Eşit Boyut ve Yükseklik)
 */
@Composable
private fun SeljukTezhipMenuCard(
  title: String,
  metric: String,
  metricUnit: String,
  accentColor: Color,
  icon: ImageVector,
  testTag: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  contentPreview: @Composable () -> Unit
) {
  TezhipCard(
    shape = RoundedCornerShape(20.dp),
    containerColor = CanvasSurface,
    elevation = 2.5.dp,
    showCornerOrnaments = false,
    onClick = onClick,
    modifier = modifier
      .height(135.dp)
      .testTag(testTag)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(2.dp),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = title,
          fontSize = 15.5.sp,
          fontWeight = FontWeight.ExtraBold,
          color = TextPrimary
        )

        SeljukStarBox(
          size = 30.dp,
          backgroundColor = accentColor
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(15.dp)
          )
        }
      }

      if (metric.isNotBlank()) {
        Row(
          verticalAlignment = Alignment.Bottom
        ) {
          Text(
            text = metric,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = accentColor
          )
          if (metricUnit.isNotBlank()) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = metricUnit,
              fontSize = 11.5.sp,
              fontWeight = FontWeight.Bold,
              color = TextSecondary,
              modifier = Modifier.padding(bottom = 2.dp)
            )
          }
        }
      } else {
        Spacer(modifier = Modifier.height(1.dp))
      }

      contentPreview()
    }
  }
}

