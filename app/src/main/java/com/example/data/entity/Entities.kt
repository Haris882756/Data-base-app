package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val passwordHash: String,
    val saltHex: String,
    val role: String = "OWNER",
    val mustChangePassword: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "databases",
    indices = [Index(value = ["name"], unique = true)]
)
data class DatabaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val iconName: String = "storage",
    val colorHex: String = "#0284C7",
    val isEncrypted: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "tables",
    foreignKeys = [
        ForeignKey(
            entity = DatabaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["databaseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["databaseId"]),
        Index(value = ["databaseId", "name"], unique = true)
    ]
)
data class TableEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val databaseId: Long,
    val name: String,
    val displayName: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "columns",
    foreignKeys = [
        ForeignKey(
            entity = TableEntity::class,
            parentColumns = ["id"],
            childColumns = ["tableId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["tableId"]),
        Index(value = ["tableId", "name"], unique = true)
    ]
)
data class ColumnEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tableId: Long,
    val name: String,
    val displayName: String,
    val dataType: String, // from ColumnType.name
    val isRequired: Boolean = false,
    val isUnique: Boolean = false,
    val defaultValue: String = "",
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val minValue: Double? = null,
    val maxValue: Double? = null,
    val optionsJson: String = "[]", // JSON array for choice/dropdown options
    val isSearchable: Boolean = true,
    val isHidden: Boolean = false,
    val sortOrder: Int = 0,
    val isEncrypted: Boolean = false
)

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "records",
    foreignKeys = [
        ForeignKey(
            entity = TableEntity::class,
            parentColumns = ["id"],
            childColumns = ["tableId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["tableId"]),
        Index(value = ["tableId", "isDeleted"])
    ]
)
data class RecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tableId: Long,
    val valuesJson: String, // Map<String, String> of columnName -> value (encrypted values stored as base64 ciphertext)
    val isDeleted: Boolean = false, // Soft-delete for Recycle Bin
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "activity_logs",
    indices = [Index(value = ["timestamp"])]
)
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String, // AUTH, SCHEMA, RECORD, BACKUP, EXPORT, SECURITY
    val description: String,
    val severity: String = "INFO", // INFO, WARNING, ERROR, SECURITY_ALERT
    val username: String = "System"
)

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "backup_metadata",
    indices = [Index(value = ["timestamp"])]
)
data class BackupRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val filename: String,
    val sizeBytes: Long,
    val isEncrypted: Boolean,
    val dbCount: Int,
    val tableCount: Int,
    val recordCount: Int,
    val checksum: String
)
