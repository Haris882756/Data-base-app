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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
fun DatabaseListScreen(viewModel: MainViewModel) {
    val databases by viewModel.databases.collectAsState()
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingDb by remember { mutableStateOf<DatabaseEntity?>(null) }
    var dbToDelete by remember { mutableStateOf<DatabaseEntity?>(null) }

    // Dialog form state
    var newDbName by remember { mutableStateOf("") }
    var newDbDesc by remember { mutableStateOf("") }
    var newDbEncrypted by remember { mutableStateOf(true) }
    var dialogError by remember { mutableStateOf<String?>(null) }

    val filteredDatabases = databases.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Databases Vault",
                subtitle = "${databases.size} custom database repositories",
                onBack = { viewModel.navigateBack() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    newDbName = ""
                    newDbDesc = ""
                    newDbEncrypted = true
                    dialogError = null
                    showCreateDialog = true
                },
                containerColor = CyberCyan,
                modifier = Modifier.testTag("create_database_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Database")
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

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("database_search_input"),
                placeholder = { Text("Search databases...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = CyberCyan)
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredDatabases.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Storage,
                    title = if (searchQuery.isEmpty()) "No Databases Created" else "No Matching Databases",
                    description = if (searchQuery.isEmpty()) "Create your first database repository to organize tables and schemas." else "Try adjusting your search query.",
                    actionButtonText = if (searchQuery.isEmpty()) "Create Database" else null,
                    onActionClick = {
                        newDbName = ""
                        newDbDesc = ""
                        dialogError = null
                        showCreateDialog = true
                    }
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredDatabases) { db ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.navigateTo(Screen.DatabaseDetail(db.id)) }
                                .testTag("database_item_${db.name}"),
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
                                        color = CyberCyan.copy(alpha = 0.15f),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Storage,
                                            contentDescription = null,
                                            tint = CyberCyan,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = db.name,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (db.isEncrypted) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = "Encrypted",
                                                    tint = CyberCyan,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }

                                        if (db.description.isNotEmpty()) {
                                            Text(
                                                text = db.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            editingDb = db
                                            newDbName = db.name
                                            newDbDesc = db.description
                                            newDbEncrypted = db.isEncrypted
                                            dialogError = null
                                        }
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    IconButton(
                                        onClick = { dbToDelete = db }
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
    }

    // Create / Edit Database Dialog
    if (showCreateDialog || editingDb != null) {
        val isEditing = editingDb != null
        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
                editingDb = null
            },
            title = { Text(if (isEditing) "Edit Database" else "Create New Database") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newDbName,
                        onValueChange = { newDbName = it },
                        label = { Text("Database Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_db_name_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newDbDesc,
                        onValueChange = { newDbDesc = it },
                        label = { Text("Description (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "AES-256 Storage Encryption",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Encrypt sensitive records with Android Keystore",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = newDbEncrypted,
                            onCheckedChange = { newDbEncrypted = it }
                        )
                    }

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
                        if (newDbName.trim().isEmpty()) {
                            dialogError = "Database name cannot be blank."
                            return@Button
                        }
                        scope.launch {
                            if (isEditing) {
                                viewModel.repository.updateDatabase(
                                    editingDb!!.copy(
                                        name = newDbName.trim(),
                                        description = newDbDesc.trim(),
                                        isEncrypted = newDbEncrypted
                                    )
                                )
                                editingDb = null
                            } else {
                                viewModel.repository.createDatabase(
                                    name = newDbName.trim(),
                                    description = newDbDesc.trim(),
                                    isEncrypted = newDbEncrypted
                                )
                                showCreateDialog = false
                            }
                            viewModel.refreshStorageStats()
                        }
                    },
                    modifier = Modifier.testTag("dialog_db_save_button")
                ) {
                    Text(if (isEditing) "Update" else "Create")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreateDialog = false
                        editingDb = null
                    }
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Delete Confirmation Dialog
    if (dbToDelete != null) {
        ConfirmDialog(
            title = "Delete Database '${dbToDelete?.name}'?",
            message = "This destructive action will permanently remove this database, along with all containing tables, columns, and records. This action cannot be undone.",
            confirmText = "Delete Permanently",
            isDestructive = true,
            onConfirm = {
                scope.launch {
                    dbToDelete?.let { viewModel.repository.deleteDatabase(it) }
                    dbToDelete = null
                    viewModel.refreshStorageStats()
                }
            },
            onDismiss = { dbToDelete = null }
        )
    }
}
