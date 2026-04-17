package mx.visionebc.actorstoolkit.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mx.visionebc.actorstoolkit.data.entity.ScriptInfo
import mx.visionebc.actorstoolkit.ui.character.CharacterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterDetailScreen(
    characterId: Long,
    viewModel: CharacterViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onScriptClick: (Long) -> Unit,
    onAddScript: () -> Unit,
    onPickScript: () -> Unit
) {
    val character by viewModel.selectedCharacter.collectAsState()
    val scripts by viewModel.scripts.collectAsState()

    LaunchedEffect(characterId) {
        viewModel.loadCharacter(characterId)
        viewModel.setCharacterId(characterId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(character?.name ?: "Character", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = { IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        val ch = character
        if (ch == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (ch.notes.isNotBlank()) {
                item {
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Row(Modifier.padding(12.dp)) {
                            Icon(Icons.Default.Notes, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(8.dp))
                            Text(ch.notes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Scripts section
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Scripts", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilledTonalIconButton(onClick = onPickScript) { Icon(Icons.Default.LibraryAdd, "Pick from Bank") }
                        FilledTonalIconButton(onClick = onAddScript) { Icon(Icons.Default.NoteAdd, "Import New") }
                    }
                }
            }

            if (scripts.isEmpty()) {
                item {
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Description, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Text("No scripts assigned", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = onPickScript, shape = RoundedCornerShape(12.dp)) {
                                    Icon(Icons.Default.LibraryAdd, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("From Bank")
                                }
                                FilledTonalButton(onClick = onAddScript, shape = RoundedCornerShape(12.dp)) {
                                    Icon(Icons.Default.NoteAdd, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Import New")
                                }
                            }
                        }
                    }
                }
            } else {
                items(scripts, key = { it.id }) { script ->
                    ScriptMiniCard(
                        script = script,
                        onClick = { onScriptClick(script.id) },
                        onUnlink = { viewModel.unlinkScript(characterId, script.id) }
                    )
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun ScriptMiniCard(script: ScriptInfo, onClick: () -> Unit, onUnlink: () -> Unit) {
    var showUnlinkDialog by remember { mutableStateOf(false) }

    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Description, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(script.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(script.fileType.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (script.practiceCount > 0) {
                Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Text("${script.practiceCount}x", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(Modifier.width(4.dp))
            }
            IconButton(onClick = { showUnlinkDialog = true }, Modifier.size(32.dp)) {
                Icon(Icons.Default.LinkOff, "Unlink", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
        }
    }

    if (showUnlinkDialog) {
        AlertDialog(
            onDismissRequest = { showUnlinkDialog = false },
            title = { Text("Unlink Script") },
            text = { Text("Remove \"${script.title}\" from this character? The script stays in the bank.") },
            confirmButton = { TextButton(onClick = { onUnlink(); showUnlinkDialog = false }) { Text("Unlink") } },
            dismissButton = { TextButton(onClick = { showUnlinkDialog = false }) { Text("Cancel") } }
        )
    }
}
