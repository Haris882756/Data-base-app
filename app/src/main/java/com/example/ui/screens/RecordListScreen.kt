package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.entity.RecordEntity
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecordListScreen(databaseId: Long, tableId: Long, viewModel: MainViewModel) {
    val scope = rememberCoroutineScope()
    var table by remember { mutableStateOf<TableEntity?>(null) }
    val columns by viewModel.repository.getColumnsForTableFlow(tableId).collectAsState(initial = emptyList())
    val rawRecords by viewModel.repository.getActiveRecordsForTableFlow(tableId).collectAsState(initial = emptyList())

    LaunchedEffect(tableId) {
        table = viewModel.repository.getTableById(tableId)
    }

    var searchQuery by remember { mutableStateOf("") }
    var sortAscending by remember { mutableStateOf(false) }
    var selectedColumnFilter by remember { mutableStateOf<String?>(null) }
    var filterDropdownOpen by remember { mutableStateOf(false) }

    // Multi-select bulk state
    var isSelectMode by remember { mutableStateOf(false) }
    val selectedRecordIds = remember { mutableStateListOf<Long>() }
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    // Filter & Search Logic
    val filteredRecords = remember(rawRecords, columns, searchQuery, selectedColumnFilter, sortAscending) {
        rawRecords.filter { rec ->
            val plain = viewModel.repository.getDecryptedRecordValues(rec, columns)
            if (searchQuery.isBlank()) {
                true
            } else if (selectedColumnFilter != null) {
                plain[selectedColumnFilter]?.contains(searchQuery, ignoreCase = true) == true
            } else {
                plain.values.any { it.contains(searchQuery, ignoreCase = true) }
            }
        }.sortedWith(Comparator { r1, r2 ->
            if (sortAscending) r1.id.compareTo(r2.id) else r2.id.compareTo(r1.id)
        })
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = table?.displayName ?: "Records",
                subtitle = "${filteredRecords.size} records found",
                onBack = { viewModel.navigateBack() },
                actions = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.ImportExport(databaseId, tableId)) }
                    ) {
                        Icon(Icons.Default.ImportExport, contentDescription = "Import / Export")
                    }
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.RecycleBin(databaseId, tableId)) }
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Recycle Bin")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.navigateTo(Screen.RecordEditor(databaseId, tableId, null))
                },
                containerColor = CyberCyan,
                modifier = Modifier.testTag("add_record_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Record")
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

            // Search Bar & Filter Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("record_search_input"),
                    placeholder = {
                        Text(if (selectedColumnFilter != null) "Search in $selectedColumnFilter..." else "Search all fields...")
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = CyberCyan)
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box {
                    IconButton(onClick = { filterDropdownOpen = true }) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Filter by column",
                            tint = if (selectedColumnFilter != null) CyberCyan else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = filterDropdownOpen,
                        onDismissRequest = { filterDropdownOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Columns") },
                            onClick = {
                                selectedColumnFilter = null
                                filterDropdownOpen = false
                            }
                        )
                        columns.forEach { col ->
                            DropdownMenuItem(
                                text = { Text(col.displayName) },
                                onClick = {
                                    selectedColumnFilter = col.name
                                    filterDropdownOpen = false
                                }
                            )
                        }
                    }
                }

                IconButton(onClick = { sortAscending = !sortAscending }) {
                    Icon(
                        Icons.Default.Sort,
                        contentDescription = "Toggle Sort",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Selection Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        isSelectMode = !isSelectMode
                        selectedRecordIds.clear()
                    }
                ) {
                    Text(if (isSelectMode) "Done Selecting" else "Bulk Select")
                }

                if (isSelectMode && selectedRecordIds.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${selectedRecordIds.size} selected",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { showBulkDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete selected", tint = WarningRose)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredRecords.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Search,
                    title = if (searchQuery.isNotEmpty()) "No Matching Records" else "Table Is Empty",
                    description = if (searchQuery.isNotEmpty()) "No entries match your search criteria." else "Start adding records directly with the + button below.",
                    actionButtonText = if (searchQuery.isEmpty()) "Add Record" else null,
                    onActionClick = {
                        viewModel.navigateTo(Screen.RecordEditor(databaseId, tableId, null))
                    }
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredRecords) { record ->
                        val decryptedValues = viewModel.repository.getDecryptedRecordValues(record, columns)
                        val isSelected = selectedRecordIds.contains(record.id)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isSelectMode) {
                                        if (isSelected) selectedRecordIds.remove(record.id)
                                        else selectedRecordIds.add(record.id)
                                    } else {
                                        viewModel.navigateTo(Screen.RecordEditor(databaseId, tableId, record.id))
                                    }
                                }
                                .testTag("record_card_${record.id}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) CyberCyan.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) CyberCyan else SlateCardBorder
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isSelectMode) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { checked ->
                                                if (checked) selectedRecordIds.add(record.id)
                                                else selectedRecordIds.remove(record.id)
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Record #${record.id}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CyberCyan
                                        )
                                        Text(
                                            text = "Updated: ${dateFormat.format(Date(record.updatedAt))}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                viewModel.repository.duplicateRecord(record.id)
                                                viewModel.refreshStorageStats()
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    IconButton(
                                        onClick = {
                                            viewModel.navigateTo(Screen.RecordEditor(databaseId, tableId, record.id))
                                        }
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                viewModel.repository.softDeleteRecord(record.id)
                                                viewModel.refreshStorageStats()
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Move to Recycle Bin", tint = WarningRose)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Render key columns in this card
                                columns.take(4).forEach { col ->
                                    val cellVal = decryptedValues[col.name] ?: ""
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = col.displayName,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (col.isEncrypted) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    Icons.Default.Lock,
                                                    contentDescription = null,
                                                    tint = CyberCyan,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = cellVal.ifEmpty { "—" },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Bulk Delete Confirmation
    if (showBulkDeleteConfirm) {
        ConfirmDialog(
            title = "Move ${selectedRecordIds.size} records to Recycle Bin?",
            message = "Selected records will be moved to the recycle bin where they can be restored or permanently purged.",
            confirmText = "Move to Recycle Bin",
            isDestructive = true,
            onConfirm = {
                scope.launch {
                    for (id in selectedRecordIds) {
                        viewModel.repository.softDeleteRecord(id)
                    }
                    selectedRecordIds.clear()
                    isSelectMode = false
                    showBulkDeleteConfirm = false
                    viewModel.refreshStorageStats()
                }
            },
            onDismiss = { showBulkDeleteConfirm = false }
        )
    }
}
