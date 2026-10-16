package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.entity.ColumnEntity
import com.example.data.model.ColumnType
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.WarningRose

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DynamicFieldInput(
    column: ColumnEntity,
    value: String,
    errorMessage: String? = null,
    options: List<String> = emptyList(),
    onValueChange: (String) -> Unit
) {
    val colType = ColumnType.fromString(column.dataType)
    var dropdownExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = column.displayName.ifEmpty { column.name },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (column.isRequired) {
                    Text(
                        text = " *",
                        color = WarningRose,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                if (column.isEncrypted) {
                    Spacer(modifier = Modifier.size(6.dp))
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Encrypted Column",
                        tint = CyberCyan,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Text(
                text = colType.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        when (colType) {
            ColumnType.AUTO_ID -> {
                OutlinedTextField(
                    value = value.ifEmpty { "[Auto Generated Upon Save]" },
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("field_${column.name}"),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            ColumnType.BOOLEAN -> {
                val isChecked = value.lowercase() == "true" || value == "1"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isChecked) "Enabled (True)" else "Disabled (False)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = isChecked,
                        onCheckedChange = { onValueChange(it.toString()) },
                        modifier = Modifier.testTag("field_${column.name}")
                    )
                }
            }
            ColumnType.SINGLE_CHOICE -> {
                val availableOpts = if (options.isNotEmpty()) options else listOf("Option 1", "Option 2")
                Column {
                    OutlinedTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("field_${column.name}"),
                        placeholder = { Text("Select option...") },
                        trailingIcon = {
                            IconButton(onClick = { dropdownExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Expand")
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        isError = errorMessage != null
                    )
                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        availableOpts.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    onValueChange(opt)
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            ColumnType.MULTI_CHOICE, ColumnType.TAGS -> {
                val currentTags = value.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                val availableOpts = if (options.isNotEmpty()) options else listOf("Alpha", "Beta", "Gamma", "Security", "Production")

                Column {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        availableOpts.forEach { opt ->
                            val selected = currentTags.contains(opt)
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    val newSet = if (selected) currentTags - opt else currentTags + opt
                                    onValueChange(newSet.joinToString(", "))
                                },
                                label = { Text(opt) },
                                leadingIcon = if (selected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                    OutlinedTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier.fillMaxWidth().testTag("field_${column.name}"),
                        placeholder = { Text("Comma-separated values...") },
                        shape = RoundedCornerShape(12.dp),
                        isError = errorMessage != null
                    )
                }
            }
            ColumnType.LONG_TEXT, ColumnType.JSON_DOCUMENT -> {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("field_${column.name}"),
                    minLines = 3,
                    maxLines = 6,
                    placeholder = {
                        Text(if (colType == ColumnType.JSON_DOCUMENT) "{\"key\": \"value\"}" else "Enter detailed text...")
                    },
                    shape = RoundedCornerShape(12.dp),
                    isError = errorMessage != null
                )
            }
            else -> {
                val keyboardType = when (colType) {
                    ColumnType.INTEGER -> KeyboardType.Number
                    ColumnType.DECIMAL -> KeyboardType.Decimal
                    ColumnType.EMAIL -> KeyboardType.Email
                    ColumnType.PHONE -> KeyboardType.Phone
                    ColumnType.URL -> KeyboardType.Uri
                    else -> KeyboardType.Text
                }

                val placeholderText = when (colType) {
                    ColumnType.DATE -> "YYYY-MM-DD (e.g. 2026-10-16)"
                    ColumnType.TIME -> "HH:MM (e.g. 14:30)"
                    ColumnType.DATETIME -> "YYYY-MM-DD HH:MM (e.g. 2026-10-16 14:30)"
                    ColumnType.EMAIL -> "admin@example.com"
                    ColumnType.PHONE -> "+1 (555) 019-2834"
                    ColumnType.URL -> "https://example.com"
                    ColumnType.INTEGER -> "0"
                    ColumnType.DECIMAL -> "0.00"
                    else -> "Enter ${column.displayName}..."
                }

                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("field_${column.name}"),
                    placeholder = { Text(placeholderText) },
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    shape = RoundedCornerShape(12.dp),
                    isError = errorMessage != null,
                    singleLine = (colType != ColumnType.LONG_TEXT)
                )
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = WarningRose,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
