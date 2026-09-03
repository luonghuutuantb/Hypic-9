package com.projecth.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

enum class MaskMode { BRUSH, RADIAL, LINEAR, SUBJECT, FACE }

data class MaskLayer(
    val id: Long,
    val name: String,
    val mode: MaskMode,
    val feather: Float = 0.5f,
    val density: Float = 1f,
    val invert: Boolean = false,
    val bounds: RectF? = null,
    val points: List<Pair<Float, Float>> = emptyList()
)

object MaskEngine {
    fun radial(width: Int, height: Int, centerX: Float, centerY: Float, radius: Float, feather: Float, invert: Boolean): FloatArray {
        val out = FloatArray(width * height)
        val f = max(1f, radius * feather.coerceIn(0f, 1f))
        for (y in 0 until height) for (x in 0 until width) {
            val d = hypot(x - centerX, y - centerY)
            val a = ((radius - d + f) / f).coerceIn(0f, 1f)
            out[y * width + x] = if (invert) 1f - a else a
        }
        return out
    }

    fun brush(width: Int, height: Int, points: List<Pair<Float, Float>>, radius: Float, feather: Float, invert: Boolean): FloatArray {
        val out = FloatArray(width * height)
        val f = max(1f, radius * feather.coerceIn(0f, 1f))
        for (y in 0 until height) for (x in 0 until width) {
            var a = 0f
            for ((px, py) in points) {
                val d = hypot(x - px, y - py)
                val v = ((radius - d + f) / f).coerceIn(0f, 1f)
                if (v > a) a = v
            }
            out[y * width + x] = if (invert) 1f - a else a
        }
        return out
    }
}
