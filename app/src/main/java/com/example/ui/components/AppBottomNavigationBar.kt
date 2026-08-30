package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen

data class NavItem(
  val screen: AppScreen,
  val title: String,
  val icon: ImageVector,
  val activeColor: Color,
  val testTag: String
)

@Composable
fun AppBottomNavigationBar(
  currentScreen: AppScreen,
  onNavigate: (AppScreen) -> Unit,
  modifier: Modifier = Modifier
) {
  val navItems = listOf(
    NavItem(
      screen = AppScreen.MAIN_MENU,
      title = "Ana Sayfa",
      icon = Icons.Default.Home,
      activeColor = DeepBlueNavy,
      testTag = "bottom_nav_main_menu"
    ),
    NavItem(
      screen = AppScreen.SCHEDULE,
      title = "Yoklama",
      icon = Icons.Default.FactCheck,
      activeColor = DeepBlueNavy,
      testTag = "bottom_nav_schedule"
    ),
    NavItem(
      screen = AppScreen.STUDENTS,
      title = "Talebeler",
      icon = Icons.Default.People,
      activeColor = FeatureStudentsBlue,
      testTag = "bottom_nav_students"
    ),
    NavItem(
      screen = AppScreen.ATTENDANCE,
      title = "Görevler",
      icon = Icons.Default.TaskAlt,
      activeColor = FeatureAttendanceGreen,
      testTag = "bottom_nav_attendance"
    ),
    NavItem(
      screen = AppScreen.MEMORIZATION,
      title = "Ezber",
      icon = Icons.AutoMirrored.Filled.MenuBook,
      activeColor = FeatureMemorizationPurple,
      testTag = "bottom_nav_memorization"
    ),
    NavItem(
      screen = AppScreen.REPORTS,
      title = "Karneler",
      icon = Icons.Default.Assessment,
      activeColor = FeatureReportsOrange,
      testTag = "bottom_nav_reports"
    )
  )

  // 🕌 Geleneksel Tezhip & Selçuklu Zarafeti Alt Gezinme Çubuğu
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .shadow(
        elevation = 8.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        ambientColor = Color.Black.copy(alpha = 0.08f),
        spotColor = Color.Black.copy(alpha = 0.08f)
      ),
    color = CanvasSurface,
    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    border = androidx.compose.foundation.BorderStroke(
      width = 1.2.dp,
      brush = GoldSubtleBorderGradient
    )
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .windowInsetsPadding(NavigationBarDefaults.windowInsets)
        .padding(horizontal = 6.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically
    ) {
      navItems.forEach { item ->
        val isSelected = currentScreen == item.screen
        val interactionSource = remember { MutableInteractionSource() }

        val scale by animateFloatAsState(
          targetValue = if (isSelected) 1.05f else 1.0f,
          animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
          label = "tezhip_nav_scale"
        )

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
          modifier = Modifier
            .weight(1f)
            .heightIn(min = 54.dp)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) item.activeColor.copy(alpha = 0.10f) else Color.Transparent)
            .then(
              if (isSelected) {
                Modifier.border(1.dp, GoldStar.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
              } else Modifier
            )
            .clickable(
              interactionSource = interactionSource,
              indication = ripple(bounded = true, color = TezhipGold.copy(alpha = 0.2f))
            ) {
              if (!isSelected) {
                onNavigate(item.screen)
              }
            }
            .padding(vertical = 5.dp, horizontal = 2.dp)
            .testTag(item.testTag)
        ) {
          // Selçuklu İkon Yuvası (Seçili ise 8 köşeli yıldız, değilse sade)
          if (isSelected) {
            SeljukStarBox(
              size = 28.dp,
              backgroundColor = item.activeColor
            ) {
              Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = Color.White,
                modifier = Modifier.size(15.dp)
              )
            }
          } else {
            Box(
              modifier = Modifier.size(28.dp),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = TextMuted,
                modifier = Modifier.size(19.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(3.dp))

          // Metin Etiketi
          Text(
            text = item.title,
            fontSize = 10.5.sp,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
            color = if (isSelected) item.activeColor else TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
          )
        }
      }
    }
  }
}
