package dev.suspension.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun SuspensionSetupTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val appColors = if (dark) DarkAppColors else LightAppColors
    val appTypography = AppTypography()

    // Material3 is only used for ripple/scaffold mechanics here — the actual visual language
    // comes from AppColors/AppTypography (spec §4), which are semantic, not theme-derived.
    val materialScheme = if (dark) {
        darkColorScheme(background = appColors.bg, surface = appColors.surface, onSurface = appColors.ink)
    } else {
        lightColorScheme(background = appColors.bg, surface = appColors.surface, onSurface = appColors.ink)
    }

    CompositionLocalProvider(
        LocalAppColors provides appColors,
        LocalAppTypography provides appTypography,
    ) {
        MaterialTheme(colorScheme = materialScheme, content = content)
    }
}

object AppTheme {
    val colors: AppColors
        @Composable get() = LocalAppColors.current
    val type: AppTypography
        @Composable get() = LocalAppTypography.current
}
