package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.ActivityLogEntity
import com.example.data.entity.BackupRecordEntity
import com.example.data.entity.ColumnEntity
import com.example.data.entity.DatabaseEntity
import com.example.data.entity.RecordEntity
import com.example.data.entity.TableEntity
import com.example.data.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY id ASC")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY id ASC")
    suspend fun getAllUsers(): List<UserEntity>

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)
}

@Dao
interface DatabaseDao {
    @Query("SELECT * FROM databases ORDER BY updatedAt DESC")
    fun getAllDatabasesFlow(): Flow<List<DatabaseEntity>>

    @Query("SELECT * FROM databases ORDER BY updatedAt DESC")
    suspend fun getAllDatabases(): List<DatabaseEntity>

    @Query("SELECT * FROM databases WHERE id = :id LIMIT 1")
    suspend fun getDatabaseById(id: Long): DatabaseEntity?

    @Query("SELECT COUNT(*) FROM databases")
    suspend fun getDatabaseCount(): Int

    @Query("SELECT COUNT(*) FROM databases")
    fun getDatabaseCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDatabase(database: DatabaseEntity): Long

    @Update
    suspend fun updateDatabase(database: DatabaseEntity)

    @Delete
    suspend fun deleteDatabase(database: DatabaseEntity)

    @Query("DELETE FROM databases WHERE id = :id")
    suspend fun deleteDatabaseById(id: Long)
}

@Dao
interface TableDao {
    @Query("SELECT * FROM tables WHERE databaseId = :databaseId ORDER BY displayName ASC")
    fun getTablesForDatabaseFlow(databaseId: Long): Flow<List<TableEntity>>

    @Query("SELECT * FROM tables WHERE databaseId = :databaseId ORDER BY displayName ASC")
    suspend fun getTablesForDatabase(databaseId: Long): List<TableEntity>

    @Query("SELECT * FROM tables ORDER BY updatedAt DESC")
    suspend fun getAllTables(): List<TableEntity>

    @Query("SELECT * FROM tables WHERE id = :id LIMIT 1")
    suspend fun getTableById(id: Long): TableEntity?

    @Query("SELECT COUNT(*) FROM tables")
    suspend fun getTotalTableCount(): Int

    @Query("SELECT COUNT(*) FROM tables")
    fun getTotalTableCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM tables WHERE databaseId = :databaseId")
    suspend fun getTableCountForDatabase(databaseId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTable(table: TableEntity): Long

    @Update
    suspend fun updateTable(table: TableEntity)

    @Delete
    suspend fun deleteTable(table: TableEntity)

    @Query("DELETE FROM tables WHERE id = :id")
    suspend fun deleteTableById(id: Long)
}

@Dao
interface ColumnDao {
    @Query("SELECT * FROM columns WHERE tableId = :tableId ORDER BY sortOrder ASC, id ASC")
    fun getColumnsForTableFlow(tableId: Long): Flow<List<ColumnEntity>>

    @Query("SELECT * FROM columns WHERE tableId = :tableId ORDER BY sortOrder ASC, id ASC")
    suspend fun getColumnsForTable(tableId: Long): List<ColumnEntity>

    @Query("SELECT * FROM columns ORDER BY id ASC")
    suspend fun getAllColumns(): List<ColumnEntity>

    @Query("SELECT * FROM columns WHERE id = :id LIMIT 1")
    suspend fun getColumnById(id: Long): ColumnEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertColumn(column: ColumnEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertColumns(columns: List<ColumnEntity>): List<Long>

    @Update
    suspend fun updateColumn(column: ColumnEntity)

    @Delete
    suspend fun deleteColumn(column: ColumnEntity)

    @Query("DELETE FROM columns WHERE id = :id")
    suspend fun deleteColumnById(id: Long)
}

@Dao
interface RecordDao {
    @Query("SELECT * FROM records WHERE tableId = :tableId AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getActiveRecordsForTableFlow(tableId: Long): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE tableId = :tableId AND isDeleted = 0 ORDER BY updatedAt DESC")
    suspend fun getActiveRecordsForTable(tableId: Long): List<RecordEntity>

    @Query("SELECT * FROM records WHERE tableId = :tableId AND isDeleted = 1 ORDER BY deletedAt DESC")
    fun getRecycleBinRecordsForTableFlow(tableId: Long): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getAllRecycleBinRecordsFlow(): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    suspend fun getAllRecycleBinRecords(): List<RecordEntity>

    @Query("SELECT * FROM records WHERE id = :id LIMIT 1")
    suspend fun getRecordById(id: Long): RecordEntity?

    @Query("SELECT * FROM records ORDER BY id ASC")
    suspend fun getAllRecords(): List<RecordEntity>

    @Query("SELECT COUNT(*) FROM records WHERE isDeleted = 0")
    suspend fun getTotalActiveRecordCount(): Int

    @Query("SELECT COUNT(*) FROM records WHERE isDeleted = 0")
    fun getTotalActiveRecordCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM records WHERE isDeleted = 1")
    fun getRecycleBinCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM records WHERE tableId = :tableId AND isDeleted = 0")
    suspend fun getRecordCountForTable(tableId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: RecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<RecordEntity>): List<Long>

    @Update
    suspend fun updateRecord(record: RecordEntity)

    @Delete
    suspend fun deleteRecord(record: RecordEntity)

    @Query("DELETE FROM records WHERE id = :id")
    suspend fun deleteRecordById(id: Long)

    @Query("DELETE FROM records WHERE tableId = :tableId")
    suspend fun deleteRecordsForTable(tableId: Long)

    @Query("DELETE FROM records WHERE isDeleted = 1")
    suspend fun purgeAllRecycleBin()

    @Query("DELETE FROM records WHERE isDeleted = 1 AND deletedAt < :olderThanTimestamp")
    suspend fun purgeOldRecycleBinRecords(olderThanTimestamp: Long)
}

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogsFlow(limit: Int = 100): Flow<List<ActivityLogEntity>>

    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC")
    suspend fun getAllLogs(): List<ActivityLogEntity>

    @Insert
    suspend fun insertLog(log: ActivityLogEntity): Long

    @Query("DELETE FROM activity_logs")
    suspend fun clearAllLogs()

    @Query("DELETE FROM activity_logs WHERE timestamp < :olderThanTimestamp")
    suspend fun purgeOldLogs(olderThanTimestamp: Long)
}

@Dao
interface BackupDao {
    @Query("SELECT * FROM backup_metadata ORDER BY timestamp DESC")
    fun getAllBackupsFlow(): Flow<List<BackupRecordEntity>>

    @Query("SELECT * FROM backup_metadata ORDER BY timestamp DESC LIMIT 1")
    fun getLatestBackupFlow(): Flow<BackupRecordEntity?>

    @Query("SELECT * FROM backup_metadata ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestBackup(): BackupRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackupMetadata(metadata: BackupRecordEntity): Long

    @Delete
    suspend fun deleteBackupMetadata(metadata: BackupRecordEntity)
}
