package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

/**
 * Tactile Neumorphic 3D Coin Emblem for Mekteb-i İrfan
 */
@Composable
fun AppLogoEmblem(
  modifier: Modifier = Modifier,
  size: Dp = 100.dp,
  showRays: Boolean = true
) {
  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier.size(size)
  ) {
    // Outer Soft Drop Highlight & Shadow
    Box(
      modifier = Modifier
        .fillMaxSize()
        .shadow(
          elevation = 10.dp,
          shape = CircleShape,
          ambientColor = NeumorphDarkShadow,
          spotColor = NeumorphDarkShadow
        )
        .clip(CircleShape)
        .background(CanvasSurface)
        .border(
          width = 1.5.dp,
          brush = Brush.linearGradient(
            0.0f to NeumorphLight,
            1.0f to NeumorphDarkShadow.copy(alpha = 0.5f),
            start = Offset(0f, 0f),
            end = Offset(200f, 200f)
          ),
          shape = CircleShape
        )
    )

    // Inner Recessed Socket (Sunken Bevel)
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .size(size * 0.78f)
        .clip(CircleShape)
        .background(
          Brush.linearGradient(
            listOf(
              Color(0xFF1E293B), // Tactile Dark Slate
              Color(0xFF0F172A)
            )
          )
        )
        .border(
          width = 1.2.dp,
          brush = Brush.linearGradient(
            0.0f to NeumorphDarkShadow,
            1.0f to NeumorphLight.copy(alpha = 0.3f)
          ),
          shape = CircleShape
        )
    ) {
      if (showRays) {
        Icon(
          imageVector = Icons.Default.AutoAwesome,
          contentDescription = null,
          tint = Color(0xFFFDE68A),
          modifier = Modifier
            .size(size * 0.20f)
            .align(Alignment.TopCenter)
            .padding(top = 4.dp)
        )
      }

      // Quran Holy Book Icon
      Icon(
        imageVector = Icons.AutoMirrored.Filled.MenuBook,
        contentDescription = "Mekteb-i İrfan Logo",
        tint = Color.White,
        modifier = Modifier
          .size(size * 0.44f)
          .align(Alignment.Center)
      )
    }
  }
}
