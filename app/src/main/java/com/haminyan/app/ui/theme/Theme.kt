package com.haminyan.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haminyan.app.data.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF0E5E6F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB9E9F2),
    onPrimaryContainer = Color(0xFF00323C),
    secondary = Color(0xFF7C5800),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDF9E),
    onSecondaryContainer = Color(0xFF271900),
    tertiary = Color(0xFF4E6355),
    background = Color(0xFFF6FAFB),
    onBackground = Color(0xFF171C1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF171C1E),
    surfaceVariant = Color(0xFFE3EAEC),
    onSurfaceVariant = Color(0xFF40484B),
    outline = Color(0xFF70787B),
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF85D2E4),
    onPrimary = Color(0xFF00363F),
    primaryContainer = Color(0xFF004E5B),
    onPrimaryContainer = Color(0xFFB9E9F2),
    secondary = Color(0xFFF7BD48),
    onSecondary = Color(0xFF412D00),
    secondaryContainer = Color(0xFF5D4200),
    onSecondaryContainer = Color(0xFFFFDF9E),
    tertiary = Color(0xFFB5CCBA),
    background = Color(0xFF0F1416),
    onBackground = Color(0xFFDEE3E5),
    surface = Color(0xFF171C1E),
    onSurface = Color(0xFFDEE3E5),
    surfaceVariant = Color(0xFF40484B),
    onSurfaceVariant = Color(0xFFC0C8CB),
    outline = Color(0xFF8A9295),
    error = Color(0xFFFFB4AB),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val AppTypography = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    bodySmall = TextStyle(fontSize = 12.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp),
)

@Composable
fun HaMinyanTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            shapes = AppShapes,
            typography = AppTypography,
            content = content,
        )
    }
}
