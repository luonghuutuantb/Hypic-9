package com.projecth.editor

data class NonDestructiveState(
    val adjustments: List<AdjustmentLayer> = emptyList(),
    val masks: List<MaskLayer> = emptyList()
)

class NonDestructiveEditor {
    private val adjustments = mutableListOf<AdjustmentLayer>()
    private val masks = mutableListOf<MaskLayer>()

    fun state() = NonDestructiveState(adjustments.toList(), masks.toList())
    fun addAdjustment(layer: AdjustmentLayer) { adjustments += layer }
    fun addMask(mask: MaskLayer) { masks += mask }
    fun removeAdjustment(id: Long) { adjustments.removeAll { it.id == id } }
    fun removeMask(id: Long) { masks.removeAll { it.id == id } }
    fun toggleAdjustment(id: Long, enabled: Boolean) {
        val i = adjustments.indexOfFirst { it.id == id }
        if (i >= 0) adjustments[i] = adjustments[i].copy(enabled = enabled)
    }
}
