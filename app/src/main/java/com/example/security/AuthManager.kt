package com.example.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Authentication and Session Management Service for Haris Secure Database Builder Pro.
 * Enforces salted PBKDF2WithHmacSHA256 password hashing, constant-time verification,
 * rate-limiting lockout against brute-force attacks, and session timeout policies.
 */
object AuthManager {
    const val DEFAULT_ADMIN_USERNAME = "Haris Rajput"
    const val INITIAL_ADMIN_PASSWORD_SEED = "haris@574"

    private const val HASH_ITERATIONS = 12000
    private const val HASH_KEY_LENGTH = 256
    private const val SALT_LENGTH = 16
    private const val MAX_FAILED_ATTEMPTS = 5
    private const val BASE_LOCKOUT_MS = 30_000L // 30 seconds

    private val secureRandom = SecureRandom()

    // In-memory rate limiting and lockout state
    private var failedAttempts = 0
    private var lockoutUntilTimestamp = 0L

    // In-memory session state
    var currentUsername: String? = null
        private set
    var currentRole: String = "OWNER"
        private set
    var lastActiveTimestamp: Long = System.currentTimeMillis()
        private set
    var isBiometricEnabled: Boolean = false
    var isScreenSecureEnabled: Boolean = false
    var autoLockMinutes: Int = 5 // 1, 5, 15, 30, or -1 (Never)

    /**
     * Generates a cryptographically secure 16-byte random salt as hex string.
     */
    fun generateSalt(): String {
        val salt = ByteArray(SALT_LENGTH)
        secureRandom.nextBytes(salt)
        return salt.joinToString("") { "%02x".format(it) }
    }

    /**
     * Hashes password with PBKDF2WithHmacSHA256 and given salt.
     */
    fun hashPassword(password: String, saltHex: String): String {
        val saltBytes = hexToBytes(saltHex)
        val spec = PBEKeySpec(password.toCharArray(), saltBytes, HASH_ITERATIONS, HASH_KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Constant-time comparison between entered password and stored salted hash.
     */
    fun verifyPassword(password: String, saltHex: String, storedHash: String): Boolean {
        val computedHash = hashPassword(password, saltHex)
        return MessageDigest.isEqual(
            computedHash.toByteArray(Charsets.UTF_8),
            storedHash.toByteArray(Charsets.UTF_8)
        )
    }

    /**
     * Checks if authentication is currently locked out due to failed attempts.
     * Returns remaining lockout seconds (0 if not locked).
     */
    fun getRemainingLockoutSeconds(): Int {
        val now = System.currentTimeMillis()
        return if (now < lockoutUntilTimestamp) {
            (((lockoutUntilTimestamp - now) / 1000) + 1).toInt()
        } else {
            0
        }
    }

    /**
     * Records a failed login attempt. If threshold exceeded, triggers lockout.
     */
    fun recordFailedAttempt(): Int {
        failedAttempts++
        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            val multiplier = failedAttempts - MAX_FAILED_ATTEMPTS + 1
            lockoutUntilTimestamp = System.currentTimeMillis() + (BASE_LOCKOUT_MS * multiplier)
        }
        return failedAttempts
    }

    /**
     * Resets failed attempt counter after successful authentication.
     */
    fun recordSuccessfulLogin(username: String, role: String = "OWNER") {
        failedAttempts = 0
        lockoutUntilTimestamp = 0L
        currentUsername = username
        currentRole = role
        lastActiveTimestamp = System.currentTimeMillis()
    }

    /**
     * Checks if session has expired based on auto-lock timeout.
     */
    fun isSessionExpired(): Boolean {
        if (currentUsername == null) return true
        if (autoLockMinutes <= 0) return false // Never auto-lock
        val elapsedMillis = System.currentTimeMillis() - lastActiveTimestamp
        val timeoutMillis = autoLockMinutes * 60 * 1000L
        return elapsedMillis > timeoutMillis
    }

    /**
     * Refreshes user activity timestamp.
     */
    fun refreshActivity() {
        lastActiveTimestamp = System.currentTimeMillis()
    }

    /**
     * Clears current authenticated session.
     */
    fun logout() {
        currentUsername = null
        lastActiveTimestamp = 0L
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
