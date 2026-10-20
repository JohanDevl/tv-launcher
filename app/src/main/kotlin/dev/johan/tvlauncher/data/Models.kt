package dev.johan.tvlauncher.data

import android.content.ComponentName

/** A launchable TV app. [key] is the package name: stable across app updates. */
data class AppEntry(
    val key: String,
    val label: String,
    val component: ComponentName,
)

/** A connected physical input (HDMI, composite...) that can be opened directly. */
data class InputEntry(
    val id: String,
    val label: String,
)

data class HomeState(
    val loading: Boolean = true,
    val favorites: List<AppEntry> = emptyList(),
    val apps: List<AppEntry> = emptyList(),
    val hiddenApps: List<AppEntry> = emptyList(),
    val inputs: List<InputEntry> = emptyList(),
    val favoriteKeys: Set<String> = emptySet(),
)

data class LauncherSettings(
    val favorites: List<String> = emptyList(),
    val hidden: Set<String> = emptySet(),
    val initialized: Boolean = false,
)
