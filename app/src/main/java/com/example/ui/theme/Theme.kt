package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NeonStarDarkColorScheme = darkColorScheme(
  primary = NeonPurple,
  onPrimary = StarPrimaryText,
  primaryContainer = StarCardElevated,
  onPrimaryContainer = ElectricBlue,
  secondary = ElectricBlue,
  onSecondary = StarBackground,
  secondaryContainer = StarCardBackground,
  onSecondaryContainer = StarPrimaryText,
  tertiary = AccentPink,
  onTertiary = StarPrimaryText,
  background = StarBackground,
  onBackground = StarPrimaryText,
  surface = StarCardBackground,
  onSurface = StarPrimaryText,
  surfaceVariant = StarCardElevated,
  onSurfaceVariant = StarSecondaryText,
  outline = StarOutlineBorder,
  error = StarErrorRed,
  onError = StarPrimaryText,
)

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = NeonStarDarkColorScheme,
    typography = Typography,
    content = content,
  )
}
