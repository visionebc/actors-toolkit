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

// ── Blue Theme ──
private val BlueColorScheme = darkColorScheme(
    primary = Color(0xFF64B5F6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1A3A5C),
    onPrimaryContainer = Color(0xFFBBDEFB),
    secondary = Color(0xFF4DD0E1),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF0A3040),
    onSecondaryContainer = Color(0xFFB2EBF2),
    tertiary = Color(0xFF80CBC4),
    onTertiary = Color(0xFF0A2020),
    tertiaryContainer = Color(0xFF0A2A2A),
    onTertiaryContainer = Color(0xFFB2DFDB),
    background = Color(0xFF0A1929),
    onBackground = Color(0xFFE3F2FD),
    surface = Color(0xFF102840),
    onSurface = Color(0xFFE3F2FD),
    surfaceVariant = Color(0xFF1A3550),
    onSurfaceVariant = Color(0xFF90CAF9),
    outline = Color(0xFF2A4A6A),
    error = Color(0xFFFF6B6B),
    onError = Color.White,
    errorContainer = Color(0xFF3A1230),
    onErrorContainer = Color(0xFFFFB3C9)
)

// ── Deep Blue Theme (strong/vivid) ──
private val DeepBlueColorScheme = darkColorScheme(
    primary = Color(0xFF2962FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0D1F5C),
    onPrimaryContainer = Color(0xFF82B1FF),
    secondary = Color(0xFF448AFF),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF0A1840),
    onSecondaryContainer = Color(0xFF82B1FF),
    tertiary = Color(0xFF00B0FF),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF002850),
    onTertiaryContainer = Color(0xFF80D8FF),
    background = Color(0xFF050D1E),
    onBackground = Color(0xFFE3F2FD),
    surface = Color(0xFF0A1830),
    onSurface = Color(0xFFE3F2FD),
    surfaceVariant = Color(0xFF0F2545),
    onSurfaceVariant = Color(0xFF82B1FF),
    outline = Color(0xFF1A3568),
    error = Color(0xFFFF6B6B),
    onError = Color.White,
    errorContainer = Color(0xFF3A1230),
    onErrorContainer = Color(0xFFFFB3C9)
)

// ── Pink & Violet Theme ──
private val PinkVioletColorScheme = darkColorScheme(
    primary = Color(0xFFE91E63),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4A0A2A),
    onPrimaryContainer = Color(0xFFF8BBD0),
    secondary = Color(0xFFAB47BC),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF301040),
    onSecondaryContainer = Color(0xFFE1BEE7),
    tertiary = Color(0xFFFF80AB),
    onTertiary = Color(0xFF3A0A1A),
    tertiaryContainer = Color(0xFF3A1530),
    onTertiaryContainer = Color(0xFFFFCDD2),
    background = Color(0xFF1A0A1A),
    onBackground = Color(0xFFFCE4EC),
    surface = Color(0xFF2A1030),
    onSurface = Color(0xFFFCE4EC),
    surfaceVariant = Color(0xFF3A1540),
    onSurfaceVariant = Color(0xFFCE93D8),
    outline = Color(0xFF5A2A60),
    error = Color(0xFFFF6B6B),
    onError = Color.White,
    errorContainer = Color(0xFF3A1230),
    onErrorContainer = Color(0xFFFFB3C9)
)

// ── Green Theme ──
private val GreenColorScheme = darkColorScheme(
    primary = Color(0xFF66BB6A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1A3A1A),
    onPrimaryContainer = Color(0xFFC8E6C9),
    secondary = Color(0xFF26A69A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF0A302A),
    onSecondaryContainer = Color(0xFFB2DFDB),
    tertiary = Color(0xFF81C784),
    onTertiary = Color(0xFF0A200A),
    tertiaryContainer = Color(0xFF0A2A15),
    onTertiaryContainer = Color(0xFFA5D6A7),
    background = Color(0xFF0A1A0A),
    onBackground = Color(0xFFE8F5E9),
    surface = Color(0xFF102A15),
    onSurface = Color(0xFFE8F5E9),
    surfaceVariant = Color(0xFF1A3A20),
    onSurfaceVariant = Color(0xFFA5D6A7),
    outline = Color(0xFF2A5A30),
    error = Color(0xFFFF6B6B),
    onError = Color.White,
    errorContainer = Color(0xFF3A1230),
    onErrorContainer = Color(0xFFFFB3C9)
)

// ── Yellow Theme ──
private val YellowColorScheme = darkColorScheme(
    primary = Color(0xFFFFD54F),
    onPrimary = Color(0xFF1A1500),
    primaryContainer = Color(0xFF3A3000),
    onPrimaryContainer = Color(0xFFFFF9C4),
    secondary = Color(0xFFFFB74D),
    onSecondary = Color(0xFF1A1000),
    secondaryContainer = Color(0xFF3A2800),
    onSecondaryContainer = Color(0xFFFFE0B2),
    tertiary = Color(0xFFFFCC80),
    onTertiary = Color(0xFF1A1500),
    tertiaryContainer = Color(0xFF2A2000),
    onTertiaryContainer = Color(0xFFFFF3E0),
    background = Color(0xFF1A1500),
    onBackground = Color(0xFFFFF8E1),
    surface = Color(0xFF2A2200),
    onSurface = Color(0xFFFFF8E1),
    surfaceVariant = Color(0xFF3A3010),
    onSurfaceVariant = Color(0xFFFFE082),
    outline = Color(0xFF5A4A20),
    error = Color(0xFFFF6B6B),
    onError = Color.White,
    errorContainer = Color(0xFF3A1230),
    onErrorContainer = Color(0xFFFFB3C9)
)

// ── iOS Theme (clean light, iOS system palette) ──
private val IOSColorScheme = lightColorScheme(
    primary = Color(0xFF007AFF),                // iOS system blue
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9EBFF),
    onPrimaryContainer = Color(0xFF003E80),
    secondary = Color(0xFF34C759),              // iOS green
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD4F7DC),
    onSecondaryContainer = Color(0xFF0A3A1A),
    tertiary = Color(0xFF5AC8FA),               // iOS teal
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD6F2FB),
    onTertiaryContainer = Color(0xFF08384A),
    background = Color(0xFFF2F2F7),             // iOS grouped bg
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFF2F2F7),
    onSurfaceVariant = Color(0xFF3C3C43),
    outline = Color(0xFFC6C6C8),                // iOS separator
    outlineVariant = Color(0xFFE5E5EA),
    error = Color(0xFFFF3B30),                  // iOS red
    onError = Color.White,
    errorContainer = Color(0xFFFFE5E3),
    onErrorContainer = Color(0xFF5A0C08)
)

// ── Modern Theme (vibrant neon-dark, OLED-friendly) ──
private val ModernColorScheme = darkColorScheme(
    primary = Color(0xFF8B5CF6),                // vivid violet
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2A1065),
    onPrimaryContainer = Color(0xFFE9D5FF),
    secondary = Color(0xFF06B6D4),              // electric cyan
    onSecondary = Color(0xFF001018),
    secondaryContainer = Color(0xFF083344),
    onSecondaryContainer = Color(0xFFA5F3FC),
    tertiary = Color(0xFFF472B6),               // hot pink accent
    onTertiary = Color(0xFF2A0618),
    tertiaryContainer = Color(0xFF4A0B2E),
    onTertiaryContainer = Color(0xFFFCE7F3),
    background = Color(0xFF0A0A0F),             // near-black (OLED)
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF14141C),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E1E2A),
    onSurfaceVariant = Color(0xFFA1A1AA),
    outline = Color(0xFF2D2D3A),
    outlineVariant = Color(0xFF1A1A24),
    error = Color(0xFFFB7185),
    onError = Color.White,
    errorContainer = Color(0xFF3F0A18),
    onErrorContainer = Color(0xFFFECDD3),
    inverseSurface = Color(0xFFF8FAFC),
    inverseOnSurface = Color(0xFF14141C),
    inversePrimary = Color(0xFFC4B5FD)
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
    themeStyle: String = "",
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeStyle) {
        "BLUE" -> BlueColorScheme
        "DEEP_BLUE" -> DeepBlueColorScheme
        "PINK_VIOLET" -> PinkVioletColorScheme
        "GREEN" -> GreenColorScheme
        "YELLOW" -> YellowColorScheme
        "IOS" -> IOSColorScheme
        "MODERN" -> ModernColorScheme
        else -> if (darkTheme) DarkColorScheme else LightColorScheme
    }

    val isLightBars = when (themeStyle) {
        "BLUE", "DEEP_BLUE", "PINK_VIOLET", "GREEN", "YELLOW", "MODERN" -> false
        "IOS" -> true
        else -> !darkTheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = isLightBars
                    isAppearanceLightNavigationBars = isLightBars
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
