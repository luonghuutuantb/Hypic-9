package com.projecth.editor

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions

data class SmartObject(
    val id: Int,
    val bounds: Rect,
    val label: String,
    val confidence: Float
)

class SmartObjectDetector {
    private val options = ObjectDetectorOptions.Builder()
        .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
        .enableMultipleObjects()
        .enableClassification()
        .build()

    private val detector = ObjectDetection.getClient(options)

    fun detect(bitmap: Bitmap, onResult: (List<SmartObject>) -> Unit) {
        detector.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { objects ->
                val result = objects.map { obj ->
                    val label = obj.labels.maxByOrNull { it.confidence }
                    SmartObject(
                        id = obj.trackingId ?: obj.boundingBox.hashCode(),
                        bounds = obj.boundingBox,
                        label = label?.text ?: "Object",
                        confidence = label?.confidence ?: 0f
                    )
                }
                onResult(result)
            }
            .addOnFailureListener { onResult(emptyList()) }
    }
}
