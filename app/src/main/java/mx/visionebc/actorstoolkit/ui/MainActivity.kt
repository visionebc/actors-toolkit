package mx.visionebc.actorstoolkit.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import mx.visionebc.actorstoolkit.BuildConfig
import mx.visionebc.actorstoolkit.ui.navigation.AppNavHost
import mx.visionebc.actorstoolkit.ui.navigation.NavRoutes
import mx.visionebc.actorstoolkit.ui.theme.ActorsToolkitTheme
import mx.visionebc.actorstoolkit.updater.AppUpdater
import mx.visionebc.actorstoolkit.updater.UpdateChecker
import mx.visionebc.actorstoolkit.updater.UpdateInfo

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ActorsToolkitTheme {
                MainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    val bottomNavItems = remember {
        listOf(
            BottomNavItem("Scripts", Icons.Default.MenuBook, NavRoutes.ScriptList.route),
            BottomNavItem("Auditions", Icons.Default.TheaterComedy, NavRoutes.AuditionList.route),
            BottomNavItem("Self-Tapes", Icons.Default.Videocam, NavRoutes.SelfTapeList.route)
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Determine if bottom bar should be visible (only on top-level screens)
    val showBottomBar = currentRoute in listOf(
        NavRoutes.ScriptList.route,
        NavRoutes.AuditionList.route,
        NavRoutes.SelfTapeList.route
    )

    LaunchedEffect(Unit) {
        try {
            val info = UpdateChecker.checkForUpdate(BuildConfig.VERSION_CODE)
            if (info != null) {
                updateInfo = info
                showUpdateDialog = true
            }
        } catch (_: Exception) {}
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    item.icon,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        AppNavHost(navController = navController, modifier = Modifier.padding(innerPadding))
    }

    if (showUpdateDialog && updateInfo != null) {
        val info = updateInfo!!
        val sizeMb = String.format("%.1f", info.apkSizeBytes / 1_048_576.0)
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            icon = { Icon(Icons.Default.SystemUpdate, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Update Available") },
            text = {
                Column {
                    Text("A new version of ${info.appName} is available!", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    Text("Version ${info.versionName} ($sizeMb MB)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text("Current: ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { Button(onClick = { showUpdateDialog = false; AppUpdater.downloadAndInstall(context, info) }) { Text("Update Now") } },
            dismissButton = { TextButton(onClick = { showUpdateDialog = false }) { Text("Later") } }
        )
    }
}
