package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val AvatarColors = listOf(
  Color(0xFF2D6BE4), // Electric Blue
  Color(0xFF1D9E75), // Teal Emerald
  Color(0xFF7F77DD), // Indigo Purple
  Color(0xFFE8820C), // Warm Amber
  Color(0xFF0284C7), // Sky Blue
  Color(0xFFD97706), // Gold Orange
  Color(0xFF0D9488), // Deep Teal
  Color(0xFF9333EA)  // Deep Violet
)

fun getInitials(name: String): String {
  val parts = name.trim().split("\\s+".toRegex())
  return when {
    parts.isEmpty() || parts[0].isEmpty() -> "?"
    parts.size == 1 -> parts[0].take(2).uppercase()
    else -> "${parts[0].first().uppercase()}${parts.last().first().uppercase()}"
  }
}

@Composable
fun StudentAvatar(
  name: String,
  colorIndex: Int,
  size: Dp = 44.dp,
  fontSize: Int = 15
) {
  val bg = AvatarColors[colorIndex.coerceIn(0, AvatarColors.lastIndex)]
  val initials = getInitials(name)

  Box(
    modifier = Modifier
      .size(size)
      .clip(CircleShape)
      .background(bg),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = initials,
      color = Color.White,
      fontSize = fontSize.sp,
      fontWeight = FontWeight.Bold
    )
  }
}
