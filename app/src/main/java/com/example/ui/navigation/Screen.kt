package com.example.ui.navigation

sealed interface Screen {
    data object Login : Screen
    data object Dashboard : Screen
    data object DatabaseList : Screen
    data class DatabaseDetail(val databaseId: Long) : Screen
    data class TableBuilder(val databaseId: Long, val tableId: Long) : Screen
    data class RecordList(val databaseId: Long, val tableId: Long) : Screen
    data class RecordEditor(val databaseId: Long, val tableId: Long, val recordId: Long? = null) : Screen
    data class RecycleBin(val databaseId: Long? = null, val tableId: Long? = null) : Screen
    data class ImportExport(val databaseId: Long, val tableId: Long) : Screen
    data object BackupRestore : Screen
    data object ActivityLogs : Screen
    data object Settings : Screen
}
