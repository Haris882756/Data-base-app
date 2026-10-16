package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.ActivityLogDao
import com.example.data.dao.BackupDao
import com.example.data.dao.ColumnDao
import com.example.data.dao.DatabaseDao
import com.example.data.dao.RecordDao
import com.example.data.dao.TableDao
import com.example.data.dao.UserDao
import com.example.data.entity.ActivityLogEntity
import com.example.data.entity.BackupRecordEntity
import com.example.data.entity.ColumnEntity
import com.example.data.entity.DatabaseEntity
import com.example.data.entity.RecordEntity
import com.example.data.entity.TableEntity
import com.example.data.entity.UserEntity
import com.example.security.AuthManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Database(
    entities = [
        UserEntity::class,
        DatabaseEntity::class,
        TableEntity::class,
        ColumnEntity::class,
        RecordEntity::class,
        ActivityLogEntity::class,
        BackupRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun databaseDao(): DatabaseDao
    abstract fun tableDao(): TableDao
    abstract fun columnDao(): ColumnDao
    abstract fun recordDao(): RecordDao
    abstract fun activityLogDao(): ActivityLogDao
    abstract fun backupDao(): BackupDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private val seedMutex = kotlinx.coroutines.sync.Mutex()

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "haris_secure_db_pro.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun seedInitialAdminAndSampleData(database: AppDatabase) {
            seedMutex.withLock {
                val userDao = database.userDao()
                if (userDao.getUserByUsername(AuthManager.DEFAULT_ADMIN_USERNAME) == null) {
                    val salt = AuthManager.generateSalt()
                    val passwordHash = AuthManager.hashPassword(AuthManager.INITIAL_ADMIN_PASSWORD_SEED, salt)
                    userDao.insertUser(
                        UserEntity(
                            username = AuthManager.DEFAULT_ADMIN_USERNAME,
                            passwordHash = passwordHash,
                            saltHex = salt,
                            role = "OWNER",
                            mustChangePassword = true // Prompt password change after first login
                        )
                    )

                    // Log initial system provisioning
                    database.activityLogDao().insertLog(
                        ActivityLogEntity(
                            eventType = "AUTH",
                            description = "Initial administrator account provisioned for ${AuthManager.DEFAULT_ADMIN_USERNAME}",
                            severity = "INFO",
                            username = "System"
                        )
                    )
                }

                // Seed starter database if none exists
                if (database.databaseDao().getDatabaseCount() == 0) {
                    val dbId = database.databaseDao().insertDatabase(
                    DatabaseEntity(
                        name = "Security & Asset Vault",
                        description = "Hardware inventory, cryptographic keys, and credential records",
                        iconName = "security",
                        colorHex = "#0284C7",
                        isEncrypted = true
                    )
                )

                val tableId = database.tableDao().insertTable(
                    TableEntity(
                        databaseId = dbId,
                        name = "devices",
                        displayName = "Managed Devices",
                        description = "Corporate hardware, serial numbers and warranty status"
                    )
                )

                val col1 = ColumnEntity(
                    tableId = tableId,
                    name = "asset_tag",
                    displayName = "Asset Tag ID",
                    dataType = "SHORT_TEXT",
                    isRequired = true,
                    isUnique = true,
                    sortOrder = 0
                )
                val col2 = ColumnEntity(
                    tableId = tableId,
                    name = "device_name",
                    displayName = "Device Name",
                    dataType = "SHORT_TEXT",
                    isRequired = true,
                    sortOrder = 1
                )
                val col3 = ColumnEntity(
                    tableId = tableId,
                    name = "device_type",
                    displayName = "Device Category",
                    dataType = "SINGLE_CHOICE",
                    optionsJson = "[\"Workstation\",\"Server\",\"Mobile Phone\",\"Hardware Security Key\",\"Router / Switch\"]",
                    sortOrder = 2
                )
                val col4 = ColumnEntity(
                    tableId = tableId,
                    name = "assigned_admin",
                    displayName = "Admin In-Charge",
                    dataType = "SHORT_TEXT",
                    defaultValue = "Haris Rajput",
                    sortOrder = 3
                )
                val col5 = ColumnEntity(
                    tableId = tableId,
                    name = "purchase_date",
                    displayName = "Procurement Date",
                    dataType = "DATE",
                    defaultValue = "2026-01-15",
                    sortOrder = 4
                )
                val col6 = ColumnEntity(
                    tableId = tableId,
                    name = "is_encrypted_drive",
                    displayName = "Storage Encrypted",
                    dataType = "BOOLEAN",
                    defaultValue = "true",
                    sortOrder = 5
                )

                database.columnDao().insertColumns(listOf(col1, col2, col3, col4, col5, col6))

                // Insert sample record
                val sampleValues = mapOf(
                    "asset_tag" to "HDB-SRV-001",
                    "device_name" to "Vault Production Database Primary",
                    "device_type" to "Server",
                    "assigned_admin" to "Haris Rajput",
                    "purchase_date" to "2026-01-15",
                    "is_encrypted_drive" to "true"
                )
                val moshi = com.squareup.moshi.Moshi.Builder()
                    .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                    .build()
                val mapAdapter = moshi.adapter<Map<String, String>>(
                    com.squareup.moshi.Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
                )
                database.recordDao().insertRecord(
                    RecordEntity(
                        tableId = tableId,
                        valuesJson = mapAdapter.toJson(sampleValues)
                    )
                )

                database.activityLogDao().insertLog(
                    ActivityLogEntity(
                        eventType = "SCHEMA",
                        description = "Provisioned starter vault database 'Security & Asset Vault'",
                        severity = "INFO",
                        username = "System"
                    )
                )
            }
        }
    }
}
}

