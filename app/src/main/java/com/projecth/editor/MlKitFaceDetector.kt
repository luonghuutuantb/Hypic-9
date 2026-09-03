package com.projecth.editor

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark

/** V9: ML Kit landmarks for every face + contour pass for the most prominent face. */
object MlKitFaceDetector {
    private val landmarkOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setMinFaceSize(0.08f)
        .build()

    private val contourOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
        .setMinFaceSize(0.08f)
        .build()

    fun detect(context: Context, bitmap: Bitmap, onResult: (List<DetectedFace>) -> Unit) {
        val landmarkDetector = FaceDetection.getClient(landmarkOptions)
        landmarkDetector.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { faces ->
                val mapped = faces.mapIndexed { index, face -> mapFace(index, face) }.toMutableList()
                val contourDetector = FaceDetection.getClient(contourOptions)
                contourDetector.process(InputImage.fromBitmap(bitmap, 0))
                    .addOnSuccessListener { contourFaces ->
                        if (contourFaces.isNotEmpty() && mapped.isNotEmpty()) {
                            val cf = contourFaces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }!!
                            val target = nearest(mapped, cf)
                            val c = mapContours(cf)
                            val idx = mapped.indexOf(target)
                            if (idx >= 0) mapped[idx] = target.copy(contours = c, source = "ML Kit + Contour")
                        }
                        onResult(mapped)
                        contourDetector.close()
                        landmarkDetector.close()
                    }
                    .addOnFailureListener {
                        onResult(mapped)
                        contourDetector.close()
                        landmarkDetector.close()
                    }
            }
            .addOnFailureListener {
                onResult(FaceDetectorFallback.detectFaces(bitmap).take(5))
                landmarkDetector.close()
            }
    }

    private fun nearest(faces: List<DetectedFace>, contourFace: Face): DetectedFace {
        val cx = contourFace.boundingBox.centerX().toFloat()
        val cy = contourFace.boundingBox.centerY().toFloat()
        return faces.minBy {
            val dx = it.bounds.centerX - cx
            val dy = it.bounds.centerY - cy
            dx * dx + dy * dy
        }
    }

    private fun points(f: Face, type: Int): List<PointF2> =
        f.getContour(type)?.points?.map { PointF2(it.x, it.y) } ?: emptyList()

    private fun mapContours(f: Face): FaceContours = FaceContours(
        faceOval = points(f, FaceContour.FACE),
        leftEye = points(f, FaceContour.LEFT_EYE),
        rightEye = points(f, FaceContour.RIGHT_EYE),
        upperLipTop = points(f, FaceContour.UPPER_LIP_TOP),
        upperLipBottom = points(f, FaceContour.UPPER_LIP_BOTTOM),
        lowerLipTop = points(f, FaceContour.LOWER_LIP_TOP),
        lowerLipBottom = points(f, FaceContour.LOWER_LIP_BOTTOM),
        noseBridge = points(f, FaceContour.NOSE_BRIDGE),
        noseBottom = points(f, FaceContour.NOSE_BOTTOM),
        leftEyebrow = points(f, FaceContour.LEFT_EYEBROW_TOP) + points(f, FaceContour.LEFT_EYEBROW_BOTTOM),
        rightEyebrow = points(f, FaceContour.RIGHT_EYEBROW_TOP) + points(f, FaceContour.RIGHT_EYEBROW_BOTTOM)
    )

    private fun mapFace(id: Int, f: Face): DetectedFace {
        val b = f.boundingBox
        val box = RectF2(b.left.toFloat(), b.top.toFloat(), b.right.toFloat(), b.bottom.toFloat())
        val c = box.centerX; val top = box.top; val w = box.width; val h = box.height
        fun lm(type: Int, fallback: PointF2): PointF2 {
            val p = f.getLandmark(type)?.position
            return p?.let { PointF2(it.x, it.y) } ?: fallback
        }
        val leftEye = lm(FaceLandmark.LEFT_EYE, PointF2(box.left+w*.32f, top+h*.32f))
        val rightEye = lm(FaceLandmark.RIGHT_EYE, PointF2(box.left+w*.68f, top+h*.32f))
        val nose = lm(FaceLandmark.NOSE_BASE, PointF2(c, top+h*.53f))
        val leftCheek = lm(FaceLandmark.LEFT_CHEEK, PointF2(box.left+w*.20f, top+h*.55f))
        val rightCheek = lm(FaceLandmark.RIGHT_CHEEK, PointF2(box.left+w*.80f, top+h*.55f))
        val leftMouth = lm(FaceLandmark.MOUTH_LEFT, PointF2(c-w*.10f, top+h*.68f))
        val rightMouth = lm(FaceLandmark.MOUTH_RIGHT, PointF2(c+w*.10f, top+h*.68f))
        val mouth = lm(FaceLandmark.MOUTH_BOTTOM, PointF2(c, top+h*.72f))
        val bridge = PointF2(c, top+h*.40f)
        val chin = PointF2(c, box.bottom-h*.02f)
        val forehead = PointF2(c, top+h*.05f)
        val leftTemple = PointF2(box.left+w*.15f, top+h*.30f)
        val rightTemple = PointF2(box.left+w*.85f, top+h*.30f)
        val confidence = listOfNotNull(f.smilingProbability, f.leftEyeOpenProbability, f.rightEyeOpenProbability)
            .let { if (it.isEmpty()) .90f else (.82f + it.average().toFloat()*.18f).coerceIn(.82f,.99f) }
        return DetectedFace(id, box, FaceLandmarks(leftEye,rightEye,nose,mouth,chin,leftCheek,rightCheek,forehead,leftTemple,rightTemple,bridge,leftMouth,rightMouth), confidence, "ML Kit")
    }
}
