package com.guitarvault.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.guitarvault.app.ui.viewmodel.SortMode

/**
 * Sort mode dropdown shown in the collection screen.
 */
@Composable
fun SortDropdown(
    selected: SortMode,
    onSelected: (SortMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(onClick = { expanded = true }) {
            Text("Sort: ${selected.displayName}")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SortMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.displayName) },
                    onClick = { onSelected(mode); expanded = false }
                )
            }
        }
    }
}

/**
 * Small linear progress bar showing spec completeness on guitar cards.
 */
@Composable
fun SpecCompletenessBar(
    completeness: Float,
    modifier: Modifier = Modifier
) {
    val percent = (completeness * 100).toInt()
    val color = when {
        completeness >= 0.8f -> MaterialTheme.colorScheme.tertiary
        completeness >= 0.4f -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.error
    }
    Row(
        modifier = modifier,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        LinearProgressIndicator(
            progress = { completeness },
            color = color,
            trackColor = color.copy(alpha = 0.2f),
            modifier = Modifier.width(60.dp).height(4.dp)
        )
        Text(
            text = "$percent%",
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
