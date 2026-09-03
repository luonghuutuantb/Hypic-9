package com.projecth.editor

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.*

object SmartEraserEngine {
    /**
     * V18 offline content-aware-ish healing foundation.
     * Uses several surrounding patches instead of a single clone direction.
     */
    fun heal(
        source: Bitmap,
        centerX: Int,
        centerY: Int,
        radius: Int,
        strength: Float = 1f
    ): Bitmap {
        if (radius < 2) return source
        val w=source.width; val h=source.height
        val src=IntArray(w*h); source.getPixels(src,0,w,0,0,w,h)
        val out=src.copyOf()
        val r=radius.coerceIn(2,min(w,h)/3)
        val offsets=listOf(
            -r*2 to 0, r*2 to 0, 0 to -r*2, 0 to r*2,
            -r to -r, r to -r, -r to r, r to r
        )
        for(y in max(0,centerY-r)..min(h-1,centerY+r))
            for(x in max(0,centerX-r)..min(w-1,centerX+r)){
                val d=hypot((x-centerX).toFloat(),(y-centerY).toFloat())
                if(d>r) continue
                val feather=((1f-d/r).coerceIn(0f,1f)).pow(.65f)*strength.coerceIn(0f,1f)
                var sr=0;var sg=0;var sb=0;var n=0
                for((ox,oy) in offsets){
                    val sx=(x+ox).coerceIn(0,w-1)
                    val sy=(y+oy).coerceIn(0,h-1)
                    val q=src[sy*w+sx]
                    sr+=Color.red(q);sg+=Color.green(q);sb+=Color.blue(q);n++
                }
                if(n>0){
                    val base=out[y*w+x]
                    val rr=sr/n;val gg=sg/n;val bb=sb/n
                    out[y*w+x]=Color.rgb(
                        (Color.red(base)*(1-feather)+rr*feather).roundToInt().coerceIn(0,255),
                        (Color.green(base)*(1-feather)+gg*feather).roundToInt().coerceIn(0,255),
                        (Color.blue(base)*(1-feather)+bb*feather).roundToInt().coerceIn(0,255)
                    )
                }
            }
        source.setPixels(out,0,w,0,0,w,h)
        return source
    }
}
