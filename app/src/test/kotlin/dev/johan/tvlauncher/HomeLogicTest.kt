package dev.johan.tvlauncher

import android.content.ComponentName
import dev.johan.tvlauncher.data.AppEntry
import dev.johan.tvlauncher.data.HomeLogic
import dev.johan.tvlauncher.data.LauncherSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class HomeLogicTest {

    @Test
    fun defaultFavoritesKeepPreferredOrderAndSkipMissingApps() {
        val installed = listOf("org.videolan.vlc", "com.netflix.ninja", "com.example.other", "com.disney.disneyplus")

        assertEquals(
            listOf("com.netflix.ninja", "com.disney.disneyplus", "org.videolan.vlc"),
            HomeLogic.defaultFavorites(installed),
        )
    }

    @Test
    fun addFavoriteAppendsOnce() {
        val once = HomeLogic.addFavorite(listOf("a"), "b")
        assertEquals(listOf("a", "b"), once)
        assertEquals(once, HomeLogic.addFavorite(once, "b"))
    }

    @Test
    fun moveFavoriteShiftsAndClampsToBounds() {
        val favorites = listOf("a", "b", "c")
        val visible = favorites.toSet()

        assertEquals(listOf("b", "a", "c"), HomeLogic.moveFavorite(favorites, "a", 1, visible))
        assertEquals(listOf("c", "a", "b"), HomeLogic.moveFavorite(favorites, "c", -2, visible))
        assertSame(favorites, HomeLogic.moveFavorite(favorites, "a", -1, visible))
        assertSame(favorites, HomeLogic.moveFavorite(favorites, "missing", 1, visible))
    }

    @Test
    fun moveFavoriteSkipsAndPrunesEntriesNotOnScreen() {
        val favorites = listOf("a", "uninstalled", "b")

        assertEquals(listOf("b", "a"), HomeLogic.moveFavorite(favorites, "a", 1, visible = setOf("a", "b")))
    }

    @Test
    fun buildStateHidesAppsAndDropsUninstalledFavorites() {
        val apps = listOf(app("netflix"), app("vlc"), app("philips"))
        val settings = LauncherSettings(
            favorites = listOf("vlc", "gone", "philips", "netflix"),
            hidden = setOf("philips"),
            initialized = true,
        )

        val state = HomeLogic.buildState(apps, settings, inputs = emptyList())

        assertEquals(listOf("vlc", "netflix"), state.favorites.map { it.key })
        assertEquals(listOf("netflix", "vlc"), state.apps.map { it.key })
        assertEquals(listOf("philips"), state.hiddenApps.map { it.key })
        assertEquals(setOf("vlc", "netflix"), state.favoriteKeys)
        assertEquals(false, state.loading)
    }

    @Test
    fun buildStateStaysLoadingUntilFavoritesAreSeeded() {
        val state = HomeLogic.buildState(listOf(app("netflix")), LauncherSettings(), inputs = emptyList())

        assertEquals(true, state.loading)
    }

    private fun app(key: String) = AppEntry(key = key, label = key, component = ComponentName(key, "$key.Main"))
}
