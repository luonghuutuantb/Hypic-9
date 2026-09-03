package com.projecth.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProFinalPanel(
    settings: ProSettings = ProSettings(),
    onNewProject: () -> Unit = {},
    onSaveProject: () -> Unit = {},
    onUndo: () -> Unit = {},
    onRedo: () -> Unit = {}
) {
    Column(Modifier.fillMaxWidth().padding(12.dp)) {
        Text("Project H Pro", style = MaterialTheme.typography.titleMedium)
        Text(
            "Project, History, Mask và hiệu năng được tách khỏi bitmap gốc.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = onNewProject) { Text("New") }
            OutlinedButton(onClick = onSaveProject) { Text("Save") }
            OutlinedButton(onClick = onUndo) { Text("Undo") }
            OutlinedButton(onClick = onRedo) { Text("Redo") }
        }
        Spacer(Modifier.height(8.dp))
        Text("Preview: ${settings.previewMaxSide}px • Undo: ${settings.maxUndo} • Memory guard: ${(settings.maxImagePixels/1_000_000)}MP",
            style = MaterialTheme.typography.bodySmall)
    }
}
