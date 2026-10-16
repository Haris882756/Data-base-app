package com.example.backup

import com.example.data.entity.ColumnEntity
import com.example.data.entity.DatabaseEntity
import com.example.data.entity.RecordEntity
import com.example.data.entity.TableEntity
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AppBackupPayload(
    val version: String = "1.0",
    val app: String = "Haris Secure Database Builder Pro",
    val createdAt: Long = System.currentTimeMillis(),
    val databases: List<DatabaseBackupItem>,
    val checksum: String = ""
)

@JsonClass(generateAdapter = true)
data class DatabaseBackupItem(
    val database: DatabaseEntity,
    val tables: List<TableBackupItem>
)

@JsonClass(generateAdapter = true)
data class TableBackupItem(
    val table: TableEntity,
    val columns: List<ColumnEntity>,
    val records: List<RecordEntity>
)

@JsonClass(generateAdapter = true)
data class TableExportPayload(
    val exportType: String = "TABLE_EXPORT",
    val exportedAt: Long = System.currentTimeMillis(),
    val table: TableEntity,
    val columns: List<ColumnEntity>,
    val records: List<Map<String, String>>
)
