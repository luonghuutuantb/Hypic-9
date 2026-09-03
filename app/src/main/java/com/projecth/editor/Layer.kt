package com.projecth.editor

enum class LayerType { TEXT, STICKER }

data class EditorLayer(
    val id: Long,
    val type: LayerType,
    val text: String = "",
    val sticker: String = "",
    val x: Float = .5f,
    val y: Float = .5f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val alpha: Float = 1f,
    val color: Int = 0xFFFFFFFF.toInt(),
    val size: Float = 0.08f,
    val font: String = "Sans",
    val strokeWidth: Float = 0f,
    val strokeColor: Int = 0xFF000000.toInt(),
    val shadow: Float = 0f,
    val shadowColor: Int = 0x99000000.toInt(),
    val bold: Boolean = false,
    val italic: Boolean = false,
    val visible: Boolean = true
)
