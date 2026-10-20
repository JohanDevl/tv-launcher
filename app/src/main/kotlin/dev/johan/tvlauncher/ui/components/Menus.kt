package dev.johan.tvlauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.johan.tvlauncher.ui.theme.Palette

data class MenuItem(val label: String, val icon: Int, val onSelect: () -> Unit)

private val PanelShape = RoundedCornerShape(22.dp)

/** Compact action sheet opened by a long press. First item is focused on open. */
@Composable
fun ActionMenu(title: String, subtitle: String?, items: List<MenuItem>, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Panel(title, subtitle) {
            val first = remember { FocusRequester() }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.heightIn(max = 360.dp),
            ) {
                itemsIndexed(items) { index, item ->
                    MenuRow(
                        label = item.label,
                        icon = item.icon,
                        onClick = { onDismiss(); item.onSelect() },
                        modifier = if (index == 0) Modifier.focusRequester(first) else Modifier,
                    )
                }
            }
            LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
        }
    }
}

@Composable
fun Panel(title: String, subtitle: String?, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .width(400.dp)
            .background(Palette.BackgroundRaised, PanelShape)
            .padding(horizontal = 18.dp, vertical = 22.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            color = Palette.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp),
        )
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.TextSecondary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        content()
    }
}

@Composable
fun MenuRow(label: String, icon: Int?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Palette.BackgroundRaised,
            contentColor = Palette.TextPrimary,
            focusedContainerColor = Palette.SurfaceFocused,
            focusedContentColor = Palette.OnFocused,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterStart).padding(horizontal = 14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (icon != null) Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(22.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
