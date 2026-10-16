package com.example

import com.example.data.model.ColumnType
import com.example.data.model.FieldValidator
import com.example.data.model.UserRole
import com.example.security.AuthManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityAndAuthTest {

    @Test
    fun testSaltGenerationAndPbkdf2Hash() {
        val salt1 = AuthManager.generateSalt()
        val salt2 = AuthManager.generateSalt()
        assertTrue("Salts should be non-empty", salt1.isNotEmpty() && salt2.isNotEmpty())
        assertTrue("Random salts should be distinct", salt1 != salt2)

        val pass = "haris@574"
        val hash1 = AuthManager.hashPassword(pass, salt1)
        val hash2 = AuthManager.hashPassword(pass, salt1)
        assertEquals("Hashing same password and salt must be deterministic", hash1, hash2)

        val hashWithDiffSalt = AuthManager.hashPassword(pass, salt2)
        assertTrue("Different salt must yield different hash", hash1 != hashWithDiffSalt)
    }

    @Test
    fun testConstantTimePasswordVerification() {
        val salt = AuthManager.generateSalt()
        val correctPass = "SuperSecret_2026!"
        val storedHash = AuthManager.hashPassword(correctPass, salt)

        assertTrue(
            "Valid password must verify true",
            AuthManager.verifyPassword(correctPass, salt, storedHash)
        )

        assertFalse(
            "Wrong password must verify false",
            AuthManager.verifyPassword("WrongPassword", salt, storedHash)
        )
    }

    @Test
    fun testFailedLoginRateLimitingAndLockout() {
        // Reset state
        AuthManager.recordSuccessfulLogin("Haris Rajput")

        // Record 4 failed attempts - should not be locked out yet
        for (i in 1..4) {
            val count = AuthManager.recordFailedAttempt()
            assertEquals(i, count)
        }
        assertEquals("Lockout should be 0 before 5 attempts", 0, AuthManager.getRemainingLockoutSeconds())

        // 5th attempt triggers lockout
        AuthManager.recordFailedAttempt()
        assertTrue("Lockout should be active after 5 failed attempts", AuthManager.getRemainingLockoutSeconds() > 0)

        // Successful login clears lockout
        AuthManager.recordSuccessfulLogin("Haris Rajput")
        assertEquals(0, AuthManager.getRemainingLockoutSeconds())
    }

    @Test
    fun testUserRolePermissions() {
        val owner = UserRole.OWNER
        val admin = UserRole.ADMIN
        val editor = UserRole.EDITOR
        val viewer = UserRole.VIEWER

        assertTrue(owner.canManageUsers())
        assertFalse(admin.canManageUsers())
        assertFalse(editor.canManageUsers())

        assertTrue(owner.canDeleteDatabase())
        assertFalse(admin.canDeleteDatabase())

        assertTrue(owner.canAddEditRecords())
        assertTrue(admin.canAddEditRecords())
        assertTrue(editor.canAddEditRecords())
        assertFalse(viewer.canAddEditRecords())
    }

    @Test
    fun testFieldValidatorDataTypes() {
        // Email
        assertTrue(FieldValidator.validate("test@example.com", ColumnType.EMAIL, true).isValid)
        assertFalse(FieldValidator.validate("invalid-email", ColumnType.EMAIL, true).isValid)

        // Integer
        assertTrue(FieldValidator.validate("42", ColumnType.INTEGER, true).isValid)
        assertFalse(FieldValidator.validate("42.5", ColumnType.INTEGER, true).isValid)
        assertFalse(FieldValidator.validate("abc", ColumnType.INTEGER, true).isValid)

        // Decimal
        assertTrue(FieldValidator.validate("19.99", ColumnType.DECIMAL, true).isValid)
        assertTrue(FieldValidator.validate("100", ColumnType.DECIMAL, true).isValid)
        assertFalse(FieldValidator.validate("not_a_number", ColumnType.DECIMAL, true).isValid)

        // Date
        assertTrue(FieldValidator.validate("2026-10-16", ColumnType.DATE, true).isValid)
        assertFalse(FieldValidator.validate("16-10-2026", ColumnType.DATE, true).isValid)

        // Required Check
        assertFalse(FieldValidator.validate("", ColumnType.SHORT_TEXT, true).isValid)
        assertTrue(FieldValidator.validate("", ColumnType.SHORT_TEXT, false).isValid)
    }
}
