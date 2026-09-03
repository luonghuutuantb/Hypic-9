package com.projecth.editor

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import com.google.mlkit.vision.segmentation.SegmentationMask
import java.nio.ByteBuffer
import kotlin.math.roundToInt

/**
 * V13: real on-device selfie segmentation.
 * Returns foreground confidence per pixel in the model mask.
 */
class SegmentationService(context: Context) {
    private val options = SelfieSegmenterOptions.Builder()
        .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
        .enableRawSizeMask()
        .build()

    private val segmenter = Segmentation.getClient(options)

    fun process(bitmap: Bitmap, onResult: (SegmentationResult?) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)
        segmenter.process(image)
            .addOnSuccessListener { mask ->
                onResult(readMask(mask))
            }
            .addOnFailureListener { onResult(null) }
    }

    private fun readMask(mask: SegmentationMask): SegmentationResult {
        val buffer: ByteBuffer = mask.buffer
        buffer.rewind()
        val w = mask.width
        val h = mask.height
        val values = FloatArray(w * h)
        var i = 0
        while (buffer.remaining() >= 4 && i < values.size) {
            values[i++] = buffer.float
        }
        return SegmentationResult(w, h, values)
    }
}

data class SegmentationResult(
    val width: Int,
    val height: Int,
    val confidence: FloatArray
) {
    fun at(x: Int, y: Int): Float {
        if (width <= 0 || height <= 0) return 0f
        val sx = (x.toFloat() / width).coerceIn(0f, 0.99999f)
        val sy = (y.toFloat() / height).coerceIn(0f, 0.99999f)
        val ix = (sx * width).roundToInt().coerceIn(0, width - 1)
        val iy = (sy * height).roundToInt().coerceIn(0, height - 1)
        return confidence[iy * width + ix].coerceIn(0f, 1f)
    }
}
