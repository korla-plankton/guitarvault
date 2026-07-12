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
import com.guitarvault.app.data.model.Guitar
import com.guitarvault.app.data.model.ValueEntry
import com.guitarvault.app.ui.components.SpecSection
import com.guitarvault.app.ui.components.SpecRow
import com.guitarvault.app.ui.components.formatCurrency
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ValuationTab(
    guitar: Guitar,
    onUpdateValuation: (com.guitarvault.app.data.model.Valuation) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddValueDialog by remember { mutableStateOf(false) }
    val df = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Current valuation summary
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Current Value", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = formatCurrency(guitar.valuation.currentValue ?: guitar.valuation.estimatedValue ?: 0.0),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                SpecRow("Purchase Price", guitar.valuation.purchasePrice?.let { formatCurrency(it) } ?: "")
                SpecRow("Purchase Date", guitar.valuation.purchaseDate?.let { df.format(Date(it)) } ?: "")
                SpecRow("Purchase Source", guitar.valuation.purchaseSource)
                SpecRow("Estimated Value", guitar.valuation.estimatedValue?.let { formatCurrency(it) } ?: "")

                val profit = (guitar.valuation.currentValue ?: 0.0) - (guitar.valuation.purchasePrice ?: 0.0)
                if (guitar.valuation.currentValue != null && guitar.valuation.purchasePrice != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Gain/Loss: ${if (profit >= 0) "+" else ""}${formatCurrency(profit)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (profit >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Value history
        SpecSection(title = "Value History") {
            if (guitar.valuation.valueHistory.isEmpty()) {
                Text("No value history recorded yet.", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                guitar.valuation.valueHistory.sortedByDescending { it.recordedAt }.forEach { entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(formatCurrency(entry.value), style = MaterialTheme.typography.bodyMedium)
                            Text(df.format(Date(entry.recordedAt)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (entry.source.isNotBlank()) {
                                Text(entry.source, style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    HorizontalDivider()
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { showAddValueDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Record New Valuation")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Insurance section
        SpecSection(title = "Insurance") {
            SpecRow("Insured", if (guitar.insurance.insured) "Yes" else "No")
            SpecRow("Insured Value", guitar.insurance.insuredValue?.let { formatCurrency(it) } ?: "")
            SpecRow("Provider", guitar.insurance.provider)
            SpecRow("Policy Number", guitar.insurance.policyNumber)
            SpecRow("Coverage Type", guitar.insurance.coverageType)
            SpecRow("Deductible", guitar.insurance.deductible?.let { formatCurrency(it) } ?: "")
            SpecRow("Policy Start", guitar.insurance.policyStart?.let { df.format(Date(it)) } ?: "")
            SpecRow("Policy End", guitar.insurance.policyEnd?.let { df.format(Date(it)) } ?: "")
            if (guitar.insurance.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Notes: ${guitar.insurance.notes}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    if (showAddValueDialog) {
        AddValueDialog(
            onConfirm = { value, source, notes ->
                onUpdateValuation(guitar.valuation.copy(
                    currentValue = value,
                    valueHistory = guitar.valuation.valueHistory + ValueEntry(value = value, source = source, notes = notes)
                ))
                showAddValueDialog = false
            },
            onDismiss = { showAddValueDialog = false }
        )
    }
}

@Composable
private fun AddValueDialog(
    onConfirm: (Double, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var valueStr by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Valuation") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = valueStr, onValueChange = { valueStr = it },
                    label = { Text("Value ($)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = source, onValueChange = { source = it },
                    label = { Text("Source (appraisal, market, etc.)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    label = { Text("Notes") }, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { valueStr.toDoubleOrNull()?.let { onConfirm(it, source, notes) } },
                enabled = valueStr.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
