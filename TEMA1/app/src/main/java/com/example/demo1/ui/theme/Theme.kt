package com.example.demo1.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = Indigo80,
    onPrimary = OnIndigoContainer10,
    primaryContainer = IndigoContainer30,
    onPrimaryContainer = IndigoContainer90,
    secondary = Violet80,
    onSecondary = OnVioletContainer10,
    secondaryContainer = VioletContainer30,
    onSecondaryContainer = VioletContainer90,
    tertiary = Coral80,
    tertiaryContainer = CoralContainer30,
    onTertiaryContainer = CoralContainer90,
    background = Background10,
    onBackground = OnSurface90,
    surface = Surface12,
    onSurface = OnSurface90,
    surfaceVariant = SurfaceVariant30,
    onSurfaceVariant = OnSurfaceVariant80,
    surfaceContainer = Surface12,
    surfaceContainerLow = Background10,
    surfaceContainerHigh = SurfaceVariant30,
)

private val LightColorScheme = lightColorScheme(
    primary = Indigo40,
    onPrimary = Color.White,
    primaryContainer = IndigoContainer90,
    onPrimaryContainer = OnIndigoContainer10,
    secondary = Violet40,
    onSecondary = Color.White,
    secondaryContainer = VioletContainer90,
    onSecondaryContainer = OnVioletContainer10,
    tertiary = Coral40,
    tertiaryContainer = CoralContainer90,
    background = Background99,
    onBackground = OnSurface10,
    surface = Surface99,
    onSurface = OnSurface10,
    surfaceVariant = SurfaceVariant90,
    onSurfaceVariant = OnSurfaceVariant30,
    surfaceContainer = Surface99,
    surfaceContainerLow = Background99,
    surfaceContainerHigh = SurfaceVariant90,
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun Demo1Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+; off by default to keep the app's own palette
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
        shapes = AppShapes,
        content = content
    )
}
