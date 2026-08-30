package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppLogoEmblem
import com.example.ui.components.NeumorphicButton
import com.example.ui.components.NeumorphicSunkenWell
import com.example.ui.theme.*

@Composable
fun SplashScreen(
  onEnterClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(CanvasBackground)
      .systemBarsPadding()
      .padding(24.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.fillMaxWidth()
    ) {
      Spacer(modifier = Modifier.weight(1f))

      // Neumorphic 3D Emblem Sconce
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(144.dp)
          .shadow(
            elevation = 14.dp,
            shape = RoundedCornerShape(36.dp),
            ambientColor = NeumorphDarkShadow,
            spotColor = NeumorphDarkShadow
          )
          .clip(RoundedCornerShape(36.dp))
          .background(CanvasSurface)
          .border(
            width = 1.5.dp,
            brush = Brush.linearGradient(
              0.0f to NeumorphLight,
              1.0f to NeumorphDarkShadow.copy(alpha = 0.5f),
              start = Offset(0f, 0f),
              end = Offset(250f, 250f)
            ),
            shape = RoundedCornerShape(36.dp)
          )
          .testTag("splash_app_logo")
      ) {
        AppLogoEmblem(
          size = 112.dp,
          showRays = false
        )
      }

      Spacer(modifier = Modifier.height(28.dp))

      // Sunken Neumorphic Tag Capsule
      NeumorphicSunkenWell(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.padding(bottom = 12.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
          Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = FeatureStudentsBlue,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Talebe Takip & Ezber Sistemi",
            color = FeatureStudentsBlue,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
          )
        }
      }

      // Display Typography
      Text(
        text = "Mekteb-i İrfan",
        color = TextSecondary,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        letterSpacing = 1.sp
      )

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = "Ezber Çetelesi",
        color = TextPrimary,
        fontSize = 32.sp,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center,
        letterSpacing = (-0.5).sp
      )

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = "Kur'an-ı Kerim, sure, risale ve tesbihat ezberlerini kolayca takip edin, günlük yoklama ve gelişim raporlarını yönetin.",
        color = TextSecondary,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        lineHeight = 21.sp,
        modifier = Modifier.padding(horizontal = 16.dp)
      )

      Spacer(modifier = Modifier.weight(1f))

      // Tactile Extruded Action Button
      NeumorphicButton(
        text = "Sisteme Giriş Yap",
        onClick = onEnterClick,
        icon = Icons.AutoMirrored.Filled.ArrowForward,
        accentColor = FeatureStudentsBlue,
        testTag = "splash_enter_button",
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}
