package dev.johan.tvlauncher.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme
import dev.johan.tvlauncher.R

/** Design tokens. Dark, warm and quiet: the artwork of the apps provides the color. */
object Palette {
    val Background = Color(0xFF07080C)
    val BackgroundRaised = Color(0xFF0E1118)
    val Surface = Color(0xFF151923)
    val SurfaceFocused = Color(0xFFF4F1EA)
    val TextPrimary = Color(0xFFF4F1EA)
    val TextSecondary = Color(0xFF9BA1AE)
    val TextMuted = Color(0xFF5E6573)
    val Accent = Color(0xFFF5B04C)
    val OnFocused = Color(0xFF0B0D12)
    val FocusRing = Color(0xFFF4F1EA)
}

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun manrope(weight: Int) = Font(
    R.font.manrope_variable,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Manrope = FontFamily(manrope(300), manrope(400), manrope(500), manrope(600), manrope(700), manrope(800))

private val typography = Typography(
    displayLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight(300), fontSize = 64.sp, letterSpacing = (-1.5).sp),
    headlineMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight(700), fontSize = 26.sp, letterSpacing = (-0.4).sp),
    titleMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight(600), fontSize = 15.sp, letterSpacing = 0.1.sp),
    titleSmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight(700), fontSize = 12.sp, letterSpacing = 1.6.sp),
    bodyLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight(500), fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight(500), fontSize = 14.sp),
    labelLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight(600), fontSize = 13.sp, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight(600), fontSize = 12.sp),
)

private val colors = darkColorScheme(
    primary = Palette.Accent,
    onPrimary = Palette.OnFocused,
    background = Palette.Background,
    onBackground = Palette.TextPrimary,
    surface = Palette.Surface,
    onSurface = Palette.TextPrimary,
    onSurfaceVariant = Palette.TextSecondary,
    inverseSurface = Palette.SurfaceFocused,
    inverseOnSurface = Palette.OnFocused,
)

@Composable
fun LauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}
