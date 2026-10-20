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

    /** Moves [key] by [delta] positions, clamped to the list bounds. */
    fun moveFavorite(favorites: List<String>, key: String, delta: Int): List<String> {
        val from = favorites.indexOf(key)
        if (from < 0) return favorites
        val to = (from + delta).coerceIn(0, favorites.lastIndex)
        if (to == from) return favorites
        return favorites.toMutableList().apply { add(to, removeAt(from)) }
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
            loading = false,
            favorites = favorites,
            apps = visible,
            hiddenApps = apps.filter { it.key in settings.hidden },
            inputs = inputs,
            favoriteKeys = favorites.mapTo(HashSet()) { it.key },
        )
    }
}
