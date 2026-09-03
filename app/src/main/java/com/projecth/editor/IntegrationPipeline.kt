package com.projecth.editor

import android.graphics.Bitmap

/**
 * Single integration point for the V26-V30 systems.
 * The current renderer can consume the same model while newer GPU backends
 * are introduced incrementally.
 */
data class IntegrationSettings(
    val color: ColorManagement = ColorManagement(),
    val hdr: HdrSettings = HdrSettings(),
    val gpu: Boolean = true,
    val masks: List<AdvancedMask> = emptyList(),
    val curve: ToneCurve = ToneCurve(),
    val hsl: HslMixer = HslMixer(),
    val grading: ColorGrading = ColorGrading()
)

class IntegrationPipeline(
    private val settings: IntegrationSettings = IntegrationSettings()
) {
    fun prepare(bitmap: Bitmap): Bitmap =
        BitmapMemoryGuard.downsample(bitmap)

    fun render(bitmap: Bitmap): Bitmap {
        // Compatibility renderer entry point. Future GPU/tiled backends can
        // replace this method without changing the editor state model.
        return prepare(bitmap)
    }

    fun gpuPlan(): GpuRenderPlan = GpuRenderPlan()
}
