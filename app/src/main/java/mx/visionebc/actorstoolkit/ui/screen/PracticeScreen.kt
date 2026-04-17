package mx.visionebc.actorstoolkit.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import mx.visionebc.actorstoolkit.data.entity.BlockingMark
import mx.visionebc.actorstoolkit.data.entity.MovementPath
import mx.visionebc.actorstoolkit.data.entity.ScriptLine
import mx.visionebc.actorstoolkit.data.entity.StageItem
import mx.visionebc.actorstoolkit.ui.PracticeMode
import mx.visionebc.actorstoolkit.ui.PracticeViewModel
import mx.visionebc.actorstoolkit.ui.theme.*
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PracticeScreen(
    scriptId: Long,
    viewModel: PracticeViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Respect the "Play own recordings" preference during practice
    val settingsDs = remember { mx.visionebc.actorstoolkit.data.preferences.SettingsDataStore(context.applicationContext) }
    val playOwn by settingsDs.playOwnRecordings.collectAsState(initial = false)
    LaunchedEffect(playOwn) { viewModel.setPlayOwnRecordings(playOwn) }

    // Permission handling
    var hasRecordPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasRecordPermission = granted
    }

    // Pending record action after permission grant
    var pendingRecordLineIndex by remember { mutableIntStateOf(-1) }

    var showVoicesDialog by remember { mutableStateOf(false) }
    var voicePickerFor by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(hasRecordPermission, pendingRecordLineIndex) {
        if (hasRecordPermission && pendingRecordLineIndex >= 0) {
            viewModel.startRecording(pendingRecordLineIndex)
            pendingRecordLineIndex = -1
        }
    }

    val currentBlockingMarks = remember(state.currentLineIndex, state.blockingMarks, state.lines, state.characters) {
        viewModel.getCurrentBlockingMarks()
    }
    val currentMovementPaths = remember(state.currentLineIndex, state.movementPaths, state.lines) {
        viewModel.getCurrentMovementPaths()
    }
    val hasBlockingData = state.blockingMarks.isNotEmpty() || state.stageItems.isNotEmpty()

    LaunchedEffect(Unit) {
        viewModel.setAppContext(context)
        viewModel.tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                viewModel.tts?.language = Locale.US
            }
        }
    }

    LaunchedEffect(scriptId) {
        viewModel.loadScript(scriptId)
    }

    LaunchedEffect(state.currentLineIndex) {
        if (state.lines.isNotEmpty() && state.currentLineIndex in state.lines.indices) {
            scope.launch {
                listState.animateScrollToItem(state.currentLineIndex)
            }
        }
    }

    // Edit line dialog
    state.editingLine?.let { editingLine ->
        EditLineDialog(
            line = editingLine,
            onDismiss = { viewModel.closeEditLine() },
            onSaveDialogue = { newText -> viewModel.saveEditedDialogue(editingLine.id, newText) },
            onToggleIgnoredWord = { wordIdx -> viewModel.toggleIgnoredWord(editingLine.id, wordIdx) },
            onReset = { viewModel.resetLineEdits(editingLine.id) }
        )
    }

    val lineCountText = if (state.lines.isNotEmpty()) {
        val safeIndex = state.currentLineIndex.coerceIn(0, state.lines.size - 1)
        val skippedCount = state.lines.count { it.isSkipped }
        val recordingCount = state.recordings.size
        val baseText = if (skippedCount > 0) {
            "Line ${safeIndex + 1} of ${state.lines.size} ($skippedCount skipped)"
        } else {
            "Line ${safeIndex + 1} of ${state.lines.size}"
        }
        if (recordingCount > 0) "$baseText • $recordingCount recorded" else baseText
    } else {
        "No lines"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            state.scriptTitle,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            lineCountText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.stopTts()
                        viewModel.stopPlayback()
                        if (state.isRecording) viewModel.stopRecording()
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Blocking overlay toggle
                    if (hasBlockingData) {
                        FilledIconToggleButton(
                            checked = state.showBlocking,
                            onCheckedChange = { viewModel.toggleBlocking() },
                            modifier = Modifier.size(40.dp),
                            colors = IconButtonDefaults.filledIconToggleButtonColors(
                                checkedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                checkedContentColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Icon(
                                Icons.Default.Map,
                                "Stage Blocking",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    // TTS toggle
                    FilledIconToggleButton(
                        checked = state.ttsEnabled,
                        onCheckedChange = { viewModel.toggleTts() },
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.filledIconToggleButtonColors(
                            checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            checkedContentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            if (state.ttsEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            "Toggle TTS",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    // Character voices picker
                    IconButton(
                        onClick = { showVoicesDialog = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.RecordVoiceOver, "Character voices", modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(4.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (state.lines.isNotEmpty()) {
                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column {
                        // Progress bar
                        LinearProgressIndicator(
                            progress = { state.progress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp),
                            color = if (state.isAutoPlaying) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            // Mode chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PracticeMode.entries.filter { it != PracticeMode.HIDE_MY_LINES }.forEach { mode ->
                                    val isSelected = state.mode == mode
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.setMode(mode) },
                                        label = {
                                            Text(
                                                when (mode) {
                                                    PracticeMode.READ_THROUGH -> "Read"
                                                    PracticeMode.MEMORIZATION -> "Memorize"
                                                    else -> ""
                                                },
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        enabled = !state.isAutoPlaying,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            // Control buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Previous
                                FilledIconButton(
                                    onClick = { viewModel.previousLine() },
                                    enabled = state.currentLineIndex > 0 && !state.isAutoPlaying,
                                    modifier = Modifier.size(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Icon(Icons.Default.SkipPrevious, "Previous", Modifier.size(22.dp))
                                }

                                // Next
                                Button(
                                    onClick = { viewModel.nextLine() },
                                    enabled = state.currentLineIndex < state.lines.size - 1 && !state.isAutoPlaying,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(Icons.Default.NavigateNext, null, Modifier.size(20.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Next", fontWeight = FontWeight.SemiBold)
                                }

                                // Play All / Stop
                                if (state.isAutoPlaying) {
                                    Button(
                                        onClick = { viewModel.stopAutoPlay() },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Icon(Icons.Default.Stop, null, Modifier.size(20.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Stop", fontWeight = FontWeight.SemiBold)
                                    }
                                } else {
                                    FilledTonalButton(
                                        onClick = { viewModel.playAll() },
                                        enabled = state.currentLineIndex < state.lines.size - 1,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    ) {
                                        Icon(Icons.Default.PlayArrow, null, Modifier.size(20.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Play All", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
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
                            "Loading...",
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Error, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(16.dp))
                        Text(state.error ?: "", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            state.showRoleSelector -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
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
                            Icons.Default.Person,
                            null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "Select Your Role",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Which character are you playing?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(24.dp))

                    state.characters.forEach { character ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { viewModel.selectRole(character) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        character.name.take(1),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        character.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "${character.lineCount} lines",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    Icons.Default.ChevronRight,
                                    null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
            else -> {
                // Recording overlay banner
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    // Recording indicator banner
                    AnimatedVisibility(
                        visible = state.isRecording,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.error)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "Recording...",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(Modifier.weight(1f))
                                FilledTonalButton(
                                    onClick = { viewModel.stopRecording() },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Stop, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Stop", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }

                    // ── Mini stage blocking overlay ──────────
                    AnimatedVisibility(
                        visible = state.showBlocking && hasBlockingData,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        MiniStageOverlay(
                            marks = currentBlockingMarks,
                            stageItems = state.stageItems,
                            movementPaths = currentMovementPaths,
                            characters = state.characters.map { it.name },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    // ── Lines list ───────────────────────────
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(state.lines, key = { index, line -> "practice_${index}_${line.id}" }) { index, line ->
                            val isCurrentLine = index == state.currentLineIndex
                            val isUserLine = line.character == state.userRole?.name
                            val isHidden = isUserLine && state.mode != PracticeMode.READ_THROUGH &&
                                    line.id !in state.revealedLines
                            val isPastLine = index < state.currentLineIndex
                            val hasRec = state.recordings.containsKey(line.lineNumber)
                            val isPlayingThis = state.isPlayingRecording && state.playingLineIndex == index
                            val isRecordingThis = state.isRecording && state.recordingLineIndex == index

                            PracticeLineCard(
                                line = line,
                                isCurrentLine = isCurrentLine,
                                isUserLine = isUserLine,
                                isHidden = isHidden,
                                isPastLine = isPastLine,
                                mode = state.mode,
                                onReveal = { viewModel.revealLine(line.id) },
                                onTap = {
                                    if (!state.isAutoPlaying) viewModel.goToLine(index)
                                },
                                onToggleSkip = { viewModel.toggleSkipLine(line) },
                                onEdit = { viewModel.openEditLine(line) },
                                isAutoPlaying = state.isAutoPlaying,
                                hasRecording = hasRec,
                                isPlayingRecording = isPlayingThis,
                                isRecordingLine = isRecordingThis,
                                onRecord = {
                                    if (state.isRecording) {
                                        viewModel.stopRecording()
                                    } else {
                                        if (hasRecordPermission) {
                                            viewModel.startRecording(index)
                                        } else {
                                            pendingRecordLineIndex = index
                                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                },
                                onPlayRecording = { viewModel.playRecording(index) },
                                onDeleteRecording = { viewModel.deleteRecording(index) },
                                onUpdateNotes = { notes -> viewModel.updateUserNotes(line.id, notes) }
                            )
                        }

                        item(key = "end") {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        null,
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        "End of Script",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Character voices dialog ──
    if (showVoicesDialog) {
        val distinctChars = remember(state.lines) {
            state.lines.map { it.character }.filter { it.isNotBlank() }.distinct().sorted()
        }
        AnimatedDialog(
            onDismissRequest = { showVoicesDialog = false },
            title = "Character Voices",
            icon = { Icon(Icons.Default.RecordVoiceOver, null, tint = MaterialTheme.colorScheme.primary) },
            confirmButton = {
                Button(onClick = { showVoicesDialog = false }, shape = RoundedCornerShape(12.dp)) { Text("Done") }
            }
        ) {
            Text(
                "Assign a TTS voice to each character. Empty = default voice. Your own role's voice still only plays if the toggle in Settings is on.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            distinctChars.forEach { ch ->
                val current = state.characterVoices[ch].orEmpty()
                val isMe = state.userRole?.name == ch
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(ch, style = MaterialTheme.typography.bodyLarge)
                            if (isMe) {
                                Spacer(Modifier.width(8.dp))
                                Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                    Text("my role", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                        Text(
                            if (current.isBlank()) "Default voice" else current,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    TextButton(onClick = { voicePickerFor = ch }) { Text("Change") }
                    if (current.isNotBlank()) {
                        IconButton(onClick = { viewModel.setCharacterVoice(ch, null) }) {
                            Icon(Icons.Default.Close, "Reset", Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }

    // ── Voice picker sub-dialog ──
    voicePickerFor?.let { ch ->
        val voices = remember { viewModel.availableVoices() }
        val current = state.characterVoices[ch].orEmpty()
        AnimatedDialog(
            onDismissRequest = { voicePickerFor = null },
            title = "Voice for \"$ch\"",
            icon = { Icon(Icons.Default.RecordVoiceOver, null, tint = MaterialTheme.colorScheme.primary) },
            confirmButton = {
                Button(onClick = { voicePickerFor = null }, shape = RoundedCornerShape(12.dp)) { Text("Done") }
            }
        ) {
            if (voices.isEmpty()) {
                Text("No TTS voices available on this device.", color = MaterialTheme.colorScheme.error)
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                viewModel.setCharacterVoice(ch, null)
                            }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = current.isBlank(), onClick = { viewModel.setCharacterVoice(ch, null) })
                            Text("Default voice", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    items(voices.size) { idx ->
                        val (name, locale) = voices[idx]
                        Row(
                            Modifier.fillMaxWidth().clickable { viewModel.setCharacterVoice(ch, name) }.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = current == name, onClick = { viewModel.setCharacterVoice(ch, name) })
                            Column(Modifier.weight(1f)) {
                                Text(name, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                Text(locale, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { viewModel.previewVoice(name) }) {
                                Icon(Icons.Default.PlayArrow, "Preview", Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Mini Stage Overlay (read-only blocking view) ─────────
@Composable
fun MiniStageOverlay(
    marks: List<BlockingMark>,
    stageItems: List<StageItem>,
    movementPaths: List<MovementPath>,
    characters: List<String>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF080A1A))
            .border(1.dp, Color(0xFF2A2D5A), RoundedCornerShape(12.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
            val w = size.width
            val h = size.height

            // Stage floor
            drawRect(Color(0xFF0D0F25), Offset.Zero, Size(w, h))

            // Grid
            val gridColor = Color(0xFF1E2148)
            for (i in 1..7) {
                drawLine(gridColor, Offset(w * i / 8, 0f), Offset(w * i / 8, h), 0.5f)
                drawLine(gridColor, Offset(0f, h * i / 8), Offset(w, h * i / 8), 0.5f)
            }

            // Stage labels
            val labelPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(60, 180, 145, 46)
                textSize = 16f
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }
            drawContext.canvas.nativeCanvas.drawText("AUDIENCE", w / 2, h - 4f, labelPaint)
            drawContext.canvas.nativeCanvas.drawText("UPSTAGE", w / 2, 14f, labelPaint)

            // Props (simplified)
            stageItems.forEach { item ->
                val ix = item.posX * w
                val iy = item.posY * h
                val s = item.scaleX.coerceIn(0.5f, 3f) * 0.6f
                val propColor = when (item.itemType) {
                    "chair" -> Color(0xFF5A35CC)
                    "table" -> Color(0xFF3A4088)
                    "sofa" -> Color(0xFF4A3580)
                    "door" -> Color(0xFF3A3D6A)
                    "window" -> Color(0xFF4B7BFF)
                    "plant" -> Color(0xFF2A5B6B)
                    else -> Color(0xFF3A3D6A)
                }
                rotate(item.rotation, pivot = Offset(ix, iy)) {
                    drawRect(propColor, Offset(ix - 10f * s, iy - 10f * s), Size(20f * s, 20f * s))
                }
            }

            // Movement paths
            movementPaths.forEach { path ->
                val points = path.getPoints()
                if (points.size >= 2) {
                    val charIdx = characters.indexOf(path.characterName).coerceAtLeast(0)
                    val color = CharacterColors[charIdx % CharacterColors.size]
                    val canvasPath = Path()
                    canvasPath.moveTo(points[0].first * w, points[0].second * h)
                    for (i in 1 until points.size - 1) {
                        val midX = (points[i].first + points[i + 1].first) / 2f * w
                        val midY = (points[i].second + points[i + 1].second) / 2f * h
                        canvasPath.quadraticBezierTo(
                            points[i].first * w, points[i].second * h,
                            midX, midY
                        )
                    }
                    canvasPath.lineTo(points.last().first * w, points.last().second * h)
                    drawPath(canvasPath, color.copy(alpha = 0.5f), style = Stroke(2f))

                    // Arrowhead
                    val last = points.last()
                    val prev = points[points.size - 2]
                    val angle = atan2(
                        (last.second - prev.second) * h,
                        (last.first - prev.first) * w
                    ).toDouble()
                    val arrowLen = 7f
                    val lx = last.first * w
                    val ly = last.second * h
                    val arrowPath = Path().apply {
                        moveTo(lx, ly)
                        lineTo(
                            lx - arrowLen * cos(angle - PI / 6).toFloat(),
                            ly - arrowLen * sin(angle - PI / 6).toFloat()
                        )
                        lineTo(
                            lx - arrowLen * cos(angle + PI / 6).toFloat(),
                            ly - arrowLen * sin(angle + PI / 6).toFloat()
                        )
                        close()
                    }
                    drawPath(arrowPath, color.copy(alpha = 0.5f))
                }
            }

            // Character marks
            marks.forEach { mark ->
                val mx = mark.posX * w
                val my = mark.posY * h
                val charIdx = characters.indexOf(mark.characterName).coerceAtLeast(0)
                val color = CharacterColors[charIdx % CharacterColors.size]
                drawCircle(color.copy(alpha = 0.3f), 14f, Offset(mx, my))
                drawCircle(color, 10f, Offset(mx, my))
                drawCircle(Color.Black.copy(alpha = 0.3f), 7f, Offset(mx, my))

                val paint = android.graphics.Paint().apply {
                    this.color = android.graphics.Color.WHITE
                    textSize = 14f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                drawContext.canvas.nativeCanvas.drawText(
                    mark.characterName.take(1).uppercase(),
                    mx, my + 5f, paint
                )
            }

            // Border
            drawRect(
                Color(0xFF2A2D5A).copy(alpha = 0.5f),
                Offset.Zero, Size(w, h),
                style = Stroke(1f)
            )
        }

        // Label overlay
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp),
            shape = RoundedCornerShape(6.dp),
            color = Color.Black.copy(alpha = 0.6f)
        ) {
            Text(
                "BLOCKING",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF7B4DFF),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                fontSize = 9.sp
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PracticeLineCard(
    line: ScriptLine,
    isCurrentLine: Boolean,
    isUserLine: Boolean,
    isHidden: Boolean,
    isPastLine: Boolean,
    mode: PracticeMode,
    onReveal: () -> Unit,
    onTap: () -> Unit,
    onToggleSkip: () -> Unit,
    onEdit: () -> Unit,
    isAutoPlaying: Boolean,
    hasRecording: Boolean = false,
    isPlayingRecording: Boolean = false,
    isRecordingLine: Boolean = false,
    onRecord: () -> Unit = {},
    onPlayRecording: () -> Unit = {},
    onDeleteRecording: () -> Unit = {},
    onUpdateNotes: (String?) -> Unit = {}
) {
    val containerColor = when {
        isRecordingLine -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        line.isSkipped -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        isCurrentLine && isUserLine -> MaterialTheme.colorScheme.primaryContainer
        isCurrentLine -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        isUserLine -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        else -> MaterialTheme.colorScheme.surface
    }

    val borderColor = when {
        isRecordingLine -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
        isCurrentLine && !line.isSkipped -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
    }

    val hasEdits = line.editedDialogue != null || !line.ignoredWords.isNullOrBlank()

    // Delete confirmation
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Recording?") },
            text = { Text("This will permanently delete the voice recording for this line.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteRecording()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(
                when {
                    line.isSkipped -> 0.4f
                    isPastLine && !isCurrentLine -> 0.55f
                    else -> 1f
                }
            )
            .combinedClickable(
                onClick = onTap,
                onLongClick = { if (!isAutoPlaying) onToggleSkip() }
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCurrentLine && !line.isSkipped) 4.dp else 0.dp
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Current line indicator
                if (isCurrentLine && !line.isSkipped) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(Modifier.width(8.dp))
                }

                // Character initial badge
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(
                            if (isUserLine) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        line.character.take(1),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isUserLine) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.width(8.dp))

                Text(
                    line.character,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isUserLine) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    textDecoration = if (line.isSkipped) TextDecoration.LineThrough else TextDecoration.None
                )

                if (isUserLine && !line.isSkipped) {
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

                if (hasEdits) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Default.Edit,
                        "Edited",
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)
                    )
                }

                // Recording indicator dot
                if (hasRecording && !isRecordingLine) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Default.Mic,
                        "Has recording",
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }

                Spacer(Modifier.weight(1f))

                // Action buttons
                if (!isAutoPlaying) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            "Edit line",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    IconButton(
                        onClick = onToggleSkip,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (line.isSkipped) Icons.Default.VisibilityOff else Icons.Default.RemoveCircleOutline,
                            if (line.isSkipped) "Unskip" else "Skip",
                            modifier = Modifier.size(18.dp),
                            tint = if (line.isSkipped) MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            if (line.isSkipped) {
                Spacer(Modifier.height(2.dp))
                Text(
                    "Skipped",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                    fontStyle = FontStyle.Italic
                )
            } else {
                if (!line.stageDirection.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "(${line.stageDirection})",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                val displayText = line.effectiveDialogue
                if (displayText.isNotBlank()) {
                    if (isHidden) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                                .clickable(onClick = onReveal),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Visibility,
                                    null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (mode == PracticeMode.MEMORIZATION)
                                        "Say your line, then tap to check"
                                    else
                                        "Tap to reveal your line",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    } else {
                        Spacer(Modifier.height(6.dp))
                        val ignoredIndices = line.ignoredWordIndices
                        if (ignoredIndices.isNotEmpty()) {
                            DialogueWithIgnoredWords(
                                text = displayText,
                                ignoredIndices = ignoredIndices
                            )
                        } else {
                            Text(
                                displayText,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // ── Voice Recording Controls ──
                if (!isAutoPlaying && !line.isSkipped) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (hasRecording && !isRecordingLine) {
                            // Play / Stop playback button
                            FilledTonalButton(
                                onClick = onPlayRecording,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (isPlayingRecording)
                                        MaterialTheme.colorScheme.errorContainer
                                    else MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = if (isPlayingRecording)
                                        MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onTertiaryContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    if (isPlayingRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    if (isPlayingRecording) "Stop" else "Play",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Re-record button
                            OutlinedButton(
                                onClick = onRecord,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    Icons.Default.Mic,
                                    null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Re-record",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(Modifier.weight(1f))

                            // Delete button
                            IconButton(
                                onClick = { showDeleteConfirm = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    "Delete recording",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                )
                            }
                        } else if (isRecordingLine) {
                            // Active recording indicator
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.error)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Recording...",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(Modifier.weight(1f))
                                    FilledTonalButton(
                                        onClick = onRecord,
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = MaterialTheme.colorScheme.error,
                                            contentColor = MaterialTheme.colorScheme.onError
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Stop, null, Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Save", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            // No recording — show record button
                            OutlinedButton(
                                onClick = onRecord,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                            ) {
                                Icon(
                                    Icons.Default.Mic,
                                    null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Record",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Playing recording indicator
                if (isPlayingRecording) {
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.tertiary,
                        trackColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                    )
                }

                // User notes (not read by TTS)
                if (!line.isSkipped && !isAutoPlaying) {
                    var showNotesEdit by remember { mutableStateOf(false) }
                    var notesText by remember(line.id, line.userNotes) { mutableStateOf(line.userNotes ?: "") }

                    if (!line.userNotes.isNullOrBlank() && !showNotesEdit) {
                        Spacer(Modifier.height(6.dp))
                        Surface(
                            onClick = { showNotesEdit = true },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                        ) {
                            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.StickyNote2, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.tertiary)
                                Spacer(Modifier.width(6.dp))
                                Text(line.userNotes!!, style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else if (showNotesEdit) {
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = notesText,
                            onValueChange = { notesText = it },
                            placeholder = { Text("Add a note...", style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            textStyle = MaterialTheme.typography.bodySmall,
                            maxLines = 3,
                            trailingIcon = {
                                Row {
                                    IconButton(onClick = { onUpdateNotes(notesText.ifBlank { null }); showNotesEdit = false }, Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Check, "Save", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { notesText = line.userNotes ?: ""; showNotesEdit = false }, Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Close, "Cancel", Modifier.size(16.dp))
                                    }
                                }
                            }
                        )
                    } else {
                        Spacer(Modifier.height(4.dp))
                        Surface(
                            onClick = { showNotesEdit = true },
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Transparent
                        ) {
                            Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.StickyNote2, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                Spacer(Modifier.width(4.dp))
                                Text("Add note", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DialogueWithIgnoredWords(
    text: String,
    ignoredIndices: Set<Int>
) {
    val words = text.split("\\s+".toRegex())
    val annotatedString = androidx.compose.ui.text.buildAnnotatedString {
        words.forEachIndexed { index, word ->
            if (index > 0) append(" ")
            if (index in ignoredIndices) {
                pushStyle(
                    androidx.compose.ui.text.SpanStyle(
                        textDecoration = TextDecoration.LineThrough,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                )
                append(word)
                pop()
            } else {
                append(word)
            }
        }
    }
    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditLineDialog(
    line: ScriptLine,
    onDismiss: () -> Unit,
    onSaveDialogue: (String?) -> Unit,
    onToggleIgnoredWord: (Int) -> Unit,
    onReset: () -> Unit
) {
    var editedText by remember(line.id) { mutableStateOf(line.effectiveDialogue) }
    val hasEdits = line.editedDialogue != null || !line.ignoredWords.isNullOrBlank()
    val currentWords = remember(editedText) { editedText.split("\\s+".toRegex()).filter { it.isNotBlank() } }
    val ignoredIndices = line.ignoredWordIndices

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        line.character.take(1),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Edit Line", style = MaterialTheme.typography.titleLarge)
                    Text(
                        line.character,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Original text (if edited)
                if (line.editedDialogue != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            Text(
                                "Original",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                line.dialogue,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = editedText,
                    onValueChange = { editedText = it },
                    label = { Text("Dialogue") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 6,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(16.dp))

                // Ignore words section
                Text(
                    "Tap words to ignore (skipped by TTS)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    currentWords.forEachIndexed { index, word ->
                        val isIgnored = index in ignoredIndices
                        FilterChip(
                            selected = isIgnored,
                            onClick = { onToggleIgnoredWord(index) },
                            label = {
                                Text(
                                    word,
                                    style = MaterialTheme.typography.bodySmall,
                                    textDecoration = if (isIgnored) TextDecoration.LineThrough else TextDecoration.None,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            border = if (isIgnored) BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)) else null
                        )
                    }
                }

                if (ignoredIndices.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${ignoredIndices.size} word(s) ignored",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (hasEdits) {
                    TextButton(onClick = onReset) {
                        Text("Reset", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Button(
                    onClick = { onSaveDialogue(editedText) },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save")
                }
            }
        }
    )
}
