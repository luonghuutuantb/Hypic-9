package com.projecth.editor

data class BatchPhoto(
    val id: Long,
    val name: String,
    val uriString: String,
    val selected: Boolean = true
)

data class EditorTemplate(
    val id: String,
    val name: String,
    val emoji: String,
    val filter: FilterType = FilterType.NONE,
    val exposure: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val warmth: Float = 0f,
    val skinBright: Float = 0f,
    val smooth: Float = 0f
)

object BuiltInTemplates {
    val all = listOf(
        EditorTemplate("clean","Clean","✨",FilterType.NONE, .05f,.05f,.04f,0f,.08f,.08f),
        EditorTemplate("film","Film","🎞️",FilterType.FILM,0f,.10f,.04f,-.04f,.05f,.10f),
        EditorTemplate("warm","Warm","☀️",FilterType.WARM,.03f,.04f,.06f,.18f,.08f,.08f),
        EditorTemplate("cool","Cool","❄️",FilterType.COOL,0f,.05f,.02f,-.15f,.04f,.06f),
        EditorTemplate("mono","Mono","🖤",FilterType.BW,0f,.12f,-1f,0f,.02f,.04f)
    )
}
