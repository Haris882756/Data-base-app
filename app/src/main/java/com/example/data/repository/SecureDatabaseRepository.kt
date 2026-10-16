package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.entity.ActivityLogEntity
import com.example.data.entity.BackupRecordEntity
import com.example.data.entity.ColumnEntity
import com.example.data.entity.DatabaseEntity
import com.example.data.entity.RecordEntity
import com.example.data.entity.TableEntity
import com.example.data.entity.UserEntity
import com.example.data.model.ColumnType
import com.example.data.model.FieldValidator
import com.example.data.model.ValidationResult
import com.example.security.AuthManager
import com.example.security.CryptoManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SecureDatabaseRepository(private val db: AppDatabase) {
    private val userDao = db.userDao()
    private val databaseDao = db.databaseDao()
    private val tableDao = db.tableDao()
    private val columnDao = db.columnDao()
    private val recordDao = db.recordDao()
    private val activityLogDao = db.activityLogDao()
    private val backupDao = db.backupDao()
    private val moshi = Moshi.Builder()
        .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
        .build()
    private val mapType = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
    private val mapAdapter = moshi.adapter<Map<String, String>>(mapType)

    private val listStringType = Types.newParameterizedType(List::class.java, String::class.java)
    private val listStringAdapter = moshi.adapter<List<String>>(listStringType)

    // Flow Observers
    fun getAllDatabasesFlow(): Flow<List<DatabaseEntity>> = databaseDao.getAllDatabasesFlow()
    fun getDatabaseCountFlow(): Flow<Int> = databaseDao.getDatabaseCountFlow()
    fun getTotalTableCountFlow(): Flow<Int> = tableDao.getTotalTableCountFlow()
    fun getTotalActiveRecordCountFlow(): Flow<Int> = recordDao.getTotalActiveRecordCountFlow()
    fun getRecycleBinCountFlow(): Flow<Int> = recordDao.getRecycleBinCountFlow()
    fun getRecentLogsFlow(limit: Int = 50): Flow<List<ActivityLogEntity>> = activityLogDao.getRecentLogsFlow(limit)
    fun getLatestBackupFlow(): Flow<BackupRecordEntity?> = backupDao.getLatestBackupFlow()
    fun getAllBackupsFlow(): Flow<List<BackupRecordEntity>> = backupDao.getAllBackupsFlow()
    fun getAllUsersFlow(): Flow<List<UserEntity>> = userDao.getAllUsersFlow()

    fun getTablesForDatabaseFlow(databaseId: Long): Flow<List<TableEntity>> =
        tableDao.getTablesForDatabaseFlow(databaseId)

    fun getColumnsForTableFlow(tableId: Long): Flow<List<ColumnEntity>> =
        columnDao.getColumnsForTableFlow(tableId)

    fun getActiveRecordsForTableFlow(tableId: Long): Flow<List<RecordEntity>> =
        recordDao.getActiveRecordsForTableFlow(tableId)

    fun getRecycleBinRecordsForTableFlow(tableId: Long): Flow<List<RecordEntity>> =
        recordDao.getRecycleBinRecordsForTableFlow(tableId)

    fun getAllRecycleBinRecordsFlow(): Flow<List<RecordEntity>> =
        recordDao.getAllRecycleBinRecordsFlow()

    // Moshi Helpers
    fun parseValues(json: String): Map<String, String> {
        return try {
            mapAdapter.fromJson(json) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun serializeValues(values: Map<String, String>): String {
        return mapAdapter.toJson(values)
    }

    fun parseOptions(json: String): List<String> {
        return try {
            listStringAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun serializeOptions(options: List<String>): String {
        return listStringAdapter.toJson(options)
    }

    // Decrypt / Encrypt record values
    fun getDecryptedRecordValues(record: RecordEntity, columns: List<ColumnEntity>): Map<String, String> {
        val rawValues = parseValues(record.valuesJson)
        val decrypted = mutableMapOf<String, String>()
        val encryptedCols = columns.filter { it.isEncrypted }.map { it.name }.toSet()

        for ((colName, rawVal) in rawValues) {
            if (encryptedCols.contains(colName) && rawVal.isNotEmpty()) {
                decrypted[colName] = CryptoManager.decryptField(rawVal)
            } else {
                decrypted[colName] = rawVal
            }
        }
        return decrypted
    }

    fun prepareEncryptedRecordValues(
        plainValues: Map<String, String>,
        columns: List<ColumnEntity>
    ): Map<String, String> {
        val encrypted = mutableMapOf<String, String>()
        val encryptedCols = columns.filter { it.isEncrypted }.map { it.name }.toSet()

        for (col in columns) {
            val raw = plainValues[col.name]?.trim() ?: col.defaultValue.trim()
            if (encryptedCols.contains(col.name) && raw.isNotEmpty()) {
                encrypted[col.name] = CryptoManager.encryptField(raw)
            } else {
                encrypted[col.name] = raw
            }
        }
        return encrypted
    }

    // User & Authentication Management
    suspend fun getUserByUsername(username: String): UserEntity? = userDao.getUserByUsername(username)

    suspend fun updateUserPassword(user: UserEntity, newPassword: String):Boolean {
        val newSalt = AuthManager.generateSalt()
        val newHash = AuthManager.hashPassword(newPassword, newSalt)
        userDao.updateUser(
            user.copy(
                passwordHash = newHash,
                saltHex = newSalt,
                mustChangePassword = false,
                updatedAt = System.currentTimeMillis()
            )
        )
        logEvent("AUTH", "Password updated successfully for ${user.username}", "INFO")
        return true
    }

    suspend fun createUser(username: String, initialPass: String, role: String): Boolean {
        if (userDao.getUserByUsername(username) != null) return false
        val salt = AuthManager.generateSalt()
        val hash = AuthManager.hashPassword(initialPass, salt)
        userDao.insertUser(
            UserEntity(
                username = username,
                passwordHash = hash,
                saltHex = salt,
                role = role,
                mustChangePassword = true
            )
        )
        logEvent("AUTH", "Created new local profile '$username' with role $role", "INFO")
        return true
    }

    // Database Operations
    suspend fun getDatabaseById(id: Long): DatabaseEntity? = databaseDao.getDatabaseById(id)
    suspend fun getAllDatabases(): List<DatabaseEntity> = databaseDao.getAllDatabases()

    suspend fun createDatabase(
        name: String,
        description: String,
        iconName: String = "storage",
        colorHex: String = "#0284C7",
        isEncrypted: Boolean = true
    ): Long {
        val id = databaseDao.insertDatabase(
            DatabaseEntity(
                name = name.trim(),
                description = description.trim(),
                iconName = iconName,
                colorHex = colorHex,
                isEncrypted = isEncrypted
            )
        )
        logEvent("SCHEMA", "Created database '$name'", "INFO")
        return id
    }

    suspend fun updateDatabase(database: DatabaseEntity) {
        databaseDao.updateDatabase(database.copy(updatedAt = System.currentTimeMillis()))
        logEvent("SCHEMA", "Updated database '${database.name}' metadata", "INFO")
    }

    suspend fun deleteDatabase(database: DatabaseEntity) {
        databaseDao.deleteDatabase(database)
        logEvent("SCHEMA", "Deleted database '${database.name}' and all associated tables/records", "WARNING")
    }

    // Table Operations
    suspend fun getTableById(id: Long): TableEntity? = tableDao.getTableById(id)
    suspend fun getTablesForDatabase(databaseId: Long): List<TableEntity> = tableDao.getTablesForDatabase(databaseId)

    suspend fun createTable(
        databaseId: Long,
        name: String,
        displayName: String,
        description: String = ""
    ): Long {
        val sanitizedName = name.trim().lowercase().replace(Regex("[^a-z0-9_]"), "_")
        val id = tableDao.insertTable(
            TableEntity(
                databaseId = databaseId,
                name = sanitizedName,
                displayName = displayName.trim(),
                description = description.trim()
            )
        )
        // Automatically add standard Auto-ID column
        columnDao.insertColumn(
            ColumnEntity(
                tableId = id,
                name = "id",
                displayName = "ID",
                dataType = ColumnType.AUTO_ID.name,
                isRequired = true,
                isUnique = true,
                sortOrder = 0
            )
        )
        logEvent("SCHEMA", "Created table '$displayName' ($sanitizedName)", "INFO")
        return id
    }

    suspend fun updateTable(table: TableEntity) {
        tableDao.updateTable(table.copy(updatedAt = System.currentTimeMillis()))
        logEvent("SCHEMA", "Updated table '${table.displayName}'", "INFO")
    }

    suspend fun deleteTable(table: TableEntity) {
        tableDao.deleteTable(table)
        logEvent("SCHEMA", "Deleted table '${table.displayName}' and all records", "WARNING")
    }

    // Column Operations
    suspend fun getColumnsForTable(tableId: Long): List<ColumnEntity> = columnDao.getColumnsForTable(tableId)

    suspend fun addColumn(
        tableId: Long,
        name: String,
        displayName: String,
        dataType: ColumnType,
        isRequired: Boolean = false,
        isUnique: Boolean = false,
        defaultValue: String = "",
        minLength: Int? = null,
        maxLength: Int? = null,
        minValue: Double? = null,
        maxValue: Double? = null,
        options: List<String> = emptyList(),
        isSearchable: Boolean = true,
        isHidden: Boolean = false,
        isEncrypted: Boolean = false
    ): Long {
        val sanitizedName = name.trim().lowercase().replace(Regex("[^a-z0-9_]"), "_")
        val currentColumns = columnDao.getColumnsForTable(tableId)
        val nextOrder = (currentColumns.maxOfOrNull { it.sortOrder } ?: 0) + 1

        val col = ColumnEntity(
            tableId = tableId,
            name = sanitizedName,
            displayName = displayName.trim(),
            dataType = dataType.name,
            isRequired = isRequired,
            isUnique = isUnique,
            defaultValue = defaultValue.trim(),
            minLength = minLength,
            maxLength = maxLength,
            minValue = minValue,
            maxValue = maxValue,
            optionsJson = serializeOptions(options),
            isSearchable = isSearchable,
            isHidden = isHidden,
            sortOrder = nextOrder,
            isEncrypted = isEncrypted
        )
        val id = columnDao.insertColumn(col)
        logEvent("SCHEMA", "Added column '$displayName' (${dataType.name}) to table #$tableId", "INFO")
        return id
    }

    suspend fun updateColumn(column: ColumnEntity) {
        columnDao.updateColumn(column)
        logEvent("SCHEMA", "Updated column '${column.displayName}'", "INFO")
    }

    suspend fun deleteColumn(column: ColumnEntity) {
        columnDao.deleteColumn(column)
        logEvent("SCHEMA", "Removed column '${column.displayName}' from table #${column.tableId}", "WARNING")
    }

    // Record Operations with Validation & Unique Constraints
    suspend fun getRecordById(id: Long): RecordEntity? = recordDao.getRecordById(id)
    suspend fun getActiveRecordsForTable(tableId: Long): List<RecordEntity> = recordDao.getActiveRecordsForTable(tableId)

    suspend fun validateRecordInput(
        tableId: Long,
        plainValues: Map<String, String>,
        excludeRecordId: Long? = null
    ): Pair<Boolean, Map<String, String>> {
        val columns = columnDao.getColumnsForTable(tableId)
        val errors = mutableMapOf<String, String>()
        val activeRecords = recordDao.getActiveRecordsForTable(tableId)

        for (col in columns) {
            val colType = ColumnType.fromString(col.dataType)
            if (colType == ColumnType.AUTO_ID) continue

            val valStr = plainValues[col.name] ?: col.defaultValue
            val opts = parseOptions(col.optionsJson)

            val vRes = FieldValidator.validate(
                value = valStr,
                type = colType,
                isRequired = col.isRequired,
                minLength = col.minLength,
                maxLength = col.maxLength,
                minValue = col.minValue,
                maxValue = col.maxValue,
                options = opts
            )
            if (!vRes.isValid) {
                errors[col.name] = vRes.errorMessage ?: "Invalid input"
            }

            // Check unique value constraint if enabled
            if (col.isUnique && valStr.trim().isNotEmpty()) {
                val conflict = activeRecords.any { rec ->
                    if (excludeRecordId != null && rec.id == excludeRecordId) {
                        false
                    } else {
                        val recValues = getDecryptedRecordValues(rec, columns)
                        recValues[col.name]?.trim()?.equals(valStr.trim(), ignoreCase = true) == true
                    }
                }
                if (conflict) {
                    errors[col.name] = "Duplicate value: '${valStr.trim()}' already exists for unique column '${col.displayName}'."
                }
            }
        }

        return Pair(errors.isEmpty(), errors)
    }

    suspend fun insertRecord(
        tableId: Long,
        plainValues: Map<String, String>
    ): Pair<Long?, Map<String, String>> {
        val columns = columnDao.getColumnsForTable(tableId)
        val (isValid, errors) = validateRecordInput(tableId, plainValues)
        if (!isValid) {
            return Pair(null, errors)
        }

        // Generate Auto-ID values if column exists
        val mutablePlain = plainValues.toMutableMap()
        for (col in columns) {
            if (ColumnType.fromString(col.dataType) == ColumnType.AUTO_ID && mutablePlain[col.name].isNullOrEmpty()) {
                val currentMaxId = recordDao.getRecordCountForTable(tableId) + 1
                mutablePlain[col.name] = "REC-${String.format(Locale.US, "%04d", currentMaxId)}"
            }
        }

        val encryptedValues = prepareEncryptedRecordValues(mutablePlain, columns)
        val recordId = recordDao.insertRecord(
            RecordEntity(
                tableId = tableId,
                valuesJson = serializeValues(encryptedValues),
                isDeleted = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
        logEvent("RECORD", "Added record #$recordId in table #$tableId", "INFO")
        return Pair(recordId, emptyMap())
    }

    suspend fun updateRecord(
        recordId: Long,
        tableId: Long,
        plainValues: Map<String, String>
    ): Pair<Boolean, Map<String, String>> {
        val existing = recordDao.getRecordById(recordId) ?: return Pair(false, mapOf("general" to "Record not found"))
        val (isValid, errors) = validateRecordInput(tableId, plainValues, excludeRecordId = recordId)
        if (!isValid) {
            return Pair(false, errors)
        }

        val columns = columnDao.getColumnsForTable(tableId)
        val encryptedValues = prepareEncryptedRecordValues(plainValues, columns)

        recordDao.updateRecord(
            existing.copy(
                valuesJson = serializeValues(encryptedValues),
                updatedAt = System.currentTimeMillis()
            )
        )
        logEvent("RECORD", "Updated record #$recordId in table #$tableId", "INFO")
        return Pair(true, emptyMap())
    }

    suspend fun softDeleteRecord(recordId: Long) {
        val record = recordDao.getRecordById(recordId) ?: return
        recordDao.updateRecord(
            record.copy(
                isDeleted = true,
                deletedAt = System.currentTimeMillis()
            )
        )
        logEvent("RECORD", "Moved record #$recordId to Recycle Bin", "WARNING")
    }

    suspend fun restoreRecord(recordId: Long) {
        val record = recordDao.getRecordById(recordId) ?: return
        recordDao.updateRecord(
            record.copy(
                isDeleted = false,
                deletedAt = null,
                updatedAt = System.currentTimeMillis()
            )
        )
        logEvent("RECORD", "Restored record #$recordId from Recycle Bin", "INFO")
    }

    suspend fun permanentlyDeleteRecord(recordId: Long) {
        recordDao.deleteRecordById(recordId)
        logEvent("RECORD", "Permanently purged record #$recordId", "WARNING")
    }

    suspend fun purgeAllRecycleBin() {
        recordDao.purgeAllRecycleBin()
        logEvent("RECORD", "Purged all records from Recycle Bin", "WARNING")
    }

    suspend fun duplicateRecord(recordId: Long): Long? {
        val original = recordDao.getRecordById(recordId) ?: return null
        val columns = columnDao.getColumnsForTable(original.tableId)
        val plainValues = getDecryptedRecordValues(original, columns).toMutableMap()

        // Modify auto-id or append "(Copy)" to first text field
        for (col in columns) {
            if (ColumnType.fromString(col.dataType) == ColumnType.AUTO_ID) {
                plainValues[col.name] = ""
            }
        }
        val firstTextCol = columns.firstOrNull { it.dataType == ColumnType.SHORT_TEXT.name && !it.isUnique }
        if (firstTextCol != null) {
            plainValues[firstTextCol.name] = "${plainValues[firstTextCol.name] ?: ""} (Copy)"
        }

        val (newId, _) = insertRecord(original.tableId, plainValues)
        return newId
    }

    // System Logging & Auditing
    suspend fun logEvent(eventType: String, description: String, severity: String = "INFO") {
        activityLogDao.insertLog(
            ActivityLogEntity(
                timestamp = System.currentTimeMillis(),
                eventType = eventType,
                description = description,
                severity = severity,
                username = AuthManager.currentUsername ?: "Haris Rajput"
            )
        )
    }

    suspend fun getAllLogs(): List<ActivityLogEntity> = activityLogDao.getAllLogs()
    suspend fun clearAllLogs() = activityLogDao.clearAllLogs()

    // Backup Record Metadata
    suspend fun recordBackupMetadata(metadata: BackupRecordEntity) = backupDao.insertBackupMetadata(metadata)
    suspend fun getLatestBackup(): BackupRecordEntity? = backupDao.getLatestBackup()

    // Dashboard Statistics Calculation
    suspend fun calculateStorageStats(): StorageStats {
        val dbs = databaseDao.getDatabaseCount()
        val tables = tableDao.getTotalTableCount()
        val records = recordDao.getTotalActiveRecordCount()
        val recycle = recordDao.getAllRecycleBinRecords().size
        val logs = activityLogDao.getAllLogs().size

        // Estimate size in bytes
        val estimatedBytes = (dbs * 2048) + (tables * 1024) + (records * 512) + (recycle * 512) + (logs * 256) + 40960L
        return StorageStats(
            databaseCount = dbs,
            tableCount = tables,
            recordCount = records,
            recycleCount = recycle,
            logCount = logs,
            estimatedBytes = estimatedBytes
        )
    }
}

data class StorageStats(
    val databaseCount: Int,
    val tableCount: Int,
    val recordCount: Int,
    val recycleCount: Int,
    val logCount: Int,
    val estimatedBytes: Long
) {
    fun formattedSize(): String {
        return when {
            estimatedBytes < 1024 -> "$estimatedBytes B"
            estimatedBytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", estimatedBytes / 1024f)
            else -> String.format(Locale.US, "%.2f MB", estimatedBytes / (1024f * 1024f))
        }
    }
}
