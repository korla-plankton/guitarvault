package com.guitarvault.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.guitarvault.app.data.model.ConditionRating
import com.guitarvault.app.data.model.ConditionRecord
import com.guitarvault.app.data.model.Guitar
import com.guitarvault.app.data.model.MaintenanceEntry
import com.guitarvault.app.data.model.MaintenanceType
import com.guitarvault.app.ui.components.ConditionBadge
import com.guitarvault.app.ui.components.SpecSection
import com.guitarvault.app.ui.components.formatCurrency
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ConditionTab(
    guitar: Guitar,
    onAddCondition: (ConditionRecord) -> Unit,
    onAddMaintenance: (MaintenanceEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    var showConditionDialog by remember { mutableStateOf(false) }
    var showMaintenanceDialog by remember { mutableStateOf(false) }
    val df = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Current condition
        SpecSection(title = "Current Condition") {
            guitar.currentCondition?.let { record ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        ConditionBadge(rating = record.rating)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(df.format(Date(record.recordedAt)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (record.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(record.notes, style = MaterialTheme.typography.bodyMedium)
                }
                if (record.issues.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Issues:", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    record.issues.forEach { issue ->
                        Text("• $issue", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } ?: Text("No condition records yet.", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { showConditionDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Record Condition")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Condition history
        if (guitar.conditionHistory.size > 1) {
            SpecSection(title = "Condition History") {
                guitar.conditionHistory.sortedByDescending { it.recordedAt }.forEach { record ->
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ConditionBadge(rating = record.rating)
                        Text(df.format(Date(record.recordedAt)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Maintenance log
        SpecSection(title = "Maintenance Log") {
            if (guitar.maintenanceLog.isEmpty()) {
                Text("No maintenance records yet.", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                guitar.maintenanceLog.sortedByDescending { it.date }.forEach { entry ->
                    HorizontalDivider()
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(entry.type.displayName, style = MaterialTheme.typography.bodyMedium)
                            Text(df.format(Date(entry.date)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(entry.description, style = MaterialTheme.typography.bodyMedium)
                        entry.cost?.let { Text(formatCurrency(it), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        if (entry.technician.isNotBlank()) {
                            Text("Tech: ${entry.technician}", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { showMaintenanceDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Log Maintenance")
            }
        }
    }

    if (showConditionDialog) {
        AddConditionDialog(
            onConfirm = { rating, notes, issues ->
                onAddCondition(ConditionRecord(
                    rating = rating, notes = notes,
                    issues = issues.split("\n").filter { it.isNotBlank() }
                ))
                showConditionDialog = false
            },
            onDismiss = { showConditionDialog = false }
        )
    }
    if (showMaintenanceDialog) {
        AddMaintenanceDialog(
            onConfirm = { type, desc, cost, tech ->
                onAddMaintenance(MaintenanceEntry(
                    type = type, description = desc, cost = cost, technician = tech
                ))
                showMaintenanceDialog = false
            },
            onDismiss = { showMaintenanceDialog = false }
        )
    }
}

@Composable
private fun AddConditionDialog(
    onConfirm: (ConditionRating, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var rating by remember { mutableStateOf(ConditionRating.EXCELLENT) }
    var notes by remember { mutableStateOf("") }
    var issues by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Condition") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    OutlinedButton(onClick = { expanded = true }) {
                        Text(rating.displayName)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        ConditionRating.entries.sortedByDescending { it.sortOrder }.forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r.displayName) },
                                onClick = { rating = r; expanded = false }
                            )
                        }
                    }
                }
                OutlinedTextField(value = notes, onValueChange = { notes = it },
                    label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = issues, onValueChange = { issues = it },
                    label = { Text("Issues (one per line)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(rating, notes, issues) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AddMaintenanceDialog(
    onConfirm: (MaintenanceType, String, Double?, String) -> Unit,
    onDismiss: () -> Unit
) {
    var type by remember { mutableStateOf(MaintenanceType.SETUP) }
    var desc by remember { mutableStateOf("") }
    var costStr by remember { mutableStateOf("") }
    var tech by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Maintenance") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    OutlinedButton(onClick = { expanded = true }) { Text(type.displayName) }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        MaintenanceType.entries.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.displayName) },
                                onClick = { type = t; expanded = false }
                            )
                        }
                    }
                }
                OutlinedTextField(value = desc, onValueChange = { desc = it },
                    label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = costStr, onValueChange = { costStr = it },
                    label = { Text("Cost ($)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = tech, onValueChange = { tech = it },
                    label = { Text("Technician") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(type, desc, costStr.toDoubleOrNull(), tech)
            }, enabled = desc.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
