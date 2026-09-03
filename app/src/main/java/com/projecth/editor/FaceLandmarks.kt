package com.projecth.editor

import android.graphics.Bitmap
import kotlin.math.*

data class PointF2(val x:Float,val y:Float)

data class FaceLandmarks(
    val leftEye:PointF2,val rightEye:PointF2,val noseTip:PointF2,
    val mouthCenter:PointF2,val chin:PointF2,val leftCheek:PointF2,
    val rightCheek:PointF2,val forehead:PointF2,val leftTemple:PointF2,
    val rightTemple:PointF2,val noseBridge:PointF2,val leftMouth:PointF2,
    val rightMouth:PointF2
)

data class FaceContours(
    val faceOval:List<PointF2> = emptyList(),
    val leftEye:List<PointF2> = emptyList(),
    val rightEye:List<PointF2> = emptyList(),
    val upperLipTop:List<PointF2> = emptyList(),
    val upperLipBottom:List<PointF2> = emptyList(),
    val lowerLipTop:List<PointF2> = emptyList(),
    val lowerLipBottom:List<PointF2> = emptyList(),
    val noseBridge:List<PointF2> = emptyList(),
    val noseBottom:List<PointF2> = emptyList(),
    val leftEyebrow:List<PointF2> = emptyList(),
    val rightEyebrow:List<PointF2> = emptyList()
)

data class DetectedFace(
    val id:Int,
    val bounds:RectF2,
    val landmarks:FaceLandmarks,
    val confidence:Float,
    val source:String,
    val contours:FaceContours? = null
)

data class RectF2(val left:Float,val top:Float,val right:Float,val bottom:Float){
    val width:Float get()=right-left
    val height:Float get()=bottom-top
    val centerX:Float get()=(left+right)/2f
    val centerY:Float get()=(top+bottom)/2f
}

object FaceDetectorFallback {
    fun detectFaces(bitmap:Bitmap):List<DetectedFace>{
        if(bitmap.width<160 || bitmap.height<160) return emptyList()
        val w=bitmap.width; val h=bitmap.height
        val box=RectF2(w*.20f,h*.08f,w*.80f,h*.92f)
        return listOf(makeFace(0,box,.20f))
    }
    private fun makeFace(id:Int,b:RectF2,score:Float):DetectedFace{
        val fw=b.width; val fh=b.height; val cx=b.centerX
        val lm=FaceLandmarks(
            PointF2(b.left+fw*.34f,b.top+fh*.31f),PointF2(b.left+fw*.66f,b.top+fh*.31f),
            PointF2(cx,b.top+fh*.52f),PointF2(cx,b.top+fh*.68f),PointF2(cx,b.top+fh*.96f),
            PointF2(b.left+fw*.19f,b.top+fh*.56f),PointF2(b.left+fw*.81f,b.top+fh*.56f),
            PointF2(cx,b.top+fh*.03f),PointF2(b.left+fw*.18f,b.top+fh*.30f),PointF2(b.left+fw*.82f,b.top+fh*.30f),
            PointF2(cx,b.top+fh*.39f),PointF2(cx-fw*.10f,b.top+fh*.68f),PointF2(cx+fw*.10f,b.top+fh*.68f)
        )
        return DetectedFace(id,b,lm,score,"Fallback")
    }
}
