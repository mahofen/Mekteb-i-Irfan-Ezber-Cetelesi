package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.auth.StudentSession
import com.example.data.model.DailyDutyRecord
import com.example.data.model.MemorizationRecord
import com.example.data.model.Student
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.StudentProgressStats

@Composable
fun StudentDashboardScreen(
  student: Student?,
  studentSession: StudentSession,
  stats: StudentProgressStats,
  activeMemorizations: List<MemorizationRecord>,
  completedMemorizations: List<MemorizationRecord>,
  recentDuties: List<DailyDutyRecord>,
  todayDate: String,
  onNavigateToMemorization: (isUpcomingTab: Boolean) -> Unit,
  onNavigateToDuties: () -> Unit,
  onNavigateToProfile: () -> Unit,
  onLogout: () -> Unit,
  viewModel: AppViewModel,
  modifier: Modifier = Modifier
) {
  val scrollState = rememberScrollState()
  val currentStudent = student ?: Student(fullName = studentSession.studentName, grade = studentSession.grade)
  val avatarColor = getAvatarColor(currentStudent.avatarColorIndex)

  val todayDuty = recentDuties.find { it.date == todayDate } ?: DailyDutyRecord(studentId = currentStudent.id, date = todayDate)

  Scaffold(
    containerColor = CanvasBackground,
    contentWindowInsets = WindowInsets(0, 0, 0, 0)
  ) { paddingValues ->
    Column(
      modifier = modifier
        .fillMaxSize()
        .padding(paddingValues)
        .verticalScroll(scrollState)
    ) {
      // 🕌 Top Hero Header with Ottoman / Seljuk Motif
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(EmeraldGradient)
          .padding(start = 20.dp, end = 20.dp, top = 36.dp, bottom = 24.dp)
      ) {
        Column {
          // Top Row: App Title & Logout
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              SeljukStarBox(
                size = 36.dp,
                backgroundColor = TezhipGold
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.MenuBook,
                  contentDescription = "Mekteb-i İrfan",
                  tint = DarkSlateNavy,
                  modifier = Modifier.size(20.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "Mekteb-i İrfan",
                  fontSize = 17.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
                Text(
                  text = "Talebe Ezber & Gelişim Takip Portalı",
                  fontSize = 11.5.sp,
                  color = LightGoldAccent,
                  fontWeight = FontWeight.Medium
                )
              }
            }

            IconButton(
              onClick = onLogout,
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f))
                .testTag("student_logout_button")
            ) {
              Icon(
                imageVector = Icons.Default.Logout,
                contentDescription = "Çıkış Yap",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(18.dp))

          // Student Identity Banner (Görüntüleme Amaçlı)
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onNavigateToProfile() },
            color = Color.White.copy(alpha = 0.12f),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(avatarColor)
                    .border(2.dp, TezhipGold, CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = currentStudent.fullName.take(1).uppercase(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                  )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                  Text(
                    text = currentStudent.fullName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                  )
                  Text(
                    text = "${currentStudent.grade} • No: ${studentSession.username}",
                    fontSize = 12.5.sp,
                    color = LightGoldAccent
                  )
                }
              }

              // Profil Oku
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Profil Detayı",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }
      }

      // 💡 Bilgilendirme / Rehber Bannerı
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        color = FeatureMemorizationPurple.copy(alpha = 0.08f),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, FeatureMemorizationPurple.copy(alpha = 0.3f))
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Bilgi",
            tint = FeatureMemorizationPurple,
            modifier = Modifier.size(22.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "Ezberinizi hazırladığınızda ders halkasında hocanıza dinletiniz. Görev ve ezber değerlendirmeleriniz hocanız tarafından güncellenmektedir.",
            fontSize = 12.sp,
            color = TextPrimary,
            lineHeight = 16.sp
          )
        }
      }

      // 📊 Quick Metric Cards (2x2 Grid)
      Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Card 1: Completed Memorizations (Geçmiş Ezber)
          MetricCard(
            title = "Geçmiş Ezberler",
            value = "${stats.completedMemorizationsCount}",
            subtitle = "Teslim edilen ezber",
            icon = Icons.Default.CheckCircle,
            badgeColor = FeatureAttendanceGreen,
            modifier = Modifier
              .weight(1f)
              .clickable { onNavigateToMemorization(false) }
              .testTag("metric_completed_memorizations")
          )

          // Card 2: Upcoming & Active Memorizations (Gelecek Ezber)
          MetricCard(
            title = "Gelecek & Aktif",
            value = "${stats.activeMemorizationsCount}",
            subtitle = "Sıradaki ezber hedefi",
            icon = Icons.AutoMirrored.Filled.MenuBook,
            badgeColor = FeatureMemorizationPurple,
            modifier = Modifier
              .weight(1f)
              .clickable { onNavigateToMemorization(true) }
              .testTag("metric_active_memorizations")
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Card 3: Daily Duty Status
          MetricCard(
            title = "Günlük Vazifeler",
            value = "%${stats.weeklyDutyScorePercent}",
            subtitle = "Hoca onaylı vird puanı",
            icon = Icons.Default.AssignmentTurnedIn,
            badgeColor = FeatureDevelopmentTeal,
            modifier = Modifier
              .weight(1f)
              .clickable { onNavigateToDuties() }
              .testTag("metric_daily_duties")
          )

          // Card 4: Average Rating
          MetricCard(
            title = "Ezber Başarısı",
            value = "⭐ ${stats.averageRating}",
            subtitle = "Ortalama hoca notu",
            icon = Icons.Default.Star,
            badgeColor = TezhipGold,
            modifier = Modifier
              .weight(1f)
              .clickable { onNavigateToProfile() }
              .testTag("metric_total_rating")
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // ⚡ EZBER HIZI & SON 1 AYLIK GELİŞİM GRAFİK BİLEŞENİ
      Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        StudentMemorizationPaceChart(
          student = currentStudent,
          records = completedMemorizations + activeMemorizations
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // 📖 GELECEK & SIRADAKİ EZBERLERİM (Upcoming / In Progress)
      Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            SeljukStarBox(
              size = 28.dp,
              backgroundColor = FeatureMemorizationPurple
            ) {
              Icon(
                imageVector = Icons.Default.HourglassTop,
                contentDescription = "Gelecek Ezberler",
                tint = Color.White,
                modifier = Modifier.size(15.dp)
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Gelecek & Sıradaki Ezberlerim",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
          }

          TextButton(
            onClick = { onNavigateToMemorization(true) },
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = "Tümü (${activeMemorizations.size})",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = FeatureMemorizationPurple
            )
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = "Tümü",
              tint = FeatureMemorizationPurple,
              modifier = Modifier.size(14.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (activeMemorizations.isEmpty()) {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            color = CanvasSurface,
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Icon(
                imageVector = Icons.Default.CheckCircleOutline,
                contentDescription = "Ezber Yok",
                tint = FeatureAttendanceGreen,
                modifier = Modifier.size(36.dp)
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "Tüm aktif ezberleriniz tamamlanmış!",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
              )
              Text(
                text = "Yeni ezber hedefleriniz hocanız tarafından tanımlanacaktır.",
                fontSize = 11.5.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
              )
            }
          }
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            activeMemorizations.take(3).forEach { record ->
              StudentActiveMemorizationCard(
                record = record,
                onClick = { onNavigateToMemorization(true) }
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // 🕌 GÜNLÜK VAZİFE VE VİRD DURUMU (HOCA ONAYLI GÖRÜNÜM)
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp)
          .shadow(4.dp, RoundedCornerShape(18.dp)),
        color = CanvasSurface,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, GoldSubtleBorderGradient)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              SeljukStarBox(
                size = 28.dp,
                backgroundColor = FeatureAttendanceGreen
              ) {
                Icon(
                  imageVector = Icons.Default.TaskAlt,
                  contentDescription = "Günün Görevleri",
                  tint = Color.White,
                  modifier = Modifier.size(15.dp)
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = "Bugünkü Vazife & Vird Çetelesi",
                  fontSize = 14.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary
                )
                Text(
                  text = "Eğitmen Kaydı ($todayDate)",
                  fontSize = 11.sp,
                  color = TextSecondary
                )
              }
            }

            TextButton(
              onClick = onNavigateToDuties,
              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = "Detay",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = EmeraldGreen
              )
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Detay",
                tint = EmeraldGreen,
                modifier = Modifier.size(14.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // 5 Daily Prayers Status Display (Read-Only)
          Text(
            text = "5 Vakit Namaz Durumu",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
          )

          Spacer(modifier = Modifier.height(6.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            PrayerStatusChip(name = "Sabah", isDone = todayDuty.fajr, modifier = Modifier.weight(1f))
            PrayerStatusChip(name = "Öğle", isDone = todayDuty.dhuhr, modifier = Modifier.weight(1f))
            PrayerStatusChip(name = "İkindi", isDone = todayDuty.asr, modifier = Modifier.weight(1f))
            PrayerStatusChip(name = "Akşam", isDone = todayDuty.maghrib, modifier = Modifier.weight(1f))
            PrayerStatusChip(name = "Yatsı", isDone = todayDuty.isha, modifier = Modifier.weight(1f))
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Reading & Dhikr Status Summary
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            DutySummaryBadge(
              title = "Kur'an Tilaveti",
              value = "${todayDuty.quranPages} Sayfa",
              isCompleted = todayDuty.quranPages > 0,
              modifier = Modifier.weight(1f)
            )
            DutySummaryBadge(
              title = "Risale-i Nur",
              value = "${todayDuty.risalePages} Sayfa",
              isCompleted = todayDuty.risalePages > 0,
              modifier = Modifier.weight(1f)
            )
            DutySummaryBadge(
              title = "Salavat-ı Şerife",
              value = "${todayDuty.salavatCount} Adet",
              isCompleted = todayDuty.salavatCount > 0,
              modifier = Modifier.weight(1f)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // 🏆 GEÇMİŞ & YAPILAN EZBERLER (Recently Completed Archives)
      Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            SeljukStarBox(
              size = 28.dp,
              backgroundColor = FeatureAttendanceGreen
            ) {
              Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = "Yapılan Ezberler",
                tint = Color.White,
                modifier = Modifier.size(15.dp)
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "Yapılan & Teslim Edilen Ezberler",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
              )
              Text(
                text = "${completedMemorizations.size} Hoca Onaylı Ezber",
                fontSize = 11.sp,
                color = FeatureAttendanceGreen,
                fontWeight = FontWeight.SemiBold
              )
            }
          }

          TextButton(
            onClick = { onNavigateToMemorization(false) },
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = "Tümü (${completedMemorizations.size})",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = FeatureAttendanceGreen
            )
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = "Tümü",
              tint = FeatureAttendanceGreen,
              modifier = Modifier.size(14.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (completedMemorizations.isEmpty()) {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            color = CanvasSurface,
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = "Henüz tamamlanmış ezber kaydı bulunmamaktadır.",
                fontSize = 12.5.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Ders halkasında hocanıza dinlettiğiniz ezberler burada listelenir.",
                fontSize = 11.sp,
                color = TextMuted,
                textAlign = TextAlign.Center
              )
            }
          }
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            completedMemorizations.take(5).forEach { record ->
              StudentCompletedMemorizationCard(
                record = record,
                onClick = { onNavigateToMemorization(false) }
              )
            }

            if (completedMemorizations.size > 5) {
              Surface(
                onClick = { onNavigateToMemorization(false) },
                color = FeatureAttendanceGreen.copy(alpha = 0.08f),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, FeatureAttendanceGreen.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                  horizontalArrangement = Arrangement.Center,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "Daha Önceki ${completedMemorizations.size - 5} Tamamlanan Ezberi Gör",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = FeatureAttendanceGreen
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = FeatureAttendanceGreen,
                    modifier = Modifier.size(14.dp)
                  )
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(28.dp))
    }
  }
}

@Composable
fun MetricCard(
  title: String,
  value: String,
  subtitle: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  badgeColor: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier.shadow(3.dp, RoundedCornerShape(16.dp)),
    color = CanvasSurface,
    shape = RoundedCornerShape(16.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
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
        Text(
          text = title,
          fontSize = 12.5.sp,
          fontWeight = FontWeight.SemiBold,
          color = TextSecondary
        )

        Box(
          modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(badgeColor.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = icon,
            contentDescription = title,
            tint = badgeColor,
            modifier = Modifier.size(16.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = value,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
      )

      Spacer(modifier = Modifier.height(2.dp))

      Text(
        text = subtitle,
        fontSize = 10.5.sp,
        color = TextSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
fun StudentActiveMemorizationCard(
  record: MemorizationRecord,
  onClick: () -> Unit
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .clickable { onClick() },
    color = CanvasSurface,
    shape = RoundedCornerShape(14.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, FeatureMemorizationPurple.copy(alpha = 0.35f))
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
          Surface(
            color = FeatureMemorizationPurple.copy(alpha = 0.12f),
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(
              text = record.category,
              fontSize = 10.5.sp,
              fontWeight = FontWeight.Bold,
              color = FeatureMemorizationPurple,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }

          Spacer(modifier = Modifier.width(6.dp))

          Text(
            text = record.title,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        Surface(
          color = TezhipGold.copy(alpha = 0.15f),
          shape = RoundedCornerShape(6.dp)
        ) {
          Text(
            text = "Hazırlanıyor",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = DarkSlateNavy,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Progress bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        LinearProgressIndicator(
          progress = { (record.progressPercent / 100f).coerceIn(0f, 1f) },
          modifier = Modifier
            .weight(1f)
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp)),
          color = FeatureMemorizationPurple,
          trackColor = CanvasBackground
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
          text = "%${record.progressPercent}",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = FeatureMemorizationPurple
        )
      }

      if (record.teacherNotes.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Hoca Notu: ${record.teacherNotes}",
          fontSize = 11.sp,
          color = TextSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}

@Composable
fun StudentCompletedMemorizationCard(
  record: MemorizationRecord,
  onClick: () -> Unit
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .clickable { onClick() },
    color = CanvasSurface,
    shape = RoundedCornerShape(14.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, FeatureAttendanceGreen.copy(alpha = 0.35f))
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f)
      ) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(FeatureAttendanceGreen.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Verified,
            contentDescription = "Tamamlandı",
            tint = FeatureAttendanceGreen,
            modifier = Modifier.size(20.dp)
          )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column {
          Text(
            text = record.title,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "${record.category} • Teslim: ${record.date.ifBlank { "Tamamlandı" }}",
              fontSize = 11.sp,
              color = TextSecondary
            )
            if (record.repeatCount > 0) {
              Spacer(modifier = Modifier.width(4.dp))
              Text("• 🔄 ${record.repeatCount}", fontSize = 10.5.sp, color = FeatureMemorizationPurple, fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      Column(horizontalAlignment = Alignment.End) {
        // Star rating
        Row {
          repeat(record.rating.coerceIn(1, 5)) {
            Text(text = "⭐", fontSize = 10.sp)
          }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Surface(
          color = FeatureAttendanceGreen.copy(alpha = 0.12f),
          shape = RoundedCornerShape(6.dp)
        ) {
          Text(
            text = "Kabul Edildi",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = FeatureAttendanceGreen,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
          )
        }
      }
    }
  }
}

@Composable
fun PrayerStatusChip(
  name: String,
  isDone: Boolean,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier,
    color = if (isDone) FeatureAttendanceGreen.copy(alpha = 0.15f) else CanvasBackground,
    shape = RoundedCornerShape(8.dp),
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      if (isDone) FeatureAttendanceGreen.copy(alpha = 0.6f) else BorderStroke
    )
  ) {
    Column(
      modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Icon(
        imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
        contentDescription = name,
        tint = if (isDone) FeatureAttendanceGreen else TextMuted,
        modifier = Modifier.size(15.dp)
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = name,
        fontSize = 10.5.sp,
        fontWeight = if (isDone) FontWeight.Bold else FontWeight.Normal,
        color = if (isDone) FeatureAttendanceGreen else TextPrimary
      )
    }
  }
}

@Composable
fun DutySummaryBadge(
  title: String,
  value: String,
  isCompleted: Boolean,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier,
    color = CanvasBackground,
    shape = RoundedCornerShape(10.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
  ) {
    Column(
      modifier = Modifier.padding(8.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = title,
        fontSize = 10.5.sp,
        color = TextSecondary
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = value,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = if (isCompleted) EmeraldGreen else TextPrimary
      )
    }
  }
}
