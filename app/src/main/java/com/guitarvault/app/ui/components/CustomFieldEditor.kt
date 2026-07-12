package com.guitarvault.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.guitarvault.app.data.model.CustomField
import com.guitarvault.app.data.model.CustomFieldType

/**
 * Editor for managing custom user-defined fields on a guitar.
 */
@Composable
fun CustomFieldEditor(
    fields: List<CustomField>,
    onAdd: (CustomField) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        fields.forEach { field ->
            CustomFieldRow(field = field, onRemove = { onRemove(field.id) })
        }
        TextButton(onClick = { showAddDialog = true }) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add Custom Field")
        }
    }

    if (showAddDialog) {
        AddCustomFieldDialog(
            onConfirm = { name, value, type ->
                onAdd(CustomField(name = name, value = value, fieldType = type))
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun CustomFieldRow(field: CustomField, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = field.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = field.value,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = field.fieldType.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Delete, contentDescription = "Remove field")
        }
    }
}

@Composable
private fun AddCustomFieldDialog(
    onConfirm: (String, String, CustomFieldType) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(CustomFieldType.TEXT) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Field") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Field Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Value") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Type: ")
                    Spacer(modifier = Modifier.width(8.dp))
                    CustomFieldTypeDropdown(selected = type, onSelected = { type = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name, value, type) },
                enabled = name.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CustomFieldTypeDropdown(
    selected: CustomFieldType,
    onSelected: (CustomFieldType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) { Text(selected.displayName) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            CustomFieldType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.displayName) },
                    onClick = { onSelected(type); expanded = false }
                )
            }
        }
    }
}
