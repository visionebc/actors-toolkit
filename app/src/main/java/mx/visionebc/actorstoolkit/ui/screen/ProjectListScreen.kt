package mx.visionebc.actorstoolkit.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import mx.visionebc.actorstoolkit.R
import kotlinx.coroutines.launch
import mx.visionebc.actorstoolkit.data.entity.Project
import mx.visionebc.actorstoolkit.data.entity.ProjectJsonConverter
import mx.visionebc.actorstoolkit.ui.project.ProjectViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    viewModel: ProjectViewModel,
    onAddClick: () -> Unit,
    onProjectClick: (Long) -> Unit,
    crashLog: String? = null
) {
    val context = LocalContext.current
    val shareScope = rememberCoroutineScope()
    val projects by viewModel.filteredProjects.collectAsState()
    val allProjects by viewModel.projects.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var showCrashDialog by remember { mutableStateOf(crashLog != null) }
    var shareTargetProject by remember { mutableStateOf<Project?>(null) }
    var showAddChoice by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            shareScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val count = mx.visionebc.actorstoolkit.util.DatabaseBackup.importProjectsFromJson(context, uri)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        if (count > 0) "$count project(s) imported" else "Import failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.logo),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Actors Toolkit",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (allProjects.isEmpty()) "Organize your acting work"
                                else "${allProjects.size} project${if (allProjects.size != 1) "s" else ""}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            val rotation by animateFloatAsState(if (showAddChoice) 45f else 0f, label = "fabRotation")
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AnimatedVisibility(
                    visible = showAddChoice,
                    enter = fadeIn() + slideInVertically(
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                    ) { it / 2 } + scaleIn(initialScale = 0.3f),
                    exit = fadeOut() + slideOutVertically { it / 2 } + scaleOut(targetScale = 0.3f)
                ) {
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        MiniAction(
                            label = "Start New",
                            icon = Icons.Default.Add,
                            onClick = { showAddChoice = false; onAddClick() }
                        )
                        MiniAction(
                            label = "Import File",
                            icon = Icons.Default.FileDownload,
                            onClick = {
                                showAddChoice = false
                                importLauncher.launch(arrayOf("application/json", "application/zip", "application/octet-stream", "*/*"))
                            }
                        )
                    }
                }
                FloatingActionButton(
                    onClick = { showAddChoice = !showAddChoice },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(Icons.Default.Add, "New Project", modifier = Modifier.size(28.dp).rotate(rotation))
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
      Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            if (allProjects.size > 3) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search projects...") },
                    leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(20.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, "Clear", Modifier.size(20.dp))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            if (allProjects.isEmpty()) {
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
                                Icons.Default.Folder,
                                null,
                                Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(32.dp))
                        Text(
                            "No projects yet",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Organize your acting work into projects\nand keep everything in one place",
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(32.dp))
                        FilledTonalButton(
                            onClick = { showAddChoice = true },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("New Project")
                        }
                    }
                }
            } else if (projects.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No projects match \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val reorderable = searchQuery.isBlank()
                var localOrder by remember { mutableStateOf(projects) }
                LaunchedEffect(projects) {
                    // Only sync when not actively dragging and content actually changed
                    if (projects.map { it.id } != localOrder.map { it.id } ||
                        projects != localOrder) {
                        localOrder = projects
                    }
                }

                val listState = rememberLazyListState()
                var draggingId by remember { mutableStateOf<Long?>(null) }
                var draggingOffset by remember { mutableStateOf(0f) }

                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(localOrder, key = { _, it -> it.id }) { _, project ->
                        val isDragging = draggingId == project.id
                        val offsetY = if (isDragging) draggingOffset.roundToInt() else 0
                        val elevation by animateFloatAsState(if (isDragging) 10f else 0f, label = "dragElev")

                        val dragHandle = if (reorderable) {
                            Modifier.pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggingId = project.id
                                        draggingOffset = 0f
                                    },
                                    onDragEnd = {
                                        val finalOrder = localOrder.map { it.id }
                                        draggingId = null
                                        draggingOffset = 0f
                                        viewModel.reorderProjects(finalOrder)
                                    },
                                    onDragCancel = {
                                        draggingId = null
                                        draggingOffset = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        draggingOffset += dragAmount.y
                                        val currentId = draggingId ?: return@detectDragGesturesAfterLongPress
                                        val info = listState.layoutInfo.visibleItemsInfo.find { it.key == currentId } ?: return@detectDragGesturesAfterLongPress
                                        val draggedCenter = info.offset + info.size / 2f + draggingOffset
                                        val target = listState.layoutInfo.visibleItemsInfo.find {
                                            it.key != currentId && it.key is Long &&
                                                draggedCenter.toInt() in it.offset..(it.offset + it.size)
                                        } ?: return@detectDragGesturesAfterLongPress
                                        val fromIdx = localOrder.indexOfFirst { it.id == currentId }
                                        val toIdx = localOrder.indexOfFirst { it.id == target.key }
                                        if (fromIdx >= 0 && toIdx >= 0 && fromIdx != toIdx) {
                                            localOrder = localOrder.toMutableList().also {
                                                val item = it.removeAt(fromIdx)
                                                it.add(toIdx, item)
                                            }
                                            draggingOffset += (info.offset - target.offset).toFloat()
                                        }
                                    }
                                )
                            }
                        } else Modifier

                        Box(
                            Modifier
                                .zIndex(if (isDragging) 1f else 0f)
                                .offset { IntOffset(0, offsetY) }
                                .shadow(elevation.dp, RoundedCornerShape(20.dp))
                        ) {
                            ProjectCard(
                                project = project,
                                onClick = { onProjectClick(project.id) },
                                onDelete = { viewModel.deleteProject(project) },
                                onClone = { newName -> viewModel.cloneProject(project, newName) },
                                onShare = { shareTargetProject = project },
                                dragHandleModifier = dragHandle,
                                showDragHandle = reorderable
                            )
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        // Scrim for speed-dial (taps outside close it)
        AnimatedVisibility(
            visible = showAddChoice,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.matchParentSize()
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.25f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showAddChoice = false }
            )
        }
      }
    }

    // Share options dialog
    shareTargetProject?.let { proj ->
        var optDates by remember(proj.id) { mutableStateOf(true) }
        var optLinks by remember(proj.id) { mutableStateOf(true) }
        var optTeam by remember(proj.id) { mutableStateOf(true) }
        var optContacts by remember(proj.id) { mutableStateOf(true) }
        var optAuditions by remember(proj.id) { mutableStateOf(true) }
        var optScripts by remember(proj.id) { mutableStateOf(true) }
        var optCastings by remember(proj.id) { mutableStateOf(true) }
        var optRecordings by remember(proj.id) { mutableStateOf(false) }

        AnimatedDialog(
            onDismissRequest = { shareTargetProject = null },
            title = "Share \"${proj.name}\"",
            icon = { Icon(Icons.Default.Share, null, tint = MaterialTheme.colorScheme.primary) },
            confirmButton = {
                Button(
                    onClick = {
                        val opts = mx.visionebc.actorstoolkit.util.DatabaseBackup.ShareOptions(
                            datesAndLocations = optDates,
                            linksAndAttachments = optLinks,
                            team = optTeam,
                            contacts = optContacts,
                            auditions = optAuditions,
                            scripts = optScripts,
                            castingsAndCharacters = optCastings,
                            recordings = optRecordings && optScripts
                        )
                        val pid = proj.id
                        shareTargetProject = null
                        shareScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            mx.visionebc.actorstoolkit.util.DatabaseBackup.shareProject(context, pid, opts)
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Share")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { shareTargetProject = null }, shape = RoundedCornerShape(12.dp)) { Text("Cancel") }
            }
        ) {
            Text(
                "Basic project info (name, director, notes) is always included. Select what else to share:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            @Composable
            fun row(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
                Row(
                    Modifier.fillMaxWidth().clickable { onChange(!checked) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = onChange,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                row("Dates & locations", optDates) { optDates = it }
                row("Links & attachments", optLinks) { optLinks = it }
                row("Team members", optTeam) { optTeam = it }
                row("Contacts", optContacts) { optContacts = it }
                row("Auditions", optAuditions) { optAuditions = it }
                row("Scripts (full text)", optScripts) { optScripts = it }
                row("Castings & characters", optCastings) { optCastings = it }
                row("Audio recordings (larger file)", optRecordings && optScripts) { optRecordings = it }
                if (optRecordings && !optScripts) {
                    Text(
                        "Enable Scripts to include recordings.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 40.dp)
                    )
                }
            }
        }
    }

    // Crash log dialog
    if (showCrashDialog && crashLog != null) {
        AnimatedDialog(
            onDismissRequest = { showCrashDialog = false },
            title = "Crash Report",
            icon = {
                Icon(
                    Icons.Default.BugReport,
                    null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = { showCrashDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Dismiss") }
            }
        ) {
            Text(
                crashLog,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 20,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProjectCard(
    project: Project,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onClone: (String) -> Unit = {},
    onShare: () -> Unit = {},
    dragHandleModifier: Modifier = Modifier,
    showDragHandle: Boolean = false
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCloneDialog by remember { mutableStateOf(false) }
    var cloneName by remember { mutableStateOf("") }
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val team = remember(project.teamJson) { ProjectJsonConverter.teamFromJson(project.teamJson) }

    val cardBgColor = if (project.cardColor.isNotBlank()) {
        try { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(project.cardColor)) } catch (_: Exception) { MaterialTheme.colorScheme.surface }
    } else MaterialTheme.colorScheme.surface

    val iconBgColor = if (project.cardColor.isNotBlank()) {
        cardBgColor.copy(alpha = 0.3f)
    } else MaterialTheme.colorScheme.primaryContainer

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (project.cardImageUri.isNotBlank()) {
                        coil.compose.AsyncImage(
                            model = android.net.Uri.parse(project.cardImageUri),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Folder, null, Modifier.size(24.dp), tint = if (project.cardColor.isNotBlank()) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.primary)
                    }
                }
                val hasColor = project.cardColor.isNotBlank()
                val textColor = if (hasColor) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface
                val subColor = if (hasColor) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant

                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(project.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = textColor)
                    if (project.director.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Movie, null, Modifier.size(13.dp), tint = subColor.copy(alpha = 0.6f))
                            Spacer(Modifier.width(4.dp))
                            Text(project.director, style = MaterialTheme.typography.bodySmall, color = subColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (project.castingDirector.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, null, Modifier.size(13.dp), tint = subColor.copy(alpha = 0.6f))
                            Spacer(Modifier.width(4.dp))
                            Text(project.castingDirector, style = MaterialTheme.typography.bodySmall, color = subColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                IconButton(onClick = onShare, Modifier.size(36.dp)) {
                    Icon(Icons.Default.Share, "Share", Modifier.size(18.dp), tint = subColor.copy(alpha = 0.5f))
                }
                IconButton(onClick = { cloneName = "${project.name} (Copy)"; showCloneDialog = true }, Modifier.size(36.dp)) {
                    Icon(Icons.Default.ContentCopy, "Clone", Modifier.size(18.dp), tint = subColor.copy(alpha = 0.5f))
                }
                IconButton(onClick = { showDeleteDialog = true }, Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Delete, "Delete", Modifier.size(18.dp), tint = subColor.copy(alpha = 0.5f))
                }
                if (showDragHandle) {
                    Box(dragHandleModifier.size(36.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.DragHandle, "Drag to reorder", Modifier.size(20.dp), tint = subColor.copy(alpha = 0.7f))
                    }
                }
            }

            // Badges row
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (project.startDate != null) {
                    InfoBadge(dateFormat.format(Date(project.startDate!!)), MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                }
                if (project.endDate != null) {
                    InfoBadge("→ ${dateFormat.format(Date(project.endDate!!))}", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                }
                if (team.isNotEmpty()) {
                    InfoBadge("${team.size} people", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }
    }

    if (showDeleteDialog) {
        AnimatedDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = "Delete Project",
            icon = {
                Icon(
                    Icons.Default.Delete, null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
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
        ) {
            Text("Delete \"${project.name}\"? This cannot be undone.")
        }
    }

    if (showCloneDialog) {
        AnimatedDialog(
            onDismissRequest = { showCloneDialog = false },
            title = "Clone Project",
            icon = { Icon(Icons.Default.ContentCopy, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)) },
            confirmButton = {
                Button(
                    onClick = { onClone(cloneName.trim()); showCloneDialog = false },
                    shape = RoundedCornerShape(12.dp),
                    enabled = cloneName.isNotBlank()
                ) { Text("Clone") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCloneDialog = false }, shape = RoundedCornerShape(12.dp)) { Text("Cancel") }
            }
        ) {
            Text("Create a copy of \"${project.name}\" with all its data.")
            OutlinedTextField(
                value = cloneName,
                onValueChange = { cloneName = it },
                label = { Text("New project name") },
                leadingIcon = { Icon(Icons.Default.Folder, null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }
    }
}

@Composable
private fun InfoBadge(text: String, bgColor: androidx.compose.ui.graphics.Color, textColor: androidx.compose.ui.graphics.Color) {
    Surface(shape = RoundedCornerShape(6.dp), color = bgColor) {
        Text(text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = textColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

@Composable
private fun MiniAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            shadowElevation = 4.dp
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = CircleShape
        ) {
            Icon(icon, label, Modifier.size(20.dp))
        }
    }
}
