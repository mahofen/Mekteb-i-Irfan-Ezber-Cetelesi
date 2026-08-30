package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.auth.StudentSession
import com.example.data.model.MemorizationRecord
import com.example.data.model.Student
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudentProgressStats

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentProfileScreen(
  student: Student?,
  studentSession: StudentSession,
  stats: StudentProgressStats,
  memorizations: List<MemorizationRecord> = emptyList(),
  onBackClick: () -> Unit,
  onLogout: () -> Unit,
  modifier: Modifier = Modifier
) {
  val scrollState = rememberScrollState()
  val currentStudent = student ?: Student(
    fullName = studentSession.studentName,
    grade = studentSession.grade,
    username = studentSession.username,
    accessCode = studentSession.accessCode
  )
  val avatarColor = getAvatarColor(currentStudent.avatarColorIndex)

  Scaffold(
    containerColor = CanvasBackground,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "Talebe Profili & Başarılar",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
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
        colors = TopAppBarDefaults.topAppBarColors(containerColor = CanvasSurface)
      )
    }
  ) { paddingValues ->
    Column(
      modifier = modifier
        .fillMaxSize()
        .padding(paddingValues)
        .verticalScroll(scrollState)
        .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Profile Avatar & Card
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .shadow(8.dp, RoundedCornerShape(20.dp)),
        color = CanvasSurface,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, GoldSubtleBorderGradient)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Box(
            modifier = Modifier
              .size(68.dp)
              .clip(CircleShape)
              .background(avatarColor)
              .border(3.dp, TezhipGold, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = currentStudent.fullName.take(1).uppercase(),
              fontSize = 28.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          Text(
            text = currentStudent.fullName,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )

          Text(
            text = "${currentStudent.grade} • Durum: ${currentStudent.status}",
            fontSize = 13.sp,
            color = TextSecondary
          )

          Spacer(modifier = Modifier.height(14.dp))

          // Credentials Pill Info
          Surface(
            color = CanvasBackground,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              horizontalArrangement = Arrangement.SpaceAround,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Kullanıcı Adı", fontSize = 11.sp, color = TextSecondary)
                Text(
                  text = if (currentStudent.username.isNotBlank()) currentStudent.username else currentStudent.fullName.lowercase().take(8),
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary
                )
              }
              Divider(
                modifier = Modifier
                  .height(24.dp)
                  .width(1.dp),
                color = BorderStroke
              )
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Giriş Kodu (PIN)", fontSize = 11.sp, color = TextSecondary)
                Text(
                  text = if (currentStudent.accessCode.isNotBlank()) currentStudent.accessCode else "1234",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = TezhipGold
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Overall Progress & Badges Section
      Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CanvasSurface,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ) {
          Text(
            text = "🏆 Talebe Rozetleri & İrfan Madalyaları",
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )

          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            BadgeBox(
              icon = Icons.Default.MilitaryTech,
              title = "Hafız Adayı",
              desc = "${stats.completedMemorizationsCount} Ezber",
              color = FeatureAttendanceGreen,
              modifier = Modifier.weight(1f)
            )
            BadgeBox(
              icon = Icons.Default.LocalFireDepartment,
              title = "Zincir Ustası",
              desc = "${stats.streakDays} Gün Kesintisiz",
              color = TezhipGold,
              modifier = Modifier.weight(1f)
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            BadgeBox(
              icon = Icons.Default.Repeat,
              title = "Tekrar Azmi",
              desc = "${stats.totalRepeatCount} Tekrar",
              color = FeatureMemorizationPurple,
              modifier = Modifier.weight(1f)
            )
            BadgeBox(
              icon = Icons.Default.Star,
              title = "Hoca Takdiri",
              desc = "⭐ ${String.format("%.1f", stats.averageRating)}",
              color = EmeraldGreen,
              modifier = Modifier.weight(1f)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // ⚡ Ezber Hızı & 1 Aylık Gelişim Grafiği
      StudentMemorizationPaceChart(
        student = currentStudent,
        records = memorizations
      )

      Spacer(modifier = Modifier.height(20.dp))

      // Logout / Switch Student Button
      Button(
        onClick = onLogout,
        colors = ButtonDefaults.buttonColors(
          containerColor = StatusRed.copy(alpha = 0.12f),
          contentColor = StatusRed
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Logout,
          contentDescription = "Çıkış Yap",
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Talebe Oturumunu Kapat",
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold
        )
      }

      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}

@Composable
fun BadgeBox(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  desc: String,
  color: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier,
    color = CanvasBackground,
    shape = RoundedCornerShape(12.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
  ) {
    Column(
      modifier = Modifier.padding(10.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(
        modifier = Modifier
          .size(32.dp)
          .clip(CircleShape)
          .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = color,
          modifier = Modifier.size(18.dp)
        )
      }
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
      )
      Text(
        text = desc,
        fontSize = 10.5.sp,
        color = TextSecondary,
        textAlign = TextAlign.Center
      )
    }
  }
}
