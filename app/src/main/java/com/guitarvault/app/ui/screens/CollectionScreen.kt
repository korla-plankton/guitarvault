package com.guitarvault.app.ui.screens

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guitarvault.app.data.model.Guitar
import com.guitarvault.app.data.model.GuitarType
import com.guitarvault.app.ui.components.*
import com.guitarvault.app.ui.components.SortDropdown
import com.guitarvault.app.ui.components.SpecCompletenessBar
import com.guitarvault.app.ui.viewmodel.CollectionViewModel
import com.guitarvault.app.ui.viewmodel.SortMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    onGuitarClick: (String) -> Unit,
    onAddGuitar: () -> Unit,
    onWishlistClick: () -> Unit,
    onDailySpec: () -> Unit = {},
    viewModel: CollectionViewModel = viewModel()
) {
    val guitars by viewModel.guitars.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val filterType by viewModel.filterType.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()
    val wishlist by viewModel.wishlist.collectAsState()

    var showFilterMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("De-GAS") },
                actions = {
                    IconButton(onClick = onDailySpec) {
                        Icon(Icons.Default.Casino, contentDescription = "Daily Spec Challenge")
                    }
                    IconButton(onClick = onWishlistClick) {
                        BadgedBox(badge = {
                            if (wishlist.isNotEmpty()) {
                                Badge { Text(wishlist.size.toString()) }
                            }
                        }) {
                            Icon(Icons.Default.Favorite, contentDescription = "Wishlist")
                        }
                    }
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort")
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
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddGuitar,
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
                EmptyState(onAddGuitar = onAddGuitar)
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
