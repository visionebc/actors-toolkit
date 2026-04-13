package mx.visionebc.actorstoolkit.ui.screen

import androidx.media3.common.util.UnstableApi

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import mx.visionebc.actorstoolkit.data.entity.Audition
import mx.visionebc.actorstoolkit.ui.selftape.SelfTapeViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelfTapePlayerScreen(
    tapeId: Long,
    viewModel: SelfTapeViewModel,
    auditionMap: Map<Long, Audition>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val tape by viewModel.selectedTape.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditTitle by remember { mutableStateOf(false) }
    var editedTitle by remember { mutableStateOf("") }
    var editedNotes by remember { mutableStateOf("") }

    // Trim state
    var showTrimControls by remember { mutableStateOf(false) }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }

    // Player state
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    LaunchedEffect(tapeId) {
        viewModel.loadTape(tapeId)
    }

    val currentTape = tape ?: run {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // Initialize trim state from tape
    LaunchedEffect(currentTape) {
        editedTitle = currentTape.title
        editedNotes = currentTape.notes
        if (currentTape.durationMs > 0) {
            trimStart = currentTape.trimStartMs.toFloat() / currentTape.durationMs
            trimEnd = if (currentTape.trimEndMs > 0) currentTape.trimEndMs.toFloat() / currentTape.durationMs else 1f
        }
    }

    val exoPlayer = remember(currentTape.videoPath) {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(Uri.fromFile(File(currentTape.videoPath)))
            setMediaItem(mediaItem)
            prepare()
        }
    }

    // Track player state
    LaunchedEffect(exoPlayer) {
        while (true) {
            isPlaying = exoPlayer.isPlaying
            currentPosition = exoPlayer.currentPosition
            if (exoPlayer.duration > 0) duration = exoPlayer.duration
            delay(200)
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy  HH:mm", Locale.getDefault()) }
    val linkedAudition = currentTape.auditionId?.let { auditionMap[it] }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentTape.title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = {
                        exoPlayer.release()
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditTitle = true }) {
                        Icon(Icons.Default.Edit, "Edit")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Outlined.Delete, "Delete")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Video player
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(0.dp))
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Play/pause overlay
                IconButton(
                    onClick = {
                        if (exoPlayer.isPlaying) exoPlayer.pause()
                        else exoPlayer.play()
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(64.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.4f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        "Play/Pause",
                        Modifier.size(40.dp)
                    )
                }
            }

            // Progress bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Slider(
                    value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                    onValueChange = { fraction ->
                        exoPlayer.seekTo((fraction * duration).toLong())
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        formatTimestamp(currentPosition),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        formatTimestamp(duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Playback controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { exoPlayer.seekTo(maxOf(0, exoPlayer.currentPosition - 10_000)) }) {
                    Icon(Icons.Default.Replay10, "Back 10s")
                }
                Spacer(Modifier.width(16.dp))
                FilledIconButton(
                    onClick = {
                        if (exoPlayer.isPlaying) exoPlayer.pause()
                        else exoPlayer.play()
                    },
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        "Play/Pause",
                        Modifier.size(32.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                IconButton(onClick = { exoPlayer.seekTo(minOf(duration, exoPlayer.currentPosition + 10_000)) }) {
                    Icon(Icons.Default.Forward10, "Forward 10s")
                }
            }

            Spacer(Modifier.height(16.dp))

            // Trim controls
            Surface(
                onClick = { showTrimControls = !showTrimControls },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ContentCut,
                        null,
                        Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Trim Controls",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        if (showTrimControls) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (showTrimControls && duration > 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        "Start: ${formatTimestamp((trimStart * duration).toLong())}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = trimStart,
                        onValueChange = { trimStart = it.coerceAtMost(trimEnd - 0.01f) },
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.tertiary,
                            activeTrackColor = MaterialTheme.colorScheme.tertiary
                        )
                    )

                    Text(
                        "End: ${formatTimestamp((trimEnd * duration).toLong())}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = trimEnd,
                        onValueChange = { trimEnd = it.coerceAtLeast(trimStart + 0.01f) },
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.tertiary,
                            activeTrackColor = MaterialTheme.colorScheme.tertiary
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = {
                                exoPlayer.seekTo((trimStart * duration).toLong())
                                exoPlayer.play()
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Preview Trim")
                        }

                        Button(
                            onClick = {
                                val startMs = (trimStart * duration).toLong()
                                val endMs = (trimEnd * duration).toLong()
                                viewModel.updateTrimPoints(tapeId, startMs, endMs)
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Save, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Save Trim")
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Info section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                if (linkedAudition != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.TheaterComedy,
                                null,
                                Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "${linkedAudition.projectName} — ${linkedAudition.roleName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Text(
                    dateFormat.format(Date(currentTape.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (currentTape.notes.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        currentTape.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Edit dialog
    if (showEditTitle) {
        AlertDialog(
            onDismissRequest = { showEditTitle = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Edit Self-Tape") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editedTitle,
                        onValueChange = { editedTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editedNotes,
                        onValueChange = { editedNotes = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateTape(currentTape.copy(title = editedTitle, notes = editedNotes))
                        showEditTitle = false
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditTitle = false }) { Text("Cancel") }
            }
        )
    }

    // Delete dialog
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
            title = { Text("Delete Self-Tape") },
            text = { Text("Delete \"${currentTape.title}\"? The video file will also be removed.") },
            confirmButton = {
                Button(
                    onClick = {
                        exoPlayer.release()
                        viewModel.deleteTape(currentTape) { onBack() }
                        showDeleteDialog = false
                    },
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

private fun formatTimestamp(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}
