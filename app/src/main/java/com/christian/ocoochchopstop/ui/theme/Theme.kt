package com.christian.ocoochchopstop.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ocoochBlue80,
    onPrimary = onDark,
    primaryContainer = ocoochBlue90,
    onPrimaryContainer = onDark,
    inversePrimary = ocoochBlue40,
    secondary = ocoochGray60,
    onSecondary = ocoochGray50,
    secondaryContainer = ocoochGray80,
    onSecondaryContainer = onDark,
    tertiary = ocoochOrange80,
    onTertiary = Color.Black,
    tertiaryContainer = ocoochOrange40,
    onTertiaryContainer = onDark,
    background = ocoochGray70,
    onBackground = onDark,
    surface = ocoochGray70,
    onSurface = Color.Black,
    surfaceVariant = ocoochGray90,
    onSurfaceVariant = onDark,
    surfaceTint = Color.Transparent,
    inverseSurface = ocoochBlue40,
    inverseOnSurface = onDark,
    error = errorDark,
    onError = onDark,
    errorContainer = errorContainerDark,
    onErrorContainer = Color.Black,
    outline = ocoochGray60,
    outlineVariant = ocoochGray80,
    scrim = Color.Black.copy(alpha = 0.32f),
    surfaceBright = ocoochGray40,
    surfaceContainer = ocoochGray50.copy(alpha = 0.5f),
    surfaceContainerHigh = ocoochGray60.copy(alpha = 0.5f),
    surfaceContainerHighest = ocoochGray60.copy(alpha = 0.75f),
    surfaceContainerLow = Color(0xFF252525).copy(alpha = 0.5f),
    surfaceContainerLowest = Color(0xFF1F1F1F),
    surfaceDim = Color(0xFF1A1A1A)
)

private val LightColorScheme = lightColorScheme(
    primary = ocoochBlue40,
    onPrimary = onLight,
    primaryContainer = ocoochBlue80,
    onPrimaryContainer = onLight,
    inversePrimary = ocoochBlue80,
    secondary = ocoochGray10,
    onSecondary = ocoochGray50,
    secondaryContainer = ocoochGray40,
    onSecondaryContainer = onLight,
    tertiary = ocoochOrange80,
    onTertiary = Color.Black,
    tertiaryContainer = ocoochOrange40,
    onTertiaryContainer = onLight,
    background = ocoochGray20,
    onBackground = Color.Black,
    surface = ocoochGray10,
    onSurface = Color.Black,
    surfaceVariant = ocoochGray70,
    onSurfaceVariant = onLight,
    surfaceTint = Color.Transparent,
    inverseSurface = ocoochBlue80,
    inverseOnSurface = onLight,
    error = errorLight,
    onError = onLight,
    errorContainer = errorContainerLight,
    onErrorContainer = Color.Black,
    outline = ocoochGray20,
    outlineVariant = ocoochGray40,
    scrim = Color.Black.copy(alpha = 0.32f),
    surfaceBright = ocoochGray05,
    surfaceContainer = ocoochGray05.copy(alpha = 0.7f),
    surfaceContainerHigh = ocoochGray10.copy(alpha = 0.5f),
    surfaceContainerHighest = ocoochGray40.copy(alpha = 0.75f),
    surfaceContainerLow = Color(0xFFC0C0C0).copy(alpha = 0.5f),
    surfaceContainerLowest = Color(0xFFD8D8D8),
    surfaceDim = Color(0xFFA0A0A0)
)

@Composable
fun ocoochChopStopTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      content = content
    )
}