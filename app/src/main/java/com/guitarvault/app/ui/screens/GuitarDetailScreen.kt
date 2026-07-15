package com.guitarvault.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.guitarvault.app.data.model.Guitar
import com.guitarvault.app.data.model.GuitarPhoto
import com.guitarvault.app.ui.components.PhotoGallery
import com.guitarvault.app.ui.viewmodel.CollectionViewModel

enum class DetailTab(val label: String) {
    SPECS("Specs"),
    PHOTOS("Photos"),
    VALUE("Value & Insurance"),
    CONDITION("Condition & Maintenance")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuitarDetailScreen(
    guitarId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: () -> Unit,
    onOpenCamera: (String) -> Unit,
    onSpecLookup: (String) -> Unit,
    viewModel: CollectionViewModel = viewModel()
) {
    val guitars by viewModel.guitars.collectAsState()
    val guitar = remember(guitarId, guitars) { guitars.find { it.id == guitarId } }
    var selectedTab by remember { mutableStateOf(DetailTab.SPECS) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showSoldDialog by remember { mutableStateOf(false) }
    var showAcquireDialog by remember { mutableStateOf(false) }
    var fullscreenPhotoIndex by remember { mutableStateOf<Int?>(null) }

    if (guitar == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Guitar not found")
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(guitar.displayName, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onSpecLookup(guitarId) }) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = "Auto-fill specs")
                    }
                    // Status-based actions
                    val currentStatus = when {
                        guitar.isSold -> com.guitarvault.app.data.model.GuitarStatus.SOLD
                        guitar.isWishlist -> com.guitarvault.app.data.model.GuitarStatus.WISHLIST
                        else -> guitar.status
                    }
                    when (currentStatus) {
                        com.guitarvault.app.data.model.GuitarStatus.WISHLIST -> {
                            IconButton(onClick = { showAcquireDialog = true }) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = "Acquire")
                            }
                        }
                        com.guitarvault.app.data.model.GuitarStatus.OWNED -> {
                            IconButton(onClick = { showSoldDialog = true }) {
                                Icon(Icons.Default.Sell, contentDescription = "Mark as sold")
                            }
                        }
                        com.guitarvault.app.data.model.GuitarStatus.SOLD -> {}
                    }
                    IconButton(onClick = { onEdit(guitarId) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Hero photo (tappable for full-screen)
            guitar.primaryPhoto?.let { photo ->
                val photoModel = viewModel.getPhotoModel(photo)
                val heroIndex = guitar.photos.indexOfFirst { it.id == photo.id }
                if (photoModel != null) {
                    AsyncImage(
                        model = photoModel,
                        contentDescription = guitar.displayName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                            .clickable { if (heroIndex >= 0) fullscreenPhotoIndex = heroIndex }
                    )
                }
            }

            // Tabs
            TabRow(selectedTabIndex = selectedTab.ordinal) {
                DetailTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(tab.label) }
                    )
                }
            }

            when (selectedTab) {
                DetailTab.SPECS -> SpecsTab(guitar = guitar)
                DetailTab.PHOTOS -> PhotosTab(
                    guitar = guitar,
                    viewModel = viewModel,
                    onOpenCamera = { onOpenCamera(guitarId) },
                    onPhotoClick = { index -> fullscreenPhotoIndex = index }
                )
                DetailTab.VALUE -> ValuationTab(
                    guitar = guitar,
                    onUpdateValuation = { newVal -> viewModel.updateValuation(guitarId, newVal) },
                    onUpdateInsurance = { newIns -> viewModel.updateInsurance(guitarId, newIns) },
                    onDeleteValueEntry = { entryId -> viewModel.deleteValueEntry(guitarId, entryId) }
                )
                DetailTab.CONDITION -> ConditionTab(
                    guitar = guitar,
                    onAddCondition = { record -> viewModel.addConditionRecord(guitarId, record) },
                    onAddMaintenance = { entry -> viewModel.addMaintenanceEntry(guitarId, entry) }
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Guitar") },
            text = { Text("Delete \"${guitar.displayName}\"? This cannot be undone. Photos will also be deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteGuitar(guitarId)
                    showDeleteConfirm = false
                    onDelete()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }

    // Mark as sold dialog
    if (showSoldDialog) {
        var soldPriceStr by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSoldDialog = false },
            title = { Text("Mark as Sold") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Mark \"${guitar.displayName}\" as sold?")
                    OutlinedTextField(
                        value = soldPriceStr,
                        onValueChange = { soldPriceStr = it },
                        label = { Text("Sold Price ($)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    soldPriceStr.toDoubleOrNull()?.let { viewModel.markAsSold(guitarId, it) }
                    showSoldDialog = false
                }) { Text("Mark Sold") }
            },
            dismissButton = { TextButton(onClick = { showSoldDialog = false }) { Text("Cancel") } }
        )
    }

    // Acquire from wishlist dialog
    if (showAcquireDialog) {
        var acquirePriceStr by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAcquireDialog = false },
            title = { Text("Acquire Guitar") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Move \"${guitar.displayName}\" from wishlist to your collection?")
                    OutlinedTextField(
                        value = acquirePriceStr,
                        onValueChange = { acquirePriceStr = it },
                        label = { Text("Purchase Price ($)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.acquireFromWishlist(guitarId, acquirePriceStr.toDoubleOrNull())
                    showAcquireDialog = false
                }) { Text("Acquire") }
            },
            dismissButton = { TextButton(onClick = { showAcquireDialog = false }) { Text("Cancel") } }
        )
    }

    // Full-screen photo viewer
    fullscreenPhotoIndex?.let { index ->
        if (guitar.photos.isNotEmpty()) {
            FullScreenPhotoViewer(
                photos = guitar.photos,
                initialIndex = index,
                photoModelProvider = { photo -> viewModel.getPhotoModel(photo) },
                onClose = { fullscreenPhotoIndex = null }
            )
        }
    }
}

@Composable
private fun PhotosTab(
    guitar: Guitar,
    viewModel: CollectionViewModel,
    onOpenCamera: () -> Unit,
    onPhotoClick: (Int) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var pasteStatus by remember { mutableStateOf<String?>(null) }
    val bgProgress by viewModel.bgRemovalProgress.collectAsState()

    // Photo picker launcher
    val photoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val base64 = com.guitarvault.app.util.GalleryImageReader.readImageAsBase64(context, uri)
            if (base64 != null) {
                viewModel.addPhotoToGuitar(guitar.id, GuitarPhoto(
                    base64Data = base64,
                    photoType = com.guitarvault.app.data.model.PhotoType.GENERAL,
                    isPrimary = guitar.photos.isEmpty()
                ))
                pasteStatus = "✅ Photo imported from gallery"
            } else {
                pasteStatus = "❌ Failed to load image from gallery"
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        pasteStatus?.let {
            Text(it, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp))
        }
        PhotoGallery(
            photos = guitar.photos,
            photoModelProvider = { photo -> viewModel.getPhotoModel(photo) },
            onAddPhoto = onOpenCamera,
            onPastePhoto = {
                val base64 = com.guitarvault.app.util.ClipboardImageReader.readImageAsBase64(context)
                if (base64 != null) {
                    viewModel.addPhotoToGuitar(guitar.id, GuitarPhoto(
                        base64Data = base64,
                        photoType = com.guitarvault.app.data.model.PhotoType.GENERAL,
                        isPrimary = guitar.photos.isEmpty()
                    ))
                    pasteStatus = "✅ Photo pasted from clipboard"
                } else {
                    pasteStatus = "❌ No image found on clipboard. Copy an image first."
                }
            },
            onPickFromGallery = {
                photoPickerLauncher.launch(
                    androidx.activity.result.PickVisualMediaRequest(
                        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
            },
            onRemovePhoto = { photo ->
                viewModel.removePhotoFromGuitar(guitar.id, photo.id)
            },
            onSetPrimary = { photo ->
                viewModel.setPrimaryPhoto(guitar.id, photo.id)
            },
            onRemoveBackground = { photo ->
                viewModel.removeBackgroundFromPhoto(guitar.id, photo.id) { success, message ->
                    pasteStatus = if (success) "✅ $message" else "❌ $message"
                }
            },
            onUndoBackgroundRemoval = { photo ->
                viewModel.undoBackgroundRemoval(guitar.id, photo.id)
                pasteStatus = "↩️ Background removal undone"
            },
            bgRemovalProgress = bgProgress,
            onPhotoClick = { photo ->
                val index = guitar.photos.indexOfFirst { it.id == photo.id }
                if (index >= 0) onPhotoClick(index)
            }
        )
    }
}
