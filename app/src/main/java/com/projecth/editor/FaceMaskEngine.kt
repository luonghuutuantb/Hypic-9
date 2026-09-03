package com.projecth.editor

import android.graphics.PointF
import kotlin.math.*

/** V10: soft/feathered masks for face, eyes, lips and skin. */
object FaceMaskEngine {
    fun isFace(x:Int, y:Int, face:DetectedFace):Boolean =
        faceWeight(x.toFloat(), y.toFloat(), face) > 0.01f

    fun faceWeight(x:Float, y:Float, face:DetectedFace, feather:Float = 0.08f):Float {
        val c = face.contours
        if (c != null && c.faceOval.size >= 8) {
            val inside = insidePolygon(x, y, c.faceOval)
            if (inside) return 1f
            val d = distanceToPolygon(x, y, c.faceOval)
            val scale = maxOf(face.bounds.width, face.bounds.height).toFloat() * feather
            return (1f - d / max(1f, scale)).coerceIn(0f, 1f)
        }
        val b=face.bounds
        val nx=(x-b.centerX)/(b.width*.50f)
        val ny=(y-b.centerY)/(b.height*.53f)
        val q=nx*nx+ny*ny
        return (1f-q).coerceIn(0f,1f)
    }

    fun isSkinInFace(x:Int,y:Int,color:Int,face:DetectedFace,feather:Float=1f):Boolean {
        if (faceWeight(x.toFloat(), y.toFloat(), face) <= 0f) return false
        val r=(color shr 16) and 255
        val g=(color shr 8) and 255
        val b=color and 255
        val mx=max(r,max(g,b)); val mn=min(r,min(g,b))
        return r>45 && g>25 && b>15 && r>=g && g>=b*.70f && r-g>4 && mx-mn>10
    }

    fun featureWeight(x:Float,y:Float,points:List<PointF2>,pad:Float=0f,feather:Float=0.04f):Float {
        if(points.size<3) return 0f
        if(inFeature(x,y,points,pad)) return 1f
        val d=distanceToPolygon(x,y,points)
        val boxW=(points.maxOf{it.x}-points.minOf{it.x}).coerceAtLeast(1f)
        val boxH=(points.maxOf{it.y}-points.minOf{it.y}).coerceAtLeast(1f)
        val scale=max(boxW,boxH)*feather
        return (1f-d/max(1f,scale)).coerceIn(0f,1f)
    }

    fun inFeature(x:Float,y:Float,points:List<PointF2>,pad:Float=1f):Boolean {
        if(points.size < 3) return false
        if(insidePolygon(x,y,points)) return true
        return pad > 0f && distanceToPolygon(x,y,points) <= pad
    }

    fun inFeature(x:Int,y:Int,points:List<PointF2>,pad:Float=1f):Boolean =
        inFeature(x.toFloat(),y.toFloat(),points,pad)

    private fun insidePolygonPadded(x:Float,y:Float,points:List<PointF2>,pad:Float):Boolean {
        if(insidePolygon(x,y,points)) return true
        if(pad <= 0f) return false
        return distanceToPolygon(x,y,points) <= pad
    }

    private fun insidePolygon(x:Float,y:Float,p:List<PointF2>):Boolean {
        var inside=false; var j=p.lastIndex
        for(i in p.indices){
            val xi=p[i].x; val yi=p[i].y; val xj=p[j].x; val yj=p[j].y
            val cross=((yi>y)!=(yj>y)) && (x < (xj-xi)*(y-yi)/(yj-yi+0.00001f)+xi)
            if(cross) inside=!inside
            j=i
        }
        return inside
    }

    private fun distanceToPolygon(x:Float,y:Float,p:List<PointF2>):Float {
        var best=Float.MAX_VALUE
        for(i in p.indices){
            val a=p[i]; val b=p[(i+1)%p.size]
            best=min(best, distanceToSegment(x,y,a.x,a.y,b.x,b.y))
        }
        return best
    }

    private fun distanceToSegment(px:Float,py:Float,ax:Float,ay:Float,bx:Float,by:Float):Float {
        val dx=bx-ax; val dy=by-ay
        val len2=dx*dx+dy*dy
        if(len2<=0.0001f) return hypot(px-ax,py-ay)
        val t=((px-ax)*dx+(py-ay)*dy)/len2
        val u=t.coerceIn(0f,1f)
        return hypot(px-(ax+u*dx),py-(ay+u*dy))
    }
}
