package com.projecth.editor

import org.json.JSONArray
import org.json.JSONObject

data class ProjectState(
    val version: Int = 1,
    val imageUri: String? = null,
    val adjustments: List<AdjustmentLayer> = emptyList(),
    val masks: List<MaskLayer> = emptyList(),
    val layers: List<EditorLayer> = emptyList()
) {
    fun toJson(): String = JSONObject().apply {
        put("version", version)
        put("imageUri", imageUri)
        put("adjustments", JSONArray().apply {
            adjustments.forEach { a ->
                put(JSONObject().apply {
                    put("id", a.id); put("name", a.name); put("type", a.type.name)
                    put("amount", a.amount); put("enabled", a.enabled); put("opacity", a.opacity)
                })
            }
        })
        put("masks", JSONArray().apply {
            masks.forEach { m ->
                put(JSONObject().apply {
                    put("id", m.id); put("name", m.name); put("mode", m.mode.name)
                    put("feather", m.feather); put("density", m.density); put("invert", m.invert)
                    m.bounds?.let { b ->
                        put("left", b.left); put("top", b.top); put("right", b.right); put("bottom", b.bottom)
                    }
                })
            }
        })
    }.toString()
}
