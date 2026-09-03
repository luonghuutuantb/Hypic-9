package com.projecth.editor

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class HslChannel(
    val hue:Float=0f,
    val saturation:Float=0f,
    val luminance:Float=0f
)

data class HslMixer(
    val red:HslChannel=HslChannel(),
    val orange:HslChannel=HslChannel(),
    val yellow:HslChannel=HslChannel(),
    val green:HslChannel=HslChannel(),
    val aqua:HslChannel=HslChannel(),
    val blue:HslChannel=HslChannel(),
    val purple:HslChannel=HslChannel(),
    val magenta:HslChannel=HslChannel()
)
