package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  lightColorScheme(
    primary = DeepBlueNavy,
    onPrimary = Color.White,
    secondary = FeatureStudentsBlue,
    onSecondary = Color.White,
    tertiary = FeatureMemorizationPurple,
    background = CanvasBackground,
    surface = CanvasSurface,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantColor,
    onSurfaceVariant = TextSecondary,
    outline = BorderLight,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = DeepBlueNavy,
    onPrimary = Color.White,
    secondary = FeatureStudentsBlue,
    onSecondary = Color.White,
    tertiary = FeatureMemorizationPurple,
    background = CanvasBackground,
    surface = CanvasSurface,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantColor,
    onSurfaceVariant = TextSecondary,
    outline = BorderLight,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Keep consistent brand identity
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
