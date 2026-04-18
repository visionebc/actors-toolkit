package mx.visionebc.actorstoolkit.ui.screen

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mx.visionebc.actorstoolkit.R
import kotlinx.coroutines.launch
import mx.visionebc.actorstoolkit.BuildConfig
import mx.visionebc.actorstoolkit.data.preferences.ThemeMode
import mx.visionebc.actorstoolkit.ui.settings.SettingsViewModel
import mx.visionebc.actorstoolkit.util.DatabaseBackup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onRestartApp: () -> Unit,
    projects: List<mx.visionebc.actorstoolkit.data.entity.Project> = emptyList(),
    onOpenManual: () -> Unit = {}
) {
    val context = LocalContext.current
    val themeMode by viewModel.themeMode.collectAsState()
    var showImportConfirm by remember { mutableStateOf(false) }
    var showSelectProjectsDialog by remember { mutableStateOf(false) }
    var showBackupOptionsDialog by remember { mutableStateOf(false) }
    var backupAllProjects by remember { mutableStateOf(true) }
    var selectedProjectIds by remember { mutableStateOf(setOf<Long>()) }
    val scope = rememberCoroutineScope()

    // Full backup restore
    val fullImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val success = DatabaseBackup.importFullBackup(context, uri)
            if (success) {
                Toast.makeText(context, "Backup restored. Restarting...", Toast.LENGTH_SHORT).show()
                onRestartApp()
            } else {
                Toast.makeText(context, "Import failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Selective JSON import
    val selectiveImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val count = DatabaseBackup.importProjectsFromJson(context, uri)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    Toast.makeText(context, if (count > 0) "$count project(s) imported" else "Import failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // User manual entry (placed above Appearance)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth().clickable { onOpenManual() }
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MenuBook, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "User Manual",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "How to use Actors Toolkit — every feature explained",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                        )
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            // Theme section (collapsable)
            var appearanceExpanded by remember { mutableStateOf(false) }
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column {
                    Surface(onClick = { appearanceExpanded = !appearanceExpanded }, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Palette, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Icon(if (appearanceExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, "Toggle", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            androidx.compose.animation.AnimatedVisibility(visible = appearanceExpanded) {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(4.dp)) {
                        data class ThemeOption(val mode: ThemeMode, val label: String, val desc: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val previewColor: Long?)
                        val options = listOf(
                            ThemeOption(ThemeMode.SYSTEM, "System", "Follow device settings", Icons.Default.Settings, null),
                            ThemeOption(ThemeMode.LIGHT, "Light", "Light theme", Icons.Default.LightMode, null),
                            ThemeOption(ThemeMode.DARK, "Dark", "Default dark purple", Icons.Default.DarkMode, 0xFF7B4DFF),
                            ThemeOption(ThemeMode.BLUE, "Ocean Blue", "Blue & cyan tones", Icons.Default.Water, 0xFF64B5F6),
                            ThemeOption(ThemeMode.DEEP_BLUE, "Deep Blue", "Strong vivid blue", Icons.Default.Water, 0xFF2962FF),
                            ThemeOption(ThemeMode.PINK_VIOLET, "Pink Violet", "Pink & violet vibes", Icons.Default.Favorite, 0xFFE91E63),
                            ThemeOption(ThemeMode.GREEN, "Forest Green", "Green & teal nature", Icons.Default.Park, 0xFF66BB6A),
                            ThemeOption(ThemeMode.YELLOW, "Golden Yellow", "Warm yellow & amber", Icons.Default.WbSunny, 0xFFFFD54F),
                            ThemeOption(ThemeMode.IOS, "iOS", "Clean iOS-inspired light", Icons.Default.PhoneIphone, 0xFF007AFF),
                            ThemeOption(ThemeMode.MODERN, "Modern", "Vivid neon on deep black", Icons.Default.AutoAwesome, 0xFF8B5CF6)
                        )
                        options.forEach { opt ->
                            Row(
                                Modifier.fillMaxWidth().clickable { viewModel.setThemeMode(opt.mode) }.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = themeMode == opt.mode, onClick = { viewModel.setThemeMode(opt.mode) })
                                Spacer(Modifier.width(8.dp))
                                if (opt.previewColor != null) {
                                    Box(Modifier.size(20.dp).clip(RoundedCornerShape(4.dp)).background(androidx.compose.ui.graphics.Color(opt.previewColor)))
                                } else {
                                    Icon(opt.icon, null, Modifier.size(20.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(opt.label, style = MaterialTheme.typography.bodyLarge)
                                    Text(opt.desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            // Practice preferences
            val playOwn by viewModel.playOwnRecordings.collectAsState()
            Text("Practice", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp).clickable { viewModel.setPlayOwnRecordings(!playOwn) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.RecordVoiceOver, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Hear my own recordings", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            if (playOwn) "Own-role recordings play during practice"
                            else "Own-role lines stay silent so you can say them live",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = playOwn, onCheckedChange = { viewModel.setPlayOwnRecordings(it) })
                }
            }

            // Backup section
            Text("Backup & Restore", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Export
                    Text("Export", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Button(
                        onClick = { backupAllProjects = true; showBackupOptionsDialog = true },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                        Text("Backup All Projects")
                    }
                    OutlinedButton(
                        onClick = { selectedProjectIds = emptySet(); showSelectProjectsDialog = true },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        enabled = projects.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Checklist, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                        Text("Backup Selected Projects")
                    }

                    HorizontalDivider(Modifier.padding(vertical = 4.dp))

                    // Import
                    Text("Import", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    OutlinedButton(
                        onClick = { showImportConfirm = true },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CloudDownload, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                        Text("Restore Full Backup (.zip or .db)")
                    }
                    OutlinedButton(
                        onClick = { selectiveImportLauncher.launch(arrayOf("application/json", "*/*")) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.FileOpen, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                        Text("Import Projects (.json)")
                    }
                    Text("Full restore replaces all data. JSON import adds projects without removing existing ones.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // About section
            Text("About", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.logo),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Actors Toolkit", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                "Version ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text("by visionebc", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Text(
                        "Actors Toolkit is your rehearsal companion — built by and for working actors. It keeps every project, casting, audition, script and line rehearsal in one place so you can focus on the craft instead of the paperwork.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text("What you can do", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        AboutBullet(Icons.Default.Folder, "Organize projects with dates, locations, links, team and contacts")
                        AboutBullet(Icons.Default.Groups, "Plan castings and characters — link them to scripts for practice")
                        AboutBullet(Icons.Default.TheaterComedy, "Track auditions with date, time, status and notes")
                        AboutBullet(Icons.Default.Description, "Import PDF / TXT / FDX scripts — the app extracts characters and lines")
                        AboutBullet(Icons.Default.PlayCircle, "Rehearse with TTS reading other parts; record your own lines")
                        AboutBullet(Icons.Default.Send, "Send your recorded lines to a scene partner")
                        AboutBullet(Icons.Default.CloudSync, "Share projects with fine-grained options; import merges smartly")
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Text("Links", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)

                    val websiteUrl = "https://www.actorstoolkit.visionebc.com"
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth().clickable {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(websiteUrl)))
                            } catch (_: Exception) {
                                Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Public, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Project website", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("www.actorstoolkit.visionebc.com", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                            }
                            Icon(Icons.Default.OpenInNew, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }

                    Text(
                        "© visionebc — Made with coffee, caffeine and a lot of line runs.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // ── Select projects dialog (step 1 for selective backup) ──
    if (showSelectProjectsDialog) {
        AlertDialog(
            onDismissRequest = { showSelectProjectsDialog = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Select Projects to Backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (projects.isEmpty()) {
                        Text("No projects available", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Row(Modifier.fillMaxWidth().clickable {
                            selectedProjectIds = if (selectedProjectIds.size == projects.size) emptySet() else projects.map { it.id }.toSet()
                        }, verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = selectedProjectIds.size == projects.size, onCheckedChange = {
                                selectedProjectIds = if (it) projects.map { p -> p.id }.toSet() else emptySet()
                            })
                            Spacer(Modifier.width(8.dp))
                            Text("Select All", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider()
                        projects.forEach { project ->
                            Row(Modifier.fillMaxWidth().clickable {
                                selectedProjectIds = if (project.id in selectedProjectIds) selectedProjectIds - project.id else selectedProjectIds + project.id
                            }, verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = project.id in selectedProjectIds, onCheckedChange = {
                                    selectedProjectIds = if (it) selectedProjectIds + project.id else selectedProjectIds - project.id
                                })
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(project.name, style = MaterialTheme.typography.bodyMedium)
                                    if (project.director.isNotBlank()) Text(project.director, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSelectProjectsDialog = false
                        backupAllProjects = false
                        showBackupOptionsDialog = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    enabled = selectedProjectIds.isNotEmpty()
                ) { Text("Next") }
            },
            dismissButton = { OutlinedButton(onClick = { showSelectProjectsDialog = false }, shape = RoundedCornerShape(12.dp)) { Text("Cancel") } }
        )
    }

    // ── Backup options dialog (same for all and selected) ──
    if (showBackupOptionsDialog) {
        var includeRecordings by remember { mutableStateOf(true) }
        var includeDocuments by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showBackupOptionsDialog = false },
            shape = RoundedCornerShape(24.dp),
            icon = { Icon(Icons.Default.CloudUpload, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary) },
            title = { Text(if (backupAllProjects) "Backup All Projects" else "Backup ${selectedProjectIds.size} Project(s)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Choose what to include:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))

                    // Data (always included)
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = true, onCheckedChange = null, enabled = false)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Data", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text("Projects, scripts, auditions, contacts, settings", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Voice Recordings
                    Row(Modifier.fillMaxWidth().clickable { includeRecordings = !includeRecordings }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = includeRecordings, onCheckedChange = { includeRecordings = it })
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Voice Recordings", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text("Recorded audio for practice lines", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Documents
                    Row(Modifier.fillMaxWidth().clickable { includeDocuments = !includeDocuments }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = includeDocuments, onCheckedChange = { includeDocuments = it })
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Documents & Images", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text("Attachments, card images, audition photos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        when {
                            !includeRecordings && !includeDocuments -> "Smallest backup - data only"
                            includeRecordings && includeDocuments -> "Full backup - largest file size"
                            else -> "Partial backup"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBackupOptionsDialog = false
                        val doRecordings = includeRecordings
                        val doDocs = includeDocuments
                        val isAll = backupAllProjects
                        val ids = selectedProjectIds.toList()
                        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            val uri = if (isAll) {
                                DatabaseBackup.exportBackup(context, doRecordings, doDocs)
                            } else {
                                DatabaseBackup.exportBackup(context, doRecordings, doDocs, ids)
                            }
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                if (uri != null) {
                                    DatabaseBackup.shareFile(context, uri)
                                } else {
                                    Toast.makeText(context, "Backup failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text("Create Backup")
                }
            },
            dismissButton = { OutlinedButton(onClick = { showBackupOptionsDialog = false }, shape = RoundedCornerShape(12.dp)) { Text("Cancel") } }
        )
    }

    // ── Full restore confirmation ──
    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false },
            shape = RoundedCornerShape(24.dp),
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp)) },
            title = { Text("Restore Full Backup") },
            text = { Text("This will replace ALL current data (projects, scripts, recordings) with the backup. The app will restart. Continue?") },
            confirmButton = {
                Button(
                    onClick = { showImportConfirm = false; fullImportLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Restore") }
            },
            dismissButton = { OutlinedButton(onClick = { showImportConfirm = false }, shape = RoundedCornerShape(12.dp)) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AboutBullet(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            icon, null,
            Modifier.size(18.dp).padding(top = 2.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
