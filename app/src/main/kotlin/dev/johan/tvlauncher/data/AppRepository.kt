package dev.johan.tvlauncher.data

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import java.text.Collator

/** Lists TV apps and re-emits whenever a package is installed, updated or removed. */
class AppRepository(private val context: Context) {

    private val pm: PackageManager = context.packageManager

    fun apps(): Flow<List<AppEntry>> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(query())
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        context.registerReceiver(receiver, filter)
        send(query())
        awaitClose { context.unregisterReceiver(receiver) }
    }.conflate().flowOn(Dispatchers.IO)

    private fun query(): List<AppEntry> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val collator = Collator.getInstance().apply { strength = Collator.PRIMARY }
        return pm.queryIntentActivities(intent, 0)
            .asSequence()
            .map { it.activityInfo }
            .filter { it.packageName != context.packageName && it.isEnabled && it.applicationInfo.enabled }
            .distinctBy { it.packageName }
            .map { info ->
                AppEntry(
                    key = info.packageName,
                    label = info.loadLabel(pm).toString().trim(),
                    component = ComponentName(info.packageName, info.name),
                )
            }
            .sortedWith { a, b -> collator.compare(a.label, b.label) }
            .toList()
    }
}
