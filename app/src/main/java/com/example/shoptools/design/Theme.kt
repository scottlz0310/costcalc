package com.example.shoptools.design

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.shoptools.feature.settings.data.FontSizePreset

private val Pink80 = Color(0xFFFFB3C6)
private val PinkGrey80 = Color(0xFFE8C8D4)
private val Lavender80 = Color(0xFFD4B8E0)
private val Pink40 = Color(0xFFD63384)
private val PinkGrey40 = Color(0xFF8C5A6A)
private val Lavender40 = Color(0xFF7B4F9E)

private val LightColorScheme = lightColorScheme(
    primary = Pink40,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD6E4),
    onPrimaryContainer = Color(0xFF3D0019),
    secondary = PinkGrey40,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD8E8),
    onSecondaryContainer = Color(0xFF2C151D),
    tertiary = Lavender40,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDD9FF),
    onTertiaryContainer = Color(0xFF2D0052),
    background = Color(0xFFFFF8FA),
    surface = Color(0xFFFFF8FA),
    surfaceVariant = Color(0xFFF3DDE6),
    onSurfaceVariant = Color(0xFF4D3440),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
)

private fun buildTypography(scale: Float) = Typography(
    displayLarge = TextStyle(fontSize = (57 * scale).sp, fontWeight = FontWeight.Normal),
    displayMedium = TextStyle(fontSize = (45 * scale).sp, fontWeight = FontWeight.Normal),
    displaySmall = TextStyle(fontSize = (36 * scale).sp, fontWeight = FontWeight.Normal),
    headlineLarge = TextStyle(fontSize = (32 * scale).sp, fontWeight = FontWeight.SemiBold),
    headlineMedium = TextStyle(fontSize = (28 * scale).sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = TextStyle(fontSize = (24 * scale).sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = (22 * scale).sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = (18 * scale).sp, fontWeight = FontWeight.Medium),
    titleSmall = TextStyle(fontSize = (16 * scale).sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = (18 * scale).sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = (16 * scale).sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = (14 * scale).sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = (16 * scale).sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = (14 * scale).sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = (12 * scale).sp, fontWeight = FontWeight.Medium),
)

@Composable
fun ShopToolsTheme(
    fontSizePreset: FontSizePreset = FontSizePreset.NORMAL,
    content: @Composable () -> Unit,
) {
    val scale = when (fontSizePreset) {
        FontSizePreset.NORMAL -> 1.0f
        FontSizePreset.LARGE -> 1.2f
        FontSizePreset.XLARGE -> 1.5f
    }
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = buildTypography(scale),
        content = content,
    )
}
