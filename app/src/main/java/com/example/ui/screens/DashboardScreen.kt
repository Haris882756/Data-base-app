package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.DatabaseEntity
import com.example.security.AuthManager
import com.example.ui.components.AppTopBar
import com.example.ui.components.StatCard
import com.example.ui.navigation.Screen
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.SecurityAmber
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.WarningRose
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val totalDatabases by viewModel.totalDatabasesCount.collectAsState()
    val totalTables by viewModel.totalTablesCount.collectAsState()
    val totalRecords by viewModel.totalRecordsCount.collectAsState()
    val recycleBinCount by viewModel.recycleBinCount.collectAsState()
    val storageStats by viewModel.storageStats.collectAsState()
    val latestBackup by viewModel.latestBackup.collectAsState()
    val recentDatabases by viewModel.databases.collectAsState()
    val recentLogs by viewModel.recentLogs.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshStorageStats()
    }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Haris Secure DB Pro",
                subtitle = "Logged in as ${currentUser?.username ?: AuthManager.DEFAULT_ADMIN_USERNAME} (${currentUser?.role ?: "OWNER"})",
                actions = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.Settings) },
                        modifier = Modifier.testTag("dashboard_settings_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    IconButton(
                        onClick = { viewModel.logout() },
                        modifier = Modifier.testTag("dashboard_logout_button")
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = WarningRose)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.navigateTo(Screen.DatabaseList) },
                containerColor = CyberCyan,
                contentColor = Color.Black,
                modifier = Modifier.testTag("dashboard_fab_create_db")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Database")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp)
        ) {
            // Security Status Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = CyberCyan.copy(alpha = 0.1f)
                    ),
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = NeonEmerald.copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = NeonEmerald,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Vault Protected & Encrypted",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Offline Keystore AES-256-GCM • Zero Remote Leaks",
                                style = MaterialTheme.typography.bodySmall,
                                color = CyberCyan
                            )
                        }
                    }
                }
            }

            // Real Metrics Grid
            item {
                Text(
                    text = "System Statistics",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "DATABASES",
                        value = totalDatabases.toString(),
                        subtext = "Active stores",
                        icon = Icons.Default.Storage,
                        iconTint = CyberCyan,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(Screen.DatabaseList) }
                    )
                    StatCard(
                        title = "TABLES",
                        value = totalTables.toString(),
                        subtext = "Custom schemas",
                        icon = Icons.Default.TableChart,
                        iconTint = Color(0xFF6366F1),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "RECORDS",
                        value = totalRecords.toString(),
                        subtext = "${recycleBinCount} in recycle bin",
                        icon = Icons.Default.ListAlt,
                        iconTint = NeonEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    val lastBackupText = if (latestBackup != null) {
                        dateFormat.format(Date(latestBackup!!.timestamp))
                    } else "Never"

                    StatCard(
                        title = "STORAGE",
                        value = storageStats.formattedSize(),
                        subtext = "Backup: $lastBackupText",
                        icon = Icons.Default.Backup,
                        iconTint = SecurityAmber,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(Screen.BackupRestore) }
                    )
                }
            }

            // Quick Actions Hub
            item {
                Text(
                    text = "Quick Operations",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 3
                ) {
                    QuickActionButton(
                        icon = Icons.Default.Add,
                        label = "New Database",
                        onClick = { viewModel.navigateTo(Screen.DatabaseList) }
                    )
                    QuickActionButton(
                        icon = Icons.Default.Storage,
                        label = "Open Databases",
                        onClick = { viewModel.navigateTo(Screen.DatabaseList) }
                    )
                    QuickActionButton(
                        icon = Icons.Default.Backup,
                        label = "Backup / Restore",
                        onClick = { viewModel.navigateTo(Screen.BackupRestore) }
                    )
                    QuickActionButton(
                        icon = Icons.Default.DeleteSweep,
                        label = "Recycle Bin",
                        badge = if (recycleBinCount > 0) recycleBinCount.toString() else null,
                        onClick = { viewModel.navigateTo(Screen.RecycleBin()) }
                    )
                    QuickActionButton(
                        icon = Icons.Default.History,
                        label = "Activity Logs",
                        onClick = { viewModel.navigateTo(Screen.ActivityLogs) }
                    )
                    QuickActionButton(
                        icon = Icons.Default.Settings,
                        label = "Security Vault",
                        onClick = { viewModel.navigateTo(Screen.Settings) }
                    )
                }
            }

            // Recently Opened / Created Databases
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Databases",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = { viewModel.navigateTo(Screen.DatabaseList) }) {
                        Text("View All ($totalDatabases)")
                    }
                }
            }

            if (recentDatabases.isEmpty()) {
                item {
                    Text(
                        text = "No databases created yet. Click '+' to start building your first schema.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(recentDatabases.take(3)) { db ->
                    DatabaseListItem(
                        database = db,
                        onClick = { viewModel.navigateTo(Screen.DatabaseDetail(db.id)) }
                    )
                }
            }

            // Recent Audit Activity Feed
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Live Audit Stream",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = { viewModel.navigateTo(Screen.ActivityLogs) }) {
                        Text("Full Audit Log")
                    }
                }
            }

            items(recentLogs.take(5)) { log ->
                AuditLogItem(log = log, dateFormat = dateFormat)
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    badge: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(106.dp)
            .clickable(onClick = onClick)
            .testTag("quick_action_${label.replace(" ", "_").lowercase()}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, SlateCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = CyberCyan,
                    modifier = Modifier.size(24.dp)
                )
                if (badge != null) {
                    Surface(
                        shape = CircleShape,
                        color = WarningRose,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(16.dp)
                    ) {
                        Text(
                            text = badge,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
fun DatabaseListItem(
    database: DatabaseEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("db_card_${database.name}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, SlateCardBorder)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = CyberCyan.copy(alpha = 0.15f),
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = database.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (database.description.isNotEmpty()) {
                    Text(
                        text = database.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            if (database.isEncrypted) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Encrypted",
                    tint = CyberCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AuditLogItem(
    log: com.example.data.entity.ActivityLogEntity,
    dateFormat: SimpleDateFormat
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val badgeColor = when (log.severity) {
            "WARNING" -> SecurityAmber
            "ERROR", "SECURITY_ALERT" -> WarningRose
            else -> CyberCyan
        }
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(badgeColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = log.description,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${dateFormat.format(Date(log.timestamp))} • [${log.eventType}] by ${log.username}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
