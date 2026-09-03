package com.projecth.editor

enum class AdjustmentType {
    EXPOSURE, CONTRAST, SATURATION, WARMTH, HIGHLIGHTS, SHADOWS,
    SMOOTH, SKIN_LIGHT, TEETH, EYE_BRIGHT,
    FACE_SLIM, EYE_SIZE, NOSE_SLIM, CHIN, MOUTH, FOREHEAD,
    CHEEK_LIFT, JAW, EYE_LIFT, LIP_SIZE, NOSE_HEIGHT,
    BODY_SLIM, WAIST_SLIM, BODY_HEIGHT,
    BG_BLUR, BG_DIM, BG_WARMTH, VIGNETTE
}

data class AdjustmentLayer(
    val id: Long,
    val name: String,
    val type: AdjustmentType,
    val amount: Float,
    val enabled: Boolean = true,
    val opacity: Float = 1f
)

class AdjustmentStack {
    private val items = mutableListOf<AdjustmentLayer>()
    fun all(): List<AdjustmentLayer> = items.toList()
    fun add(layer: AdjustmentLayer) { items += layer }
    fun setEnabled(id: Long, enabled: Boolean) {
        val i = items.indexOfFirst { it.id == id }
        if (i >= 0) items[i] = items[i].copy(enabled = enabled)
    }
    fun setOpacity(id: Long, opacity: Float) {
        val i = items.indexOfFirst { it.id == id }
        if (i >= 0) items[i] = items[i].copy(opacity = opacity.coerceIn(0f, 1f))
    }
    fun remove(id: Long) { items.removeAll { it.id == id } }
    fun moveUp(id: Long) {
        val i = items.indexOfFirst { it.id == id }
        if (i >= 0 && i < items.lastIndex) items.add(i + 1, items.removeAt(i))
    }
    fun moveDown(id: Long) {
        val i = items.indexOfFirst { it.id == id }
        if (i > 0) items.add(i - 1, items.removeAt(i))
    }
    fun clear() { items.clear() }
}
