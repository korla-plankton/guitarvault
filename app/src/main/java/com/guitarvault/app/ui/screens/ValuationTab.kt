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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.guitarvault.app.data.model.Guitar
import com.guitarvault.app.data.model.InsuranceInfo
import com.guitarvault.app.data.model.ValueEntry
import com.guitarvault.app.data.model.Valuation
import com.guitarvault.app.ui.components.SpecSection
import com.guitarvault.app.ui.components.SpecRow
import com.guitarvault.app.ui.components.formatCurrency
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ValuationTab(
    guitar: Guitar,
    onUpdateValuation: (Valuation) -> Unit,
    onUpdateInsurance: (InsuranceInfo) -> Unit,
    onDeleteValueEntry: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddValueDialog by remember { mutableStateOf(false) }
    var showPurchaseDialog by remember { mutableStateOf(false) }
    var showInsuranceDialog by remember { mutableStateOf(false) }
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
                SpecRow("Purchase Price", guitar.valuation.purchasePrice?.let { formatCurrency(it) } ?: "Not set")
                SpecRow("Purchase Date", guitar.valuation.purchaseDate?.let { df.format(Date(it)) } ?: "Not set")
                SpecRow("Purchase Source", guitar.valuation.purchaseSource)
                SpecRow("Estimated Value", guitar.valuation.estimatedValue?.let { formatCurrency(it) } ?: "Not set")

                val profit = (guitar.valuation.currentValue ?: 0.0) - (guitar.valuation.purchasePrice ?: 0.0)
                if (guitar.valuation.currentValue != null && guitar.valuation.purchasePrice != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Gain/Loss: ${if (profit >= 0) "+" else ""}${formatCurrency(profit)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (profit >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = { showPurchaseDialog = true }) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit Purchase Info")
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(formatCurrency(entry.value), style = MaterialTheme.typography.bodyMedium)
                            Text(df.format(Date(entry.recordedAt)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (entry.source.isNotBlank()) {
                                Text(entry.source, style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (entry.notes.isNotBlank()) {
                                Text(entry.notes, style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        IconButton(
                            onClick = { onDeleteValueEntry(entry.id) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete valuation entry",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
            if (guitar.insurance.insured) {
                SpecRow("Status", "✅ Insured")
            } else {
                SpecRow("Status", "❌ Not insured")
            }
            SpecRow("Insured Value", guitar.insurance.insuredValue?.let { formatCurrency(it) } ?: "Not set")
            SpecRow("Provider", guitar.insurance.provider.ifBlank { "Not set" })
            SpecRow("Policy Number", guitar.insurance.policyNumber.ifBlank { "Not set" })
            SpecRow("Coverage Type", guitar.insurance.coverageType.ifBlank { "Not set" })
            SpecRow("Deductible", guitar.insurance.deductible?.let { formatCurrency(it) } ?: "Not set")
            SpecRow("Policy Start", guitar.insurance.policyStart?.let { df.format(Date(it)) } ?: "Not set")
            SpecRow("Policy End", guitar.insurance.policyEnd?.let { df.format(Date(it)) } ?: "Not set")
            if (guitar.insurance.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Notes: ${guitar.insurance.notes}", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = { showInsuranceDialog = true }) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Edit Insurance Info")
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

    if (showPurchaseDialog) {
        PurchaseInfoDialog(
            current = guitar.valuation,
            onConfirm = { newValuation ->
                onUpdateValuation(newValuation)
                showPurchaseDialog = false
            },
            onDismiss = { showPurchaseDialog = false }
        )
    }

    if (showInsuranceDialog) {
        InsuranceDialog(
            current = guitar.insurance,
            onConfirm = { newInsurance ->
                onUpdateInsurance(newInsurance)
                showInsuranceDialog = false
            },
            onDismiss = { showInsuranceDialog = false }
        )
    }
}

@Composable
private fun PurchaseInfoDialog(
    current: Valuation,
    onConfirm: (Valuation) -> Unit,
    onDismiss: () -> Unit
) {
    var priceStr by remember { mutableStateOf(current.purchasePrice?.toString() ?: "") }
    var source by remember { mutableStateOf(current.purchaseSource) }
    var estimatedStr by remember { mutableStateOf(current.estimatedValue?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Purchase Info") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = priceStr, onValueChange = { priceStr = it },
                    label = { Text("Purchase Price ($)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = source, onValueChange = { source = it },
                    label = { Text("Purchase Source (store, online, etc.)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = estimatedStr, onValueChange = { estimatedStr = it },
                    label = { Text("Estimated Value ($)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(current.copy(
                    purchasePrice = priceStr.toDoubleOrNull(),
                    purchaseDate = if (current.purchaseDate == null) System.currentTimeMillis() else current.purchaseDate,
                    purchaseSource = source,
                    estimatedValue = estimatedStr.toDoubleOrNull()
                ))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun InsuranceDialog(
    current: InsuranceInfo,
    onConfirm: (InsuranceInfo) -> Unit,
    onDismiss: () -> Unit
) {
    var insured by remember { mutableStateOf(current.insured) }
    var insuredValueStr by remember { mutableStateOf(current.insuredValue?.toString() ?: "") }
    var provider by remember { mutableStateOf(current.provider) }
    var policyNumber by remember { mutableStateOf(current.policyNumber) }
    var coverageType by remember { mutableStateOf(current.coverageType) }
    var deductibleStr by remember { mutableStateOf(current.deductible?.toString() ?: "") }
    var notes by remember { mutableStateOf(current.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Insurance Info") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = insured, onCheckedChange = { insured = it })
                    Text("This guitar is insured")
                }
                OutlinedTextField(
                    value = insuredValueStr, onValueChange = { insuredValueStr = it },
                    label = { Text("Insured Value ($)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = provider, onValueChange = { provider = it },
                    label = { Text("Insurance Provider") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = policyNumber, onValueChange = { policyNumber = it },
                    label = { Text("Policy Number") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = coverageType, onValueChange = { coverageType = it },
                    label = { Text("Coverage Type (declared value, replacement, etc.)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = deductibleStr, onValueChange = { deductibleStr = it },
                    label = { Text("Deductible ($)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    label = { Text("Notes") }, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(current.copy(
                    insured = insured,
                    insuredValue = insuredValueStr.toDoubleOrNull(),
                    provider = provider,
                    policyNumber = policyNumber,
                    coverageType = coverageType,
                    deductible = deductibleStr.toDoubleOrNull(),
                    notes = notes
                ))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
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
