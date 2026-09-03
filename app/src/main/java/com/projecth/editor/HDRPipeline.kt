package com.projecth.editor

enum class OutputRange { SDR, HDR10, HLG }

data class HdrSettings(
    val enabled: Boolean = false,
    val output: OutputRange = OutputRange.SDR,
    val exposure: Float = 0f,
    val peakNits: Float = 1000f
)
