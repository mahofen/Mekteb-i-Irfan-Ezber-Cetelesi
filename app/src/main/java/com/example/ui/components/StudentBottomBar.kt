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
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
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

data class StudentNavItem(
  val screen: AppScreen,
  val title: String,
  val icon: ImageVector,
  val activeColor: Color,
  val testTag: String
)

@Composable
fun StudentBottomNavigationBar(
  currentScreen: AppScreen,
  onNavigate: (AppScreen) -> Unit,
  modifier: Modifier = Modifier
) {
  val navItems = listOf(
    StudentNavItem(
      screen = AppScreen.STUDENT_DASHBOARD,
      title = "Ana Sayfa",
      icon = Icons.Default.Home,
      activeColor = EmeraldGreen,
      testTag = "student_nav_dashboard"
    ),
    StudentNavItem(
      screen = AppScreen.STUDENT_UPCOMING_MEMORIZATION,
      title = "Ezberlerim",
      icon = Icons.AutoMirrored.Filled.MenuBook,
      activeColor = FeatureMemorizationPurple,
      testTag = "student_nav_memorization"
    ),
    StudentNavItem(
      screen = AppScreen.STUDENT_DAILY_DUTIES,
      title = "Görevlerim",
      icon = Icons.Default.AssignmentTurnedIn,
      activeColor = FeatureAttendanceGreen,
      testTag = "student_nav_duties"
    ),
    StudentNavItem(
      screen = AppScreen.STUDENT_PROFILE,
      title = "Profilim",
      icon = Icons.Default.Person,
      activeColor = TezhipGold,
      testTag = "student_nav_profile"
    )
  )

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .shadow(
        elevation = 10.dp,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        ambientColor = Color.Black.copy(alpha = 0.08f),
        spotColor = Color.Black.copy(alpha = 0.08f)
      ),
    color = CanvasSurface,
    shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
    border = androidx.compose.foundation.BorderStroke(
      width = 1.2.dp,
      brush = GoldSubtleBorderGradient
    )
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .windowInsetsPadding(NavigationBarDefaults.windowInsets)
        .padding(horizontal = 8.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically
    ) {
      navItems.forEach { item ->
        val isSelected = currentScreen == item.screen ||
          (item.screen == AppScreen.STUDENT_UPCOMING_MEMORIZATION && currentScreen == AppScreen.STUDENT_PAST_MEMORIZATION)
        val interactionSource = remember { MutableInteractionSource() }

        val scale by animateFloatAsState(
          targetValue = if (isSelected) 1.05f else 1.0f,
          animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
          label = "student_nav_scale"
        )

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
          modifier = Modifier
            .weight(1f)
            .heightIn(min = 54.dp)
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) item.activeColor.copy(alpha = 0.12f) else Color.Transparent)
            .then(
              if (isSelected) {
                Modifier.border(1.dp, item.activeColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
              } else Modifier
            )
            .clickable(
              interactionSource = interactionSource,
              indication = ripple(bounded = true, color = item.activeColor.copy(alpha = 0.2f))
            ) {
              if (!isSelected) {
                onNavigate(item.screen)
              }
            }
            .padding(vertical = 6.dp, horizontal = 4.dp)
            .testTag(item.testTag)
        ) {
          if (isSelected) {
            SeljukStarBox(
              size = 30.dp,
              backgroundColor = item.activeColor
            ) {
              Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
              )
            }
          } else {
            Box(
              modifier = Modifier.size(30.dp),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = TextMuted,
                modifier = Modifier.size(21.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(3.dp))

          Text(
            text = item.title,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
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
