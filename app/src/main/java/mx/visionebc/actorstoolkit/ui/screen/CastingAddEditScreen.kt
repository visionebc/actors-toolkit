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
import mx.visionebc.actorstoolkit.data.entity.Casting
import mx.visionebc.actorstoolkit.ui.casting.CastingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CastingAddEditScreen(
    castingId: Long?,
    projectId: Long,
    viewModel: CastingViewModel,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val isEditing = castingId != null && castingId != 0L
    val existingCasting by viewModel.selectedCasting.collectAsState()

    var name by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(castingId) {
        if (isEditing) viewModel.loadCasting(castingId!!)
    }

    LaunchedEffect(existingCasting) {
        if (isEditing && existingCasting != null && !loaded) {
            val c = existingCasting!!
            name = c.name
            notes = c.notes
            loaded = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Casting" else "New Casting") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Casting Name") },
                leadingIcon = { Icon(Icons.Default.Groups, null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                leadingIcon = { Icon(Icons.Default.Notes, null) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                shape = RoundedCornerShape(12.dp),
                maxLines = 6
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    val casting = if (isEditing && existingCasting != null) {
                        existingCasting!!.copy(
                            name = name.trim(),
                            notes = notes.trim(),
                            updatedAt = System.currentTimeMillis()
                        )
                    } else {
                        Casting(
                            projectId = projectId,
                            name = name.trim(),
                            notes = notes.trim()
                        )
                    }
                    viewModel.saveCasting(casting) { onSaved() }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = name.isNotBlank()
            ) {
                Icon(if (isEditing) Icons.Default.Save else Icons.Default.Add, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (isEditing) "Save Changes" else "Create Casting", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
