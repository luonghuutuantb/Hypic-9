package com.projecth.editor

data class ProSettings(
    val previewMaxSide: Int = 2048,
    val maxUndo: Int = 40,
    val maxImagePixels: Long = 18_000_000L,
    val highQualityExport: Boolean = true
)
