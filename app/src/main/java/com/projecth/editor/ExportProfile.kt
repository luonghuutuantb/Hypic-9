package com.projecth.editor

data class ExportProfile(
    val format: String = "JPG",
    val quality: Int = 92,
    val maxSide: Int = 0,
    val colorSpace: WorkingColorSpace = WorkingColorSpace.SRGB,
    val bitDepth: BitDepth = BitDepth.UINT8,
    val hdr: OutputRange = OutputRange.SDR,
    val preserveMetadata: Boolean = true
)
