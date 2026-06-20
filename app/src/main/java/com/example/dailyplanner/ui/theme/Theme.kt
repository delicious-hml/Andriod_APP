package com.example.dailyplanner.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = ButtonPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0x00000000),
    onPrimaryContainer = TextPrimary,
    secondary = CategoryStudy,
    onSecondary = Color.White,
    secondaryContainer = Color(0x00000000),
    onSecondaryContainer = TextPrimary,
    tertiary = CategoryExercise,
    background = Color(0xFFF8FAFF),
    onBackground = TextPrimary,
    surface = Color.White,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFF1F3F4),
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFFDADCE0),
    outlineVariant = Color(0xFFE8EAED)
)

@Composable
fun DailyPlannerTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
