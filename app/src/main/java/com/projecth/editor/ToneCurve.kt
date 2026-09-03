package com.projecth.editor

data class CurvePoint(val x:Float,val y:Float)

class ToneCurve(points:List<CurvePoint> = listOf(CurvePoint(0f,0f),CurvePoint(1f,1f))) {
    val points:List<CurvePoint> = points.sortedBy { it.x }.take(32)
    fun sample(x:Float):Float {
        if(points.isEmpty()) return x
        if(x<=points.first().x) return points.first().y
        if(x>=points.last().x) return points.last().y
        for(i in 0 until points.lastIndex){
            val a=points[i]; val b=points[i+1]
            if(x in a.x..b.x){
                val t=(x-a.x)/(b.x-a.x).coerceAtLeast(.0001f)
                return a.y+(b.y-a.y)*t
            }
        }
        return x
    }
}
