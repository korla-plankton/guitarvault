package com.guitarvault.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guitarvault.app.data.model.*
import com.guitarvault.app.ui.components.formatCurrency
import com.guitarvault.app.ui.viewmodel.CollectionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(
    onBack: () -> Unit,
    viewModel: CollectionViewModel = viewModel()
) {
    val wishlist by viewModel.wishlist.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wishlist") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add to wishlist")
            }
        }
    ) { padding ->
        if (wishlist.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("🛒", style = MaterialTheme.typography.displayLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Your wishlist is empty", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Add guitars you're hunting for", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(wishlist.sortedByDescending { it.priority.sortOrder }, key = { it.id }) { item ->
                    WishlistCard(
                        item = item,
                        onDelete = { viewModel.deleteWishlistItem(item.id) },
                        onPromote = { price ->
                            viewModel.addGuitar(Guitar(
                                brand = item.brand, model = item.model, year = item.year,
                                guitarType = item.guitarType, tags = item.tags,
                                notes = item.notes, valuation = Valuation(purchasePrice = price)
                            ))
                            viewModel.deleteWishlistItem(item.id)
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddWishlistDialog(
            onConfirm = { item ->
                viewModel.addWishlistItem(item)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun WishlistCard(
    item: WishlistItem,
    onDelete: () -> Unit,
    onPromote: (Double?) -> Unit
) {
    var showPromoteDialog by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    item.year?.let { Text("($it)", style = MaterialTheme.typography.bodySmall) }
                }
                PriorityBadge(priority = item.priority)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                }
            }
            if (item.guitarType != GuitarType.ELECTRIC || item.specificSpecs.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(item.guitarType.displayName, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item.targetPrice?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Target: ${formatCurrency(it)}", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary)
            }
            if (item.specificSpecs.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Specs: ${item.specificSpecs}", style = MaterialTheme.typography.bodySmall)
            }
            if (item.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(item.notes, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (item.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    item.tags.forEach { tag ->
                        AssistChip(onClick = {}, label = { Text(tag, style = MaterialTheme.typography.labelSmall) })
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { showPromoteDialog = true }) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Acquired — Add to Collection")
            }
        }
    }

    if (showPromoteDialog) {
        var priceStr by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPromoteDialog = false },
            title = { Text("Acquired!") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Add \"${item.displayName}\" to your collection?")
                    OutlinedTextField(
                        value = priceStr, onValueChange = { priceStr = it },
                        label = { Text("Purchase Price ($)") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onPromote(priceStr.toDoubleOrNull())
                    showPromoteDialog = false
                }) { Text("Add to Collection") }
            },
            dismissButton = { TextButton(onClick = { showPromoteDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun PriorityBadge(priority: WishlistPriority) {
    val color = when (priority) {
        WishlistPriority.LOW -> MaterialTheme.colorScheme.surfaceVariant
        WishlistPriority.MEDIUM -> MaterialTheme.colorScheme.secondary
        WishlistPriority.HIGH -> MaterialTheme.colorScheme.primary
        WishlistPriority.GRAIL -> MaterialTheme.colorScheme.tertiary
    }
    Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp), color = color.copy(alpha = 0.15f)) {
        Text(
            text = priority.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun AddWishlistDialog(
    onConfirm: (WishlistItem) -> Unit,
    onDismiss: () -> Unit
) {
    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var yearStr by remember { mutableStateOf("") }
    var guitarType by remember { mutableStateOf(GuitarType.ELECTRIC) }
    var targetPriceStr by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(WishlistPriority.MEDIUM) }
    var specificSpecs by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var tagsStr by remember { mutableStateOf("") }
    var typeExpanded by remember { mutableStateOf(false) }
    var priorityExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Wishlist") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = brand, onValueChange = { brand = it },
                    label = { Text("Brand") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = model, onValueChange = { model = it },
                    label = { Text("Model") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = yearStr, onValueChange = { yearStr = it },
                    label = { Text("Year") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Box {
                    OutlinedButton(onClick = { typeExpanded = true }) { Text("Type: ${guitarType.displayName}") }
                    DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        GuitarType.entries.forEach { type ->
                            DropdownMenuItem(text = { Text(type.displayName) },
                                onClick = { guitarType = type; typeExpanded = false })
                        }
                    }
                }
                OutlinedTextField(value = targetPriceStr, onValueChange = { targetPriceStr = it },
                    label = { Text("Target Price ($)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Box {
                    OutlinedButton(onClick = { priorityExpanded = true }) { Text("Priority: ${priority.displayName}") }
                    DropdownMenu(expanded = priorityExpanded, onDismissRequest = { priorityExpanded = false }) {
                        WishlistPriority.entries.sortedByDescending { it.sortOrder }.forEach { p ->
                            DropdownMenuItem(text = { Text(p.displayName) },
                                onClick = { priority = p; priorityExpanded = false })
                        }
                    }
                }
                OutlinedTextField(value = specificSpecs, onValueChange = { specificSpecs = it },
                    label = { Text("Specific Specs Required") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = notes, onValueChange = { notes = it },
                    label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = tagsStr, onValueChange = { tagsStr = it },
                    label = { Text("Tags (comma-separated)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(WishlistItem(
                    brand = brand, model = model, year = yearStr.toIntOrNull(),
                    guitarType = guitarType,
                    targetPrice = targetPriceStr.toDoubleOrNull(),
                    priority = priority, specificSpecs = specificSpecs,
                    notes = notes, tags = tagsStr.split(",").map { it.trim() }.filter { it.isNotBlank() }
                ))
            }, enabled = brand.isNotBlank() || model.isNotBlank()) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
