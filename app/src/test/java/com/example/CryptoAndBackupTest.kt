package com.example

import com.example.backup.CsvManager
import com.example.security.CryptoManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.charset.StandardCharsets

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CryptoAndBackupTest {

    @Test
    fun testAesGcmFieldEncryptionDecryption() {
        val original = "Confidential API Key #9843-XY"
        val encrypted = CryptoManager.encryptField(original)

        assertFalse("Ciphertext should not contain plaintext", encrypted.contains(original))
        assertTrue("Ciphertext should be valid Base64", encrypted.isNotEmpty())

        val decrypted = CryptoManager.decryptField(encrypted)
        assertEquals("Decrypted text must match original", original, decrypted)
    }

    @Test
    fun testPasswordProtectedBackupEncryptionDecryption() {
        val payload = "{\"database\": \"Vault\", \"records\": [1, 2, 3]}".toByteArray(StandardCharsets.UTF_8)
        val password = "StrongBackupPassword#123"

        val encryptedArchive = CryptoManager.encryptWithPassword(payload, password.toCharArray())
        assertTrue("Encrypted archive should have header + salt + IV", encryptedArchive.size > payload.size)

        val decrypted = CryptoManager.decryptWithPassword(encryptedArchive, password.toCharArray())
        val restoredStr = String(decrypted, StandardCharsets.UTF_8)
        assertEquals("Decrypted backup must match original payload", String(payload, StandardCharsets.UTF_8), restoredStr)
    }

    @Test(expected = Exception::class)
    fun testPasswordProtectedBackupWrongPasswordFails() {
        val payload = "Top Secret Data".toByteArray(StandardCharsets.UTF_8)
        val encryptedArchive = CryptoManager.encryptWithPassword(payload, "RightPass".toCharArray())
        // Decrypting with wrong password must throw authentication tag exception
        CryptoManager.decryptWithPassword(encryptedArchive, "WrongPass".toCharArray())
    }

    @Test
    fun testSha256ChecksumIntegrity() {
        val data1 = "Data integrity block 1".toByteArray(StandardCharsets.UTF_8)
        val data2 = "Data integrity block 2".toByteArray(StandardCharsets.UTF_8)

        val hash1 = CryptoManager.computeSha256(data1)
        val hash2 = CryptoManager.computeSha256(data2)

        assertEquals(64, hash1.length)
        assertNotEquals(hash1, hash2)
    }

    @Test
    fun testCsvParsingWithQuotesAndCommas() {
        val csv = "id,name,description\n1,\"Database, Core\",\"Server with \"\"redundant\"\" drives\"\n2,Mobile,Phone"
        val (headers, rows) = CsvManager.parseCsv(csv)

        assertEquals(listOf("id", "name", "description"), headers)
        assertEquals(2, rows.size)
        assertEquals("Database, Core", rows[0][1])
        assertEquals("Server with \"redundant\" drives", rows[0][2])
        assertEquals("Mobile", rows[1][1])
    }
}
