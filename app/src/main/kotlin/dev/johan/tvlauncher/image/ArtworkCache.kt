package dev.johan.tvlauncher.image

import android.content.ComponentName
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Banner (or icon fallback) of an app, rasterised once at card size, plus its accent color. */
class Artwork(val image: ImageBitmap, val isBanner: Boolean, val accent: Color, internal val bytes: Int)

/**
 * Memory-bounded artwork cache. Drawables are rasterised off the main thread at the size cards
 * actually render, so scrolling never decodes and the footprint stays predictable on 2 GB TVs.
 */
class ArtworkCache(private val pm: PackageManager) {

    private val cache = object : LruCache<String, Artwork>(MAX_BYTES) {
        override fun sizeOf(key: String, value: Artwork) = value.bytes
    }

    fun peek(key: String): Artwork? = cache.get(key)

    suspend fun load(key: String, component: ComponentName): Artwork? {
        cache.get(key)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching { render(component) }.getOrNull()?.also { cache.put(key, it) }
        }
    }

    /** Called when the launcher goes to the background: give memory back to the foreground app. */
    fun trim(fraction: Float) {
        if (fraction <= 0f) cache.evictAll() else cache.trimToSize((MAX_BYTES * fraction).toInt())
    }

    private fun render(component: ComponentName): Artwork {
        val activity = pm.getActivityInfo(component, 0)
        val banner: Drawable? = activity.loadBanner(pm) ?: activity.applicationInfo.loadBanner(pm)
        val bitmap = if (banner != null) {
            banner.toBitmap(BANNER_WIDTH, BANNER_HEIGHT)
        } else {
            activity.loadIcon(pm).toBitmap(ICON_SIZE, ICON_SIZE)
        }
        val palette = Palette.from(bitmap).maximumColorCount(12).generate()
        val swatch = palette.vibrantSwatch ?: palette.dominantSwatch
        val accent = swatch?.let { Color(it.rgb) } ?: Color(0xFF3A4255)
        return Artwork(bitmap.asImageBitmap(), banner != null, accent, bitmap.allocationByteCount)
    }

    private fun Drawable.toBitmap(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        setBounds(0, 0, width, height)
        draw(canvas)
        return bitmap
    }

    private companion object {
        const val BANNER_WIDTH = 384
        const val BANNER_HEIGHT = 216
        const val ICON_SIZE = 128
        const val MAX_BYTES = 24 * 1024 * 1024
    }
}
