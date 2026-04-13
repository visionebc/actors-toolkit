package mx.visionebc.actorstoolkit.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.visionebc.actorstoolkit.data.entity.BlockingMark
import mx.visionebc.actorstoolkit.data.entity.MovementPath
import mx.visionebc.actorstoolkit.data.entity.StageItem
import mx.visionebc.actorstoolkit.ui.blocking.BlockingViewModel
import mx.visionebc.actorstoolkit.ui.blocking.MovementArrow
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import androidx.compose.ui.platform.LocalContext
import mx.visionebc.actorstoolkit.ui.theme.*
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

// ── Prop definitions ──────────────────────────────────────
data class PropDef(val type: String, val label: String, val emoji: String)

val AVAILABLE_PROPS = listOf(
    PropDef("chair", "Chair", "\uD83E\uDE91"),
    PropDef("table", "Table", "\uD83E\uDE79"),
    PropDef("sofa", "Sofa", "\uD83D\uDECB"),
    PropDef("door", "Door", "\uD83D\uDEAA"),
    PropDef("window", "Window", "\uD83E\uDE9F"),
    PropDef("stairs", "Stairs", "\uD83E\uDEDC"),
    PropDef("bed", "Bed", "\uD83D\uDECF"),
    PropDef("desk", "Desk", "\uD83D\uDCDD"),
    PropDef("podium", "Podium", "\uD83C\uDFE4"),
    PropDef("plant", "Plant", "\uD83E\uDEB4"),
    PropDef("lamp", "Lamp", "\uD83D\uDCA1"),
    PropDef("phone", "Phone", "\u260E"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockingScreen(
    scriptId: Long,
    viewModel: BlockingViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val currentMarks = remember(state.currentLineIndex, state.marks, state.lines) {
        viewModel.getCurrentMarks()
    }
    val movementArrows = remember(state.currentLineIndex, state.marks, state.lines, state.characters, state.movementPaths) {
        viewModel.getMovementArrows()
    }
    val currentPaths = remember(state.currentLineIndex, state.movementPaths, state.lines) {
        viewModel.getCurrentPaths()
    }

    // Track selected prop for direct manipulation
    var selectedItemId by remember { mutableStateOf<Long?>(null) }

    // Initialize TTS for blocking playback
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                ttsReady = true
            }
        }
    }

    // Speak current line and wait for TTS to finish before advancing
    LaunchedEffect(state.isPlaying, ttsReady) {
        if (state.isPlaying && ttsReady && state.lines.isNotEmpty()) {
            val engine = tts ?: return@LaunchedEffect
            var idx = state.currentLineIndex
            while (idx < state.lines.size && state.isPlaying) {
                val line = state.lines[idx]
                // Skip lines marked as skipped
                if (line.isSkipped) {
                    idx++
                    continue
                }
                viewModel.goToLine(idx)
                val text = line.speakableDialogue.takeIf { it.isNotBlank() }
                if (text != null) {
                    // Speak and suspend until TTS finishes
                    suspendCancellableCoroutine { cont ->
                        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {}
                            override fun onDone(utteranceId: String?) {
                                if (cont.isActive) cont.resume(Unit)
                            }
                            @Deprecated("Deprecated in Java")
                            override fun onError(utteranceId: String?) {
                                if (cont.isActive) cont.resume(Unit)
                            }
                        })
                        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "blocking_line_\$idx")
                        cont.invokeOnCancellation { engine.stop() }
                    }
                    // Small pause between lines
                    kotlinx.coroutines.delay(500)
                } else {
                    // No dialogue on this line, brief pause then advance
                    kotlinx.coroutines.delay(800)
                }
                idx++
            }
            // Playback finished naturally
            viewModel.stopPlayback()
        }
    }

    // Cleanup TTS
    DisposableEffect(Unit) {
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    LaunchedEffect(scriptId) {
        viewModel.loadScript(scriptId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Stage Blocking",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (state.scriptTitle.isNotEmpty()) {
                            Text(
                                state.scriptTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Draw mode toggle
                    IconButton(onClick = { viewModel.toggleDrawingMode() }) {
                        Icon(
                            Icons.Default.Draw,
                            "Draw Movement",
                            tint = if (state.isDrawingMode) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Movement toggle
                    IconButton(onClick = { viewModel.toggleMovement() }) {
                        Icon(
                            Icons.Default.Timeline,
                            "Movement",
                            tint = if (state.showMovement) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Props button
                    IconButton(onClick = { viewModel.togglePropsPanel() }) {
                        Icon(
                            Icons.Default.Chair,
                            "Add Props",
                            tint = if (state.showPropsPanel) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            state.error != null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(state.error ?: "Error", color = MaterialTheme.colorScheme.error)
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    // ── Current line info ─────────────────────
                    if (state.lines.isNotEmpty()) {
                        CurrentLineCard(
                            line = state.lines[state.currentLineIndex],
                            lineIndex = state.currentLineIndex,
                            totalLines = state.lines.size,
                            isPlaying = state.isPlaying,
                            onPrev = { viewModel.previousLine() },
                            onNext = { viewModel.nextLine() },
                            onPlayToggle = { viewModel.togglePlayback() }
                        )
                    }

                    // ── Character selector ────────────────────
                    if (state.characters.isNotEmpty()) {
                        CharacterSelector(
                            characters = state.characters.map { it.name },
                            selected = state.selectedCharacter,
                            isPlacing = state.isPlacingMark && !state.isDrawingMode,
                            onSelect = { viewModel.selectCharacter(it) },
                            currentMarks = currentMarks
                        )
                    }

                    // ── Drawing mode info ─────────────────────
                    AnimatedVisibility(
                        visible = state.isDrawingMode,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Draw,
                                    null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Draw mode: drag to draw ${state.selectedCharacter ?: "character"}'s movement path",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                if (currentPaths.any { it.characterName == state.selectedCharacter }) {
                                    TextButton(
                                        onClick = { viewModel.clearPathsForCurrentLine() },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Clear", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }

                    // ── Props panel ───────────────────────────
                    AnimatedVisibility(
                        visible = state.showPropsPanel,
                        enter = slideInVertically() + fadeIn(),
                        exit = slideOutVertically() + fadeOut()
                    ) {
                        PropsPanel(
                            onAddProp = { viewModel.addStageItem(it) },
                            onDismiss = { viewModel.togglePropsPanel() }
                        )
                    }

                    // ── Selected prop action bar ─────────────
                    AnimatedVisibility(
                        visible = selectedItemId != null,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        val selItem = state.stageItems.find { it.id == selectedItemId }
                        if (selItem != null) {
                            val propDef = AVAILABLE_PROPS.find { it.type == selItem.itemType }
                            SelectedPropBar(
                                label = propDef?.label ?: selItem.itemType,
                                emoji = propDef?.emoji ?: "?",
                                onRotate = { viewModel.rotateStageItem(selItem.id) },
                                onBigger = { viewModel.resizeStageItem(selItem.id, true) },
                                onSmaller = { viewModel.resizeStageItem(selItem.id, false) },
                                onClone = { viewModel.cloneStageItem(selItem.id) },
                                onDelete = {
                                    viewModel.deleteStageItem(selItem.id)
                                    selectedItemId = null
                                },
                                onDeselect = { selectedItemId = null }
                            )
                        }
                    }

                    // ── Stage Canvas ──────────────────────────
                    StageCanvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        marks = currentMarks,
                        stageItems = state.stageItems,
                        characters = state.characters.map { it.name },
                        selectedCharacter = state.selectedCharacter,
                        isPlacingMark = state.isPlacingMark,
                        isDrawingMode = state.isDrawingMode,
                        movementPaths = currentPaths,
                        currentDrawingPoints = state.currentDrawingPoints,
                        movementArrows = movementArrows,
                        showMovement = state.showMovement,
                        selectedItemId = selectedItemId,
                        onTapStage = { x, y -> viewModel.addMark(x, y) },
                        onMoveMark = { id, x, y -> viewModel.updateMarkPosition(id, x, y) },
                        onDeleteMark = { viewModel.deleteMark(it) },
                        onMoveItem = { id, x, y -> viewModel.updateStageItemPosition(id, x, y) },
                        onSelectItem = { id -> selectedItemId = if (selectedItemId == id) null else id },
                        onTransformItem = { id, zoom, rot -> viewModel.transformStageItem(id, zoom, rot) },
                        onDeselectAll = { selectedItemId = null },
                        onUpdateDrawing = { viewModel.updateDrawingPoints(it) },
                        onFinishDrawing = { viewModel.finishDrawing(it) }
                    )

                    // ── Stage items legend ───────────────────
                    if (state.stageItems.isNotEmpty()) {
                        StageItemsLegend(
                            items = state.stageItems,
                            selectedItemId = selectedItemId,
                            onSelect = { selectedItemId = if (selectedItemId == it) null else it },
                            onDelete = {
                                viewModel.deleteStageItem(it)
                                if (selectedItemId == it) selectedItemId = null
                            },
                            onRotate = { viewModel.rotateStageItem(it) }
                        )
                    }
                }
            }
        }
    }
}

// ── Selected prop action bar ─────────────────────────────
@Composable
private fun SelectedPropBar(
    label: String,
    emoji: String,
    onRotate: () -> Unit,
    onBigger: () -> Unit,
    onSmaller: () -> Unit,
    onClone: () -> Unit,
    onDelete: () -> Unit,
    onDeselect: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        color = Gold.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(emoji, fontSize = 16.sp)
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Gold,
                modifier = Modifier.padding(start = 4.dp)
            )
            Spacer(Modifier.weight(1f))
            Text(
                "Pinch+rotate on stage",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onRotate, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.RotateRight, "Rotate 45", Modifier.size(18.dp), tint = Gold)
            }
            IconButton(onClick = onBigger, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ZoomIn, "Bigger", Modifier.size(18.dp), tint = Gold)
            }
            IconButton(onClick = onSmaller, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ZoomOut, "Smaller", Modifier.size(18.dp), tint = Gold)
            }
            IconButton(onClick = onClone, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ContentCopy, "Clone", Modifier.size(18.dp), tint = Gold)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, "Delete", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
            }
            IconButton(onClick = onDeselect, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, "Deselect", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ── Current line card ─────────────────────────────────────
@Composable
private fun CurrentLineCard(
    line: mx.visionebc.actorstoolkit.data.entity.ScriptLine,
    lineIndex: Int,
    totalLines: Int,
    isPlaying: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onPlayToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            "${lineIndex + 1}/$totalLines",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        line.character,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row {
                    IconButton(onClick = onPrev, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.SkipPrevious, "Previous", Modifier.size(20.dp))
                    }
                    IconButton(onClick = onPlayToggle, modifier = Modifier.size(32.dp)) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            if (isPlaying) "Pause" else "Play",
                            Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onNext, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.SkipNext, "Next", Modifier.size(20.dp))
                    }
                }
            }

            if (line.effectiveDialogue.isNotBlank()) {
                Text(
                    line.effectiveDialogue,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            val direction = line.stageDirection
            if (!direction.isNullOrBlank()) {
                Text(
                    "($direction)",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = Teal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

// ── Character selector ────────────────────────────────────
@Composable
private fun CharacterSelector(
    characters: List<String>,
    selected: String?,
    isPlacing: Boolean,
    onSelect: (String) -> Unit,
    currentMarks: List<BlockingMark>
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(characters) { name ->
            val isSelected = name == selected
            val colorIdx = characters.indexOf(name) % CharacterColors.size
            val color = CharacterColors[colorIdx]
            val hasMark = currentMarks.any { it.characterName == name }

            FilterChip(
                selected = isSelected,
                onClick = { onSelect(name) },
                label = {
                    Text(
                        name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(color)
                            .then(
                                if (hasMark) Modifier.border(1.dp, Color.White, CircleShape)
                                else Modifier
                            )
                    )
                },
                shape = RoundedCornerShape(10.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = color.copy(alpha = 0.2f),
                    selectedLabelColor = color
                )
            )
        }
    }

    if (isPlacing && selected != null) {
        Text(
            "Tap the stage to place $selected's mark",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
        )
    }
}

// ── Props panel ───────────────────────────────────────────
@Composable
private fun PropsPanel(
    onAddProp: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Add Props",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, "Close", Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AVAILABLE_PROPS) { prop ->
                    Surface(
                        modifier = Modifier.clickable { onAddProp(prop.type) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(prop.emoji, fontSize = 22.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                prop.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Tap prop on stage to select, then pinch to resize & rotate",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Stage Canvas ──────────────────────────────────────────
@Composable
private fun StageCanvas(
    modifier: Modifier,
    marks: List<BlockingMark>,
    stageItems: List<StageItem>,
    characters: List<String>,
    selectedCharacter: String?,
    isPlacingMark: Boolean,
    isDrawingMode: Boolean,
    movementPaths: List<MovementPath>,
    currentDrawingPoints: List<Pair<Float, Float>>,
    movementArrows: List<MovementArrow>,
    showMovement: Boolean,
    selectedItemId: Long?,
    onTapStage: (Float, Float) -> Unit,
    onMoveMark: (Long, Float, Float) -> Unit,
    onDeleteMark: (Long) -> Unit,
    onMoveItem: (Long, Float, Float) -> Unit,
    onSelectItem: (Long) -> Unit,
    onTransformItem: (Long, Float, Float) -> Unit,
    onDeselectAll: () -> Unit,
    onUpdateDrawing: (List<Pair<Float, Float>>) -> Unit,
    onFinishDrawing: (List<Pair<Float, Float>>) -> Unit
) {
    var showDeleteMarkDialog by remember { mutableStateOf<Pair<Long, String>?>(null) }

    // Use rememberUpdatedState so the gesture handler always sees
    // the latest state without restarting pointerInput
    val curMarks by rememberUpdatedState(marks)
    val curItems by rememberUpdatedState(stageItems)
    val curIsDrawing by rememberUpdatedState(isDrawingMode)
    val curIsPlacing by rememberUpdatedState(isPlacingMark)
    val curSelectedChar by rememberUpdatedState(selectedCharacter)
    val curSelectedItemId by rememberUpdatedState(selectedItemId)
    val curOnTapStage by rememberUpdatedState(onTapStage)
    val curOnMoveMark by rememberUpdatedState(onMoveMark)
    val curOnMoveItem by rememberUpdatedState(onMoveItem)
    val curOnSelectItem by rememberUpdatedState(onSelectItem)
    val curOnTransformItem by rememberUpdatedState(onTransformItem)
    val curOnDeselectAll by rememberUpdatedState(onDeselectAll)
    val curOnUpdateDrawing by rememberUpdatedState(onUpdateDrawing)
    val curOnFinishDrawing by rememberUpdatedState(onFinishDrawing)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF080A1A))
            .border(2.dp, GoldMuted.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                // KEY: Use Unit so gesture handler is never restarted
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val firstDown = awaitFirstDown(requireUnconsumed = false)
                        firstDown.consume()

                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val startPos = firstDown.position

                        // Snapshot current state at gesture start
                        val gestureItems = curItems
                        val gestureMarks = curMarks
                        val gestureIsDrawing = curIsDrawing
                        val gestureIsPlacing = curIsPlacing
                        val gestureSelectedChar = curSelectedChar
                        val gestureSelectedItemId = curSelectedItemId

                        // Find nearest elements at the start position
                        // Use larger hit area (60px) for better touch targeting
                        val nearItem = gestureItems.find { item ->
                            val ix = item.posX * w
                            val iy = item.posY * h
                            val hitR = 60f * item.scaleX.coerceIn(0.5f, 5f)
                            (startPos - Offset(ix, iy)).getDistance() < hitR
                        }
                        val nearMark = gestureMarks.find { mark ->
                            val mx = mark.posX * w
                            val my = mark.posY * h
                            (startPos - Offset(mx, my)).getDistance() < 45f
                        }

                        var hasDragged = false
                        var wasMultiTouch = false
                        var prevDist = -1f
                        var prevAngle = 0f
                        // For multi-touch: prefer selected item, then nearest item
                        val transformTarget = if (gestureSelectedItemId != null) {
                            gestureItems.find { it.id == gestureSelectedItemId }
                        } else nearItem
                        val drawPts = if (gestureIsDrawing && gestureSelectedChar != null)
                            mutableListOf(startPos.x / w to startPos.y / h)
                        else null

                        while (true) {
                            val event = awaitPointerEvent()
                            val active = event.changes.filter { it.pressed }

                            if (active.isEmpty()) {
                                event.changes.forEach { it.consume() }
                                break
                            }

                            if (active.size >= 2 && transformTarget != null) {
                                // ── Multi-touch: pinch to resize + rotate ──
                                wasMultiTouch = true
                                hasDragged = true
                                val p1 = active[0].position
                                val p2 = active[1].position
                                val dist = (p1 - p2).getDistance()
                                val angle = atan2(p2.y - p1.y, p2.x - p1.x)
                                if (prevDist > 0f) {
                                    val zoomDelta = dist / prevDist
                                    val rotDelta = angle - prevAngle
                                    curOnTransformItem(transformTarget.id, zoomDelta, rotDelta)
                                }
                                prevDist = dist
                                prevAngle = angle
                                active.forEach { it.consume() }
                            } else if (active.size >= 2) {
                                // Multi-touch but no item — just consume
                                wasMultiTouch = true
                                active.forEach { it.consume() }
                            } else if (active.size == 1 && !wasMultiTouch) {
                                // ── Single finger: drag or draw ──
                                val change = active[0]
                                val pos = change.position
                                if ((pos - startPos).getDistance() > 8f) {
                                    hasDragged = true
                                    val nx = (pos.x / w).coerceIn(0f, 1f)
                                    val ny = (pos.y / h).coerceIn(0f, 1f)
                                    if (drawPts != null) {
                                        drawPts.add(nx to ny)
                                        curOnUpdateDrawing(drawPts.toList())
                                    } else if (nearMark != null) {
                                        curOnMoveMark(nearMark.id, nx, ny)
                                    } else if (nearItem != null) {
                                        curOnMoveItem(nearItem.id, nx, ny)
                                    }
                                }
                                change.consume()
                            } else {
                                active.forEach { it.consume() }
                            }
                        }

                        // ── Gesture ended ──
                        if (!hasDragged && !wasMultiTouch) {
                            // TAP — no dialog for props, just select/deselect
                            if (nearItem != null) {
                                curOnSelectItem(nearItem.id)
                            } else if (nearMark != null) {
                                showDeleteMarkDialog = nearMark.id to nearMark.characterName
                            } else if (gestureIsPlacing && !gestureIsDrawing) {
                                curOnTapStage(startPos.x / w, startPos.y / h)
                            } else {
                                // Tapped empty area — deselect
                                curOnDeselectAll()
                            }
                        } else if (drawPts != null && drawPts.size > 2) {
                            curOnFinishDrawing(drawPts.toList())
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height

            // ── Stage floor ──────────────────────────
            drawRect(
                color = Color(0xFF0D0F25),
                topLeft = Offset.Zero,
                size = Size(w, h)
            )

            // ── Grid lines ──────────────────────────
            val gridColor = Color(0xFF1E2148)
            val gridCount = 8
            for (i in 1 until gridCount) {
                val x = w * i / gridCount
                drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
            }
            for (i in 1 until gridCount) {
                val y = h * i / gridCount
                drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
            }

            // ── Stage edge labels ───────────────────
            val labelPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(100, 180, 145, 46)
                textSize = 28f
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }

            drawContext.canvas.nativeCanvas.apply {
                drawText("AUDIENCE", w / 2, h - 8f, labelPaint)
                save()
                rotate(-90f, 16f, h / 2)
                drawText("SL", 16f, h / 2, labelPaint)
                restore()
                save()
                rotate(90f, w - 16f, h / 2)
                drawText("SR", w - 16f, h / 2, labelPaint)
                restore()
                drawText("UPSTAGE", w / 2, 24f, labelPaint)
            }

            // ── Stage items (props) ─────────────────
            stageItems.forEach { item ->
                val ix = item.posX * w
                val iy = item.posY * h
                val isSelected = item.id == selectedItemId
                drawStageItem(item, ix, iy, isSelected)
            }

            // ── Alignment guides for selected prop ──
            if (selectedItemId != null) {
                val selItem = stageItems.find { it.id == selectedItemId }
                if (selItem != null) {
                    val sx = selItem.posX * w
                    val sy = selItem.posY * h
                    val guideColor = Color(0xFF7B4DFF).copy(alpha = 0.5f)
                    val guideDash = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))

                    // Horizontal guide line
                    drawLine(
                        color = guideColor,
                        start = Offset(0f, sy),
                        end = Offset(w, sy),
                        strokeWidth = 1.5f,
                        pathEffect = guideDash
                    )
                    // Vertical guide line
                    drawLine(
                        color = guideColor,
                        start = Offset(sx, 0f),
                        end = Offset(sx, h),
                        strokeWidth = 1.5f,
                        pathEffect = guideDash
                    )

                    // Small coordinate labels
                    val coordPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(160, 180, 145, 46)
                        textSize = 20f
                        isAntiAlias = true
                        textAlign = android.graphics.Paint.Align.LEFT
                    }
                    val xPct = (selItem.posX * 100).toInt()
                    val yPct = (selItem.posY * 100).toInt()
                    drawContext.canvas.nativeCanvas.drawText(
                        "X:$xPct%", sx + 6f, 18f, coordPaint
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        "Y:$yPct%", 4f, sy - 6f, coordPaint
                    )
                }
            }

            // ── Freehand movement paths ─────────────
            if (showMovement) {
                movementPaths.forEach { path ->
                    val points = path.getPoints()
                    if (points.size >= 2) {
                        val charIdx = characters.indexOf(path.characterName).coerceAtLeast(0)
                        val color = CharacterColors[charIdx % CharacterColors.size]
                        drawFreehandPath(points, w, h, color.copy(alpha = 0.7f), 3f)
                    }
                }

                // ── Auto-generated movement arrows ──
                movementArrows.forEach { arrow ->
                    val charIdx = characters.indexOf(arrow.characterName).coerceAtLeast(0)
                    val color = CharacterColors[charIdx % CharacterColors.size]

                    val fx = arrow.fromX * w
                    val fy = arrow.fromY * h
                    val tx = arrow.toX * w
                    val ty = arrow.toY * h

                    // Ghost circle at previous position
                    drawCircle(
                        color = color.copy(alpha = 0.12f),
                        radius = 16f,
                        center = Offset(fx, fy)
                    )
                    drawCircle(
                        color = color.copy(alpha = 0.3f),
                        radius = 16f,
                        center = Offset(fx, fy),
                        style = Stroke(
                            width = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
                        )
                    )

                    val ghostPaint = android.graphics.Paint().apply {
                        this.color = android.graphics.Color.argb(
                            80,
                            (color.red * 255).toInt(),
                            (color.green * 255).toInt(),
                            (color.blue * 255).toInt()
                        )
                        textSize = 18f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        arrow.characterName.take(2).uppercase(),
                        fx, fy + 6f, ghostPaint
                    )

                    drawLine(
                        color = color.copy(alpha = 0.5f),
                        start = Offset(fx, fy),
                        end = Offset(tx, ty),
                        strokeWidth = 2.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                    )

                    val angle = atan2((ty - fy).toDouble(), (tx - fx).toDouble())
                    val arrowLen = 14f
                    val pullBack = 22f
                    val tipX = tx - pullBack * cos(angle).toFloat()
                    val tipY = ty - pullBack * sin(angle).toFloat()
                    val arrowPath = Path().apply {
                        moveTo(tipX, tipY)
                        lineTo(
                            tipX - arrowLen * cos(angle - PI / 6).toFloat(),
                            tipY - arrowLen * sin(angle - PI / 6).toFloat()
                        )
                        lineTo(
                            tipX - arrowLen * cos(angle + PI / 6).toFloat(),
                            tipY - arrowLen * sin(angle + PI / 6).toFloat()
                        )
                        close()
                    }
                    drawPath(arrowPath, color.copy(alpha = 0.6f))
                }
            }

            // ── Currently drawing path ──────────────
            if (currentDrawingPoints.size >= 2) {
                val charIdx = characters.indexOf(selectedCharacter ?: "").coerceAtLeast(0)
                val color = CharacterColors[charIdx % CharacterColors.size]
                drawFreehandPath(currentDrawingPoints, w, h, color.copy(alpha = 0.4f), 4f)
            }

            // ── Character marks ──────────────────────
            marks.forEach { mark ->
                val mx = mark.posX * w
                val my = mark.posY * h
                val charIdx = characters.indexOf(mark.characterName).coerceAtLeast(0)
                val color = CharacterColors[charIdx % CharacterColors.size]

                drawCircle(
                    color = color.copy(alpha = 0.2f),
                    radius = 26f,
                    center = Offset(mx, my)
                )
                drawCircle(
                    color = color,
                    radius = 18f,
                    center = Offset(mx, my)
                )
                drawCircle(
                    color = Color.Black.copy(alpha = 0.3f),
                    radius = 14f,
                    center = Offset(mx, my)
                )

                val initialPaint = android.graphics.Paint().apply {
                    this.color = android.graphics.Color.WHITE
                    textSize = 22f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                drawContext.canvas.nativeCanvas.drawText(
                    mark.characterName.take(2).uppercase(),
                    mx, my + 8f, initialPaint
                )
            }

            // ── Stage border ─────────────────────────
            drawRect(
                color = GoldMuted.copy(alpha = 0.3f),
                topLeft = Offset.Zero,
                size = Size(w, h),
                style = Stroke(width = 2f)
            )

            // ── Center cross ─────────────────────────
            val centerColor = Gold.copy(alpha = 0.15f)
            drawLine(centerColor, Offset(w / 2, 0f), Offset(w / 2, h), strokeWidth = 1f)
            drawLine(centerColor, Offset(0f, h / 2), Offset(w, h / 2), strokeWidth = 1f)

            // ── Drawing mode indicator ───────────────
            if (isDrawingMode) {
                drawRect(
                    color = Color(0xFF00D4FF).copy(alpha = 0.15f),
                    topLeft = Offset.Zero,
                    size = Size(w, h),
                    style = Stroke(width = 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 8f)))
                )
            }
        }

        // ── Delete mark dialog (marks only, NOT props) ──
        showDeleteMarkDialog?.let { (markId, markName) ->
            AlertDialog(
                onDismissRequest = { showDeleteMarkDialog = null },
                title = { Text(markName, fontWeight = FontWeight.Bold) },
                text = { Text("Remove ${markName}'s mark from this line?") },
                confirmButton = {
                    TextButton(onClick = {
                        onDeleteMark(markId)
                        showDeleteMarkDialog = null
                    }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteMarkDialog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

// ── Draw freehand path with smooth curves ────────────────
private fun DrawScope.drawFreehandPath(
    points: List<Pair<Float, Float>>,
    w: Float,
    h: Float,
    color: Color,
    strokeWidth: Float
) {
    if (points.size < 2) return
    val path = Path()
    path.moveTo(points[0].first * w, points[0].second * h)

    if (points.size == 2) {
        path.lineTo(points[1].first * w, points[1].second * h)
    } else {
        // Smooth curve using quadratic bezier through midpoints
        for (i in 1 until points.size - 1) {
            val midX = (points[i].first + points[i + 1].first) / 2f * w
            val midY = (points[i].second + points[i + 1].second) / 2f * h
            path.quadraticBezierTo(
                points[i].first * w, points[i].second * h,
                midX, midY
            )
        }
        path.lineTo(points.last().first * w, points.last().second * h)
    }

    drawPath(path, color, style = Stroke(width = strokeWidth))

    // Arrowhead at end
    if (points.size >= 2) {
        val last = points.last()
        val prev = points[points.size - 2]
        val lx = last.first * w
        val ly = last.second * h
        val px = prev.first * w
        val py = prev.second * h
        val angle = atan2((ly - py).toDouble(), (lx - px).toDouble())
        val arrowLen = 10f
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
        drawPath(arrowPath, color)
    }
}

// ── Stage items legend ────────────────────────────────────
@Composable
private fun StageItemsLegend(
    items: List<StageItem>,
    selectedItemId: Long?,
    onSelect: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onRotate: (Long) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(items) { item ->
            val propDef = AVAILABLE_PROPS.find { it.type == item.itemType }
            val isSelected = item.id == selectedItemId
            Surface(
                modifier = Modifier.clickable { onSelect(item.id) },
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) Gold.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = 0.5f))
                         else null
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(propDef?.emoji ?: "?", fontSize = 14.sp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        propDef?.label ?: item.itemType,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Gold else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ── Draw prop on canvas (with scale + selection highlight) ──
private fun DrawScope.drawStageItem(item: StageItem, x: Float, y: Float, isSelected: Boolean) {
    val s = item.scaleX.coerceIn(0.5f, 5f)

    // Draw selection highlight behind the prop
    if (isSelected) {
        drawCircle(
            color = Color(0xFF7B4DFF).copy(alpha = 0.25f),
            radius = 50f * s,
            center = Offset(x, y)
        )
        drawCircle(
            color = Color(0xFF7B4DFF).copy(alpha = 0.7f),
            radius = 50f * s,
            center = Offset(x, y),
            style = Stroke(
                width = 2.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
            )
        )
    }

    val propColor = when (item.itemType) {
        "chair" -> Color(0xFF5A35CC)
        "table" -> Color(0xFF3A4088)
        "sofa" -> Color(0xFF4A3580)
        "door" -> Color(0xFF3A3D6A)
        "window" -> Color(0xFF4B7BFF)
        "stairs" -> Color(0xFF4A4D7A)
        "bed" -> Color(0xFF4A3570)
        "desk" -> Color(0xFF3A4078)
        "podium" -> Color(0xFF5A4D88)
        "plant" -> Color(0xFF2A5B6B)
        "lamp" -> Color(0xFF7B6DCC)
        "phone" -> Color(0xFF3A4D8B)
        else -> Color(0xFF3A3D6A)
    }

    rotate(item.rotation, pivot = Offset(x, y)) {
        when (item.itemType) {
            "chair" -> {
                drawRect(propColor, Offset(x - 14f * s, y - 14f * s), Size(28f * s, 28f * s))
                drawRect(propColor.copy(alpha = 0.7f), Offset(x - 14f * s, y - 20f * s), Size(28f * s, 6f * s))
            }
            "table" -> {
                drawRect(propColor, Offset(x - 26f * s, y - 16f * s), Size(52f * s, 32f * s))
                drawRect(Color.Black.copy(alpha = 0.2f), Offset(x - 26f * s, y - 16f * s), Size(52f * s, 32f * s), style = Stroke(1.5f))
            }
            "sofa" -> {
                drawRoundRect(propColor, Offset(x - 28f * s, y - 12f * s), Size(56f * s, 24f * s), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f * s))
                drawRoundRect(propColor.copy(alpha = 0.7f), Offset(x - 28f * s, y - 18f * s), Size(56f * s, 6f * s), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f * s))
            }
            "door" -> {
                drawRect(propColor, Offset(x - 12f * s, y - 22f * s), Size(24f * s, 44f * s))
                drawRect(Color(0xFF5A5D8A), Offset(x - 12f * s, y - 22f * s), Size(24f * s, 44f * s), style = Stroke(2f))
                drawCircle(Color(0xFF9B6DFF), radius = 3f * s, center = Offset(x + 6f * s, y))
            }
            "window" -> {
                drawRect(propColor, Offset(x - 20f * s, y - 5f * s), Size(40f * s, 10f * s))
                drawLine(Color(0xFF4B7BFF), Offset(x - 20f * s, y), Offset(x + 20f * s, y), 1f)
                drawLine(Color(0xFF4B7BFF), Offset(x, y - 5f * s), Offset(x, y + 5f * s), 1f)
            }
            "stairs" -> {
                for (i in 0..3) {
                    val sy = y - 14f * s + i * 9f * s
                    val sw = 32f * s - i * 5f * s
                    drawRect(propColor, Offset(x - sw / 2, sy), Size(sw, 8f * s))
                }
            }
            "bed" -> {
                drawRoundRect(propColor, Offset(x - 18f * s, y - 28f * s), Size(36f * s, 56f * s), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f * s))
                drawRoundRect(Color(0xFF5A4D88), Offset(x - 16f * s, y - 26f * s), Size(32f * s, 16f * s), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f * s))
            }
            "desk" -> {
                drawRect(propColor, Offset(x - 24f * s, y - 14f * s), Size(48f * s, 28f * s))
                drawLine(Color.Black.copy(alpha = 0.3f), Offset(x - 24f * s, y), Offset(x + 24f * s, y), 1f)
            }
            "podium" -> {
                val path = Path().apply {
                    moveTo(x - 16f * s, y + 18f * s)
                    lineTo(x - 10f * s, y - 18f * s)
                    lineTo(x + 10f * s, y - 18f * s)
                    lineTo(x + 16f * s, y + 18f * s)
                    close()
                }
                drawPath(path, propColor, style = Fill)
                drawRect(propColor.copy(alpha = 0.8f), Offset(x - 12f * s, y - 20f * s), Size(24f * s, 4f * s))
            }
            "plant" -> {
                drawRect(Color(0xFF3A3570), Offset(x - 10f * s, y + 2f * s), Size(20f * s, 16f * s))
                drawCircle(propColor, radius = 16f * s, center = Offset(x, y - 8f * s))
                drawCircle(Color(0xFF3A8B3A), radius = 10f * s, center = Offset(x, y - 12f * s))
            }
            "lamp" -> {
                drawLine(propColor, Offset(x, y + 16f * s), Offset(x, y - 8f * s), 3f * s)
                drawCircle(Color(0xFFFFDD44).copy(alpha = 0.4f), radius = 18f * s, center = Offset(x, y - 10f * s))
                drawCircle(propColor, radius = 10f * s, center = Offset(x, y - 10f * s))
            }
            "phone" -> {
                drawRect(propColor, Offset(x - 8f * s, y - 10f * s), Size(16f * s, 20f * s))
                drawRect(Color(0xFF4A4A8B), Offset(x - 6f * s, y - 8f * s), Size(12f * s, 10f * s))
            }
            else -> {
                drawCircle(propColor, radius = 16f * s, center = Offset(x, y))
            }
        }

        val labelPaint = android.graphics.Paint().apply {
            color = if (isSelected) android.graphics.Color.argb(240, 180, 145, 46)
                    else android.graphics.Color.argb(180, 200, 200, 200)
            textSize = (20f * s).coerceIn(14f, 40f)
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = isSelected
            isAntiAlias = true
        }
        drawContext.canvas.nativeCanvas.drawText(
            AVAILABLE_PROPS.find { it.type == item.itemType }?.label ?: item.itemType,
            x, y + 36f * s, labelPaint
        )
    }
}
