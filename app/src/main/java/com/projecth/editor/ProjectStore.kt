package com.projecth.editor

import android.content.Context
import java.io.File

class ProjectStore(private val context: Context) {
    private val dir by lazy { File(context.filesDir, "projects").apply { mkdirs() } }

    fun save(name: String, state: ProjectState): File {
        val safe = name.trim().ifBlank { "project_h_project" }
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return File(dir, "$safe.projecth").also { it.writeText(state.toJson()) }
    }

    fun list(): List<File> = dir.listFiles()
        ?.filter { it.extension == "projecth" }
        ?.sortedByDescending { it.lastModified() }
        ?: emptyList()

    fun read(file: File): String = file.readText()
    fun delete(file: File): Boolean = file.delete()
}
