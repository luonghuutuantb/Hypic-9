package com.projecth.editor

data class ValidationIssue(val level: String, val message: String)

object ProjectValidation {
    fun validate(settings: IntegrationSettings): List<ValidationIssue> {
        val out = mutableListOf<ValidationIssue>()
        if (settings.hdr.enabled && settings.hdr.output == OutputRange.SDR) {
            out += ValidationIssue("warning", "HDR enabled but output is SDR")
        }
        if (settings.color.workingSpace == WorkingColorSpace.DISPLAY_P3 &&
            settings.color.outputProfile == WorkingColorSpace.SRGB) {
            out += ValidationIssue("info", "P3 working space will be converted to sRGB on export")
        }
        return out
    }
}
