package com.guitarvault.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guitarvault.app.data.model.Guitar
import com.guitarvault.app.data.model.GuitarStatus
import com.guitarvault.app.data.model.GuitarType
import com.guitarvault.app.ui.components.*
import com.guitarvault.app.ui.components.SortDropdown
import com.guitarvault.app.ui.components.SpecCompletenessBar
import com.guitarvault.app.ui.viewmodel.CollectionViewModel
import com.guitarvault.app.ui.viewmodel.SortMode
import kotlinx.coroutines.launch

/** How a backup file is applied to the current collection. */
enum class ImportMode { MERGE, REPLACE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    onGuitarClick: (String) -> Unit,
    onAddGuitar: (GuitarStatus) -> Unit,
    onRandomSpec: () -> Unit = {},
    onLegal: () -> Unit = {},
    viewModel: CollectionViewModel = viewModel()
) {
    val guitars by viewModel.guitars.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val filterType by viewModel.filterType.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()

    val statusFilter by viewModel.statusFilter.collectAsState()

    // Unfiltered list — used by the import confirmation dialog to report the
    // true size of the collection across all statuses (owned/sold/wishlist)
    val allGuitars by viewModel.allGuitars.collectAsState()

    var showFilterMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }
    var exportStatus by remember { mutableStateOf<String?>(null) }
    var isExporting by remember { mutableStateOf(false) }

    // Import confirmation: warn that importing replaces the whole collection.
    // pendingImportUri holds the chosen backup file while the dialog is up.
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var isImporting by remember { mutableStateOf(false) }
    var importMode by remember { mutableStateOf(ImportMode.MERGE) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // SAF export: pick a location, write collection JSON + photos as a ZIP to the returned URI
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null && isExporting) {
            scope.launch {
                val result = try {
                    val stream = context.contentResolver.openOutputStream(uri)
                    if (stream == null) null
                    else viewModel.exportCollectionZip(stream).await().also { stream.close() }
                } catch (e: Exception) {
                    android.util.Log.e("CollectionScreen", "Export write failed", e)
                    null
                }
                isExporting = false
                exportStatus = if (result != null) {
                    val n = result.totalPhotos
                    "✅ Collection exported with $n photo${if (n == 1) "" else "s"}"
                } else {
                    "❌ Export failed — could not write file"
                }
            }
        } else {
            isExporting = false
        }
    }

    // SAF import: pick a backup file (ZIP with photos, or legacy JSON), then confirm
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
        }
    }

    // Surface export/import results as a Snackbar
    LaunchedEffect(exportStatus) {
        exportStatus?.let {
            snackbarHostState.showSnackbar(it)
            exportStatus = null
        }
    }

    // Import confirmation dialog — Merge (safe) or Replace (destructive)
    if (pendingImportUri != null) {
        AlertDialog(
            onDismissRequest = { if (!isImporting) pendingImportUri = null },
            title = { Text("Import Collection?") },
            text = {
                Column {
                    Text("The backup file will be ${if (importMode == ImportMode.MERGE) "merged into" else "used to replace"} your current collection (${allGuitars.size} guitar${if (allGuitars.size == 1) "" else "s"}).")
                    Spacer(modifier = Modifier.height(12.dp))

                    // Mode selection
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { importMode = ImportMode.MERGE },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = importMode == ImportMode.MERGE,
                            onClick = { importMode = ImportMode.MERGE }
                        )
                        Column {
                            Text("Merge (recommended)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Matching guitars are combined; new ones are added. Nothing is deleted.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { importMode = ImportMode.REPLACE },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = importMode == ImportMode.REPLACE,
                            onClick = { importMode = ImportMode.REPLACE }
                        )
                        Column {
                            Text("Replace", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Wipes your current collection and loads the backup instead.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (importMode == ImportMode.REPLACE) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "⚠️ This cannot be undone. Export your current collection first if you want to keep it.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = pendingImportUri
                        val merge = importMode == ImportMode.MERGE
                        isImporting = true
                        pendingImportUri = null
                        scope.launch {
                            val result = if (uri == null) null else try {
                                val stream = context.contentResolver.openInputStream(uri)
                                if (stream == null) null
                                else viewModel.importCollectionBackup(stream, merge).await()
                            } catch (e: Exception) {
                                android.util.Log.e("CollectionScreen", "Import failed", e)
                                null
                            }
                            isImporting = false
                            exportStatus = when (result) {
                                null -> "❌ Import failed — invalid backup file"
                                is com.guitarvault.app.data.merge.CollectionMerger.BackupImportResult.Replace ->
                                    "✅ Collection imported successfully"
                                is com.guitarvault.app.data.merge.CollectionMerger.BackupImportResult.Merged -> {
                                    val s = result.stats
                                    "✅ Merged: ${s.guitarsAdded} guitar${if (s.guitarsAdded == 1) "" else "s"} added, " +
                                        "${s.guitarsUpdated} updated, ${s.guitarsUnchanged} unchanged"
                                }
                            }
                        }
                    },
                    enabled = !isImporting
                ) { Text("Import") }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingImportUri = null },
                    enabled = !isImporting
                ) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "GuitarVault",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort")
                    }
                    IconButton(onClick = { showExportMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        SortMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.displayName) },
                                onClick = { viewModel.setSortMode(mode); showSortMenu = false }
                            )
                        }
                    }
                    DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }) {
                        DropdownMenuItem(text = { Text("All Types") }, onClick = {
                            viewModel.setFilterType(null); showFilterMenu = false
                        })
                        GuitarType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.displayName) },
                                onClick = { viewModel.setFilterType(type); showFilterMenu = false }
                            )
                        }
                    }
                    // Overflow menu: challenge, legal, export/import
                    DropdownMenu(expanded = showExportMenu, onDismissRequest = { showExportMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("🎲 Random Spec Challenge") },
                            onClick = {
                                showExportMenu = false
                                onRandomSpec()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("⚖️ Legal / Privacy") },
                            onClick = {
                                showExportMenu = false
                                onLegal()
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("📤 Export Collection (ZIP)") },
                            onClick = {
                                showExportMenu = false
                                isExporting = true
                                exportLauncher.launch("guitarvault-backup-${System.currentTimeMillis()}.zip")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("📥 Import Collection") },
                            onClick = {
                                showExportMenu = false
                                importLauncher.launch(arrayOf("application/zip", "application/json", "*/*"))
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAddGuitar(statusFilter) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Guitar") }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Stats summary
            if (guitars.isNotEmpty() || searchQuery.isBlank()) {
                StatsBar(stats = stats)
            }

            // Status tabs
            TabRow(selectedTabIndex = statusFilter.ordinal) {
                GuitarStatus.entries.forEach { status ->
                    Tab(
                        selected = statusFilter == status,
                        onClick = { viewModel.setStatusFilter(status) },
                        text = { Text(status.displayName) }
                    )
                }
            }

            // View toggle row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ViewToggle(selected = viewMode, onSelected = {
                    viewModel.setViewMode(it)
                })
            }

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Content based on view mode
            if (guitars.isEmpty()) {
                EmptyState(onAddGuitar = { onAddGuitar(statusFilter) })
            } else {
                when (viewMode) {
                    CollectionViewModel.CollectionViewMode.LIST -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(guitars, key = { it.id }) { guitar ->
                                GuitarCard(
                                    guitar = guitar,
                                    photoModel = viewModel.getPhotoModel(guitar.primaryPhoto),
                                    onClick = { onGuitarClick(guitar.id) }
                                )
                            }
                        }
                    }
                    CollectionViewModel.CollectionViewMode.GROUPED -> {
                        val grouped = guitars.groupBy { it.brand.ifBlank { "Unknown" } }
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            grouped.forEach { (brand, brandGuitars) ->
                                item {
                                    Text(
                                        text = "$brand (${brandGuitars.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                    HorizontalDivider()
                                }
                                items(brandGuitars, key = { it.id }) { guitar ->
                                    GuitarCard(
                                        guitar = guitar,
                                        photoModel = viewModel.getPhotoModel(guitar.primaryPhoto),
                                        onClick = { onGuitarClick(guitar.id) }
                                    )
                                }
                            }
                        }
                    }
                    CollectionViewModel.CollectionViewMode.GRID -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            gridItems(guitars, key = { it.id }) { guitar ->
                                GuitarGridItem(
                                    guitar = guitar,
                                    photoModel = viewModel.getPhotoModel(guitar.primaryPhoto),
                                    onClick = { onGuitarClick(guitar.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsBar(stats: com.guitarvault.app.data.repository.CollectionStats) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(label = "Guitars", value = stats.totalGuitars.toString())
            StatItem(label = "Value", value = formatCurrency(stats.totalValue))
            StatItem(label = "Invested", value = formatCurrency(stats.totalInvested))
            StatItem(label = "Insured", value = formatCurrency(stats.totalInsured))
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyState(onAddGuitar: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🎸", style = MaterialTheme.typography.displayLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Your collection is empty", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Add your first guitar to start tracking your collection",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onAddGuitar) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Your First Guitar")
        }
    }
}
