package mx.visionebc.actorstoolkit.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import mx.visionebc.actorstoolkit.BuildConfig
import mx.visionebc.actorstoolkit.data.preferences.SettingsDataStore
import mx.visionebc.actorstoolkit.data.preferences.ThemeMode
import mx.visionebc.actorstoolkit.ui.navigation.AppNavHost
import mx.visionebc.actorstoolkit.ui.theme.ActorsToolkitTheme
import mx.visionebc.actorstoolkit.updater.AppUpdater
import mx.visionebc.actorstoolkit.updater.UpdateChecker
import mx.visionebc.actorstoolkit.updater.UpdateInfo

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settingsDataStore = remember { SettingsDataStore(applicationContext) }
            val themeMode by settingsDataStore.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT, ThemeMode.IOS -> false
                ThemeMode.DARK, ThemeMode.MODERN -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                else -> true // color themes are all dark-based
            }
            val themeStyle = when (themeMode) {
                ThemeMode.BLUE, ThemeMode.DEEP_BLUE, ThemeMode.PINK_VIOLET, ThemeMode.GREEN, ThemeMode.YELLOW,
                ThemeMode.IOS, ThemeMode.MODERN -> themeMode.name
                else -> ""
            }
            ActorsToolkitTheme(darkTheme = darkTheme, themeStyle = themeStyle) {
                MainApp(settingsDataStore = settingsDataStore, onRestartApp = {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                    finish()
                })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    settingsDataStore: SettingsDataStore,
    onRestartApp: () -> Unit
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val info = UpdateChecker.checkForUpdate(BuildConfig.VERSION_CODE)
            if (info != null) {
                updateInfo = info
                showUpdateDialog = true
            }
        } catch (_: Exception) {}
    }

    AppNavHost(
        navController = navController,
        settingsDataStore = settingsDataStore,
        onRestartApp = onRestartApp
    )

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
