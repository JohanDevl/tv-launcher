package dev.johan.tvlauncher.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.johan.tvlauncher.LauncherApp
import dev.johan.tvlauncher.data.HomeLogic
import dev.johan.tvlauncher.data.HomeState
import dev.johan.tvlauncher.data.LauncherSettings
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as LauncherApp
    private val store = app.settings

    val artwork = app.artwork

    private val inputRefresh = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val apps = app.apps.apps().onEach { seedFavoritesOnce(it.map { app -> app.key }) }

    val state: StateFlow<HomeState> =
        combine(apps, store.settings, app.inputs.inputs(inputRefresh)) { apps, settings, inputs ->
            HomeLogic.buildState(apps, settings, inputs)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeState())

    /** Called when the home screen becomes visible again. */
    fun onShown() {
        inputRefresh.tryEmit(Unit)
    }

    fun toggleFavorite(key: String) = edit {
        if (key in it.favorites) it.copy(favorites = HomeLogic.removeFavorite(it.favorites, key))
        else it.copy(favorites = HomeLogic.addFavorite(it.favorites, key))
    }

    fun moveFavorite(key: String, delta: Int) {
        val visible = state.value.favoriteKeys
        edit { it.copy(favorites = HomeLogic.moveFavorite(it.favorites, key, delta, visible)) }
    }

    fun setHidden(key: String, hidden: Boolean) = edit {
        if (hidden) it.copy(hidden = it.hidden + key, favorites = it.favorites - key)
        else it.copy(hidden = it.hidden - key)
    }

    private fun edit(transform: (LauncherSettings) -> LauncherSettings) {
        viewModelScope.launch { store.update(transform) }
    }

    private suspend fun seedFavoritesOnce(installed: List<String>) {
        if (store.settings.first().initialized || installed.isEmpty()) return
        store.update {
            if (it.initialized) it
            else it.copy(favorites = HomeLogic.defaultFavorites(installed), initialized = true)
        }
    }
}
