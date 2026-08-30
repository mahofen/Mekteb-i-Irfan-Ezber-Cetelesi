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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Neumorphic Soft 3D Extruded Card (Convex Plate)
 * Features dual-directional ambient drop shadow and crisp top-left highlight
 */
@Composable
fun NeumorphicCard(
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(24.dp),
  backgroundColor: Color = CanvasSurface,
  elevation: Dp = 8.dp,
  onClick: (() -> Unit)? = null,
  content: @Composable ColumnScope.() -> Unit
) {
  val baseModifier = if (onClick != null) {
    modifier
      .shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = NeumorphDarkShadow,
        spotColor = NeumorphDarkShadow.copy(alpha = 0.85f)
      )
      .clip(shape)
      .background(backgroundColor)
      .border(
        width = 1.dp,
        brush = Brush.linearGradient(
          0.0f to NeumorphLight,
          0.4f to NeumorphLight.copy(alpha = 0.5f),
          1.0f to NeumorphDarkShadow.copy(alpha = 0.3f),
          start = Offset(0f, 0f),
          end = Offset(400f, 400f)
        ),
        shape = shape
      )
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(bounded = true, color = DeepBlueNavy.copy(alpha = 0.08f))
      ) { onClick() }
  } else {
    modifier
      .shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = NeumorphDarkShadow,
        spotColor = NeumorphDarkShadow.copy(alpha = 0.85f)
      )
      .clip(shape)
      .background(backgroundColor)
      .border(
        width = 1.dp,
        brush = Brush.linearGradient(
          0.0f to NeumorphLight,
          0.4f to NeumorphLight.copy(alpha = 0.5f),
          1.0f to NeumorphDarkShadow.copy(alpha = 0.3f),
          start = Offset(0f, 0f),
          end = Offset(400f, 400f)
        ),
        shape = shape
      )
  }

  Column(
    modifier = baseModifier.padding(18.dp)
  ) {
    content()
  }
}

/**
 * Neumorphic Debossed / Sunken Inset Container (Concave Socket)
 */
@Composable
fun NeumorphicSunkenWell(
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(16.dp),
  content: @Composable BoxScope.() -> Unit
) {
  Box(
    modifier = modifier
      .clip(shape)
      .background(SurfaceVariantColor)
      .border(
        width = 1.2.dp,
        brush = Brush.linearGradient(
          0.0f to NeumorphDarkShadow.copy(alpha = 0.6f),
          1.0f to NeumorphLight,
          start = Offset(0f, 0f),
          end = Offset(200f, 200f)
        ),
        shape = shape
      )
  ) {
    content()
  }
}

/**
 * Neumorphic 3D Bevelled Button / Action Trigger
 */
@Composable
fun NeumorphicButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  accentColor: Color = FeatureStudentsBlue,
  isPressed: Boolean = false,
  testTag: String = ""
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    shadowElevation = if (isPressed) 1.dp else 6.dp,
    color = if (isPressed) SurfaceVariantColor else CanvasSurface,
    border = androidx.compose.foundation.BorderStroke(
      width = 1.dp,
      brush = Brush.linearGradient(
        colors = if (isPressed) listOf(NeumorphDarkShadow, NeumorphLight)
        else listOf(NeumorphLight, NeumorphDarkShadow.copy(alpha = 0.4f))
      )
    ),
    modifier = modifier
      .height(50.dp)
      .clip(RoundedCornerShape(16.dp))
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(bounded = true, color = accentColor.copy(alpha = 0.15f))
      ) { onClick() }
      .testTag(testTag)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 18.dp),
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
            tint = accentColor,
            modifier = Modifier.size(19.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
          text = text,
          color = TextPrimary,
          fontSize = 14.5.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.2.sp
        )
      }
    }
  }
}

/**
 * Neumorphic Extruded Tactile Icon Sconce / Socket with LED glow
 */
@Composable
fun NeumorphicIconSconce(
  icon: ImageVector,
  accentColor: Color,
  modifier: Modifier = Modifier,
  size: Dp = 48.dp,
  iconSize: Dp = 22.dp
) {
  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .size(size)
      .shadow(6.dp, RoundedCornerShape(15.dp), ambientColor = NeumorphDarkShadow, spotColor = NeumorphDarkShadow)
      .clip(RoundedCornerShape(15.dp))
      .background(CanvasSurface)
      .border(
        width = 1.dp,
        brush = Brush.linearGradient(
          0.0f to NeumorphLight,
          1.0f to NeumorphDarkShadow.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(15.dp)
      )
  ) {
    // LED Ambient Glow Dot
    Box(
      modifier = Modifier
        .size(6.dp)
        .align(Alignment.TopEnd)
        .padding(top = 4.dp, end = 4.dp)
        .clip(CircleShape)
        .background(accentColor)
    )

    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = accentColor,
      modifier = Modifier.size(iconSize)
    )
  }
}

/**
 * Tactile Neumorphic Dial Gauge (Progress Ring)
 */
@Composable
fun NeumorphicDialProgress(
  progress: Float,
  modifier: Modifier = Modifier,
  size: Dp = 68.dp,
  strokeWidth: Dp = 6.5.dp,
  accentColor: Color = FeatureAttendanceGreen
) {
  val animatedProgress by animateFloatAsState(
    targetValue = progress.coerceIn(0f, 1f),
    animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
    label = "neumorph_progress"
  )

  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .size(size)
      .shadow(4.dp, CircleShape, ambientColor = NeumorphDarkShadow, spotColor = NeumorphDarkShadow)
      .clip(CircleShape)
      .background(CanvasSurface)
  ) {
    Canvas(modifier = Modifier.size(size)) {
      val strokePx = strokeWidth.toPx()
      val radius = (size.toPx() - strokePx) / 2f
      val center = Offset(size.toPx() / 2f, size.toPx() / 2f)

      // Recessed Sunken Track
      drawCircle(
        color = SurfaceVariantColor,
        radius = radius,
        center = center,
        style = Stroke(width = strokePx, cap = StrokeCap.Round)
      )

      // Illuminated Active Ring
      drawArc(
        brush = Brush.sweepGradient(
          listOf(accentColor.copy(alpha = 0.7f), accentColor, accentColor)
        ),
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
