package com.guitarvault.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.guitarvault.app.ui.viewmodel.CollectionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewToggle(
    selected: CollectionViewModel.CollectionViewMode,
    onSelected: (CollectionViewModel.CollectionViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        CollectionViewModel.CollectionViewMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = selected == mode,
                onClick = { onSelected(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = 3),
                icon = {
                    when (mode) {
                        CollectionViewModel.CollectionViewMode.LIST -> Icon(Icons.Default.List, contentDescription = null)
                        CollectionViewModel.CollectionViewMode.GROUPED -> Icon(Icons.Default.List, contentDescription = null)
                        CollectionViewModel.CollectionViewMode.GRID -> Icon(Icons.Default.GridView, contentDescription = null)
                    }
                },
                label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
            )
        }
    }
}
