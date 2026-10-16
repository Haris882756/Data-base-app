package com.example.backup

import com.example.data.entity.ColumnEntity
import com.example.data.entity.RecordEntity
import com.example.data.repository.SecureDatabaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CsvManager {

    /**
     * Exports table columns and decrypted records into RFC 4180 CSV string.
     */
    fun exportToCsv(
        columns: List<ColumnEntity>,
        records: List<RecordEntity>,
        repository: SecureDatabaseRepository
    ): String {
        val visibleCols = columns.filter { !it.isHidden }
        val sb = StringBuilder()

        // Header
        val header = visibleCols.joinToString(",") { escapeCsvCell(it.displayName.ifEmpty { it.name }) }
        sb.append(header).append("\n")

        // Rows
        for (rec in records) {
            val decryptedValues = repository.getDecryptedRecordValues(rec, columns)
            val row = visibleCols.joinToString(",") { col ->
                val cellVal = decryptedValues[col.name] ?: ""
                escapeCsvCell(cellVal)
            }
            sb.append(row).append("\n")
        }

        return sb.toString()
    }

    private fun escapeCsvCell(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    /**
     * Parses CSV text into a header list and rows of cells, respecting quoted strings.
     */
    fun parseCsv(csvText: String): Pair<List<String>, List<List<String>>> {
        val lines = splitCsvLines(csvText)
        if (lines.isEmpty()) return Pair(emptyList(), emptyList())

        val headers = parseCsvRow(lines[0])
        val rows = lines.drop(1)
            .filter { it.isNotBlank() }
            .map { parseCsvRow(it) }

        return Pair(headers, rows)
    }

    private fun splitCsvLines(csv: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var insideQuotes = false

        for (c in csv) {
            if (c == '"') {
                insideQuotes = !insideQuotes
                sb.append(c)
            } else if ((c == '\n' || c == '\r') && !insideQuotes) {
                if (sb.isNotEmpty()) {
                    result.add(sb.toString())
                    sb.clear()
                }
            } else {
                sb.append(c)
            }
        }
        if (sb.isNotEmpty()) {
            result.add(sb.toString())
        }
        return result
    }

    private fun parseCsvRow(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    sb.append('"')
                    i++ // Skip second quote
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString().trim())
                sb.clear()
            } else {
                sb.append(c)
            }
            i++
        }
        tokens.add(sb.toString().trim())
        return tokens
    }

    /**
     * Imports parsed CSV rows according to column mapping:
     * [csvHeaderIndex -> destinationColumnName]
     */
    suspend fun importMappedRows(
        tableId: Long,
        rows: List<List<String>>,
        columnMapping: Map<Int, String>,
        repository: SecureDatabaseRepository
    ): ImportResult = withContext(Dispatchers.IO) {
        var importedCount = 0
        var skippedCount = 0
        val errorList = mutableListOf<String>()

        for ((index, row) in rows.withIndex()) {
            val recordMap = mutableMapOf<String, String>()
            for ((csvIdx, colName) in columnMapping) {
                if (csvIdx < row.size && colName.isNotEmpty()) {
                    recordMap[colName] = row[csvIdx]
                }
            }

            if (recordMap.isEmpty()) {
                skippedCount++
                continue
            }

            val (recId, errors) = repository.insertRecord(tableId, recordMap)
            if (recId != null) {
                importedCount++
            } else {
                skippedCount++
                val msg = "Row #${index + 2}: " + errors.values.joinToString("; ")
                errorList.add(msg)
            }
        }

        repository.logEvent("RECORD", "CSV Import to table #$tableId: $importedCount inserted, $skippedCount failed", "INFO")
        ImportResult(importedCount, skippedCount, errorList)
    }
}

data class ImportResult(
    val importedCount: Int,
    val failedCount: Int,
    val errors: List<String>
)
