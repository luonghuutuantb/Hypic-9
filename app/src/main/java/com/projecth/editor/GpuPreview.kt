package com.projecth.editor

import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/** Hardware-composited preview helper. Bitmap effects remain deterministic in the editor pipeline. */
fun Modifier.projectHGpuPreview(enabled: Boolean = true): Modifier =
    if (enabled && Build.VERSION.SDK_INT >= 29) {
        this.graphicsLayer {
            // Forces the preview into a hardware-composited layer on supported Android versions.
            compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Auto
        }
    } else this
