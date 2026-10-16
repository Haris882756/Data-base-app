package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.navigation.Screen
import com.example.ui.screens.ActivityLogsScreen
import com.example.ui.screens.BackupRestoreScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DatabaseDetailScreen
import com.example.ui.screens.DatabaseListScreen
import com.example.ui.screens.ImportExportScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.RecordEditorScreen
import com.example.ui.screens.RecordListScreen
import com.example.ui.screens.RecycleBinScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TableBuilderScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val viewModel: MainViewModel = viewModel()
                val currentScreen by viewModel.screen.collectAsState()
                val isScreenSecure by viewModel.isScreenSecure.collectAsState()

                // Apply FLAG_SECURE dynamically
                LaunchedEffect(isScreenSecure) {
                    if (isScreenSecure) {
                        window.setFlags(
                            WindowManager.LayoutParams.FLAG_SECURE,
                            WindowManager.LayoutParams.FLAG_SECURE
                        )
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }

                // Handle Android Back Navigation
                BackHandler(enabled = currentScreen != Screen.Login && currentScreen != Screen.Dashboard) {
                    viewModel.navigateBack()
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                        when (screen) {
                            is Screen.Login -> LoginScreen(viewModel = viewModel)
                            is Screen.Dashboard -> DashboardScreen(viewModel = viewModel)
                            is Screen.DatabaseList -> DatabaseListScreen(viewModel = viewModel)
                            is Screen.DatabaseDetail -> DatabaseDetailScreen(
                                databaseId = screen.databaseId,
                                viewModel = viewModel
                            )
                            is Screen.TableBuilder -> TableBuilderScreen(
                                databaseId = screen.databaseId,
                                tableId = screen.tableId,
                                viewModel = viewModel
                            )
                            is Screen.RecordList -> RecordListScreen(
                                databaseId = screen.databaseId,
                                tableId = screen.tableId,
                                viewModel = viewModel
                            )
                            is Screen.RecordEditor -> RecordEditorScreen(
                                databaseId = screen.databaseId,
                                tableId = screen.tableId,
                                recordId = screen.recordId,
                                viewModel = viewModel
                            )
                            is Screen.RecycleBin -> RecycleBinScreen(
                                databaseId = screen.databaseId,
                                tableId = screen.tableId,
                                viewModel = viewModel
                            )
                            is Screen.ImportExport -> ImportExportScreen(
                                databaseId = screen.databaseId,
                                tableId = screen.tableId,
                                viewModel = viewModel
                            )
                            is Screen.BackupRestore -> BackupRestoreScreen(viewModel = viewModel)
                            is Screen.ActivityLogs -> ActivityLogsScreen(viewModel = viewModel)
                            is Screen.Settings -> SettingsScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
