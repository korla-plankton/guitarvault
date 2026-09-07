package com.guitarvault.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import com.guitarvault.app.util.utcMidnightToLocal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConditionTab(
    guitar: Guitar,
    onAddCondition: (ConditionRecord) -> Unit,
    onAddMaintenance: (MaintenanceEntry) -> Unit,
    onUpdateMaintenance: (MaintenanceEntry) -> Unit = {},
    onDeleteMaintenance: (String) -> Unit = {},
    onUpdateCondition: (ConditionRecord) -> Unit = {},
    onDeleteCondition: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showConditionDialog by remember { mutableStateOf(false) }
    var showMaintenanceDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<MaintenanceEntry?>(null) }
    var deletingEntry by remember { mutableStateOf<MaintenanceEntry?>(null) }
    var editingCondition by remember { mutableStateOf<ConditionRecord?>(null) }
    var deletingCondition by remember { mutableStateOf<ConditionRecord?>(null) }
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        ConditionBadge(rating = record.rating)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(df.format(Date(record.recordedAt)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row {
                        IconButton(onClick = { editingCondition = record }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit condition record",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { deletingCondition = record }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete condition record",
                                tint = MaterialTheme.colorScheme.error)
                        }
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
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
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
                        IconButton(onClick = { editingEntry = entry }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit entry",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { deletingEntry = entry }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete entry",
                                tint = MaterialTheme.colorScheme.error)
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
            onConfirm = { type, desc, cost, tech, date ->
                onAddMaintenance(MaintenanceEntry(
                    type = type, description = desc, cost = cost, technician = tech, date = date
                ))
                showMaintenanceDialog = false
            },
            onDismiss = { showMaintenanceDialog = false }
        )
    }

    // Edit an existing maintenance entry
    editingEntry?.let { entry ->
        AddMaintenanceDialog(
            existing = entry,
            onConfirm = { type, desc, cost, tech, date ->
                onUpdateMaintenance(entry.copy(
                    type = type, description = desc, cost = cost, technician = tech, date = date
                ))
                editingEntry = null
            },
            onDismiss = { editingEntry = null }
        )
    }

    // Delete confirmation
    deletingEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = { deletingEntry = null },
            title = { Text("Delete Maintenance Entry") },
            text = { Text("Delete \"${entry.type.displayName} — ${entry.description}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteMaintenance(entry.id)
                    deletingEntry = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deletingEntry = null }) { Text("Cancel") } }
        )
    }

    // Edit an existing condition record
    editingCondition?.let { record ->
        AddConditionDialog(
            existing = record,
            onConfirm = { rating, notes, issues ->
                onUpdateCondition(record.copy(
                    rating = rating, notes = notes,
                    issues = issues.split("\n").filter { it.isNotBlank() }
                ))
                editingCondition = null
            },
            onDismiss = { editingCondition = null }
        )
    }

    // Delete confirmation for condition record
    deletingCondition?.let { record ->
        AlertDialog(
            onDismissRequest = { deletingCondition = null },
            title = { Text("Delete Condition Record") },
            text = { Text("Delete the ${record.rating.displayName} record from ${df.format(Date(record.recordedAt))}? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteCondition(record.id)
                    deletingCondition = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deletingCondition = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AddConditionDialog(
    onConfirm: (ConditionRating, String, String) -> Unit,
    onDismiss: () -> Unit,
    existing: ConditionRecord? = null
) {
    var rating by remember { mutableStateOf(existing?.rating ?: ConditionRating.EXCELLENT) }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var issues by remember { mutableStateOf(existing?.issues?.joinToString("\n") ?: "") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Record Condition" else "Edit Condition Record") },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMaintenanceDialog(
    onConfirm: (MaintenanceType, String, Double?, String, Long) -> Unit,
    onDismiss: () -> Unit,
    existing: MaintenanceEntry? = null
) {
    var type by remember { mutableStateOf(existing?.type ?: MaintenanceType.SETUP) }
    var desc by remember { mutableStateOf(existing?.description ?: "") }
    var costStr by remember { mutableStateOf(existing?.cost?.toString() ?: "") }
    var tech by remember { mutableStateOf(existing?.technician ?: "") }
    var date by remember { mutableStateOf(existing?.date ?: System.currentTimeMillis()) }
    var expanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val df = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Log Maintenance" else "Edit Maintenance Entry") },
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
                // Date of the work performed — defaults to today but is user-set,
                // so historical maintenance can be logged accurately.
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Date performed: ${df.format(Date(date))}")
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
                onConfirm(type, desc, costStr.toDoubleOrNull(), tech, date)
            }, enabled = desc.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = date)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { date = utcMidnightToLocal(it) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
