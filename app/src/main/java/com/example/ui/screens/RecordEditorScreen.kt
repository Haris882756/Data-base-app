package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import com.example.ui.components.DynamicFieldInput
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.WarningRose
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun RecordEditorScreen(
    databaseId: Long,
    tableId: Long,
    recordId: Long? = null,
    viewModel: MainViewModel
) {
    val scope = rememberCoroutineScope()
    var table by remember { mutableStateOf<TableEntity?>(null) }
    var existingRecord by remember { mutableStateOf<RecordEntity?>(null) }
    val columns by viewModel.repository.getColumnsForTableFlow(tableId).collectAsState(initial = emptyList())

    val formValues = remember { mutableStateMapOf<String, String>() }
    val fieldErrors = remember { mutableStateMapOf<String, String>() }
    var generalError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(tableId, recordId) {
        table = viewModel.repository.getTableById(tableId)
        if (recordId != null) {
            val rec = viewModel.repository.getRecordById(recordId)
            existingRecord = rec
            if (rec != null) {
                val decrypted = viewModel.repository.getDecryptedRecordValues(rec, columns)
                formValues.putAll(decrypted)
            }
        }
    }

    LaunchedEffect(columns) {
        // Apply default values for fields not already set
        if (recordId == null) {
            columns.forEach { col ->
                if (!formValues.containsKey(col.name) && col.defaultValue.isNotEmpty()) {
                    formValues[col.name] = col.defaultValue
                }
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (recordId == null) "New ${table?.displayName ?: "Record"}" else "Edit Record #${recordId}",
                subtitle = "Table: ${table?.displayName}",
                onBack = { viewModel.navigateBack() }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 40.dp, top = 8.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateCardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Record Attributes",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Fields marked with (*) are required. Encrypted columns are protected via Android Keystore.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        columns.forEach { col ->
                            val currentVal = formValues[col.name] ?: ""
                            val error = fieldErrors[col.name]
                            val opts = viewModel.repository.parseOptions(col.optionsJson)

                            DynamicFieldInput(
                                column = col,
                                value = currentVal,
                                errorMessage = error,
                                options = opts,
                                onValueChange = { newVal ->
                                    formValues[col.name] = newVal
                                    fieldErrors.remove(col.name)
                                }
                            )
                        }

                        if (generalError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = generalError ?: "",
                                color = WarningRose,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    fieldErrors.clear()
                                    generalError = null

                                    if (recordId == null) {
                                        val (newId, errors) = viewModel.repository.insertRecord(tableId, formValues)
                                        if (newId != null) {
                                            viewModel.refreshStorageStats()
                                            viewModel.navigateBack()
                                        } else {
                                            fieldErrors.putAll(errors)
                                            generalError = "Please correct the highlighted errors before saving."
                                        }
                                    } else {
                                        val (success, errors) = viewModel.repository.updateRecord(recordId, tableId, formValues)
                                        if (success) {
                                            viewModel.refreshStorageStats()
                                            viewModel.navigateBack()
                                        } else {
                                            fieldErrors.putAll(errors)
                                            generalError = "Please correct the highlighted errors before saving."
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("save_record_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (recordId == null) "Save New Record" else "Update Record",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
