package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.ColumnType
import com.example.data.repository.SecureDatabaseRepository
import com.example.security.AuthManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DatabaseCrudRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: SecureDatabaseRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = SecureDatabaseRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testInitialAdminProvisioningAndSeedData() = runBlocking {
        AppDatabase.seedInitialAdminAndSampleData(db)

        val user = repository.getUserByUsername(AuthManager.DEFAULT_ADMIN_USERNAME)
        assertNotNull("Admin user 'Haris Rajput' must be provisioned", user)
        assertEquals("OWNER", user?.role)
        assertTrue("Initial admin must be flagged to change password", user?.mustChangePassword == true)

        val isValid = AuthManager.verifyPassword(
            AuthManager.INITIAL_ADMIN_PASSWORD_SEED,
            user!!.saltHex,
            user.passwordHash
        )
        assertTrue("Seed credentials must verify successfully", isValid)

        // Verify starter database
        val dbs = repository.getAllDatabases()
        assertTrue("Starter database must be seeded", dbs.isNotEmpty())
        val starterDb = dbs.first()
        val tables = repository.getTablesForDatabase(starterDb.id)
        assertTrue("Starter tables must exist", tables.isNotEmpty())
    }

    @Test
    fun testDatabaseTableAndRecordCrudLifecycle() = runBlocking {
        // 1. Create Database
        val dbId = repository.createDatabase(
            name = "Finance Ledger",
            description = "Expense and payment tracking",
            isEncrypted = true
        )
        assertTrue("Database ID should be > 0", dbId > 0)

        // 2. Create Table
        val tableId = repository.createTable(
            databaseId = dbId,
            name = "expenses",
            displayName = "Expense Entries",
            description = "Business disbursements"
        )
        assertTrue("Table ID should be > 0", tableId > 0)

        // 3. Add Columns
        val vendorColId = repository.addColumn(
            tableId = tableId,
            name = "vendor",
            displayName = "Vendor Name",
            dataType = ColumnType.SHORT_TEXT,
            isRequired = true
        )
        val amountColId = repository.addColumn(
            tableId = tableId,
            name = "amount",
            displayName = "Amount",
            dataType = ColumnType.DECIMAL,
            isRequired = true
        )
        val invoiceNumColId = repository.addColumn(
            tableId = tableId,
            name = "invoice_num",
            displayName = "Invoice Number",
            dataType = ColumnType.SHORT_TEXT,
            isRequired = true,
            isUnique = true,
            isEncrypted = true
        )
        assertTrue(vendorColId > 0 && amountColId > 0 && invoiceNumColId > 0)

        // 4. Insert Record
        val recordValues = mapOf(
            "vendor" to "CyberSafe Solutions",
            "amount" to "1250.50",
            "invoice_num" to "INV-2026-001"
        )
        val (recordId, errors) = repository.insertRecord(tableId, recordValues)
        assertNotNull("Record insert should succeed", recordId)
        assertTrue("No errors should be reported", errors.isEmpty())

        // 5. Test Unique Constraint
        val duplicateValues = mapOf(
            "vendor" to "Another Vendor",
            "amount" to "500.00",
            "invoice_num" to "INV-2026-001" // Duplicate invoice!
        )
        val (dupId, dupErrors) = repository.insertRecord(tableId, duplicateValues)
        assertNull("Duplicate unique constraint should prevent insertion", dupId)
        assertTrue("Duplicate error should be reported", dupErrors.containsKey("invoice_num"))

        // 6. Soft Delete to Recycle Bin
        repository.softDeleteRecord(recordId!!)
        val activeRecords = repository.getActiveRecordsForTable(tableId)
        assertTrue("Record should no longer appear in active list", activeRecords.none { it.id == recordId })

        val recycleRecords = db.recordDao().getAllRecycleBinRecords()
        assertTrue("Record should be in recycle bin", recycleRecords.any { it.id == recordId })

        // 7. Restore from Recycle Bin
        repository.restoreRecord(recordId)
        val activeAfterRestore = repository.getActiveRecordsForTable(tableId)
        assertTrue("Record should be restored back to active list", activeAfterRestore.any { it.id == recordId })

        // 8. Permanent Deletion
        repository.permanentlyDeleteRecord(recordId)
        val activeFinal = repository.getActiveRecordsForTable(tableId)
        val recycleFinal = db.recordDao().getAllRecycleBinRecords()
        assertTrue(activeFinal.none { it.id == recordId })
        assertTrue(recycleFinal.none { it.id == recordId })
    }
}
