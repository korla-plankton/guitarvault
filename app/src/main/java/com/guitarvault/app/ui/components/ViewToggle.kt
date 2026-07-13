package com.guitarvault.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
                icon = {},
                label = {
                    Text(when (mode) {
                        CollectionViewModel.CollectionViewMode.LIST -> "☰"
                        CollectionViewModel.CollectionViewMode.GROUPED -> "▤"
                        CollectionViewModel.CollectionViewMode.GRID -> "▦"
                    })
                }
            )
        }
    }
}
