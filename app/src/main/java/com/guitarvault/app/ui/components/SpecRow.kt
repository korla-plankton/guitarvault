package com.guitarvault.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A labeled spec row — label on left, value on right.
 */
@Composable
fun SpecRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    if (value.isBlank()) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * A section header for grouping specs.
 */
@Composable
fun SpecSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

/**
 * An editable text field with a label.
 */
@Composable
fun LabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    placeholder: String = ""
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = if (placeholder.isNotBlank()) { { Text(placeholder) } } else null,
        singleLine = singleLine,
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * An editable number field with a label.
 */
@Composable
fun LabeledNumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = ""
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newText -> if (newText.all { it.isDigit() || it == '.' || it == '-' }) onValueChange(newText) },
        label = { Text(label) },
        placeholder = if (placeholder.isNotBlank()) { { Text(placeholder) } } else null,
        singleLine = true,
        modifier = modifier.fillMaxWidth()
    )
}
