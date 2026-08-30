package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * Gold Gradient Brushes for authentic Tezhip & Seljuk brass trim
 */
val GoldVaraqGradient = Brush.linearGradient(
  colors = listOf(
    Color(0xFFFDF0CD),
    Color(0xFFD4AF37),
    Color(0xFFE5C158),
    Color(0xFF997519)
  )
)

val GoldSubtleBorderGradient = Brush.linearGradient(
  colors = listOf(
    Color(0xFFE6CA65),
    Color(0xFFD4AF37),
    Color(0xFFB8902A)
  )
)

val SeljukEmeraldGradient = Brush.verticalGradient(
  colors = listOf(
    Color(0xFF166534),
    Color(0xFF14532D),
    Color(0xFF052E16)
  )
)

val SeljukLapisGradient = Brush.verticalGradient(
  colors = listOf(
    Color(0xFF1E3A8A),
    Color(0xFF0F2C59),
    Color(0xFF0B192C)
  )
)

val SeljukFiruzeGradient = Brush.verticalGradient(
  colors = listOf(
    Color(0xFF0E7490),
    Color(0xFF0F766E),
    Color(0xFF115E59)
  )
)

val SeljukRubyGradient = Brush.verticalGradient(
  colors = listOf(
    Color(0xFF991B1B),
    Color(0xFF7F1D1D),
    Color(0xFF450A0A)
  )
)

val SeljukGoldGradient = Brush.verticalGradient(
  colors = listOf(
    Color(0xFFD97706),
    Color(0xFFB45309),
    Color(0xFF78350F)
  )
)

/**
 * 8-Pointed Seljuk Star Shape (Rub el Hizb / Selçuklu Yıldızı)
 * Created using mathematical star polygon or dual rotated squares
 */
val SeljukEightStarShape = GenericShape { size, _ ->
  val cx = size.width / 2f
  val cy = size.height / 2f
  val rOuter = size.width / 2f
  val rInner = size.width * 0.38f
  val points = 16 // 8 outer tips, 8 inner vertices

  for (i in 0 until points) {
    val angle = (i * Math.PI / 8.0) - (Math.PI / 2.0)
    val r = if (i % 2 == 0) rOuter else rInner
    val x = (cx + r * cos(angle)).toFloat()
    val y = (cy + r * sin(angle)).toFloat()
    if (i == 0) moveTo(x, y) else lineTo(x, y)
  }
  close()
}

/**
 * Decorative 8-Pointed Seljuk Star Box container for icons, ranks, and avatars
 */
@Composable
fun SeljukStarBox(
  modifier: Modifier = Modifier,
  size: Dp = 48.dp,
  backgroundColor: Color = DeepBlueNavy,
  borderColor: Color = GoldStar,
  elevation: Dp = 3.dp,
  content: @Composable BoxScope.() -> Unit
) {
  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .size(size)
      .shadow(elevation, shape = SeljukEightStarShape)
      .clip(SeljukEightStarShape)
      .background(backgroundColor)
      .border(1.5.dp, GoldVaraqGradient, SeljukEightStarShape)
  ) {
    content()
  }
}

/**
 * Dual Overlapping Squares Seljuk Star for subtle badges & indicators
 */
@Composable
fun SeljukDualSquareBadge(
  modifier: Modifier = Modifier,
  size: Dp = 40.dp,
  backgroundColor: Color = DeepBlueNavy,
  borderColor: Color = GoldStar,
  content: @Composable BoxScope.() -> Unit
) {
  val innerSize = size * 0.78f
  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier.size(size)
  ) {
    // Square 1 (rotated 45 deg)
    Box(
      modifier = Modifier
        .size(innerSize)
        .rotate(45f)
        .clip(RoundedCornerShape(4.dp))
        .background(backgroundColor)
        .border(1.2.dp, GoldSubtleBorderGradient, RoundedCornerShape(4.dp))
    )
    // Square 2 (standard 0 deg)
    Box(
      modifier = Modifier
        .size(innerSize)
        .clip(RoundedCornerShape(4.dp))
        .background(backgroundColor)
        .border(1.2.dp, GoldSubtleBorderGradient, RoundedCornerShape(4.dp))
    )
    // Inner Content
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier.size(innerSize)
    ) {
      content()
    }
  }
}

/**
 * Artisanal Card with authentic Seljuk geometric border, warm parchment surface, and gold corner flourishes
 */
@Composable
fun TezhipCard(
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(20.dp),
  containerColor: Color = CanvasSurface,
  elevation: Dp = 2.dp,
  showCornerOrnaments: Boolean = true,
  onClick: (() -> Unit)? = null,
  content: @Composable ColumnScope.() -> Unit
) {
  val cardModifier = if (onClick != null) {
    modifier
      .fillMaxWidth()
      .clickable { onClick() }
  } else {
    modifier.fillMaxWidth()
  }

  Card(
    shape = shape,
    colors = CardDefaults.cardColors(containerColor = containerColor),
    elevation = CardDefaults.cardElevation(defaultElevation = elevation),
    border = androidx.compose.foundation.BorderStroke(1.2.dp, GoldSubtleBorderGradient),
    modifier = cardModifier
  ) {
    Box(modifier = Modifier.fillMaxWidth()) {
      if (showCornerOrnaments) {
        // Delicate Corner Tezhip Watermark Ornaments in Gold
        Canvas(modifier = Modifier.matchParentSize()) {
          val strokeWidth = 1.2f
          val cornerSize = 18.dp.toPx()
          val goldPaintColor = Color(0x35D4AF37)

          // Top-Left Corner Ornament
          drawLine(goldPaintColor, Offset(8f, 8f), Offset(8f + cornerSize, 8f), strokeWidth)
          drawLine(goldPaintColor, Offset(8f, 8f), Offset(8f, 8f + cornerSize), strokeWidth)
          drawCircle(goldPaintColor, radius = 2.5f, center = Offset(8f + cornerSize, 8f))
          drawCircle(goldPaintColor, radius = 2.5f, center = Offset(8f, 8f + cornerSize))

          // Top-Right Corner Ornament
          val w = size.width
          drawLine(goldPaintColor, Offset(w - 8f, 8f), Offset(w - 8f - cornerSize, 8f), strokeWidth)
          drawLine(goldPaintColor, Offset(w - 8f, 8f), Offset(w - 8f, 8f + cornerSize), strokeWidth)
          drawCircle(goldPaintColor, radius = 2.5f, center = Offset(w - 8f - cornerSize, 8f))
          drawCircle(goldPaintColor, radius = 2.5f, center = Offset(w - 8f, 8f + cornerSize))

          // Bottom-Right Corner Ornament
          val h = size.height
          drawLine(goldPaintColor, Offset(w - 8f, h - 8f), Offset(w - 8f - cornerSize, h - 8f), strokeWidth)
          drawLine(goldPaintColor, Offset(w - 8f, h - 8f), Offset(w - 8f, h - 8f - cornerSize), strokeWidth)
          drawCircle(goldPaintColor, radius = 2.5f, center = Offset(w - 8f - cornerSize, h - 8f))
          drawCircle(goldPaintColor, radius = 2.5f, center = Offset(w - 8f, h - 8f - cornerSize))

          // Bottom-Left Corner Ornament
          drawLine(goldPaintColor, Offset(8f, h - 8f), Offset(8f + cornerSize, h - 8f), strokeWidth)
          drawLine(goldPaintColor, Offset(8f, h - 8f), Offset(8f, h - 8f - cornerSize), strokeWidth)
          drawCircle(goldPaintColor, radius = 2.5f, center = Offset(8f + cornerSize, h - 8f))
          drawCircle(goldPaintColor, radius = 2.5f, center = Offset(8f, h - 8f - cornerSize))
        }
      }

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
      ) {
        content()
      }
    }
  }
}

/**
 * Royal Tezhip Action Button with Seljuk Emerald / Ruby / Cobalt gradients and gold trim
 */
@Composable
fun TezhipButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  containerGradient: Brush = SeljukEmeraldGradient,
  contentColor: Color = Color.White,
  testTag: String = ""
) {
  val interactionSource = remember { MutableInteractionSource() }

  Surface(
    shape = RoundedCornerShape(14.dp),
    shadowElevation = 4.dp,
    border = androidx.compose.foundation.BorderStroke(1.2.dp, GoldVaraqGradient),
    modifier = modifier
      .height(48.dp)
      .clip(RoundedCornerShape(14.dp))
      .clickable(
        interactionSource = interactionSource,
        indication = ripple(bounded = true, color = Color(0xFFFDE68A))
      ) { onClick() }
      .testTag(testTag)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(containerGradient)
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
            tint = Color(0xFFFDE68A),
            modifier = Modifier.size(19.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
          text = text,
          color = contentColor,
          fontSize = 13.5.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.3.sp
        )
      }
    }
  }
}

/**
 * Royal Tezhip Outlined Button with gold parchment trim
 */
@Composable
fun TezhipOutlinedButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  borderColor: Color = TezhipGold,
  contentColor: Color = DeepBlueNavy,
  containerColor: Color = CanvasSurface,
  testTag: String = ""
) {
  val interactionSource = remember { MutableInteractionSource() }

  Surface(
    shape = RoundedCornerShape(14.dp),
    shadowElevation = 1.dp,
    color = containerColor,
    border = androidx.compose.foundation.BorderStroke(1.2.dp, GoldSubtleBorderGradient),
    modifier = modifier
      .height(48.dp)
      .clip(RoundedCornerShape(14.dp))
      .clickable(
        interactionSource = interactionSource,
        indication = ripple(bounded = true, color = TezhipGold.copy(alpha = 0.2f))
      ) { onClick() }
      .testTag(testTag)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
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
            tint = borderColor,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
          text = text,
          color = contentColor,
          fontSize = 13.5.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.3.sp
        )
      }
    }
  }
}

/**
 * Decorative Tezhip Divider with central 8-pointed star motif
 */
@Composable
fun TezhipDivider(
  modifier: Modifier = Modifier,
  color: Color = GoldStar.copy(alpha = 0.35f)
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp)
  ) {
    HorizontalDivider(
      modifier = Modifier.weight(1f),
      color = color,
      thickness = 1.dp
    )
    Spacer(modifier = Modifier.width(8.dp))
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .size(16.dp)
        .rotate(45f)
        .background(GoldStar.copy(alpha = 0.15f))
        .border(1.dp, GoldStar, RoundedCornerShape(2.dp))
    ) {
      Box(
        modifier = Modifier
          .size(6.dp)
          .background(GoldStar, CircleShape)
      )
    }
    Spacer(modifier = Modifier.width(8.dp))
    HorizontalDivider(
      modifier = Modifier.weight(1f),
      color = color,
      thickness = 1.dp
    )
  }
}

/**
 * Ornate Tag / Capsule Badge with delicate gold border
 */
@Composable
fun TezhipBadge(
  text: String,
  modifier: Modifier = Modifier,
  accentColor: Color = DeepBlueNavy,
  textColor: Color = DeepBlueNavy,
  icon: ImageVector? = null
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = accentColor.copy(alpha = 0.10f),
    border = androidx.compose.foundation.BorderStroke(1.dp, GoldStar.copy(alpha = 0.6f)),
    modifier = modifier
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = accentColor,
          modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
      }
      Text(
        text = text,
        color = textColor,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.2.sp
      )
    }
  }
}
