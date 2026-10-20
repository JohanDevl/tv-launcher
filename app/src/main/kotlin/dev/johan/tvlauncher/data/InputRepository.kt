package dev.johan.tvlauncher.data

import android.content.Context
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * HDMI inputs with a device plugged in. When an HDMI-CEC device is listed under its port,
 * only the device is kept: its name ("Apple TV") is more useful than "HDMI 1".
 * Analog inputs are skipped: they cannot detect a connection and always report "connected".
 */
class InputRepository(private val context: Context) {

    fun inputs(): Flow<List<InputEntry>> {
        val manager = context.getSystemService(TvInputManager::class.java) ?: return flowOf(emptyList())
        // Callbacks only signal a change; the binder queries run off the main thread.
        return callbackFlow {
            val callback = object : TvInputManager.TvInputCallback() {
                override fun onInputStateChanged(inputId: String, state: Int) { trySend(Unit) }
                override fun onInputAdded(inputId: String) { trySend(Unit) }
                override fun onInputRemoved(inputId: String) { trySend(Unit) }
                override fun onInputUpdated(inputId: String) { trySend(Unit) }
            }
            manager.registerCallback(callback, Handler(Looper.getMainLooper()))
            awaitClose { manager.unregisterCallback(callback) }
        }
            .onStart { emit(Unit) }
            .conflate()
            .map { query(manager) }
            .flowOn(Dispatchers.IO)
    }

    private fun query(manager: TvInputManager): List<InputEntry> = runCatching {
        // Philips reports empty HDMI ports as CONNECTED_STANDBY, so only CONNECTED counts.
        val connected = manager.tvInputList.filter {
            it.type == TvInputInfo.TYPE_HDMI && !it.isHidden(context) &&
                manager.getInputState(it.id) == TvInputManager.INPUT_STATE_CONNECTED
        }
        val parents = connected.mapNotNullTo(HashSet()) { it.parentId }
        connected
            .filter { it.id !in parents }
            .map { InputEntry(id = it.id, label = it.displayLabel()) }
    }.getOrDefault(emptyList())

    private fun TvInputInfo.displayLabel(): String =
        (loadCustomLabel(context) ?: loadLabel(context)).toString().trim()
}
