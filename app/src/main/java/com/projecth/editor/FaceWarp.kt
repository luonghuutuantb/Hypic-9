package com.projecth.editor

import android.graphics.Bitmap
import kotlin.math.*

data class FaceWarpParams(
    val faceSlim:Float=0f,val eyeSize:Float=0f,val noseSlim:Float=0f,
    val chin:Float=0f,val mouth:Float=0f,val forehead:Float=0f,
    val cheekLift:Float=0f,val jaw:Float=0f,val eyeLift:Float=0f,
    val lipSize:Float=0f,val noseHeight:Float=0f
)

/** V11: contour-aware local mesh-like warp. Uses ML Kit contours when present. */
object FaceWarpEngine{
    private fun gaussian(d2:Float,sigma:Float)=exp(-d2/(2f*sigma*sigma))

    fun warp(source:Bitmap,face:FaceLandmarks,p:FaceWarpParams,contours:FaceContours?=null):Bitmap{
        val w=source.width;val h=source.height
        val out=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
        val src=source.copy(Bitmap.Config.ARGB_8888,false)
        val sp=IntArray(w*h);val dp=IntArray(w*h);src.getPixels(sp,0,w,0,0,w,h)
        val eyeDist=hypot(face.rightEye.x-face.leftEye.x,face.rightEye.y-face.leftEye.y).coerceAtLeast(1f)
        val fw=abs(face.rightTemple.x-face.leftTemple.x).coerceAtLeast(1f)
        val fh=abs(face.chin.y-face.forehead.y).coerceAtLeast(1f)
        fun sample(x:Float,y:Float):Int{val ix=x.roundToInt().coerceIn(0,w-1);val iy=y.roundToInt().coerceIn(0,h-1);return sp[iy*w+ix]}
        fun center(points:List<PointF2>):PointF2?=if(points.isEmpty())null else PointF2(points.map{it.x}.average().toFloat(),points.map{it.y}.average().toFloat())
        val leftEyeC=center(contours?.leftEye ?: emptyList()) ?: face.leftEye
        val rightEyeC=center(contours?.rightEye ?: emptyList()) ?: face.rightEye
        val lipC=center((contours?.upperLipTop.orEmpty()+contours?.lowerLipBottom.orEmpty())) ?: face.mouthCenter
        val noseC=center(contours?.noseBottom.orEmpty()) ?: face.noseTip

        for(y in 0 until h) for(x in 0 until w){
            var sx=x.toFloat();var sy=y.toFloat()
            val slim=p.faceSlim.coerceIn(0f,1f)
            if(slim>0f){
                val wl=gaussian((sx-face.leftCheek.x).pow(2)+(sy-face.leftCheek.y).pow(2),fw*.34f)*slim
                val wr=gaussian((sx-face.rightCheek.x).pow(2)+(sy-face.rightCheek.y).pow(2),fw*.34f)*slim
                sx+=(face.leftCheek.x-sx)*wl*.16f;sx+=(face.rightCheek.x-sx)*wr*.16f
            }
            val jaw=p.jaw.coerceIn(-1f,1f)
            if(abs(jaw)>.001f){
                val lower=((sy-face.mouthCenter.y)/(fh*.55f)).coerceIn(0f,1.4f)
                val side=(abs(sx-face.mouthCenter.x)/(fw*.5f)).coerceIn(0f,1.4f)
                val wt=lower*side
                sx += (if(sx<face.mouthCenter.x) 1f else -1f)*jaw*fw*.035f*wt
                sy -= jaw*fh*.012f*wt
            }
            val eye=p.eyeSize.coerceIn(-1f,1f)
            if(abs(eye)>.001f) for(c0 in listOf(leftEyeC,rightEyeC)){
                val dx=sx-c0.x;val dy=sy-c0.y;val d=hypot(dx,dy);val r=eyeDist*.60f
                if(d<r){val wt=(1f-d/r)*eye;sx=c0.x+(sx-c0.x)*(1f-wt*.22f);sy=c0.y+(sy-c0.y)*(1f-wt*.22f)}
            }
            val lift=p.eyeLift.coerceIn(-1f,1f)
            if(abs(lift)>.001f) for(c0 in listOf(leftEyeC,rightEyeC)){
                val dx=sx-c0.x;val dy=sy-c0.y;val wt=gaussian(dx*dx+dy*dy,eyeDist*.62f)
                sy-=lift*eyeDist*.12f*wt*(if(c0.x<face.mouthCenter.x)1f else 1f)
            }
            val ns=p.noseSlim.coerceIn(-1f,1f)
            if(abs(ns)>.001f){val dx=sx-noseC.x;val dy=sy-noseC.y;val wt=gaussian(dx*dx+dy*dy,eyeDist*.55f);sx+=dx*wt*ns*.16f}
            val nh=p.noseHeight.coerceIn(-1f,1f)
            if(abs(nh)>.001f){val dx=sx-noseC.x;val dy=sy-noseC.y;val wt=gaussian(dx*dx+dy*dy,eyeDist*.70f);sy-=nh*eyeDist*.10f*wt}
            val chin=p.chin.coerceIn(-1f,1f)
            if(abs(chin)>.001f){val dx=sx-face.chin.x;val dy=sy-face.chin.y;sy-=chin*fh*.07f*gaussian(dx*dx+dy*dy,fw*.42f)}
            val mouth=p.mouth.coerceIn(-1f,1f)
            if(abs(mouth)>.001f){val dx=sx-lipC.x;val dy=sy-lipC.y;sy-=mouth*eyeDist*.11f*gaussian(dx*dx+dy*dy,eyeDist*.70f)}
            val lip=p.lipSize.coerceIn(-1f,1f)
            if(abs(lip)>.001f){val dx=sx-lipC.x;val dy=sy-lipC.y;val wt=gaussian(dx*dx+dy*dy,eyeDist*.65f);sx=lipC.x+(sx-lipC.x)*(1f-lip*wt*.14f);sy=lipC.y+(sy-lipC.y)*(1f-lip*wt*.14f)}
            val forehead=p.forehead.coerceIn(-1f,1f)
            if(abs(forehead)>.001f){val dx=sx-face.forehead.x;val dy=sy-face.forehead.y;sy+=forehead*fh*.03f*gaussian(dx*dx+dy*dy,fw*.44f)}
            val liftCheek=p.cheekLift.coerceIn(-1f,1f)
            if(abs(liftCheek)>.001f) for(c0 in listOf(face.leftCheek,face.rightCheek)){val dx=sx-c0.x;val dy=sy-c0.y;sy-=liftCheek*fh*.022f*gaussian(dx*dx+dy*dy,fw*.28f)}
            dp[y*w+x]=sample(sx,sy)
        }
        out.setPixels(dp,0,w,0,0,w,h);src.recycle();return out
    }
}
