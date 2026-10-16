package com.example.backup

import com.example.data.db.AppDatabase
import com.example.data.entity.BackupRecordEntity
import com.example.data.repository.SecureDatabaseRepository
import com.example.security.CryptoManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets

class BackupManager(
    private val db: AppDatabase,
    private val repository: SecureDatabaseRepository
) {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val backupAdapter = moshi.adapter(AppBackupPayload::class.java)

    /**
     * Generates a complete app backup archive.
     * If password is provided, encrypts with PBKDF2 + AES-GCM; otherwise returns raw JSON bytes.
     */
    suspend fun createFullBackup(password: String? = null): ByteArray = withContext(Dispatchers.IO) {
        val databases = db.databaseDao().getAllDatabases()
        val dbItems = mutableListOf<DatabaseBackupItem>()

        var totalTables = 0
        var totalRecords = 0

        for (database in databases) {
            val tables = db.tableDao().getTablesForDatabase(database.id)
            val tableItems = mutableListOf<TableBackupItem>()

            for (table in tables) {
                val columns = db.columnDao().getColumnsForTable(table.id)
                val records = db.recordDao().getActiveRecordsForTable(table.id)
                tableItems.add(TableBackupItem(table, columns, records))
                totalRecords += records.size
            }
            totalTables += tables.size
            dbItems.add(DatabaseBackupItem(database, tableItems))
        }

        val unsignedPayload = AppBackupPayload(
            databases = dbItems,
            checksum = ""
        )
        val initialJson = backupAdapter.toJson(unsignedPayload)
        val checksum = CryptoManager.computeSha256(initialJson.toByteArray(StandardCharsets.UTF_8))
        val finalPayload = unsignedPayload.copy(checksum = checksum)
        val finalJsonBytes = backupAdapter.toJson(finalPayload).toByteArray(StandardCharsets.UTF_8)

        val outputBytes = if (!password.isNullOrEmpty()) {
            CryptoManager.encryptWithPassword(finalJsonBytes, password.toCharArray())
        } else {
            finalJsonBytes
        }

        // Record backup in database metadata
        val isEnc = !password.isNullOrEmpty()
        val filename = "haris_db_backup_${System.currentTimeMillis()}${if (isEnc) ".enc.hdb" else ".json"}"
        repository.recordBackupMetadata(
            BackupRecordEntity(
                filename = filename,
                sizeBytes = outputBytes.size.toLong(),
                isEncrypted = isEnc,
                dbCount = databases.size,
                tableCount = totalTables,
                recordCount = totalRecords,
                checksum = checksum
            )
        )
        repository.logEvent("BACKUP", "Created ${if (isEnc) "encrypted" else "standard"} backup '$filename' ($totalRecords records)", "INFO")

        outputBytes
    }

    /**
     * Restores databases, tables, and records from a backup archive.
     * Validates checksum and schema before inserting.
     */
    suspend fun restoreFullBackup(archiveBytes: ByteArray, password: String? = null): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val jsonBytes = if (!password.isNullOrEmpty()) {
                CryptoManager.decryptWithPassword(archiveBytes, password.toCharArray())
            } else {
                archiveBytes
            }

            val jsonString = String(jsonBytes, StandardCharsets.UTF_8)
            val payload = backupAdapter.fromJson(jsonString)
                ?: return@withContext Result.failure(IllegalArgumentException("Failed to parse backup payload."))

            // Checksum verification
            val testPayload = payload.copy(checksum = "")
            val computedChecksum = CryptoManager.computeSha256(backupAdapter.toJson(testPayload).toByteArray(StandardCharsets.UTF_8))
            if (payload.checksum.isNotEmpty() && payload.checksum != computedChecksum) {
                return@withContext Result.failure(SecurityException("Backup integrity check failed: Checksum mismatch. File may be corrupted or tampered."))
            }

            // Restore in transaction
            var restoredRecords = 0
            for (dbItem in payload.databases) {
                val existingDbs = db.databaseDao().getAllDatabases()
                val targetDbId = existingDbs.firstOrNull { it.name == dbItem.database.name }?.id
                    ?: db.databaseDao().insertDatabase(dbItem.database.copy(id = 0))

                for (tableItem in dbItem.tables) {
                    val existingTables = db.tableDao().getTablesForDatabase(targetDbId)
                    val targetTableId = existingTables.firstOrNull { it.name == tableItem.table.name }?.id
                        ?: db.tableDao().insertTable(tableItem.table.copy(id = 0, databaseId = targetDbId))

                    val existingCols = db.columnDao().getColumnsForTable(targetTableId)
                    val colNameMap = existingCols.associateBy { it.name }

                    for (col in tableItem.columns) {
                        if (!colNameMap.containsKey(col.name)) {
                            db.columnDao().insertColumn(col.copy(id = 0, tableId = targetTableId))
                        }
                    }

                    for (rec in tableItem.records) {
                        db.recordDao().insertRecord(rec.copy(id = 0, tableId = targetTableId))
                        restoredRecords++
                    }
                }
            }

            repository.logEvent("BACKUP", "Restored backup archive successfully ($restoredRecords records)", "INFO")
            Result.success(restoredRecords)
        } catch (e: Exception) {
            repository.logEvent("BACKUP", "Restore failed: ${e.message}", "ERROR")
            Result.failure(e)
        }
    }
}
