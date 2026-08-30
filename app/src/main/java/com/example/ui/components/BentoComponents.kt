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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Modern Bento Tile Container with squircle border and subtle elevation
 */
@Composable
fun BentoCard(
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(22.dp),
  containerColor: Color = CanvasSurface,
  borderColor: Color = BorderLight,
  elevation: Dp = 1.dp,
  onClick: (() -> Unit)? = null,
  content: @Composable ColumnScope.() -> Unit
) {
  val cardModifier = if (onClick != null) {
    modifier
      .clip(shape)
      .shadow(elevation, shape, ambientColor = Color(0x0F0F172A), spotColor = Color(0x1A0F172A))
      .background(containerColor)
      .border(1.dp, borderColor, shape)
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(bounded = true, color = DeepBlueNavy.copy(alpha = 0.08f))
      ) { onClick() }
  } else {
    modifier
      .clip(shape)
      .shadow(elevation, shape, ambientColor = Color(0x0F0F172A), spotColor = Color(0x1A0F172A))
      .background(containerColor)
      .border(1.dp, borderColor, shape)
  }

  Column(
    modifier = cardModifier.padding(18.dp)
  ) {
    content()
  }
}

/**
 * Bento Icon Badge (Squircle container with vivid accent tint)
 */
@Composable
fun BentoIconBox(
  icon: ImageVector,
  accentColor: Color,
  modifier: Modifier = Modifier,
  size: Dp = 46.dp,
  iconSize: Dp = 22.dp
) {
  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .size(size)
      .clip(RoundedCornerShape(14.dp))
      .background(accentColor.copy(alpha = 0.12f))
      .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = accentColor,
      modifier = Modifier.size(iconSize)
    )
  }
}

/**
 * Bold Typography Micro-Chip / Tag
 */
@Composable
fun BentoChip(
  text: String,
  modifier: Modifier = Modifier,
  color: Color = DeepBlueNavy,
  textColor: Color = DeepBlueNavy
) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = color.copy(alpha = 0.10f),
    border = androidx.compose.foundation.BorderStroke(0.8.dp, color.copy(alpha = 0.20f)),
    modifier = modifier
  ) {
    Text(
      text = text,
      color = textColor,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 0.3.sp,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
    )
  }
}

/**
 * Circular Progress Ring widget for Bento Hero Tiles
 */
@Composable
fun BentoProgressRing(
  progress: Float, // 0.0f to 1.0f
  modifier: Modifier = Modifier,
  size: Dp = 64.dp,
  strokeWidth: Dp = 6.dp,
  ringColor: Color = FeatureAttendanceGreen,
  trackColor: Color = SurfaceVariantColor
) {
  val animatedProgress by animateFloatAsState(
    targetValue = progress.coerceIn(0f, 1f),
    animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
    label = "bento_progress"
  )

  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier.size(size)
  ) {
    Canvas(modifier = Modifier.size(size)) {
      val strokePx = strokeWidth.toPx()
      val radius = (size.toPx() - strokePx) / 2f
      val center = Offset(size.toPx() / 2f, size.toPx() / 2f)

      // Background Track
      drawCircle(
        color = trackColor,
        radius = radius,
        center = center,
        style = Stroke(width = strokePx, cap = StrokeCap.Round)
      )

      // Progress Arc
      drawArc(
        color = ringColor,
        startAngle = -90f,
        sweepAngle = animatedProgress * 360f,
        useCenter = false,
        topLeft = Offset(strokePx / 2f, strokePx / 2f),
        size = Size(size.toPx() - strokePx, size.toPx() - strokePx),
        style = Stroke(width = strokePx, cap = StrokeCap.Round)
      )
    }

    Text(
      text = "${(animatedProgress * 100).toInt()}%",
      fontSize = 13.sp,
      fontWeight = FontWeight.Black,
      color = TextPrimary
    )
  }
}

/**
 * High-Impact Bento Action Button
 */
@Composable
fun BentoButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  containerColor: Color = DeepBlueNavy,
  contentColor: Color = Color.White,
  testTag: String = ""
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    shadowElevation = 2.dp,
    modifier = modifier
      .height(48.dp)
      .clip(RoundedCornerShape(14.dp))
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(bounded = true, color = Color.White.copy(alpha = 0.2f))
      ) { onClick() }
      .testTag(testTag)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(containerColor)
        .padding(horizontal = 16.dp),
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
            tint = contentColor,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
          text = text,
          color = contentColor,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.2.sp
        )
      }
    }
  }
}
