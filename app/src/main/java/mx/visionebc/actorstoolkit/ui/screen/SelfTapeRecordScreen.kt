package mx.visionebc.actorstoolkit.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import mx.visionebc.actorstoolkit.data.entity.Audition
import mx.visionebc.actorstoolkit.data.entity.Script
import mx.visionebc.actorstoolkit.data.entity.ScriptLine
import mx.visionebc.actorstoolkit.ui.selftape.SelfTapeViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelfTapeRecordScreen(
    viewModel: SelfTapeViewModel,
    scripts: List<Script>,
    scriptLines: Map<Long, List<ScriptLine>>,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val auditions by viewModel.auditions.collectAsState()

    // Permission state
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] == true
        hasAudioPermission = permissions[Manifest.permission.RECORD_AUDIO] == true
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission || !hasAudioPermission) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
        }
    }

    // Camera state
    var isRecording by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(0) }
    var recordingDuration by remember { mutableLongStateOf(0L) }
    var useFrontCamera by remember { mutableStateOf(true) }
    var showScriptOverlay by remember { mutableStateOf(false) }
    var selectedScriptId by remember { mutableStateOf<Long?>(null) }
    var selectedAuditionId by remember { mutableStateOf<Long?>(null) }
    var showSetupSheet by remember { mutableStateOf(false) }
    var currentRecording by remember { mutableStateOf<Recording?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var lastVideoPath by remember { mutableStateOf<String?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var tapeTitle by remember { mutableStateOf("") }
    var tapeNotes by remember { mutableStateOf("") }

    // Timer for recording duration
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingDuration = 0L
            while (isRecording) {
                delay(1000)
                recordingDuration += 1000
            }
        }
    }

    // Countdown
    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown -= 1
            if (countdown == 0) {
                // Start recording
                val videoDir = File(context.getExternalFilesDir(null), "self_tapes")
                videoDir.mkdirs()
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val videoFile = File(videoDir, "tape_$timestamp.mp4")

                val vc = videoCapture
                if (vc != null) {
                    val outputOptions = FileOutputOptions.Builder(videoFile).build()
                    try {
                        currentRecording = vc.output
                            .prepareRecording(context, outputOptions)
                            .withAudioEnabled()
                            .start(ContextCompat.getMainExecutor(context)) { event ->
                                when (event) {
                                    is VideoRecordEvent.Finalize -> {
                                        if (event.hasError()) {
                                            Log.e("SelfTape", "Recording error: ${event.error}")
                                        } else {
                                            lastVideoPath = videoFile.absolutePath
                                            showSaveDialog = true
                                        }
                                        isRecording = false
                                    }
                                }
                            }
                        isRecording = true
                    } catch (e: SecurityException) {
                        Toast.makeText(context, "Audio permission required", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    if (!hasCameraPermission || !hasAudioPermission) {
        // Permission request screen
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Record Self-Tape") },
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.VideocamOff,
                        null,
                        Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Camera & microphone\npermissions required",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = {
                            permissionLauncher.launch(
                                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                            )
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Grant Permissions")
                    }
                }
            }
        }
        return
    }

    // Camera preview
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val recorder = Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.HD))
                        .build()
                    val vc = VideoCapture.withOutput(recorder)
                    videoCapture = vc

                    val cameraSelector = if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA
                    else CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, vc)
                    } catch (e: Exception) {
                        Log.e("SelfTape", "Camera bind failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top controls bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.4f),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.ArrowBack, "Back")
            }

            if (isRecording) {
                // Recording indicator
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Red.copy(alpha = 0.8f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            formatDuration(recordingDuration),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            } else {
                Spacer(Modifier.width(48.dp))
            }

            Row {
                // Script overlay toggle
                IconButton(
                    onClick = { showScriptOverlay = !showScriptOverlay },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (showScriptOverlay) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        else Color.Black.copy(alpha = 0.4f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Description, "Script")
                }

                Spacer(Modifier.width(8.dp))

                // Flip camera
                if (!isRecording) {
                    IconButton(
                        onClick = { useFrontCamera = !useFrontCamera },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.4f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.FlipCameraAndroid, "Flip")
                    }
                }
            }
        }

        // Script overlay
        if (showScriptOverlay && selectedScriptId != null) {
            val lines = scriptLines[selectedScriptId] ?: emptyList()
            if (lines.isNotEmpty()) {
                val listState = rememberLazyListState()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.35f)
                        .align(Alignment.TopCenter)
                        .padding(top = 80.dp, start = 16.dp, end = 16.dp)
                        .background(
                            Color.Black.copy(alpha = 0.6f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    LazyColumn(state = listState) {
                        items(lines) { line ->
                            Text(
                                text = "${line.character}: ${line.dialogue}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Countdown overlay
        AnimatedVisibility(
            visible = countdown > 0,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Text(
                text = countdown.toString(),
                fontSize = 120.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.8f)
            )
        }

        // Bottom controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Settings button
            IconButton(
                onClick = { if (!isRecording) showSetupSheet = true },
                modifier = Modifier.size(56.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.4f),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Settings, "Settings", Modifier.size(28.dp))
            }

            // Record/Stop button
            IconButton(
                onClick = {
                    if (isRecording) {
                        currentRecording?.stop()
                    } else if (countdown == 0) {
                        countdown = 3
                    }
                },
                modifier = Modifier.size(80.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            if (isRecording) Color.Red.copy(alpha = 0.9f)
                            else Color.White.copy(alpha = 0.9f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isRecording) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color.White, RoundedCornerShape(4.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.Red)
                        )
                    }
                }
            }

            // Placeholder for symmetry
            Spacer(Modifier.size(56.dp))
        }
    }

    // Setup bottom sheet
    if (showSetupSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSetupSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    "Recording Setup",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(24.dp))

                // Select script for overlay
                Text(
                    "Script Overlay",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))

                Surface(
                    onClick = { selectedScriptId = null; showScriptOverlay = false },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedScriptId == null) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "None",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(Modifier.height(4.dp))

                scripts.forEach { script ->
                    Surface(
                        onClick = {
                            selectedScriptId = script.id
                            showScriptOverlay = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedScriptId == script.id) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            script.title,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }

                Spacer(Modifier.height(16.dp))

                // Link to audition
                Text(
                    "Link to Audition",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))

                Surface(
                    onClick = { selectedAuditionId = null },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedAuditionId == null) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "None",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(Modifier.height(4.dp))

                auditions.forEach { audition ->
                    Surface(
                        onClick = { selectedAuditionId = audition.id },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedAuditionId == audition.id) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "${audition.projectName} — ${audition.roleName}",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = { showSetupSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Done")
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Save dialog after recording
    if (showSaveDialog && lastVideoPath != null) {
        AlertDialog(
            onDismissRequest = {},
            shape = RoundedCornerShape(24.dp),
            icon = {
                Icon(
                    Icons.Default.Save,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Save Self-Tape") },
            text = {
                Column {
                    OutlinedTextField(
                        value = tapeTitle,
                        onValueChange = { tapeTitle = it },
                        label = { Text("Title") },
                        placeholder = { Text("e.g., Hamlet Monologue Take 1") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tapeNotes,
                        onValueChange = { tapeNotes = it },
                        label = { Text("Notes (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val title = tapeTitle.ifBlank {
                            SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date())
                        }
                        viewModel.saveTape(
                            context = context,
                            videoPath = lastVideoPath!!,
                            title = title,
                            auditionId = selectedAuditionId,
                            scriptId = selectedScriptId,
                            notes = tapeNotes
                        ) { id ->
                            showSaveDialog = false
                            onSaved(id)
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        // Discard the video
                        try { File(lastVideoPath!!).delete() } catch (_: Exception) {}
                        showSaveDialog = false
                        lastVideoPath = null
                        tapeTitle = ""
                        tapeNotes = ""
                    }
                ) { Text("Discard") }
            }
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}
