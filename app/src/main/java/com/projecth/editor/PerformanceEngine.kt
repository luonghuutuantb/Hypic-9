package com.projecth.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.LruCache
import java.io.InputStream
import kotlin.math.roundToInt

/**
 * V23 performance layer.
 * Keeps preview bitmaps bounded, reuses decoded images, and avoids accidental
 * full-resolution allocations during repeated picker/batch operations.
 */
object PerformanceEngine {
    private val cache = object : LruCache<String, Bitmap>(24 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int =
            value.byteCount / 1024
    }

    fun decodePreview(context: Context, uri: Uri, maxSide: Int = 4096): Bitmap? {
        val key = uri.toString() + "#" + maxSide
        cache.get(key)?.let { return it }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (maxOf(bounds.outWidth / sample, bounds.outHeight / sample) > maxSide) sample *= 2
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
            if (Build.VERSION.SDK_INT >= 26) inMutable = true
        }
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: return null
        cache.put(key, bitmap)
        return bitmap
    }

    fun clear() { cache.evictAll() }
}
