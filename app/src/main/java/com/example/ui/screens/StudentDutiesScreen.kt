package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyDutyRecord
import com.example.data.model.Student
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDutiesScreen(
  student: Student?,
  duties: List<DailyDutyRecord>,
  todayDate: String,
  onBackClick: () -> Unit,
  viewModel: AppViewModel,
  modifier: Modifier = Modifier
) {
  val todayDuty = duties.find { it.date == todayDate } ?: DailyDutyRecord(
    studentId = student?.id ?: 1L,
    date = todayDate
  )

  // Calculate today's duty score
  var score = 0
  if (todayDuty.fajr) score += 15
  if (todayDuty.dhuhr) score += 15
  if (todayDuty.asr) score += 15
  if (todayDuty.maghrib) score += 15
  if (todayDuty.isha) score += 15
  if (todayDuty.tesbihatDone) score += 10
  if (todayDuty.quranPages > 0) score += 5
  if (todayDuty.risalePages > 0) score += 5
  if (todayDuty.cevsenDone) score += 5
  val dutyScorePercent = score.coerceIn(0, 100)

  Scaffold(
    containerColor = CanvasBackground,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Günlük Vazife & Vird Çetelesi",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Text(
              text = "Tarih: $todayDate • Hoca Değerlendirme Puanı: %$dutyScorePercent",
              fontSize = 12.sp,
              color = TextSecondary
            )
          }
        },
        navigationIcon = {
          IconButton(onClick = onBackClick) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Geri",
              tint = TextPrimary
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = CanvasSurface
        )
      )
    }
  ) { paddingValues ->
    LazyColumn(
      modifier = modifier
        .fillMaxSize()
        .padding(paddingValues),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Top Score & Motivation Banner
      item {
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(18.dp)),
          color = CanvasSurface,
          shape = RoundedCornerShape(18.dp),
          border = androidx.compose.foundation.BorderStroke(1.2.dp, GoldSubtleBorderGradient)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Hoca Onaylı Günlük Karnesi",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Günlük namaz, tilavet ve vird durumlarınız ders halkasında eğitmenleriniz tarafından işlenmektedir.",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
              )
            }

            Spacer(modifier = Modifier.width(12.dp))

            SeljukStarBox(
              size = 52.dp,
              backgroundColor = if (dutyScorePercent >= 75) FeatureAttendanceGreen else TezhipGold
            ) {
              Text(
                text = "%$dutyScorePercent",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            }
          }
        }
      }

      // 1. Five Daily Prayers Section (Read-Only Status)
      item {
        DutySectionCard(
          title = "1. Beş Vakit Namaz & Tesbihat",
          icon = Icons.Default.AccessTimeFilled,
          iconColor = FeatureAttendanceGreen
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              ReadOnlyPrayerItem(name = "Sabah", isDone = todayDuty.fajr, modifier = Modifier.weight(1f))
              ReadOnlyPrayerItem(name = "Öğle", isDone = todayDuty.dhuhr, modifier = Modifier.weight(1f))
              ReadOnlyPrayerItem(name = "İkindi", isDone = todayDuty.asr, modifier = Modifier.weight(1f))
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              ReadOnlyPrayerItem(name = "Akşam", isDone = todayDuty.maghrib, modifier = Modifier.weight(1f))
              ReadOnlyPrayerItem(name = "Yatsı", isDone = todayDuty.isha, modifier = Modifier.weight(1f))
              ReadOnlyPrayerItem(name = "Tesbihat", isDone = todayDuty.tesbihatDone, modifier = Modifier.weight(1f))
            }
          }
        }
      }

      // 2. Quran & Risale Reading Section (Read-Only)
      item {
        DutySectionCard(
          title = "2. Kur'an-ı Kerim & Risale Okuma",
          icon = Icons.Default.MenuBook,
          iconColor = FeatureMemorizationPurple
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ReadOnlyReadingRow(
              title = "Kur'an-ı Kerim Tilaveti",
              pageCount = todayDuty.quranPages,
              unit = "Sayfa"
            )

            Divider(color = BorderStroke)

            ReadOnlyReadingRow(
              title = "Risale-i Nur Okuması",
              pageCount = todayDuty.risalePages,
              unit = "Sayfa"
            )
          }
        }
      }

      // 3. Salavat & Cevşen (Read-Only)
      item {
        DutySectionCard(
          title = "3. Salavat-ı Şerife & Cevşen Virdi",
          icon = Icons.Default.Favorite,
          iconColor = TezhipGold
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ReadOnlyReadingRow(
              title = "Salavat-ı Şerife",
              pageCount = todayDuty.salavatCount,
              unit = "Adet"
            )

            Divider(color = BorderStroke)

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = "Cevşenü'l Kebir",
                  fontSize = 13.5.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = TextPrimary
                )
                Text(
                  text = "Günlük bab okuması",
                  fontSize = 11.5.sp,
                  color = TextSecondary
                )
              }

              Surface(
                color = if (todayDuty.cevsenDone) FeatureAttendanceGreen.copy(alpha = 0.15f) else CanvasBackground,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(
                  1.dp,
                  if (todayDuty.cevsenDone) FeatureAttendanceGreen else BorderStroke
                )
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = if (todayDuty.cevsenDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (todayDuty.cevsenDone) FeatureAttendanceGreen else TextMuted,
                    modifier = Modifier.size(15.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = if (todayDuty.cevsenDone) "Okundu (Onaylı)" else "Kayıt Bekleniyor",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (todayDuty.cevsenDone) FeatureAttendanceGreen else TextSecondary
                  )
                }
              }
            }
          }
        }
      }

      // 4. Past Days History Section (Read-Only)
      if (duties.size > 1) {
        item {
          Text(
            text = "Geçmiş Günlerin Görev Çetelesi",
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(top = 8.dp)
          )
        }

        items(duties.filter { it.date != todayDate }) { pastDuty ->
          PastDutyHistoryItem(duty = pastDuty)
        }
      }
    }
  }
}

@Composable
fun DutySectionCard(
  title: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  iconColor: Color,
  content: @Composable () -> Unit
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
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
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Box(
          modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(iconColor.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconColor,
            modifier = Modifier.size(16.dp)
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = title,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      content()
    }
  }
}

@Composable
fun ReadOnlyPrayerItem(
  name: String,
  isDone: Boolean,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier,
    color = if (isDone) FeatureAttendanceGreen.copy(alpha = 0.15f) else CanvasBackground,
    shape = RoundedCornerShape(10.dp),
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      if (isDone) FeatureAttendanceGreen.copy(alpha = 0.6f) else BorderStroke
    )
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      Icon(
        imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
        contentDescription = name,
        tint = if (isDone) FeatureAttendanceGreen else TextMuted,
        modifier = Modifier.size(16.dp)
      )
      Spacer(modifier = Modifier.width(4.dp))
      Text(
        text = name,
        fontSize = 11.5.sp,
        fontWeight = if (isDone) FontWeight.Bold else FontWeight.Medium,
        color = if (isDone) FeatureAttendanceGreen else TextPrimary
      )
    }
  }
}

@Composable
fun ReadOnlyReadingRow(
  title: String,
  pageCount: Int,
  unit: String
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column {
      Text(
        text = title,
        fontSize = 13.5.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextPrimary
      )
      Text(
        text = "Eğitmen Kaydı",
        fontSize = 11.sp,
        color = TextSecondary
      )
    }

    Surface(
      color = if (pageCount > 0) EmeraldGreen.copy(alpha = 0.12f) else CanvasBackground,
      shape = RoundedCornerShape(8.dp),
      border = androidx.compose.foundation.BorderStroke(
        1.dp,
        if (pageCount > 0) EmeraldGreen.copy(alpha = 0.4f) else BorderStroke
      )
    ) {
      Text(
        text = "$pageCount $unit",
        fontSize = 12.5.sp,
        fontWeight = FontWeight.Bold,
        color = if (pageCount > 0) EmeraldGreen else TextSecondary,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
      )
    }
  }
}

@Composable
fun PastDutyHistoryItem(duty: DailyDutyRecord) {
  var score = 0
  if (duty.fajr) score += 20
  if (duty.dhuhr) score += 20
  if (duty.asr) score += 20
  if (duty.maghrib) score += 20
  if (duty.isha) score += 20

  Surface(
    modifier = Modifier.fillMaxWidth(),
    color = CanvasSurface,
    shape = RoundedCornerShape(12.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = duty.date,
          fontSize = 13.5.sp,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )
        Text(
          text = "Kur'an: ${duty.quranPages} sf • Risale: ${duty.risalePages} sf • Salavat: ${duty.salavatCount}",
          fontSize = 11.sp,
          color = TextSecondary
        )
      }

      Surface(
        color = if (score >= 80) FeatureAttendanceGreen.copy(alpha = 0.15f) else TezhipGold.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp)
      ) {
        Text(
          text = "%$score",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = if (score >= 80) FeatureAttendanceGreen else DarkSlateNavy,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
      }
    }
  }
}
