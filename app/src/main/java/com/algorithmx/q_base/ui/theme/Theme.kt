package com.algorithmx.q_base.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight
)

private val DarkColorScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark
)

private val MonochromeLightColorScheme = lightColorScheme(
    primary = primaryMonochromeLight,
    onPrimary = onPrimaryMonochromeLight,
    primaryContainer = primaryContainerMonochromeLight,
    onPrimaryContainer = onPrimaryContainerMonochromeLight,
    secondary = primaryMonochromeLight,
    onSecondary = onPrimaryMonochromeLight,
    background = backgroundMonochromeLight,
    onBackground = onBackgroundMonochromeLight,
    surface = surfaceMonochromeLight,
    onSurface = onSurfaceMonochromeLight
)

private val MonochromeDarkColorScheme = darkColorScheme(
    primary = primaryMonochromeDark,
    onPrimary = onPrimaryMonochromeDark,
    primaryContainer = primaryContainerMonochromeDark,
    onPrimaryContainer = onPrimaryContainerMonochromeDark,
    secondary = primaryMonochromeDark,
    onSecondary = onPrimaryMonochromeDark,
    background = backgroundMonochromeDark,
    onBackground = onBackgroundMonochromeDark,
    surface = surfaceMonochromeDark,
    onSurface = onSurfaceMonochromeDark
)

@Composable
fun QbaseTheme(
    themeMode: String = "SYSTEM",
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        "MONOCHROME" -> isSystemInDarkTheme() // Or handle specifically
        else -> isSystemInDarkTheme()
    }

    val colorScheme = when (themeMode) {
        "LIGHT" -> LightColorScheme
        "DARK" -> DarkColorScheme
        "MONOCHROME" -> if (isSystemInDarkTheme()) MonochromeDarkColorScheme else MonochromeLightColorScheme
        else -> {
            if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else if (darkTheme) DarkColorScheme else LightColorScheme
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
