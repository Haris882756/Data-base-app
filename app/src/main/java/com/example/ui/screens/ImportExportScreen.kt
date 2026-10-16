package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.backup.CsvManager
import com.example.backup.ImportResult
import com.example.data.entity.TableEntity
import com.example.ui.components.AppTopBar
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.WarningRose
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun ImportExportScreen(databaseId: Long, tableId: Long, viewModel: MainViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var table by remember { mutableStateOf<TableEntity?>(null) }
    val columns by viewModel.repository.getColumnsForTableFlow(tableId).collectAsState(initial = emptyList())
    val records by viewModel.repository.getActiveRecordsForTableFlow(tableId).collectAsState(initial = emptyList())

    LaunchedEffect(tableId) {
        table = viewModel.repository.getTableById(tableId)
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Export, 1: Import

    // Export State
    var exportFormat by remember { mutableStateOf("CSV") } // "CSV" or "JSON"
    var generatedExportContent by remember { mutableStateOf("") }

    // Import State
    var rawImportInput by remember { mutableStateOf("") }
    var parsedHeaders by remember { mutableStateOf<List<String>>(emptyList()) }
    var parsedRows by remember { mutableStateOf<List<List<String>>>(emptyList()) }
    val columnMapping = remember { mutableStateMapOf<Int, String>() }
    var importResult by remember { mutableStateOf<ImportResult?>(null) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Import & Export",
                subtitle = "Table: ${table?.displayName}",
                onBack = { viewModel.navigateBack() }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Export Data") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Import Data") }
                )
            }

            if (selectedTab == 0) {
                // EXPORT TAB
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Export Table Contents",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Generate offline CSV or structured JSON representation of ${records.size} records.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                exportFormat = "CSV"
                                generatedExportContent = CsvManager.exportToCsv(columns, records, viewModel.repository)
                            },
                            modifier = Modifier.testTag("export_csv_btn")
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate CSV")
                        }

                        OutlinedButton(
                            onClick = {
                                exportFormat = "JSON"
                                val plainRecords = records.map { rec ->
                                    viewModel.repository.getDecryptedRecordValues(rec, columns)
                                }
                                val moshi = com.squareup.moshi.Moshi.Builder()
                                    .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                                    .build()
                                val adapter = moshi.adapter<List<Map<String, String>>>(
                                    com.squareup.moshi.Types.newParameterizedType(
                                        List::class.java,
                                        com.squareup.moshi.Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
                                    )
                                )
                                generatedExportContent = adapter.indent("  ").toJson(plainRecords)
                            },
                            modifier = Modifier.testTag("export_json_btn")
                        ) {
                            Text("Generate JSON")
                        }
                    }

                    if (generatedExportContent.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Export Preview ($exportFormat)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(generatedExportContent))
                                    Toast.makeText(context, "$exportFormat copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = CyberCyan)
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ),
                            border = BorderStroke(1.dp, SlateCardBorder)
                        ) {
                            Text(
                                text = generatedExportContent.take(2000) + if (generatedExportContent.length > 2000) "\n... [Truncated for preview]" else "",
                                modifier = Modifier.padding(14.dp),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            } else {
                // IMPORT TAB
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Import Data into ${table?.displayName}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Paste CSV formatted data below to preview and map fields before committing.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = rawImportInput,
                        onValueChange = { rawImportInput = it },
                        label = { Text("Paste CSV Content") },
                        placeholder = { Text("header1,header2,header3\nval1,val2,val3") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .testTag("import_csv_input"),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val (headers, rows) = CsvManager.parseCsv(rawImportInput)
                            parsedHeaders = headers
                            parsedRows = rows
                            columnMapping.clear()

                            // Auto-match headers by name similarity
                            headers.forEachIndexed { idx, h ->
                                val matchedCol = columns.firstOrNull {
                                    it.name.equals(h, ignoreCase = true) ||
                                            it.displayName.equals(h, ignoreCase = true)
                                }
                                if (matchedCol != null) {
                                    columnMapping[idx] = matchedCol.name
                                }
                            }
                        },
                        enabled = rawImportInput.isNotBlank(),
                        modifier = Modifier.testTag("parse_csv_button")
                    ) {
                        Text("Parse & Map Headers")
                    }

                    if (parsedHeaders.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Map CSV Headers to Table Columns (${parsedRows.size} rows found)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        parsedHeaders.forEachIndexed { index, headerName ->
                            var expanded by remember { mutableStateOf(false) }
                            val mappedColName = columnMapping[index] ?: ""

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "CSV: \"$headerName\"",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    modifier = Modifier.weight(1f)
                                )

                                Box {
                                    OutlinedButton(onClick = { expanded = true }) {
                                        Text(mappedColName.ifEmpty { "Skip Column" })
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Skip Column (Do not import)") },
                                            onClick = {
                                                columnMapping.remove(index)
                                                expanded = false
                                            }
                                        )
                                        columns.forEach { col ->
                                            DropdownMenuItem(
                                                text = { Text("${col.displayName} (${col.name})") },
                                                onClick = {
                                                    columnMapping[index] = col.name
                                                    expanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    val res = CsvManager.importMappedRows(
                                        tableId = tableId,
                                        rows = parsedRows,
                                        columnMapping = columnMapping,
                                        repository = viewModel.repository
                                    )
                                    importResult = res
                                    viewModel.refreshStorageStats()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("commit_import_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Commit Import to Database")
                        }
                    }

                    if (importResult != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (importResult!!.failedCount == 0) NeonEmerald.copy(alpha = 0.15f) else WarningRose.copy(alpha = 0.15f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Import Summary:",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "• Successfully imported: ${importResult!!.importedCount} records\n• Skipped / Failed: ${importResult!!.failedCount} records",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (importResult!!.errors.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Errors:\n" + importResult!!.errors.take(5).joinToString("\n"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = WarningRose
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
