package com.example.data.model

enum class UserRole(val displayName: String, val level: Int) {
    OWNER("Owner / Super Admin", 4),
    ADMIN("Administrator", 3),
    EDITOR("Editor", 2),
    VIEWER("Read-Only User", 1);

    fun canManageUsers(): Boolean = this == OWNER
    fun canModifySchema(): Boolean = this == OWNER || this == ADMIN
    fun canCreateDatabase(): Boolean = this == OWNER || this == ADMIN
    fun canDeleteDatabase(): Boolean = this == OWNER
    fun canAddEditRecords(): Boolean = this == OWNER || this == ADMIN || this == EDITOR
    fun canDeleteRecords(): Boolean = this == OWNER || this == ADMIN
    fun canExport(): Boolean = this != VIEWER || true // Configurable per security policy
    fun canBackupRestore(): Boolean = this == OWNER || this == ADMIN

    companion object {
        fun fromString(roleStr: String): UserRole {
            return entries.firstOrNull { it.name.equals(roleStr, ignoreCase = true) } ?: OWNER
        }
    }
}
