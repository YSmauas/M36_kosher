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
import androidx.compose.ui.graphics.Color

// Every M3 color role is defined explicitly below. Leaving roles unset
// (as the previous version did for primaryContainer, surfaceVariant,
// onSurfaceVariant, outline, error, etc.) makes Compose silently fall
// back to Material's default purple/lavender baseline palette — which
// is what was making card borders and secondary text look washed out
// and off-brand in both light and dark mode.

private val DarkColorScheme =
  darkColorScheme(
    primary = KosherTealDark,
    onPrimary = Color(0xFF00332F),
    primaryContainer = KosherPrimaryContainerDark,
    onPrimaryContainer = KosherOnPrimaryContainerDark,

    secondary = KosherLightBlueDark,
    onSecondary = Color(0xFFE0F2F1),
    secondaryContainer = KosherSecondaryContainerDark,
    onSecondaryContainer = KosherOnSecondaryContainerDark,

    tertiary = KosherGreenDark,
    onTertiary = Color(0xFF04321C),
    tertiaryContainer = KosherTertiaryContainerDark,
    onTertiaryContainer = KosherOnTertiaryContainerDark,

    error = KosherErrorDark,
    onError = KosherOnErrorDark,
    errorContainer = KosherErrorContainerDark,
    onErrorContainer = KosherOnErrorContainerDark,

    background = KosherBgDark,
    onBackground = Color(0xFFE2E8F0),

    surface = KosherSurfaceDark,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = KosherSurfaceVariantDark,
    onSurfaceVariant = KosherOnSurfaceVariantDark,

    outline = KosherOutlineDark,
    outlineVariant = KosherOutlineVariantDark,

    surfaceDim = KosherSurfaceDimDark,
    surfaceBright = KosherSurfaceBrightDark,
    surfaceContainerLowest = KosherSurfaceContainerLowestDark,
    surfaceContainerLow = KosherSurfaceContainerLowDark,
    surfaceContainer = KosherSurfaceContainerDark,
    surfaceContainerHigh = KosherSurfaceContainerHighDark,
    surfaceContainerHighest = KosherSurfaceContainerHighestDark,

    inverseSurface = KosherInverseSurfaceDark,
    inverseOnSurface = KosherInverseOnSurfaceDark,
    inversePrimary = KosherInversePrimaryDark,
    scrim = Color.Black,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = KosherTeal,
    onPrimary = Color.White,
    primaryContainer = KosherPrimaryContainerLight,
    onPrimaryContainer = KosherOnPrimaryContainerLight,

    secondary = KosherLightBlue,
    onSecondary = Color.White,
    secondaryContainer = KosherSecondaryContainerLight,
    onSecondaryContainer = KosherOnSecondaryContainerLight,

    tertiary = KosherGreen,
    onTertiary = Color.White,
    tertiaryContainer = KosherTertiaryContainerLight,
    onTertiaryContainer = KosherOnTertiaryContainerLight,

    error = KosherErrorLight,
    onError = KosherOnErrorLight,
    errorContainer = KosherErrorContainerLight,
    onErrorContainer = KosherOnErrorContainerLight,

    background = KosherBgLight,
    onBackground = Color(0xFF114E4A), // deep teal instead of plain slate, to match theme

    surface = KosherSurfaceLight,
    onSurface = Color(0xFF1E293B),
    surfaceVariant = KosherSurfaceVariantLight,
    onSurfaceVariant = KosherOnSurfaceVariantLight,

    outline = KosherOutlineLight,
    outlineVariant = KosherOutlineVariantLight,

    surfaceDim = KosherSurfaceDimLight,
    surfaceBright = KosherSurfaceBrightLight,
    surfaceContainerLowest = KosherSurfaceContainerLowestLight,
    surfaceContainerLow = KosherSurfaceContainerLowLight,
    surfaceContainer = KosherSurfaceContainerLight,
    surfaceContainerHigh = KosherSurfaceContainerHighLight,
    surfaceContainerHighest = KosherSurfaceContainerHighestLight,

    inverseSurface = KosherInverseSurfaceLight,
    inverseOnSurface = KosherInverseOnSurfaceLight,
    inversePrimary = KosherInversePrimaryLight,
    scrim = Color.Black,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Set to false to prioritize our meticulously designed Geometric Balance colors
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
