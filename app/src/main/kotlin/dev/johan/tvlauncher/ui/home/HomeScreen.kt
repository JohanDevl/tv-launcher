package dev.johan.tvlauncher.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.johan.tvlauncher.R
import dev.johan.tvlauncher.data.AppEntry
import dev.johan.tvlauncher.data.HomeState
import dev.johan.tvlauncher.image.ArtworkCache
import dev.johan.tvlauncher.ui.components.ActionMenu
import dev.johan.tvlauncher.ui.components.ActionTile
import dev.johan.tvlauncher.ui.components.AmbientBackground
import dev.johan.tvlauncher.ui.components.AppCard
import dev.johan.tvlauncher.ui.components.Header
import dev.johan.tvlauncher.ui.components.MenuItem
import dev.johan.tvlauncher.ui.components.QuickAction
import dev.johan.tvlauncher.ui.theme.Palette
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

// Layout grid for a 960x540dp TV canvas.
private val EdgePadding = 48.dp
private val FavoriteWidth = 220.dp
private val AppWidth = 160.dp
private val Gap = 16.dp
private const val APPS_PER_ROW = 5
private val NeutralGlow = Color(0xFF3A4255)

private data class MenuTarget(val app: AppEntry, val fromFavorites: Boolean)

@Composable
fun HomeScreen(viewModel: HomeViewModel, homePresses: Flow<Unit>) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val entryFocus = remember { FocusRequester() }

    var accent by remember { mutableStateOf(NeutralGlow) }
    var menuTarget by remember { mutableStateOf<MenuTarget?>(null) }
    var movingKey by remember { mutableStateOf<String?>(null) }
    var showHidden by remember { mutableStateOf(false) }

    fun backToStart() {
        movingKey = null
        scope.launch {
            listState.scrollToItem(0)
            withFrameNanos { }
            runCatching { entryFocus.requestFocus() }
        }
    }

    // A launcher never exits: Back and Home both bring the user back to the first tile.
    BackHandler { backToStart() }
    LaunchedEffect(homePresses) { homePresses.collect { backToStart() } }
    LaunchedEffect(state.loading) {
        if (!state.loading) {
            withFrameNanos { }
            runCatching { entryFocus.requestFocus() }
        }
    }

    val openMenu = { app: AppEntry, fromFavorites: Boolean -> menuTarget = MenuTarget(app, fromFavorites) }
    val sourcesAvailable = remember { Actions.canOpenSources(context) }

    AmbientBackground(accent) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(top = 36.dp, bottom = 48.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            item(key = "header") {
                Header(Modifier.padding(horizontal = EdgePadding).padding(bottom = 28.dp)) {
                    val onFocused = { accent = NeutralGlow }
                    QuickAction(stringResource(R.string.action_live_tv), R.drawable.ic_tv, { Actions.openLiveTv(context) }, onFocused = onFocused)
                    if (sourcesAvailable) {
                        QuickAction(stringResource(R.string.action_sources), R.drawable.ic_input, { Actions.openSources(context) }, onFocused = onFocused)
                    }
                    QuickAction(stringResource(R.string.action_settings), R.drawable.ic_settings, { Actions.openSettings(context) }, onFocused = onFocused)
                }
            }

            item(key = "favorites") {
                SectionTitle(stringResource(R.string.section_favorites))
                if (state.favorites.isEmpty() && !state.loading) {
                    EmptyFavorites()
                } else {
                    FavoritesRow(
                        state = state,
                        cache = viewModel.artwork,
                        movingKey = movingKey,
                        entryFocus = entryFocus,
                        onLaunch = { Actions.launchApp(context, it) },
                        onMenu = { openMenu(it, true) },
                        onMove = { key, delta -> viewModel.moveFavorite(key, delta) },
                        onMoveDone = { movingKey = null },
                        onFocused = { accent = it },
                        onRowFocused = { scope.launch { listState.animateScrollToItem(0) } },
                    )
                }
            }

            if (state.inputs.isNotEmpty()) {
                item(key = "inputs") {
                    SectionTitle(stringResource(R.string.section_inputs))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = EdgePadding, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(Gap),
                    ) {
                        items(state.inputs, key = { it.id }) { input ->
                            ActionTile(
                                label = input.label,
                                icon = R.drawable.ic_input,
                                width = AppWidth,
                                onClick = { Actions.openInput(context, input) },
                                onFocused = { accent = NeutralGlow },
                            )
                        }
                    }
                    RowSpacer()
                }
            }

            item(key = "apps-title") { SectionTitle(stringResource(R.string.section_apps)) }
            // The hidden-apps entry closes the grid, right where a missing app would be looked for.
            val cells: List<AppEntry?> = if (state.hiddenApps.isEmpty()) state.apps else state.apps + null
            val rows = cells.chunked(APPS_PER_ROW)
            itemsIndexed(rows, key = { index, _ -> "apps-$index" }) { rowIndex, row ->
                Row(
                    modifier = Modifier.padding(horizontal = EdgePadding).padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(Gap),
                ) {
                    row.forEachIndexed { column, app ->
                        if (app == null) {
                            ActionTile(
                                label = stringResource(R.string.action_hidden_apps) + " · " + state.hiddenApps.size,
                                icon = R.drawable.ic_visibility_off,
                                width = AppWidth,
                                onClick = { showHidden = true },
                                onFocused = { accent = NeutralGlow },
                            )
                            return@forEachIndexed
                        }
                        val isEntry = state.favorites.isEmpty() && rowIndex == 0 && column == 0
                        AppCard(
                            app = app,
                            cache = viewModel.artwork,
                            width = AppWidth,
                            onClick = { Actions.launchApp(context, app) },
                            onLongClick = { openMenu(app, false) },
                            onFocused = { accent = it },
                            modifier = if (isEntry) Modifier.focusRequester(entryFocus) else Modifier,
                        )
                    }
                }
            }
        }
    }

    menuTarget?.let { target ->
        AppMenu(
            target = target,
            isFavorite = target.app.key in state.favoriteKeys,
            onDismiss = { menuTarget = null },
            onToggleFavorite = { viewModel.toggleFavorite(target.app.key) },
            onMove = { movingKey = target.app.key },
            onHide = { viewModel.setHidden(target.app.key, true) },
        )
    }

    if (showHidden && state.hiddenApps.isNotEmpty()) {
        HiddenAppsDialog(
            apps = state.hiddenApps,
            onUnhide = { viewModel.setHidden(it.key, false) },
            onDismiss = { showHidden = false },
        )
    }
}

@Composable
private fun FavoritesRow(
    state: HomeState,
    cache: ArtworkCache,
    movingKey: String?,
    entryFocus: FocusRequester,
    onLaunch: (AppEntry) -> Unit,
    onMenu: (AppEntry) -> Unit,
    onMove: (String, Int) -> Unit,
    onMoveDone: () -> Unit,
    onFocused: (Color) -> Unit,
    onRowFocused: () -> Unit,
) {
    val rowState = rememberLazyListState()
    val movingIndex = state.favorites.indexOfFirst { it.key == movingKey }
    LaunchedEffect(movingIndex) {
        if (movingIndex >= 0) {
            val visible = rowState.layoutInfo.visibleItemsInfo.map { it.index }
            if (movingIndex !in visible.drop(1).dropLast(1)) rowState.animateScrollToItem((movingIndex - 1).coerceAtLeast(0))
        }
    }
    LazyRow(
        state = rowState,
        contentPadding = PaddingValues(horizontal = EdgePadding, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(Gap + 4.dp),
        modifier = Modifier.onFocusChanged { if (it.hasFocus) onRowFocused() },
    ) {
        itemsIndexed(state.favorites, key = { _, app -> app.key }) { index, app ->
            val moving = app.key == movingKey
            var modifier: Modifier = Modifier
            if (index == 0) modifier = modifier.focusRequester(entryFocus)
            if (moving) modifier = modifier.onPreviewKeyEvent { event -> handleMoveKey(event, app.key, onMove, onMoveDone) }
            AppCard(
                app = app,
                cache = cache,
                width = FavoriteWidth,
                onClick = { onLaunch(app) },
                onLongClick = { onMenu(app) },
                onFocused = onFocused,
                moving = moving,
                modifier = modifier.animateItem(),
            )
        }
    }
    if (movingKey != null) {
        Text(
            stringResource(R.string.move_hint),
            style = MaterialTheme.typography.labelLarge,
            color = Palette.Accent,
            modifier = Modifier.padding(horizontal = EdgePadding),
        )
    }
    RowSpacer()
}

/** In move mode the D-pad reorders instead of navigating; OK or Back leaves the mode. */
private fun handleMoveKey(
    event: androidx.compose.ui.input.key.KeyEvent,
    key: String,
    onMove: (String, Int) -> Unit,
    onDone: () -> Unit,
): Boolean {
    val down = event.type == KeyEventType.KeyDown
    when (event.key) {
        Key.DirectionLeft -> if (down) onMove(key, -1)
        Key.DirectionRight -> if (down) onMove(key, 1)
        Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.Back, Key.Escape -> if (!down) onDone()
        Key.DirectionUp, Key.DirectionDown -> Unit
        else -> return false
    }
    return true
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.titleSmall,
        color = Palette.TextSecondary,
        modifier = Modifier.padding(horizontal = EdgePadding).padding(bottom = 2.dp),
    )
}

@Composable
private fun RowSpacer() {
    Box(Modifier.height(18.dp))
}

@Composable
private fun EmptyFavorites() {
    Box(
        modifier = Modifier
            .padding(horizontal = EdgePadding, vertical = 10.dp)
            .width(FavoriteWidth * 2)
            .height(FavoriteWidth * 9 / 16)
            .background(Palette.Surface.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .padding(20.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(stringResource(R.string.favorites_empty), style = MaterialTheme.typography.bodyLarge, color = Palette.TextSecondary)
    }
    RowSpacer()
}

@Composable
private fun AppMenu(
    target: MenuTarget,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMove: () -> Unit,
    onHide: () -> Unit,
) {
    val context = LocalContext.current
    val app = target.app
    val items = buildList {
        if (isFavorite) {
            if (target.fromFavorites) add(MenuItem(stringResource(R.string.menu_move), R.drawable.ic_swap, onMove))
            add(MenuItem(stringResource(R.string.menu_remove_favorite), R.drawable.ic_star_outline, onToggleFavorite))
        } else {
            add(MenuItem(stringResource(R.string.menu_add_favorite), R.drawable.ic_star, onToggleFavorite))
        }
        add(MenuItem(stringResource(R.string.menu_hide), R.drawable.ic_visibility_off, onHide))
        add(MenuItem(stringResource(R.string.menu_app_info), R.drawable.ic_info) { Actions.openAppInfo(context, app) })
        add(MenuItem(stringResource(R.string.menu_uninstall), R.drawable.ic_delete) { Actions.uninstall(context, app) })
    }
    ActionMenu(title = app.label, subtitle = null, items = items, onDismiss = onDismiss)
}

@Composable
private fun HiddenAppsDialog(apps: List<AppEntry>, onUnhide: (AppEntry) -> Unit, onDismiss: () -> Unit) {
    val items = apps.map { app -> MenuItem(app.label, R.drawable.ic_visibility) { onUnhide(app) } }
    ActionMenu(
        title = stringResource(R.string.action_hidden_apps),
        subtitle = stringResource(R.string.hidden_apps_hint),
        items = items,
        onDismiss = onDismiss,
    )
}
