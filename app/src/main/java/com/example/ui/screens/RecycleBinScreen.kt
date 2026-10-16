package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.example.data.entity.RecordEntity
import com.example.ui.components.AppTopBar
import com.example.ui.components.ConfirmDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.WarningRose
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecycleBinScreen(
    databaseId: Long? = null,
    tableId: Long? = null,
    viewModel: MainViewModel
) {
    val scope = rememberCoroutineScope()
    val recycleRecords by if (tableId != null) {
        viewModel.repository.getRecycleBinRecordsForTableFlow(tableId).collectAsState(initial = emptyList())
    } else {
        viewModel.repository.getAllRecycleBinRecordsFlow().collectAsState(initial = emptyList())
    }

    var recordToPurge by remember { mutableStateOf<RecordEntity?>(null) }
    var showPurgeAllConfirm by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Recycle Bin",
                subtitle = "${recycleRecords.size} soft-deleted records",
                onBack = { viewModel.navigateBack() },
                actions = {
                    if (recycleRecords.isNotEmpty()) {
                        TextButton(
                            onClick = { showPurgeAllConfirm = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = WarningRose)
                        ) {
                            Text("Purge All")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            if (recycleRecords.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.DeleteSweep,
                    title = "Recycle Bin is Empty",
                    description = "Deleted records can be recovered here before permanent removal."
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 40.dp)
                ) {
                    items(recycleRecords) { record ->
                        val values = viewModel.repository.parseValues(record.valuesJson)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("recycle_item_${record.id}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            border = BorderStroke(1.dp, SlateCardBorder)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Record #${record.id} (Table #${record.tableId})",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Deleted: ${record.deletedAt?.let { dateFormat.format(Date(it)) } ?: "Unknown"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = WarningRose
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                viewModel.repository.restoreRecord(record.id)
                                                viewModel.refreshStorageStats()
                                            }
                                        },
                                        modifier = Modifier.testTag("restore_btn_${record.id}")
                                    ) {
                                        Icon(
                                            Icons.Default.Restore,
                                            contentDescription = "Restore Record",
                                            tint = NeonEmerald
                                        )
                                    }

                                    IconButton(
                                        onClick = { recordToPurge = record },
                                        modifier = Modifier.testTag("purge_btn_${record.id}")
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteForever,
                                            contentDescription = "Permanently Purge",
                                            tint = WarningRose
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                val previewText = values.entries.take(3)
                                    .joinToString(" • ") { "${it.key}: ${it.value.take(20)}" }
                                Text(
                                    text = previewText.ifEmpty { "No data values" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Single Record Purge Confirmation
    if (recordToPurge != null) {
        ConfirmDialog(
            title = "Permanently delete Record #${recordToPurge?.id}?",
            message = "This record will be permanently erased from SQLite storage. This action cannot be recovered.",
            confirmText = "Erase Forever",
            isDestructive = true,
            onConfirm = {
                scope.launch {
                    recordToPurge?.let { viewModel.repository.permanentlyDeleteRecord(it.id) }
                    recordToPurge = null
                    viewModel.refreshStorageStats()
                }
            },
            onDismiss = { recordToPurge = null }
        )
    }

    // Purge All Confirmation
    if (showPurgeAllConfirm) {
        ConfirmDialog(
            title = "Empty Entire Recycle Bin?",
            message = "All ${recycleRecords.size} soft-deleted records will be permanently purged from disk.",
            confirmText = "Empty Recycle Bin",
            isDestructive = true,
            onConfirm = {
                scope.launch {
                    viewModel.repository.purgeAllRecycleBin()
                    showPurgeAllConfirm = false
                    viewModel.refreshStorageStats()
                }
            },
            onDismiss = { showPurgeAllConfirm = false }
        )
    }
}
