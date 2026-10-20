package dev.johan.tvlauncher

import android.app.Application
import android.content.ComponentCallbacks2
import dev.johan.tvlauncher.data.AppRepository
import dev.johan.tvlauncher.data.InputRepository
import dev.johan.tvlauncher.data.SettingsStore
import dev.johan.tvlauncher.image.ArtworkCache

/** Manual dependency container: the app is small enough not to need a DI framework. */
class LauncherApp : Application() {

    val apps by lazy { AppRepository(this) }
    val inputs by lazy { InputRepository(this) }
    val settings by lazy { SettingsStore(this) }
    val artwork by lazy { ArtworkCache(packageManager) }

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when {
            level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> artwork.trim(0f)
            level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> artwork.trim(0.5f)
        }
    }
}
