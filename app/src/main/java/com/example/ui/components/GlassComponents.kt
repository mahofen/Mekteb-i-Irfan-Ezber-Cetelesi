package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

// =========================================================================
// VIBRANT NEXT-GEN GRADIENTS
// =========================================================================

val GlassBorderGradient = Brush.linearGradient(
  colors = listOf(
    Color(0x99FFFFFF),
    Color(0x26FFFFFF),
    Color(0x0DFFFFFF),
    Color(0x4DFFFFFF)
  )
)

val GlassLightBorderGradient = Brush.linearGradient(
  colors = listOf(
    Color(0xFFFFFFFF),
    Color(0x80FFFFFF),
    Color(0x33E2E8F0)
  )
)

val HeroAuroraGradient = Brush.linearGradient(
  colors = listOf(
    Color(0xFF1E1B4B), // Indigo Abyss
    Color(0xFF312E81), // Royal Violet
    Color(0xFF0F172A)  // Deep Midnight
  )
)

val SkyAzureGradient = Brush.linearGradient(
  colors = listOf(
    Color(0xFF38BDF8),
    Color(0xFF0284C7)
  )
)

val EmeraldMintGradient = Brush.linearGradient(
  colors = listOf(
    Color(0xFF34D399),
    Color(0xFF059669)
  )
)

val VioletPurpleGradient = Brush.linearGradient(
  colors = listOf(
    Color(0xFFA78BFA),
    Color(0xFF7C3AED)
  )
)

val CyanTealGradient = Brush.linearGradient(
  colors = listOf(
    Color(0xFF22D3EE),
    Color(0xFF0891B2)
  )
)

val SunsetCoralGradient = Brush.linearGradient(
  colors = listOf(
    Color(0xFFFB923C),
    Color(0xFFEA580C)
  )
)

/**
 * Ambient Aurora Glow Backdrop (Translucent colored light orbs giving real glass backdrop depth)
 */
@Composable
fun AuroraMeshBackground(
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(CanvasBackground)
  ) {
    // Ambient Soft Glow Orbs
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height

      // Top-right Cyan/Sky Aura
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(Color(0x2638BDF8), Color.Transparent),
          center = Offset(w * 0.85f, h * 0.12f),
          radius = w * 0.55f
        ),
        center = Offset(w * 0.85f, h * 0.12f),
        radius = w * 0.55f
      )

      // Center-left Violet Aura
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(Color(0x1F8B5CF6), Color.Transparent),
          center = Offset(w * 0.15f, h * 0.38f),
          radius = w * 0.60f
        ),
        center = Offset(w * 0.15f, h * 0.38f),
        radius = w * 0.60f
      )

      // Bottom-right Emerald Aura
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(Color(0x2210B981), Color.Transparent),
          center = Offset(w * 0.80f, h * 0.75f),
          radius = w * 0.50f
        ),
        center = Offset(w * 0.80f, h * 0.75f),
        radius = w * 0.50f
      )
    }

    content()
  }
}

/**
 * Frosted Glass Card (iOS-style blur aesthetic with glossy border)
 */
@Composable
fun GlassCard(
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(24.dp),
  containerColor: Color = Color(0xF2FFFFFF),
  borderBrush: Brush = GlassLightBorderGradient,
  elevation: Dp = 6.dp,
  onClick: (() -> Unit)? = null,
  content: @Composable ColumnScope.() -> Unit
) {
  val cardModifier = if (onClick != null) {
    modifier
      .fillMaxWidth()
      .shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = Color(0x140F172A),
        spotColor = Color(0x1F0F172A)
      )
      .clip(shape)
      .background(containerColor)
      .border(1.2.dp, borderBrush, shape)
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(bounded = true, color = Color(0x1A6366F1))
      ) { onClick() }
  } else {
    modifier
      .fillMaxWidth()
      .shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = Color(0x140F172A),
        spotColor = Color(0x1F0F172A)
      )
      .clip(shape)
      .background(containerColor)
      .border(1.2.dp, borderBrush, shape)
  }

  Column(
    modifier = cardModifier.padding(18.dp)
  ) {
    content()
  }
}

/**
 * Glass Icon Container with Vivid iOS Gradient
 */
@Composable
fun GlassIconBox(
  icon: ImageVector,
  gradient: Brush,
  modifier: Modifier = Modifier,
  size: Dp = 50.dp,
  iconSize: Dp = 24.dp
) {
  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .size(size)
      .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = Color(0x33000000))
      .clip(RoundedCornerShape(16.dp))
      .background(gradient)
      .border(1.dp, Color(0x4DFFFFFF), RoundedCornerShape(16.dp))
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = Color.White,
      modifier = Modifier.size(iconSize)
    )
  }
}

/**
 * Frosted Glass Badge with glowing micro-indicator
 */
@Composable
fun GlassBadge(
  text: String,
  accentColor: Color,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = accentColor.copy(alpha = 0.12f),
    border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
    modifier = modifier
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
      Box(
        modifier = Modifier
          .size(6.dp)
          .clip(CircleShape)
          .background(accentColor)
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = text,
        color = accentColor,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.3.sp
      )
    }
  }
}

/**
 * Fluid iOS Gradient Action Button
 */
@Composable
fun GlassGradientButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  gradient: Brush = SkyAzureGradient,
  icon: ImageVector? = null,
  testTag: String = ""
) {
  Surface(
    shape = RoundedCornerShape(18.dp),
    shadowElevation = 8.dp,
    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4DFFFFFF)),
    modifier = modifier
      .height(52.dp)
      .clip(RoundedCornerShape(18.dp))
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(bounded = true, color = Color.White.copy(alpha = 0.3f))
      ) { onClick() }
      .testTag(testTag)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(gradient)
        .padding(horizontal = 20.dp),
      contentAlignment = Alignment.Center
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        if (icon != null) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(19.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
          text = text,
          color = Color.White,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.3.sp
        )
      }
    }
  }
}
