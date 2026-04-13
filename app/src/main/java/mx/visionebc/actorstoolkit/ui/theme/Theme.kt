package mx.visionebc.actorstoolkit.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Gold,
    onPrimary = Color.White,
    primaryContainer = GoldContainer,
    onPrimaryContainer = GoldBright,
    secondary = CrimsonAccent,
    onSecondary = Color.White,
    secondaryContainer = CrimsonContainer,
    onSecondaryContainer = Color(0xFFB3CCFF),
    tertiary = Teal,
    onTertiary = Charcoal,
    tertiaryContainer = TealContainer,
    onTertiaryContainer = Color(0xFF9DF5ED),
    background = Charcoal,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = ElevatedSurface,
    onSurfaceVariant = TextSecondary,
    outline = SubtleBorder,
    outlineVariant = Color(0xFF1E2148),
    error = Color(0xFFFF6B6B),
    onError = Color.White,
    errorContainer = Color(0xFF3A1230),
    onErrorContainer = Color(0xFFFFB3C9),
    inverseSurface = TextPrimary,
    inverseOnSurface = Charcoal,
    inversePrimary = GoldMuted
)

private val LightColorScheme = lightColorScheme(
    primary = GoldMuted,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E0FF),
    onPrimaryContainer = Color(0xFF2D1B69),
    secondary = CrimsonMuted,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDAE2FF),
    onSecondaryContainer = Color(0xFF1A2550),
    tertiary = TealMuted,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD4F5FF),
    onTertiaryContainer = Color(0xFF0A2A35),
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightElevated,
    onSurfaceVariant = LightTextSecond,
    outline = LightBorder,
    outlineVariant = Color(0xFFE0DCF0),
    error = Color(0xFFD94452),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAE0),
    onErrorContainer = Color(0xFF3A1218),
    inverseSurface = LightTextPrimary,
    inverseOnSurface = LightBg,
    inversePrimary = Gold
)

private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp
    )
)

@Composable
fun ActorsToolkitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
