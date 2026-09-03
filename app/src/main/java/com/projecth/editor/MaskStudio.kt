package com.projecth.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MaskStudio(
    masks: List<MaskLayer>,
    onAddRadial: () -> Unit,
    onAddBrush: () -> Unit,
    onDelete: (Long) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(12.dp)) {
        Text("Advanced Mask", style = MaterialTheme.typography.titleMedium)
        Text("Mask tách khỏi adjustment, cho phép chỉnh vùng cục bộ mà không phá ảnh gốc.",
            style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = onAddRadial) { Text("Radial") }
            OutlinedButton(onClick = onAddBrush) { Text("Brush") }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.heightIn(max = 240.dp)) {
            items(masks, key = { it.id }) { m ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${m.name} • ${m.mode}")
                    TextButton(onClick = { onDelete(m.id) }) { Text("Xóa") }
                }
            }
        }
    }
}
