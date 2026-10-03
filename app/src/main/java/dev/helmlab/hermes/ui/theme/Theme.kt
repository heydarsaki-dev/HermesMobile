package dev.helmlab.hermes.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import dev.helmlab.hermes.data.HermesSettings

fun parseAccent(hex: String): Color = runCatching {
    Color(android.graphics.Color.parseColor(hex))
}.getOrElse { Color(0xFF6C5CE7) }

private fun scheme(dark: Boolean, accent: Color): androidx.compose.material3.ColorScheme {
    val a = accent
    return if (dark) {
        darkColorScheme(
            primary = a,
            onPrimary = Color.White,
            primaryContainer = a.copy(alpha = 0.28f),
            onPrimaryContainer = Color.White,
            secondary = a.mix(Color(0xFF00D2FF), 0.55f),
            tertiary = a.mix(Color(0xFFFF6B9D), 0.45f),
            background = Color(0xFF0A0B14),
            onBackground = Color(0xFFE9ECF5),
            surface = Color(0xFF12141F),
            onSurface = Color(0xFFE9ECF5),
            surfaceVariant = Color(0xFF1C1F2E),
            onSurfaceVariant = Color(0xFFA9B0C4),
            outline = Color(0xFF2C3040),
            error = Color(0xFFFF6B6B)
        )
    } else {
        lightColorScheme(
            primary = a.darken(0.25f),
            onPrimary = Color.White,
            primaryContainer = a.copy(alpha = 0.14f),
            onPrimaryContainer = a.darken(0.4f),
            secondary = a.mix(Color(0xFF0089C7), 0.5f),
            tertiary = a.mix(Color(0xFFD6336C), 0.5f),
            background = Color(0xFFF5F6FB),
            onBackground = Color(0xFF14162A),
            surface = Color.White,
            onSurface = Color(0xFF14162A),
            surfaceVariant = Color(0xFFE9ECF6),
            onSurfaceVariant = Color(0xFF545A70),
            outline = Color(0xFFCFD5E4),
            error = Color(0xFFD32F2F)
        )
    }
}

fun Color.mix(other: Color, t: Float) = Color(
    red + (other.red - red) * t,
    green + (other.green - green) * t,
    blue + (other.blue - blue) * t,
    alpha
)

fun Color.darken(t: Float) = Color(red * (1 - t), green * (1 - t), blue * (1 - t), alpha)

private val FaTypography = Typography(
    displaySmall = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 42.sp),
    headlineMedium = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp),
    titleLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 26.sp),
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 28.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 24.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium)
)

@Composable
fun HermesTheme(
    settings: HermesSettings,
    content: @Composable () -> Unit
) {
    val sysDark = isSystemInDarkTheme()
    val dark = when (settings.themeMode) {
        "light" -> false
        "dark" -> true
        else -> sysDark
    }
    val accent = remember(settings.accent) { parseAccent(settings.accent) }
    val scheme = remember(dark, accent) { scheme(dark, accent) }
    val scale = settings.fontScale

    val scaled = if (scale == 1f) FaTypography else Typography(
        displaySmall = FaTypography.displaySmall.scaled(scale),
        headlineMedium = FaTypography.headlineMedium.scaled(scale),
        headlineSmall = FaTypography.headlineSmall.scaled(scale),
        titleLarge = FaTypography.titleLarge.scaled(scale),
        titleMedium = FaTypography.titleMedium.scaled(scale),
        titleSmall = FaTypography.titleSmall.scaled(scale),
        bodyLarge = FaTypography.bodyLarge.scaled(scale),
        bodyMedium = FaTypography.bodyMedium.scaled(scale),
        bodySmall = FaTypography.bodySmall.scaled(scale),
        labelLarge = FaTypography.labelLarge.scaled(scale),
        labelMedium = FaTypography.labelMedium.scaled(scale),
        labelSmall = FaTypography.labelSmall.scaled(scale)
    )

    // The app is a Persian-first product: force RTL layout regardless of device locale.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = scheme, typography = scaled, content = content)
    }
}

private fun TextStyle.scaled(f: Float) = copy(
    fontSize = (fontSize.value * f).sp,
    lineHeight = if (lineHeight != TextUnit.Unspecified) (lineHeight.value * f).sp else lineHeight
)