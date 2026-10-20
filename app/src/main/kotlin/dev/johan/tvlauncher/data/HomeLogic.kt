package dev.johan.tvlauncher.data

/** Pure list operations behind favorites and the home layout, kept free of Android types. */
object HomeLogic {

    /** Packages pinned on first run, in this order, when they are installed. */
    val DEFAULT_FAVORITES = listOf(
        "com.netflix.ninja",
        "com.google.android.youtube.tv",
        "com.disney.disneyplus",
        "com.amazon.amazonvideo.livingroom",
        "com.canal.android.canal",
        "com.apple.atve.androidtv.appletv",
        "fr.francetv.pluzz",
        "tv.twitch.android.app",
        "com.mubi",
        "com.plexapp.android",
        "org.videolan.vlc",
    )

    fun defaultFavorites(installed: Collection<String>): List<String> {
        val available = installed.toSet()
        return DEFAULT_FAVORITES.filter { it in available }
    }

    fun addFavorite(favorites: List<String>, key: String): List<String> =
        if (key in favorites) favorites else favorites + key

    fun removeFavorite(favorites: List<String>, key: String): List<String> = favorites - key

    /**
     * Moves [key] by [delta] positions among the favorites currently on screen ([visible]),
     * clamped to the row bounds. Saved entries that are not visible (uninstalled apps) would
     * otherwise swallow key presses, so they are pruned on the way.
     */
    fun moveFavorite(favorites: List<String>, key: String, delta: Int, visible: Set<String>): List<String> {
        val shown = favorites.filter { it in visible }
        val from = shown.indexOf(key)
        if (from < 0) return favorites
        val to = (from + delta).coerceIn(0, shown.lastIndex)
        if (to == from) return favorites
        return shown.toMutableList().apply { add(to, removeAt(from)) }
    }

    fun buildState(
        apps: List<AppEntry>,
        settings: LauncherSettings,
        inputs: List<InputEntry>,
    ): HomeState {
        val byKey = apps.associateBy { it.key }
        val visible = apps.filter { it.key !in settings.hidden }
        val favorites = settings.favorites
            .filter { it !in settings.hidden }
            .mapNotNull { byKey[it] }
        return HomeState(
            // Until first-run favorites are seeded, showing the screen would flash an empty row.
            loading = !settings.initialized,
            favorites = favorites,
            apps = visible,
            hiddenApps = apps.filter { it.key in settings.hidden },
            inputs = inputs,
            favoriteKeys = favorites.mapTo(HashSet()) { it.key },
        )
    }
}
