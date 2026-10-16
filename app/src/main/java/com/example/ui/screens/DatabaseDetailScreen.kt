package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.example.data.entity.DatabaseEntity
import com.example.data.entity.TableEntity
import com.example.ui.components.AppTopBar
import com.example.ui.components.ConfirmDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.navigation.Screen
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.WarningRose
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun DatabaseDetailScreen(databaseId: Long, viewModel: MainViewModel) {
    val scope = rememberCoroutineScope()
    var database by remember { mutableStateOf<DatabaseEntity?>(null) }
    val tables by viewModel.repository.getTablesForDatabaseFlow(databaseId)
        .collectAsState(initial = emptyList())

    LaunchedEffect(databaseId) {
        database = viewModel.repository.getDatabaseById(databaseId)
    }

    var showCreateTableDialog by remember { mutableStateOf(false) }
    var editingTable by remember { mutableStateOf<TableEntity?>(null) }
    var tableToDelete by remember { mutableStateOf<TableEntity?>(null) }

    var tableNameInput by remember { mutableStateOf("") }
    var tableDisplayNameInput by remember { mutableStateOf("") }
    var tableDescInput by remember { mutableStateOf("") }
    var dialogError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = database?.name ?: "Database",
                subtitle = if (database?.isEncrypted == true) "Encrypted Vault • ${tables.size} tables" else "${tables.size} tables",
                onBack = { viewModel.navigateBack() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    tableNameInput = ""
                    tableDisplayNameInput = ""
                    tableDescInput = ""
                    dialogError = null
                    showCreateTableDialog = true
                },
                containerColor = CyberCyan,
                modifier = Modifier.testTag("create_table_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Table")
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

            // Database Overview Card
            database?.let { db ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = CyberCyan.copy(alpha = 0.08f)
                    ),
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Storage, contentDescription = null, tint = CyberCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = db.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (db.isEncrypted) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Default.Lock, contentDescription = "Encrypted", tint = CyberCyan, modifier = Modifier.size(16.dp))
                            }
                        }
                        if (db.description.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = db.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Database Tables",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (tables.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.TableChart,
                    title = "No Tables in Database",
                    description = "Create a custom table with tailored columns to start storing and querying structured data.",
                    actionButtonText = "Create Table",
                    onActionClick = {
                        tableNameInput = ""
                        tableDisplayNameInput = ""
                        tableDescInput = ""
                        dialogError = null
                        showCreateTableDialog = true
                    }
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(tables) { table ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.navigateTo(Screen.RecordList(databaseId, table.id))
                                }
                                .testTag("table_card_${table.name}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            border = BorderStroke(1.dp, SlateCardBorder)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.TableChart,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = table.displayName,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Schema identifier: ${table.name}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            viewModel.navigateTo(Screen.TableBuilder(databaseId, table.id))
                                        },
                                        modifier = Modifier.testTag("table_builder_btn_${table.name}")
                                    ) {
                                        Icon(Icons.Default.Build, contentDescription = "Schema Builder", tint = CyberCyan)
                                    }

                                    IconButton(
                                        onClick = {
                                            viewModel.navigateTo(Screen.ImportExport(databaseId, table.id))
                                        }
                                    ) {
                                        Icon(Icons.Default.ImportExport, contentDescription = "Import / Export", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    IconButton(
                                        onClick = {
                                            editingTable = table
                                            tableNameInput = table.name
                                            tableDisplayNameInput = table.displayName
                                            tableDescInput = table.description
                                            dialogError = null
                                        }
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Table", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    IconButton(
                                        onClick = { tableToDelete = table }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Table", tint = WarningRose)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create / Edit Table Dialog
    if (showCreateTableDialog || editingTable != null) {
        val isEditing = editingTable != null
        AlertDialog(
            onDismissRequest = {
                showCreateTableDialog = false
                editingTable = null
            },
            title = { Text(if (isEditing) "Edit Table" else "Create New Table") },
            text = {
                Column {
                    OutlinedTextField(
                        value = tableDisplayNameInput,
                        onValueChange = {
                            tableDisplayNameInput = it
                            if (!isEditing && tableNameInput.isEmpty()) {
                                tableNameInput = it.lowercase().replace(" ", "_").replace(Regex("[^a-z0-9_]"), "")
                            }
                        },
                        label = { Text("Display Name (e.g. Invoices)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_table_display_name_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = tableNameInput,
                        onValueChange = { tableNameInput = it },
                        label = { Text("Schema Name (e.g. invoices)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = tableDescInput,
                        onValueChange = { tableDescInput = it },
                        label = { Text("Description (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (dialogError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = dialogError ?: "",
                            color = WarningRose,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tableDisplayNameInput.trim().isEmpty()) {
                            dialogError = "Display name cannot be empty."
                            return@Button
                        }
                        scope.launch {
                            if (isEditing) {
                                viewModel.repository.updateTable(
                                    editingTable!!.copy(
                                        displayName = tableDisplayNameInput.trim(),
                                        name = tableNameInput.trim().ifEmpty { tableDisplayNameInput.trim() },
                                        description = tableDescInput.trim()
                                    )
                                )
                                editingTable = null
                            } else {
                                viewModel.repository.createTable(
                                    databaseId = databaseId,
                                    name = tableNameInput.trim().ifEmpty { tableDisplayNameInput.trim() },
                                    displayName = tableDisplayNameInput.trim(),
                                    description = tableDescInput.trim()
                                )
                                showCreateTableDialog = false
                            }
                            viewModel.refreshStorageStats()
                        }
                    },
                    modifier = Modifier.testTag("dialog_table_save_button")
                ) {
                    Text(if (isEditing) "Update" else "Create")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreateTableDialog = false
                        editingTable = null
                    }
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Delete Table Dialog
    if (tableToDelete != null) {
        ConfirmDialog(
            title = "Delete Table '${tableToDelete?.displayName}'?",
            message = "This destructive operation will remove this table schema, all defined columns, and every contained record. This cannot be undone.",
            confirmText = "Delete Table",
            isDestructive = true,
            onConfirm = {
                scope.launch {
                    tableToDelete?.let { viewModel.repository.deleteTable(it) }
                    tableToDelete = null
                    viewModel.refreshStorageStats()
                }
            },
            onDismiss = { tableToDelete = null }
        )
    }
}
