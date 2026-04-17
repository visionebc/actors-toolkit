package mx.visionebc.actorstoolkit.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mx.visionebc.actorstoolkit.data.entity.Character
import mx.visionebc.actorstoolkit.ui.character.CharacterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterAddEditScreen(
    characterId: Long?,
    castingId: Long,
    viewModel: CharacterViewModel,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val isEditing = characterId != null && characterId != 0L
    val existingCharacter by viewModel.selectedCharacter.collectAsState()

    var name by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(characterId) {
        if (isEditing) viewModel.loadCharacter(characterId!!)
    }

    LaunchedEffect(existingCharacter) {
        if (isEditing && existingCharacter != null && !loaded) {
            name = existingCharacter!!.name
            notes = existingCharacter!!.notes
            loaded = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Character" else "New Character") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Character Name") },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true
            )
            OutlinedTextField(
                value = notes, onValueChange = { notes = it },
                label = { Text("Notes") },
                leadingIcon = { Icon(Icons.Default.Notes, null) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp), shape = RoundedCornerShape(12.dp), maxLines = 6
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    val character = if (isEditing && existingCharacter != null) {
                        existingCharacter!!.copy(name = name.trim(), notes = notes.trim(), updatedAt = System.currentTimeMillis())
                    } else {
                        Character(castingId = castingId, name = name.trim(), notes = notes.trim())
                    }
                    viewModel.saveCharacter(character) { onSaved() }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), enabled = name.isNotBlank()
            ) {
                Icon(if (isEditing) Icons.Default.Save else Icons.Default.Add, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (isEditing) "Save Changes" else "Create Character", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
