package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.backup.BackupManager
import com.example.backup.CsvManager
import com.example.backup.ImportResult
import com.example.data.db.AppDatabase
import com.example.data.entity.ActivityLogEntity
import com.example.data.entity.BackupRecordEntity
import com.example.data.entity.ColumnEntity
import com.example.data.entity.DatabaseEntity
import com.example.data.entity.RecordEntity
import com.example.data.entity.TableEntity
import com.example.data.entity.UserEntity
import com.example.data.model.ColumnType
import com.example.data.repository.SecureDatabaseRepository
import com.example.data.repository.StorageStats
import com.example.security.AuthManager
import com.example.ui.navigation.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    val repository = SecureDatabaseRepository(db)
    val backupManager = BackupManager(db, repository)

    // Navigation Stack
    private val _screenStack = MutableStateFlow<List<Screen>>(listOf(Screen.Login))
    val currentScreen: StateFlow<Screen> = MutableStateFlow(Screen.Login)
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Login)
    val screen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Auth State
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _lockoutSeconds = MutableStateFlow(0)
    val lockoutSeconds: StateFlow<Int> = _lockoutSeconds.asStateFlow()

    private val _showPasswordChangeDialog = MutableStateFlow(false)
    val showPasswordChangeDialog: StateFlow<Boolean> = _showPasswordChangeDialog.asStateFlow()

    // Screen Privacy
    private val _isScreenSecure = MutableStateFlow(AuthManager.isScreenSecureEnabled)
    val isScreenSecure: StateFlow<Boolean> = _isScreenSecure.asStateFlow()

    // Repositories Observers
    val databases: StateFlow<List<DatabaseEntity>> = repository.getAllDatabasesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalDatabasesCount: StateFlow<Int> = repository.getDatabaseCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalTablesCount: StateFlow<Int> = repository.getTotalTableCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalRecordsCount: StateFlow<Int> = repository.getTotalActiveRecordCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val recycleBinCount: StateFlow<Int> = repository.getRecycleBinCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val recentLogs: StateFlow<List<ActivityLogEntity>> = repository.getRecentLogsFlow(100)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestBackup: StateFlow<BackupRecordEntity?> = repository.getLatestBackupFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allBackups: StateFlow<List<BackupRecordEntity>> = repository.getAllBackupsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<UserEntity>> = repository.getAllUsersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _storageStats = MutableStateFlow(StorageStats(0, 0, 0, 0, 0, 0L))
    val storageStats: StateFlow<StorageStats> = _storageStats.asStateFlow()

    init {
        // Ensure database is initialized
        viewModelScope.launch(Dispatchers.IO) {
            AppDatabase.seedInitialAdminAndSampleData(db)
            refreshStorageStats()
        }

        // Start lockout & session timer
        viewModelScope.launch {
            while (true) {
                delay(1000)
                val remaining = AuthManager.getRemainingLockoutSeconds()
                _lockoutSeconds.value = remaining

                if (_isAuthenticated.value && AuthManager.isSessionExpired()) {
                    logout("Session expired due to inactivity.")
                }
            }
        }
    }

    fun refreshStorageStats() {
        viewModelScope.launch(Dispatchers.IO) {
            _storageStats.value = repository.calculateStorageStats()
        }
    }

    // Navigation
    fun navigateTo(destination: Screen) {
        AuthManager.refreshActivity()
        val currentList = _screenStack.value.toMutableList()
        currentList.add(destination)
        _screenStack.value = currentList
        _currentScreen.value = destination
    }

    fun navigateBack(): Boolean {
        AuthManager.refreshActivity()
        val currentList = _screenStack.value.toMutableList()
        if (currentList.size > 1) {
            currentList.removeAt(currentList.lastIndex)
            _screenStack.value = currentList
            _currentScreen.value = currentList.last()
            return true
        }
        return false
    }

    // Authentication Actions
    fun attemptLogin(username: String, passwordInput: String) {
        val remaining = AuthManager.getRemainingLockoutSeconds()
        if (remaining > 0) {
            _loginError.value = "Security lockout active. Please wait $remaining seconds."
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val user = repository.getUserByUsername(username.trim())
            if (user == null) {
                val attempts = AuthManager.recordFailedAttempt()
                repository.logEvent("AUTH", "Failed login for nonexistent user '${username.trim()}' (Attempt #$attempts)", "WARNING")
                _loginError.value = "Invalid username or password. (Attempt $attempts of 5)"
                return@launch
            }

            val isMatch = AuthManager.verifyPassword(passwordInput, user.saltHex, user.passwordHash)
            if (isMatch) {
                AuthManager.recordSuccessfulLogin(user.username, user.role)
                _currentUser.value = user
                _isAuthenticated.value = true
                _loginError.value = null
                repository.logEvent("AUTH", "Successful administrator login: ${user.username}", "INFO")

                if (user.mustChangePassword) {
                    _showPasswordChangeDialog.value = true
                }

                withContext(Dispatchers.Main) {
                    _screenStack.value = listOf(Screen.Dashboard)
                    _currentScreen.value = Screen.Dashboard
                }
            } else {
                val attempts = AuthManager.recordFailedAttempt()
                repository.logEvent("AUTH", "Failed password attempt for '${user.username}' (Attempt #$attempts)", "SECURITY_ALERT")
                _loginError.value = "Invalid username or password. (Attempt $attempts of 5)"
            }
        }
    }

    fun attemptBiometricUnlock() {
        viewModelScope.launch(Dispatchers.IO) {
            val adminUser = repository.getUserByUsername(AuthManager.DEFAULT_ADMIN_USERNAME)
                ?: db.userDao().getAllUsers().firstOrNull()

            if (adminUser != null) {
                AuthManager.recordSuccessfulLogin(adminUser.username, adminUser.role)
                _currentUser.value = adminUser
                _isAuthenticated.value = true
                _loginError.value = null
                repository.logEvent("AUTH", "Biometric unlock successful for ${adminUser.username}", "INFO")

                withContext(Dispatchers.Main) {
                    _screenStack.value = listOf(Screen.Dashboard)
                    _currentScreen.value = Screen.Dashboard
                }
            } else {
                _loginError.value = "No provisioned user found for biometric unlock."
            }
        }
    }

    fun dismissPasswordChangePrompt() {
        _showPasswordChangeDialog.value = false
    }

    fun updatePassword(currentPass: String, newPass: String, confirmPass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (newPass.length < 6) {
            onError("New password must be at least 6 characters long.")
            return
        }
        if (newPass != confirmPass) {
            onError("New passwords do not match.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val user = _currentUser.value ?: return@launch
            val isCurrentValid = AuthManager.verifyPassword(currentPass, user.saltHex, user.passwordHash)
            if (!isCurrentValid) {
                withContext(Dispatchers.Main) { onError("Current password verification failed.") }
                return@launch
            }

            repository.updateUserPassword(user, newPass)
            val updatedUser = repository.getUserByUsername(user.username)
            _currentUser.value = updatedUser
            _showPasswordChangeDialog.value = false

            withContext(Dispatchers.Main) { onSuccess() }
        }
    }

    fun logout(message: String? = null) {
        AuthManager.logout()
        _isAuthenticated.value = false
        _currentUser.value = null
        _loginError.value = message
        _screenStack.value = listOf(Screen.Login)
        _currentScreen.value = Screen.Login
    }

    fun toggleScreenSecure(enabled: Boolean) {
        AuthManager.isScreenSecureEnabled = enabled
        _isScreenSecure.value = enabled
        viewModelScope.launch(Dispatchers.IO) {
            repository.logEvent("SECURITY", "Screen Capture Protection (FLAG_SECURE) set to $enabled", "INFO")
        }
    }

    fun setAutoLockMinutes(minutes: Int) {
        AuthManager.autoLockMinutes = minutes
        viewModelScope.launch(Dispatchers.IO) {
            repository.logEvent("SECURITY", "Auto-lock timeout changed to ${if (minutes > 0) "$minutes minutes" else "Never"}", "INFO")
        }
    }
}
