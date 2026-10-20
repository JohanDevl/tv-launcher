package dev.johan.tvlauncher.data

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import java.io.IOException
import kotlinx.coroutines.flow.map

// The launcher is the only HOME app once the stock one is disabled: a corrupt or unreadable
// settings file must degrade to defaults, never crash-loop.
private val Context.dataStore by preferencesDataStore(
    name = "launcher",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

/** Persists favorites (ordered) and hidden apps. */
class SettingsStore(private val context: Context) {

    val settings: Flow<LauncherSettings> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it.toSettings() }

    suspend fun update(transform: (LauncherSettings) -> LauncherSettings) {
        try {
            context.dataStore.edit { prefs ->
                val next = transform(prefs.toSettings())
                prefs[FAVORITES] = next.favorites.joinToString(SEPARATOR)
                prefs[HIDDEN] = next.hidden
                prefs[INITIALIZED] = next.initialized
            }
        } catch (e: IOException) {
            // Losing one edit beats crashing the home screen.
        }
    }

    private fun Preferences.toSettings() = LauncherSettings(
        favorites = this[FAVORITES]?.split(SEPARATOR)?.filter { it.isNotEmpty() }.orEmpty(),
        hidden = this[HIDDEN].orEmpty(),
        initialized = this[INITIALIZED] ?: false,
    )

    private companion object {
        const val SEPARATOR = "\n"
        val FAVORITES = stringPreferencesKey("favorites")
        val HIDDEN = stringSetPreferencesKey("hidden")
        val INITIALIZED = booleanPreferencesKey("initialized")
    }
}
