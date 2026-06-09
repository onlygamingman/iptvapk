package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = NeonGreen,
    onPrimary = ButtonBlack,
    secondary = ForestGreen,
    onSecondary = WhiteText,
    tertiary = DarkGray,
    background = DarkGreenBg,
    onBackground = WhiteText,
    surface = DarkGreenSurface,
    onSurface = WhiteText
  )

private val LightColorScheme = DarkColorScheme // Always premium dark background

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force premium dark theme as requested
  dynamicColor: Boolean = false, // Disable dynamic colors to preserve branded neon look
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
