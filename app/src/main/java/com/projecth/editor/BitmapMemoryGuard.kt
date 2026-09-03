package com.projecth.editor

import android.graphics.Bitmap
import kotlin.math.max
import kotlin.math.min

object BitmapMemoryGuard {
    data class Target(val width: Int, val height: Int, val scale: Float)

    fun target(width: Int, height: Int, maxPixels: Long = 18_000_000L): Target {
        val pixels = width.toLong() * height.toLong()
        if (pixels <= maxPixels) return Target(width, height, 1f)
        val scale = kotlin.math.sqrt(maxPixels.toDouble() / pixels.toDouble()).toFloat()
        return Target(
            max(1, (width * scale).toInt()),
            max(1, (height * scale).toInt()),
            min(1f, scale)
        )
    }

    fun downsample(bitmap: Bitmap, maxPixels: Long = 18_000_000L): Bitmap {
        val t = target(bitmap.width, bitmap.height, maxPixels)
        return if (t.scale >= .999f) bitmap
        else Bitmap.createScaledBitmap(bitmap, t.width, t.height, true)
    }
}
