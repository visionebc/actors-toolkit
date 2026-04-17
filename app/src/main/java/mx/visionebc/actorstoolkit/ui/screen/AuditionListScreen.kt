package mx.visionebc.actorstoolkit.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mx.visionebc.actorstoolkit.data.entity.Audition
import mx.visionebc.actorstoolkit.ui.audition.AuditionViewModel
import java.text.SimpleDateFormat
import java.util.*

private val STATUS_LIST = listOf("SUBMITTED", "CALLED", "CALLBACK", "BOOKED", "PASSED")

private fun statusColor(status: String): Color = when (status) {
    "SUBMITTED" -> Color(0xFF74B9FF)
    "CALLED" -> Color(0xFFFFE66D)
    "CALLBACK" -> Color(0xFFFF9F43)
    "BOOKED" -> Color(0xFF00D4FF)
    "PASSED" -> Color(0xFFFF6B6B)
    else -> Color(0xFFB0ADCC)
}

private fun statusIcon(status: String) = when (status) {
    "SUBMITTED" -> Icons.Default.Send
    "CALLED" -> Icons.Default.Phone
    "CALLBACK" -> Icons.Default.Replay
    "BOOKED" -> Icons.Default.Star
    "PASSED" -> Icons.Default.Close
    else -> Icons.Default.HelpOutline
}

private fun statusLabel(status: String) = when (status) {
    "SUBMITTED" -> "Submitted"
    "CALLED" -> "Called"
    "CALLBACK" -> "Callback"
    "BOOKED" -> "Booked"
    "PASSED" -> "Passed"
    else -> status
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditionListScreen(
    viewModel: AuditionViewModel,
    onAddClick: () -> Unit,
    onAuditionClick: (Long) -> Unit
) {
    val auditions by viewModel.filteredAuditions.collectAsState()
    val allAuditions by viewModel.auditions.collectAsState()
    val filterStatus by viewModel.filterStatus.collectAsState()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            "Auditions",
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Text(
                            if (allAuditions.isEmpty()) "Track your auditions"
                            else "${allAuditions.size} audition${if (allAuditions.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(64.dp)
            ) {
                Icon(Icons.Default.Add, "Add Audition", modifier = Modifier.size(28.dp))
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Status filter chips
            if (allAuditions.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = filterStatus == null,
                            onClick = { viewModel.setFilter(null) },
                            label = { Text("All") },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                    items(STATUS_LIST) { status ->
                        val count = allAuditions.count { it.status == status }
                        if (count > 0) {
                            FilterChip(
                                selected = filterStatus == status,
                                onClick = { viewModel.setFilter(if (filterStatus == status) null else status) },
                                label = { Text("${statusLabel(status)} ($count)") },
                                leadingIcon = {
                                    Icon(
                                        statusIcon(status),
                                        null,
                                        modifier = Modifier.size(16.dp),
                                        tint = statusColor(status)
                                    )
                                },
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = statusColor(status).copy(alpha = 0.15f),
                                    selectedLabelColor = statusColor(status)
                                )
                            )
                        }
                    }
                }
            }

            if (auditions.isEmpty() && allAuditions.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(48.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            MaterialTheme.colorScheme.background
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.TheaterComedy,
                                null,
                                Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(32.dp))
                        Text(
                            "No auditions yet",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Start tracking your auditions\nand never miss an opportunity",
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(32.dp))
                        FilledTonalButton(
                            onClick = onAddClick,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Add Audition")
                        }
                    }
                }
            } else if (auditions.isEmpty()) {
                // Filter has no results
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No ${statusLabel(filterStatus ?: "").lowercase()} auditions",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(auditions, key = { it.id }) { audition ->
                        AuditionCard(
                            audition = audition,
                            onClick = { onAuditionClick(audition.id) },
                            onDelete = { viewModel.deleteAudition(audition) },
                            onStatusChange = { newStatus -> viewModel.updateStatus(audition, newStatus) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
fun AuditionCard(
    audition: Audition,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onStatusChange: (String) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showStatusMenu by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault()) }
    val color = statusColor(audition.status)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    statusIcon(audition.status),
                    null,
                    Modifier.size(24.dp),
                    tint = color
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    audition.projectName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (audition.roleName.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        audition.roleName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status badge
                    Box {
                        Surface(
                            onClick = { showStatusMenu = true },
                            shape = RoundedCornerShape(6.dp),
                            color = color.copy(alpha = 0.15f)
                        ) {
                            Text(
                                statusLabel(audition.status),
                                style = MaterialTheme.typography.labelSmall,
                                color = color,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showStatusMenu,
                            onDismissRequest = { showStatusMenu = false }
                        ) {
                            STATUS_LIST.forEach { status ->
                                DropdownMenuItem(
                                    text = { Text(statusLabel(status)) },
                                    onClick = {
                                        onStatusChange(status)
                                        showStatusMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            statusIcon(status),
                                            null,
                                            tint = statusColor(status),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    trailingIcon = {
                                        if (audition.status == status) {
                                            Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Date
                    if (audition.auditionDate != null) {
                        Text(
                            dateFormat.format(Date(audition.auditionDate)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (audition.castingDirector.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Person,
                            null,
                            Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            audition.castingDirector,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            IconButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            shape = RoundedCornerShape(24.dp),
            icon = {
                Icon(
                    Icons.Default.Delete,
                    null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Delete Audition", style = MaterialTheme.typography.titleLarge) },
            text = { Text("Delete \"${audition.projectName}\"? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { onDelete(); showDeleteDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Delete") }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Cancel") }
            }
        )
    }
}
