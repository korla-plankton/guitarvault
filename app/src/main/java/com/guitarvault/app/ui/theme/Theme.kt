package com.guitarvault.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Fallback color schemes (used when dynamic color is unavailable, i.e. < API 31)
private val LightColors = lightColorScheme(
    primary = Amber40,
    onPrimary = Neutral10,
    primaryContainer = Amber90,
    onPrimaryContainer = Amber10,
    secondary = Amber30,
    onSecondary = Neutral99,
    secondaryContainer = Amber80,
    onSecondaryContainer = Amber10,
    tertiary = Green40,
    onTertiary = Neutral99,
    background = Neutral99,
    onBackground = Neutral10,
    surface = Neutral95,
    onSurface = Neutral10,
    surfaceVariant = Neutral90,
    onSurfaceVariant = Neutral20,
    error = Red40,
    onError = Neutral99,
)

private val DarkColors = darkColorScheme(
    primary = DarkAmber80,
    onPrimary = Neutral10,
    primaryContainer = Amber30,
    onPrimaryContainer = DarkAmber90,
    secondary = DarkAmber80,
    onSecondary = Neutral10,
    secondaryContainer = Amber20,
    onSecondaryContainer = DarkAmber90,
    tertiary = Green80,
    onTertiary = Neutral10,
    background = DarkNeutral10,
    onBackground = DarkNeutral90,
    surface = DarkNeutral20,
    onSurface = DarkNeutral90,
    surfaceVariant = Neutral20,
    onSurfaceVariant = DarkNeutral90,
    error = Red80,
    onError = Neutral10,
)

@Composable
fun GuitarVaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,   // Material You dynamic color
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GuitarVaultTypography,
        content = content
    )
}
