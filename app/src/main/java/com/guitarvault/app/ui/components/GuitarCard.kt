package com.guitarvault.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.guitarvault.app.data.model.Guitar
import com.guitarvault.app.data.model.ConditionRating
import java.io.File
import java.text.NumberFormat
import java.util.Locale

/**
 * Card showing a guitar in the flat list view.
 */
@Composable
fun GuitarCard(
    guitar: Guitar,
    photoFile: File?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            // Photo thumbnail
            if (photoFile != null && photoFile.exists()) {
                AsyncImage(
                    model = photoFile,
                    contentDescription = "Photo of ${guitar.displayName}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Surface(
                    modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🎸", style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Guitar info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = guitar.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (guitar.serialNumber.isNotBlank()) {
                    Text(
                        text = "S/N: ${guitar.serialNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = {},
                        label = { Text(guitar.guitarType.displayName, style = MaterialTheme.typography.labelSmall) }
                    )
                    guitar.currentCondition?.let { cond ->
                        ConditionBadge(rating = cond.rating)
                    }
                }
                guitar.valuation.currentValue?.let { value ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatCurrency(value),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                SpecCompletenessBar(completeness = guitar.specCompleteness)
            }
        }
    }
}

/**
 * Compact grid item for gallery view.
 */
@Composable
fun GuitarGridItem(
    guitar: Guitar,
    photoFile: File?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            if (photoFile != null && photoFile.exists()) {
                AsyncImage(
                    model = photoFile,
                    contentDescription = guitar.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(140.dp)
                )
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🎸", style = MaterialTheme.typography.headlineLarge)
                    }
                }
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = guitar.displayName,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                guitar.year?.let {
                    Text(
                        text = it.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ConditionBadge(rating: ConditionRating) {
    val color = when (rating) {
        ConditionRating.MINT -> MaterialTheme.colorScheme.tertiary
        ConditionRating.NEAR_MINT -> MaterialTheme.colorScheme.tertiary
        ConditionRating.EXCELLENT -> MaterialTheme.colorScheme.primary
        ConditionRating.VERY_GOOD -> MaterialTheme.colorScheme.primary
        ConditionRating.GOOD -> MaterialTheme.colorScheme.secondary
        ConditionRating.FAIR -> MaterialTheme.colorScheme.error
        ConditionRating.POOR -> MaterialTheme.colorScheme.error
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = rating.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

fun formatCurrency(amount: Double): String {
    return NumberFormat.getCurrencyInstance(Locale.US).format(amount)
}
