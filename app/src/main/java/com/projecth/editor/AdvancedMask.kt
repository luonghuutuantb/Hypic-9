package com.projecth.editor

enum class AdvancedMaskType { BRUSH, RADIAL, LINEAR, COLOR_RANGE, LUMINANCE_RANGE, SUBJECT, FACE }

data class AdvancedMask(
    val type: AdvancedMaskType = AdvancedMaskType.BRUSH,
    val amount: Float = 1f,
    val feather: Float = 0.5f,
    val invert: Boolean = false
)
