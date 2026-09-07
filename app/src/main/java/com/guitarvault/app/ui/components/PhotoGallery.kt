package com.guitarvault.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.guitarvault.app.data.model.GuitarPhoto
import com.guitarvault.app.data.model.PhotoType

/**
 * Horizontal scrolling photo gallery with add/remove capabilities.
 */
@Composable
fun PhotoGallery(
    photos: List<GuitarPhoto>,
    photoModelProvider: (GuitarPhoto) -> Any?,
    onAddPhoto: () -> Unit,
    onPastePhoto: () -> Unit = {},
    onPickFromGallery: () -> Unit = {},
    onRemovePhoto: (GuitarPhoto) -> Unit,
    onSetPrimary: (GuitarPhoto) -> Unit,
    onPhotoClick: (GuitarPhoto) -> Unit,
    onRemoveBackground: (GuitarPhoto) -> Unit = {},
    onUndoBackgroundRemoval: (GuitarPhoto) -> Unit = {},
    onUpdatePhoto: (GuitarPhoto) -> Unit = {},
    bgRemovalProgress: Map<String, String> = emptyMap(),
    modifier: Modifier = Modifier
) {
    var editingPhoto by remember { mutableStateOf<GuitarPhoto?>(null) }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(photos, key = { it.id }) { photo ->
            PhotoThumbnail(
                photo = photo,
                model = photoModelProvider(photo),
                bgProgress = bgRemovalProgress[photo.id],
                onRemove = { onRemovePhoto(photo) },
                onSetPrimary = { onSetPrimary(photo) },
                onClick = { onPhotoClick(photo) },
                onRemoveBackground = { onRemoveBackground(photo) },
                onUndoBackgroundRemoval = { onUndoBackgroundRemoval(photo) },
                onEditDetails = { editingPhoto = photo }
            )
        }
        item {
            AddPhotoButton(onClick = onAddPhoto)
        }
        item {
            PastePhotoButton(onClick = onPastePhoto)
        }
        item {
            GalleryPickerButton(onClick = onPickFromGallery)
        }
    }

    // Caption + tags editor
    editingPhoto?.let { photo ->
        // Suggestions: built-in defaults + tags already used on this guitar's
        // photos (keeps spelling consistent, e.g. always "headstock" not "head stock")
        val usedTags = photos.flatMap { it.tags }.distinct()
        PhotoDetailsDialog(
            photo = photo,
            suggestedTags = (DEFAULT_PHOTO_TAGS + usedTags).distinct(),
            onConfirm = { caption, tags ->
                onUpdatePhoto(photo.copy(caption = caption, tags = tags))
                editingPhoto = null
            },
            onDismiss = { editingPhoto = null }
        )
    }
}

/** Built-in tag suggestions for photo details. */
private val DEFAULT_PHOTO_TAGS = listOf(
    "front", "back", "side", "headstock", "neck", "body",
    "pickups", "electronics", "hardware", "case", "damage", "repair"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PhotoDetailsDialog(
    photo: GuitarPhoto,
    suggestedTags: List<String>,
    onConfirm: (String, List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var caption by remember { mutableStateOf(photo.caption) }
    var tags by remember { mutableStateOf(photo.tags.toMutableList()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Photo Details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = caption, onValueChange = { caption = it },
                    label = { Text("Caption") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Selected tags as removable chips
                Text("Tags", style = MaterialTheme.typography.labelMedium)
                if (tags.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        tags.forEach { tag ->
                            InputChip(
                                selected = true,
                                onClick = { tags = tags.filter { it != tag }.toMutableList() },
                                label = { Text(tag) },
                                trailingIcon = {
                                    Icon(Icons.Default.Close, contentDescription = "Remove tag",
                                        modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                    }
                }

                // Suggestions: tap to add
                val remaining = suggestedTags.filter { it !in tags }
                if (remaining.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        remaining.forEach { tag ->
                            SuggestionChip(
                                onClick = { tags = (tags + tag).toMutableList() },
                                label = { Text(tag) },
                                border = null
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(caption.trim(), tags)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun PhotoThumbnail(
    photo: GuitarPhoto,
    model: Any?,
    bgProgress: String? = null,
    onRemove: () -> Unit,
    onSetPrimary: () -> Unit,
    onClick: () -> Unit,
    onRemoveBackground: () -> Unit = {},
    onUndoBackgroundRemoval: () -> Unit = {},
    onEditDetails: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .size(120.dp, 140.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
    ) {
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = photo.caption.ifEmpty { photo.photoType.displayName },
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text("🎸", style = MaterialTheme.typography.headlineMedium)
            }
        }

        // Processing overlay
        if (bgProgress != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = bgProgress,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
        }

        // Top-left: magic wand (remove bg) or undo button
        if (bgProgress == null) {
            if (photo.backgroundRemoved) {
                // Show undo button
                IconButton(
                    onClick = onUndoBackgroundRemoval,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(24.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                ) {
                    Icon(
                        Icons.Default.Undo,
                        contentDescription = "Undo background removal",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                // Show magic wand button
                IconButton(
                    onClick = onRemoveBackground,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(24.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                ) {
                    Icon(
                        Icons.Default.AutoFixHigh,
                        contentDescription = "Remove background",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Top-right: remove button
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove photo",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }

        // Bottom: photo type label + set primary button
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = photo.photoType.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
            if (!photo.isPrimary) {
                Text(
                    text = "★ Set",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.clickable(onClick = onSetPrimary)
                )
            }
        }

        // Bottom-right: edit caption/tags button
        IconButton(
            onClick = onEditDetails,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 26.dp)
                .size(24.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
        ) {
            Icon(
                Icons.Default.Edit,
                contentDescription = "Edit photo details",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }

        // BG removed badge
        if (photo.backgroundRemoved) {
            Surface(
                color = MaterialTheme.colorScheme.tertiary,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(2.dp)
            ) {
                Text(
                    text = "AI",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun AddPhotoButton(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.size(120.dp, 140.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Add, contentDescription = "Add photo")
                Spacer(modifier = Modifier.height(4.dp))
                Text("Take Photo", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun PastePhotoButton(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.size(120.dp, 140.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Add, contentDescription = "Paste photo")
                Spacer(modifier = Modifier.height(4.dp))
                Text("Paste Photo", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun GalleryPickerButton(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.size(120.dp, 140.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = "Pick from gallery")
                Spacer(modifier = Modifier.height(4.dp))
                Text("Gallery", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun PhotoTypeDropdown(
    selected: PhotoType,
    onSelected: (PhotoType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(onClick = { expanded = true }) {
            Text(selected.displayName)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            PhotoType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.displayName) },
                    onClick = {
                        onSelected(type)
                        expanded = false
                    }
                )
            }
        }
    }
}
