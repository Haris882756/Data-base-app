package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.entity.ColumnEntity
import com.example.data.entity.TableEntity
import com.example.data.model.ColumnType
import com.example.ui.components.AppTopBar
import com.example.ui.components.ConfirmDialog
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.WarningRose
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun TableBuilderScreen(databaseId: Long, tableId: Long, viewModel: MainViewModel) {
    val scope = rememberCoroutineScope()
    var table by remember { mutableStateOf<TableEntity?>(null) }
    val columns by viewModel.repository.getColumnsForTableFlow(tableId)
        .collectAsState(initial = emptyList())

    LaunchedEffect(tableId) {
        table = viewModel.repository.getTableById(tableId)
    }

    var showAddColumnDialog by remember { mutableStateOf(false) }
    var editingColumn by remember { mutableStateOf<ColumnEntity?>(null) }
    var columnToDelete by remember { mutableStateOf<ColumnEntity?>(null) }

    // Column Form State
    var colDisplayName by remember { mutableStateOf("") }
    var colName by remember { mutableStateOf("") }
    var colType by remember { mutableStateOf(ColumnType.SHORT_TEXT) }
    var colRequired by remember { mutableStateOf(false) }
    var colUnique by remember { mutableStateOf(false) }
    var colEncrypted by remember { mutableStateOf(false) }
    var colDefaultValue by remember { mutableStateOf("") }
    var colOptionsInput by remember { mutableStateOf("") }
    var typeDropdownOpen by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }

    fun resetForm() {
        colDisplayName = ""
        colName = ""
        colType = ColumnType.SHORT_TEXT
        colRequired = false
        colUnique = false
        colEncrypted = false
        colDefaultValue = ""
        colOptionsInput = ""
        formError = null
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "${table?.displayName ?: "Table"} Schema",
                subtitle = "${columns.size} columns defined",
                onBack = { viewModel.navigateBack() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    resetForm()
                    showAddColumnDialog = true
                },
                containerColor = CyberCyan,
                modifier = Modifier.testTag("add_column_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Column")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Guidance Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = BorderStroke(1.dp, SlateCardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null, tint = CyberCyan)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Customize fields, data types, and encryption rules. The record editor automatically constructs live form inputs matching this schema.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(columns) { col ->
                    val typeEnum = ColumnType.fromString(col.dataType)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("column_item_${col.name}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, SlateCardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (col.isEncrypted) CyberCyan.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = if (col.isEncrypted) Icons.Default.Lock else Icons.Default.TableChart,
                                    contentDescription = null,
                                    tint = if (col.isEncrypted) CyberCyan else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = col.displayName,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (col.isRequired) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = WarningRose.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "REQUIRED",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = WarningRose,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    if (col.isUnique) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = NeonEmerald.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "UNIQUE",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = NeonEmerald,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "${typeEnum.displayName} (${col.name})${if (col.isEncrypted) " • AES-256 Encrypted" else ""}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (col.dataType != ColumnType.AUTO_ID.name) {
                                IconButton(
                                    onClick = {
                                        editingColumn = col
                                        colDisplayName = col.displayName
                                        colName = col.name
                                        colType = ColumnType.fromString(col.dataType)
                                        colRequired = col.isRequired
                                        colUnique = col.isUnique
                                        colEncrypted = col.isEncrypted
                                        colDefaultValue = col.defaultValue
                                        val opts = viewModel.repository.parseOptions(col.optionsJson)
                                        colOptionsInput = opts.joinToString(", ")
                                        formError = null
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                IconButton(
                                    onClick = { columnToDelete = col }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = WarningRose)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Column Dialog
    if (showAddColumnDialog || editingColumn != null) {
        val isEditing = editingColumn != null
        AlertDialog(
            onDismissRequest = {
                showAddColumnDialog = false
                editingColumn = null
            },
            title = { Text(if (isEditing) "Edit Column" else "Add Custom Column") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = colDisplayName,
                        onValueChange = {
                            colDisplayName = it
                            if (!isEditing && colName.isEmpty()) {
                                colName = it.lowercase().replace(" ", "_").replace(Regex("[^a-z0-9_]"), "")
                            }
                        },
                        label = { Text("Display Label (e.g. Phone Number)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("col_display_name_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = colName,
                        onValueChange = { colName = it },
                        label = { Text("Field Key (e.g. phone_number)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Data Type Dropdown Selector
                    Box {
                        OutlinedTextField(
                            value = colType.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Data Type") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("col_type_selector"),
                            trailingIcon = {
                                IconButton(onClick = { typeDropdownOpen = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                        DropdownMenu(
                            expanded = typeDropdownOpen,
                            onDismissRequest = { typeDropdownOpen = false }
                        ) {
                            ColumnType.entries.filter { it != ColumnType.AUTO_ID }.forEach { type ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(type.displayName, fontWeight = FontWeight.Bold)
                                            Text(type.description, style = MaterialTheme.typography.labelSmall)
                                        }
                                    },
                                    onClick = {
                                        colType = type
                                        typeDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }

                    if (colType == ColumnType.SINGLE_CHOICE || colType == ColumnType.MULTI_CHOICE || colType == ColumnType.TAGS) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = colOptionsInput,
                            onValueChange = { colOptionsInput = it },
                            label = { Text("Choice Options (comma-separated)") },
                            placeholder = { Text("e.g. High, Medium, Low") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = colDefaultValue,
                        onValueChange = { colDefaultValue = it },
                        label = { Text("Default Value (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Constraints toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Required Field (*)")
                        Switch(
                            checked = colRequired,
                            onCheckedChange = { colRequired = it },
                            modifier = Modifier.testTag("col_required_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Unique Value Constraint")
                        Switch(
                            checked = colUnique,
                            onCheckedChange = { colUnique = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Encrypt Column Data")
                            Text("AES-256-GCM hardware protection", style = MaterialTheme.typography.labelSmall, color = CyberCyan)
                        }
                        Switch(
                            checked = colEncrypted,
                            onCheckedChange = { colEncrypted = it }
                        )
                    }

                    if (formError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = formError ?: "",
                            color = WarningRose,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (colDisplayName.trim().isEmpty()) {
                            formError = "Column display label cannot be empty."
                            return@Button
                        }
                        val opts = colOptionsInput.split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }

                        scope.launch {
                            if (isEditing) {
                                viewModel.repository.updateColumn(
                                    editingColumn!!.copy(
                                        displayName = colDisplayName.trim(),
                                        name = colName.trim().ifEmpty { colDisplayName.trim() },
                                        dataType = colType.name,
                                        isRequired = colRequired,
                                        isUnique = colUnique,
                                        isEncrypted = colEncrypted,
                                        defaultValue = colDefaultValue.trim(),
                                        optionsJson = viewModel.repository.serializeOptions(opts)
                                    )
                                )
                                editingColumn = null
                            } else {
                                viewModel.repository.addColumn(
                                    tableId = tableId,
                                    name = colName.trim().ifEmpty { colDisplayName.trim() },
                                    displayName = colDisplayName.trim(),
                                    dataType = colType,
                                    isRequired = colRequired,
                                    isUnique = colUnique,
                                    defaultValue = colDefaultValue.trim(),
                                    options = opts,
                                    isEncrypted = colEncrypted
                                )
                                showAddColumnDialog = false
                            }
                            viewModel.refreshStorageStats()
                        }
                    },
                    modifier = Modifier.testTag("dialog_col_save_button")
                ) {
                    Text(if (isEditing) "Update" else "Add Column")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddColumnDialog = false
                        editingColumn = null
                    }
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Delete Column Confirmation
    if (columnToDelete != null) {
        ConfirmDialog(
            title = "Remove Column '${columnToDelete?.displayName}'?",
            message = "Warning: Any existing data stored for this column across all records will become inaccessible. Ensure you have backed up your database before removing schema columns.",
            confirmText = "Remove Column",
            isDestructive = true,
            onConfirm = {
                scope.launch {
                    columnToDelete?.let { viewModel.repository.deleteColumn(it) }
                    columnToDelete = null
                    viewModel.refreshStorageStats()
                }
            },
            onDismiss = { columnToDelete = null }
        )
    }
}
