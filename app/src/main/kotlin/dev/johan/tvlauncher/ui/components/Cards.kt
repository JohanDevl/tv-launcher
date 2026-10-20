package dev.johan.tvlauncher.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.johan.tvlauncher.R
import dev.johan.tvlauncher.data.AppEntry
import dev.johan.tvlauncher.image.Artwork
import dev.johan.tvlauncher.image.ArtworkCache
import dev.johan.tvlauncher.ui.theme.Palette

private val CardShape = RoundedCornerShape(14.dp)
private const val FOCUS_SCALE = 1.08f

@Composable
private fun focusBorder(color: Color = Palette.FocusRing) =
    ClickableSurfaceDefaults.border(
        focusedBorder = Border(BorderStroke(2.5.dp, color), inset = 3.dp, shape = CardShape),
    )

/**
 * App banner card. The label appears under the card only while it is focused, so rows stay
 * quiet and the artwork does the talking.
 */
@Composable
fun AppCard(
    app: AppEntry,
    cache: ArtworkCache,
    width: Dp,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFocused: (Color) -> Unit,
    modifier: Modifier = Modifier,
    moving: Boolean = false,
) {
    val artwork by produceState(cache.peek(app.key), app.key) {
        if (value == null) value = cache.load(app.key, app.component)
    }
    var focused by remember { mutableStateOf(false) }
    val captionAlpha by animateFloatAsState(if (focused || moving) 1f else 0f, tween(180), label = "caption")

    Column(modifier = modifier.width(width)) {
        Surface(
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = Modifier
                .width(width)
                .height(width * 9 / 16)
                .onFocusChanged {
                    focused = it.isFocused
                    if (it.isFocused) onFocused(artwork?.accent ?: Palette.Surface)
                },
            shape = ClickableSurfaceDefaults.shape(CardShape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Palette.Surface,
                focusedContainerColor = Palette.Surface,
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = FOCUS_SCALE),
            border = focusBorder(if (moving) Palette.Accent else Palette.FocusRing),
        ) {
            ArtworkContent(app, artwork)
            if (moving) MoveOverlay()
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.labelLarge,
            color = Palette.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 2.dp).graphicsLayer { alpha = captionAlpha },
        )
    }
}

@Composable
private fun ArtworkContent(app: AppEntry, artwork: Artwork?) {
    when {
        artwork == null -> Box(Modifier.fillMaxSize().background(Palette.Surface))
        artwork.isBanner -> Image(
            bitmap = artwork.image,
            contentDescription = app.label,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        else -> IconTile(app, artwork)
    }
}

/** Fallback for apps without a TV banner: icon and name on a gradient of the icon's color. */
@Composable
private fun IconTile(app: AppEntry, artwork: Artwork) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(artwork.accent.copy(alpha = 0.55f), Palette.Surface)))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(bitmap = artwork.image, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)))
        Text(
            text = app.label,
            style = MaterialTheme.typography.titleMedium,
            color = Palette.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MoveOverlay() {
    Row(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = null, tint = Palette.Accent, modifier = Modifier.size(36.dp))
        Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = Palette.Accent, modifier = Modifier.size(36.dp))
    }
}

/** Glyph + label tile used for inputs and system shortcuts. Inverts to a light chip on focus. */
@Composable
fun ActionTile(
    label: String,
    icon: Int,
    width: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocused: () -> Unit = {},
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .width(width)
            .height(width * 9 / 16)
            .onFocusChanged { if (it.isFocused) onFocused() },
        shape = ClickableSurfaceDefaults.shape(CardShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Palette.Surface,
            contentColor = Palette.TextPrimary,
            focusedContainerColor = Palette.SurfaceFocused,
            focusedContentColor = Palette.OnFocused,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = FOCUS_SCALE),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(26.dp))
            Text(label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Pill-shaped shortcut shown in the header (Live TV, Sources, Settings). */
@Composable
fun QuickAction(label: String, icon: Int, onClick: () -> Unit, modifier: Modifier = Modifier, onFocused: () -> Unit = {}) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp).onFocusChanged { if (it.isFocused) onFocused() },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(50)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Palette.Surface.copy(alpha = 0.75f),
            contentColor = Palette.TextPrimary,
            focusedContainerColor = Palette.SurfaceFocused,
            focusedContentColor = Palette.OnFocused,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
    ) {
        Row(
            modifier = Modifier.align(Alignment.Center).padding(start = 14.dp, end = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}
