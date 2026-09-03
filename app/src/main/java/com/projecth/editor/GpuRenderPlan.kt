package com.projecth.editor

enum class GpuPass { BASE, ADJUSTMENTS, MASKS, BEAUTY, BACKGROUND, LAYERS, TONE_MAP }

data class GpuRenderPlan(
    val passes: List<GpuPass> = GpuPass.values().toList()
)
