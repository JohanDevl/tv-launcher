package dev.johan.tvlauncher.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import dev.johan.tvlauncher.ui.theme.Palette

/**
 * Full-screen backdrop: a soft glow tinted by the focused app's artwork. Only gradients are
 * drawn (no blur, no offscreen layers), which keeps it cheap on low-end TV GPUs.
 */
@Composable
fun AmbientBackground(accent: Color, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val glow by animateColorAsState(accent, animationSpec = tween(durationMillis = 700), label = "ambient")
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(Palette.Background)
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(glow.copy(alpha = 0.50f), glow.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width * 0.18f, -size.height * 0.12f),
                        radius = size.width * 0.75f,
                    ),
                )
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(glow.copy(alpha = 0.18f), Color.Transparent),
                        center = Offset(size.width * 1.0f, size.height * 1.05f),
                        radius = size.width * 0.55f,
                    ),
                )
            },
        content = content,
    )
}
