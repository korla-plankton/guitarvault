package com.guitarvault.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.guitarvault.app.data.model.GuitarPhoto
import java.io.File

/**
 * Full-screen photo viewer with pinch-to-zoom, pan, and swipe between photos.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullScreenPhotoViewer(
    photos: List<GuitarPhoto>,
    initialIndex: Int,
    photoFileProvider: (String) -> File,
    onClose: () -> Unit
) {
    if (photos.isEmpty()) { onClose(); return }

    val pagerState = rememberPagerState(initialPage = initialIndex) { photos.size }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val photo = photos[page]
            val file = photoFileProvider(photo.filePath)

            // Each page has its own zoom/pan state
            var scale by remember(page) { mutableFloatStateOf(1f) }
            var offsetX by remember(page) { mutableFloatStateOf(0f) }
            var offsetY by remember(page) { mutableFloatStateOf(0f) }

            AsyncImage(
                model = file,
                contentDescription = photo.caption.ifEmpty { photo.photoType.displayName },
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
                    .pointerInput(page) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            if (scale > 1f) {
                                offsetX += pan.x
                                offsetY += pan.y
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    }
            )
        }

        // Close button
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        // Photo type label
        val currentPhoto = photos.getOrNull(pagerState.currentPage)
        if (currentPhoto != null) {
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = currentPhoto.photoType.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                    if (currentPhoto.backgroundRemoved) {
                        Text(
                            text = "✨ AI background removed",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    if (currentPhoto.caption.isNotBlank()) {
                        Text(
                            text = currentPhoto.caption,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}
