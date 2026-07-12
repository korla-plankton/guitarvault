package com.guitarvault.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
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
import java.io.File

/**
 * Horizontal scrolling photo gallery with add/remove capabilities.
 */
@Composable
fun PhotoGallery(
    photos: List<GuitarPhoto>,
    photoFileProvider: (String) -> File,
    onAddPhoto: () -> Unit,
    onRemovePhoto: (GuitarPhoto) -> Unit,
    onSetPrimary: (GuitarPhoto) -> Unit,
    onPhotoClick: (GuitarPhoto) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(photos, key = { it.id }) { photo ->
            PhotoThumbnail(
                photo = photo,
                file = photoFileProvider(photo.filePath),
                onRemove = { onRemovePhoto(photo) },
                onSetPrimary = { onSetPrimary(photo) },
                onClick = { onPhotoClick(photo) }
            )
        }
        item {
            AddPhotoButton(onClick = onAddPhoto)
        }
    }
}

@Composable
private fun PhotoThumbnail(
    photo: GuitarPhoto,
    file: File,
    onRemove: () -> Unit,
    onSetPrimary: () -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(120.dp, 140.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
    ) {
        if (file.exists()) {
            AsyncImage(
                model = file,
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

        // Top row: primary star + remove button
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (photo.isPrimary) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = "Primary photo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove photo",
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
                Text("Add Photo", style = MaterialTheme.typography.labelSmall)
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
