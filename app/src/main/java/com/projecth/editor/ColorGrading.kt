package com.projecth.editor

data class ColorWheel(val hue:Float=0f,val saturation:Float=0f)

data class ColorGrading(
    val shadows:ColorWheel=ColorWheel(),
    val midtones:ColorWheel=ColorWheel(),
    val highlights:ColorWheel=ColorWheel(),
    val balance:Float=0f,
    val blending:Float=50f
)
