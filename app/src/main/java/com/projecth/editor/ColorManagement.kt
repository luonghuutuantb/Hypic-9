package com.projecth.editor

enum class WorkingColorSpace { SRGB, DISPLAY_P3, LINEAR_SRGB }

data class ColorManagement(
    val workingSpace: WorkingColorSpace = WorkingColorSpace.SRGB,
    val outputProfile: WorkingColorSpace = WorkingColorSpace.SRGB
)
