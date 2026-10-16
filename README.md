# Haris Secure Database Builder Pro

**Haris Secure Database Builder Pro** is a production-grade, offline-first Android application designed for creating and managing custom databases, relational tables, dynamic schemas, and records directly on-device with enterprise defense-in-depth security.

---

## 1. Key Implemented Features

### Offline-First Architecture & Security
- **Air-Gapped Operation**: 100% offline. No internet permissions are declared in `AndroidManifest.xml` (`INTERNET` permission is completely omitted to guarantee zero remote leaks).
- **Hardware-Backed Encryption**: Android Keystore integration using AES-256-GCM (`AES/GCM/NoPadding`) with 96-bit random IVs and 128-bit authentication tags for columns marked sensitive.
- **Salted Master Password Hashing**: PBKDF2WithHmacSHA256 with 12,000 rounds and cryptographically secure 16-byte random salts.
- **Constant-Time Verification**: Prevents timing side-channel attacks via `MessageDigest.isEqual`.
- **Brute-Force Rate Limiting & Lockout**: Exponential delay timer triggered after 5 failed login attempts.
- **Inactivity Auto-Lock**: Configurable session timeouts (1 min, 5 min, 15 min, or Never).
- **Screen Protection**: Dynamic `FLAG_SECURE` window toggles to prevent unauthorized screenshots, screen recordings, and app-switcher snooping.
- **Biometric Unlock**: Biometric prompt and keyguard authentication shortcut.
- **Cloud Backup Exclusion**: `android:allowBackup="false"` in manifest to prevent unencrypted cloud extraction.

### Custom Database Builder
- **Multi-Database Management**: Create, rename, customize, and delete isolated databases with metadata (names, descriptions, color accents).
- **Custom Tables**: Visual table creator without requiring SQL knowledge.
- **16 Supported Column Data Types**:
  1. Short Text
  2. Long Text
  3. Integer
  4. Decimal
  5. Boolean (Toggle switch)
  6. Date (YYYY-MM-DD)
  7. Time (HH:MM)
  8. Date & Time (YYYY-MM-DD HH:MM)
  9. Email (with regex validation)
  10. Phone Number (with international format validation)
  11. URL / Link
  12. Single-Choice Selection (Dropdown)
  13. Multiple-Choice Selection (Filter chips)
  14. Tags
  15. JSON Document
  16. Auto-Generated Identifier (e.g. `REC-0001`)
- **Column Constraints**: Required (`*`), Unique value checks, Default values, Min/Max limits, Encrypted column flag.

### Dynamic Record Management
- **Adaptive Form Generation**: Automatically creates customized Compose input controls for every column type with real-time input validation.
- **Fast Local Search & Filters**: Search across all columns or filter by specific columns; sort ascending or descending.
- **Bulk Operations**: Multi-select mode for batch deletions and batch exports.
- **Record Duplication**: Quick clone of existing records with auto-id generation.
- **Recycle Bin & Safe Recovery**: Soft-delete records to the Recycle Bin with individual/bulk restore and permanent wipe options.

### Backup, Recovery & Data Portability
- **Cryptographic Backups**: Standalone JSON archives with SHA-256 integrity checksums.
- **Encrypted Archives**: Optional PBKDF2 (65,536 rounds) + AES-256-GCM password protection for safe off-device file storage.
- **Data Integrity Verification**: Validates checksum before applying any restoration.
- **CSV Import & Export**: RFC 4180 compliant CSV export and CSV import with automatic column header mapping and row preview.
- **JSON Import & Export**: Table-level and database-level structured JSON portability.
- **Comprehensive Audit Ledger**: Filterable activity log recording logins, schema changes, record CRUD, exports, and backup events.

---

## 2. Default Administrator Credentials

During first-run provisioning, the database seeds the following master administrator account:
- **Username**: `Haris Rajput`
- **Initial Password**: `haris@574`

*Note: The password is never stored in plaintext. It is immediately hashed with a cryptographically secure random salt upon first launch. The application prompts for a password update after the first successful login.*

---

## 3. Technology Stack

- **Platform**: Native Android (Kotlin 2.2.10)
- **UI Framework**: Jetpack Compose with Material Design 3
- **Local Persistence**: Room 2.7.0 with SQLite engine and KSP
- **Cryptography**: Android Keystore, Java `javax.crypto` (AES-256-GCM, PBKDF2WithHmacSHA256, SHA-256)
- **Serialization**: Moshi Kotlin 1.15.2
- **Testing**: JUnit 4, Robolectric 4.16.1

---

## 4. Build and Installation Instructions

### Prerequisites
- JDK 17 or higher
- Android SDK with `compileSdk 36` and `minSdk 24`

### Compiling and Running Unit Tests
To execute all local JVM and Robolectric tests:
```bash
gradle :app:testDebugUnitTest
```

### Building the Debug APK
```bash
gradle :app:assembleDebug
```
The output APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### Building the Release APK
```bash
gradle :app:assembleRelease
```
The signed release APK will be located at:
`app/build/outputs/apk/release/app-release.apk`

---

## 5. Security Threat Model & Known Limitations

### Protections Provided
1. **At-Rest Field Encryption**: Sensitive data marked as encrypted is shielded via the hardware Keystore.
2. **Offline Isolation**: No network connectivity eliminates network-based exfiltration.
3. **Protection against Brute-Force**: Progressive lockout periods throttle automated PIN or password attacks.
4. **App-Switcher Privacy**: `FLAG_SECURE` prevents OS-level screen capture and task-switcher previews.

### Limitations & Advisory
1. **Device Compromise**: If an attacker gains root access to an unlocked device, memory inspection can theoretically inspect in-memory data during active use.
2. **Offline Password Recovery**: Because this application is strictly offline, there is no remote "forgot password" email reset. Users must maintain an encrypted backup archive to recover access if passwords are forgotten.
