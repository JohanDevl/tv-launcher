package dev.johan.tvlauncher.ui.home

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import dev.johan.tvlauncher.R
import dev.johan.tvlauncher.data.AppEntry
import dev.johan.tvlauncher.data.InputEntry

/** Intents fired from the home screen. Every launch fails soft with a toast. */
object Actions {

    private const val PHILIPS_LIVE_TV = "org.droidtv.playtv"
    private const val ACTION_VIEW_INPUTS = "com.android.tv.action.VIEW_INPUTS"

    fun launchApp(context: Context, app: AppEntry) {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
            .setComponent(app.component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        start(context, intent, app.label)
    }

    fun openInput(context: Context, input: InputEntry) {
        val intent = Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(input.id))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        start(context, intent, input.label)
    }

    fun openLiveTv(context: Context) {
        val intent = context.packageManager.getLeanbackLaunchIntentForPackage(PHILIPS_LIVE_TV)
            ?: Intent(Intent.ACTION_VIEW, TvContract.Channels.CONTENT_URI)
        start(context, intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), context.getString(R.string.action_live_tv))
    }

    fun openSources(context: Context) =
        start(context, Intent(ACTION_VIEW_INPUTS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), context.getString(R.string.action_sources))

    fun openSettings(context: Context) =
        start(context, Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), context.getString(R.string.action_settings))

    fun openAppInfo(context: Context, app: AppEntry) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", app.key, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        start(context, intent, app.label)
    }

    @Suppress("DEPRECATION")
    fun uninstall(context: Context, app: AppEntry) {
        val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE, Uri.fromParts("package", app.key, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        start(context, intent, app.label)
    }

    fun canOpenSources(context: Context): Boolean =
        Intent(ACTION_VIEW_INPUTS).resolveActivity(context.packageManager) != null

    private fun start(context: Context, intent: Intent, label: String) {
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            fail(context, label)
        } catch (e: SecurityException) {
            fail(context, label)
        }
    }

    private fun fail(context: Context, label: String) {
        Toast.makeText(context, context.getString(R.string.launch_failed, label), Toast.LENGTH_SHORT).show()
    }
}
