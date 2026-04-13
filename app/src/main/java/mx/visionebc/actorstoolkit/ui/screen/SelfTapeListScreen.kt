package mx.visionebc.actorstoolkit.ui.screen

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mx.visionebc.actorstoolkit.data.entity.Audition
import mx.visionebc.actorstoolkit.data.entity.SelfTape
import mx.visionebc.actorstoolkit.ui.selftape.SelfTapeViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelfTapeListScreen(
    viewModel: SelfTapeViewModel,
    onRecordClick: () -> Unit,
    onTapeClick: (Long) -> Unit
) {
    val tapes by viewModel.allTapes.collectAsState()
    val auditions by viewModel.auditions.collectAsState()
    val auditionMap = remember(auditions) { auditions.associateBy { it.id } }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            "Self-Tapes",
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Text(
                            if (tapes.isEmpty()) "Record your audition tapes"
                            else "${tapes.size} tape${if (tapes.size != 1) "s" else ""}",
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
                onClick = onRecordClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(64.dp)
            ) {
                Icon(Icons.Default.Videocam, "Record", modifier = Modifier.size(28.dp))
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (tapes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
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
                            Icons.Default.Videocam,
                            null,
                            Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(32.dp))
                    Text(
                        "No self-tapes yet",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Record audition tapes with\ncountdown timer and script display",
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(32.dp))
                    FilledTonalButton(
                        onClick = onRecordClick,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.Videocam, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Record Self-Tape")
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 80.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tapes, key = { it.id }) { tape ->
                    SelfTapeCard(
                        tape = tape,
                        audition = tape.auditionId?.let { auditionMap[it] },
                        onClick = { onTapeClick(tape.id) },
                        onDelete = { viewModel.deleteTape(tape) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SelfTapeCard(
    tape: SelfTape,
    audition: Audition?,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy  HH:mm", Locale.getDefault()) }

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
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val thumbBitmap = remember(tape.thumbnailPath) {
                    if (tape.thumbnailPath.isNotBlank() && File(tape.thumbnailPath).exists()) {
                        try { BitmapFactory.decodeFile(tape.thumbnailPath)?.asImageBitmap() }
                        catch (_: Exception) { null }
                    } else null
                }
                if (thumbBitmap != null) {
                    Image(
                        bitmap = thumbBitmap,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.VideoFile,
                        null,
                        Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Duration overlay
                if (tape.durationMs > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .background(
                                MaterialTheme.colorScheme.scrim.copy(alpha = 0.7f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            formatDuration(tape.durationMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    tape.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    dateFormat.format(Date(tape.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (audition != null) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.TheaterComedy,
                            null,
                            Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            audition.projectName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (tape.trimStartMs > 0 || tape.trimEndMs > 0) {
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ContentCut,
                            null,
                            Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Trimmed",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Play icon
            Icon(
                Icons.Default.PlayCircle,
                null,
                Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )

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
            title = { Text("Delete Self-Tape", style = MaterialTheme.typography.titleLarge) },
            text = { Text("Delete \"${tape.title}\"? The video file will also be removed.") },
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

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) "${minutes}:${seconds.toString().padStart(2, '0')}"
    else "0:${seconds.toString().padStart(2, '0')}"
}
