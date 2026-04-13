package mx.visionebc.actorstoolkit.ui.screen

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mx.visionebc.actorstoolkit.ui.PracticeViewModel
import mx.visionebc.actorstoolkit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptDetailScreen(
    scriptId: Long,
    viewModel: PracticeViewModel,
    onPractice: () -> Unit,
    onBlocking: () -> Unit,
    onBack: () -> Unit
) {
    Log.d("ScriptDetail", "Composing ScriptDetailScreen for scriptId=$scriptId")
    val state by viewModel.state.collectAsState()

    LaunchedEffect(scriptId) {
        Log.d("ScriptDetail", "LaunchedEffect firing loadScript($scriptId)")
        viewModel.loadScript(scriptId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.scriptTitle.ifEmpty { "Script" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Loading script...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            state.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.errorContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "Something went wrong",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            state.error ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.loadScript(scriptId) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Retry")
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ── Overview card ────────────────────────
                    item(key = "overview") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    "Overview",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    StatItem(
                                        icon = Icons.Default.List,
                                        value = "${state.lines.size}",
                                        label = "Lines"
                                    )
                                    StatItem(
                                        icon = Icons.Default.Person,
                                        value = "${state.characters.size}",
                                        label = "Characters"
                                    )
                                    val role = state.userRole
                                    if (role != null) {
                                        StatItem(
                                            icon = Icons.Default.Star,
                                            value = role.name,
                                            label = "Your Role"
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── Action Buttons ───────────────────────
                    if (!state.isLoading && state.lines.isNotEmpty() && state.error == null) {
                        item(key = "actions") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Practice button
                                Button(
                                    onClick = onPractice,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    contentPadding = PaddingValues(vertical = 14.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, null, Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Practice", fontWeight = FontWeight.SemiBold)
                                }

                                // Stage Blocking button
                                OutlinedButton(
                                    onClick = onBlocking,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Teal
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.5.dp, Teal.copy(alpha = 0.5f)
                                    ),
                                    contentPadding = PaddingValues(vertical = 14.dp)
                                ) {
                                    Icon(Icons.Default.GridOn, null, Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Blocking", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    // ── Role selector ────────────────────────
                    if (state.characters.isNotEmpty()) {
                        item(key = "roles") {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text(
                                        if (state.userRole != null) "Your Role" else "Select Your Role",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    state.characters.forEachIndexed { idx, character ->
                                        val isSelected = character.name == state.userRole?.name
                                        val chipColors = if (isSelected) {
                                            FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                selectedLabelColor = MaterialTheme.colorScheme.primary
                                            )
                                        } else {
                                            FilterChipDefaults.filterChipColors()
                                        }
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { viewModel.selectRole(character) },
                                            label = {
                                                Text(
                                                    "${character.name} (${character.lineCount} lines)",
                                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                                )
                                            },
                                            leadingIcon = if (isSelected) {
                                                {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        null,
                                                        Modifier.size(16.dp),
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            } else null,
                                            shape = RoundedCornerShape(10.dp),
                                            colors = chipColors,
                                            modifier = Modifier.padding(end = 8.dp, bottom = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── No lines message ─────────────────────
                    if (state.lines.isEmpty()) {
                        item(key = "empty") {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        "No dialogue lines detected",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "The parser could not find structured dialogue. Try a script with character names in ALL CAPS followed by dialogue.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // ── Lines preview ────────────────────────
                    if (state.lines.isNotEmpty()) {
                        item(key = "preview_header") {
                            Text(
                                "Lines Preview",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }

                        val previewLines = state.lines.take(50)
                        itemsIndexed(previewLines, key = { index, line -> "line_${index}_${line.id}" }) { index, line ->
                            val isUserLine = line.character == state.userRole?.name
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isUserLine)
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    else
                                        MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Character initial badge
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isUserLine) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                    else MaterialTheme.colorScheme.surfaceVariant
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                line.character.take(1),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isUserLine) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            line.character,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isUserLine) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (isUserLine) {
                                            Spacer(Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    "YOU",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    val direction = line.stageDirection
                                    if (!direction.isNullOrBlank()) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "($direction)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                        )
                                    }
                                    if (line.dialogue.isNotBlank()) {
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            line.dialogue,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }

                        if (state.lines.size > 50) {
                            item(key = "more") {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        "... and ${state.lines.size - 50} more lines",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        }
                    }

                    item(key = "spacer") { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
        )
    }
}
