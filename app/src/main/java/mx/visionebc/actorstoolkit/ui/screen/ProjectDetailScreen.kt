package mx.visionebc.actorstoolkit.ui.screen

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.visionebc.actorstoolkit.data.entity.*
import mx.visionebc.actorstoolkit.ui.casting.CastingViewModel
import mx.visionebc.actorstoolkit.ui.project.ProjectViewModel
import java.text.SimpleDateFormat
import java.util.*

enum class ProjectTab(val label: String, val icon: ImageVector) {
    PROJECT("Project", Icons.Default.TheaterComedy),
    AUDITIONS("Auditions", Icons.Default.CalendarMonth),
    CASTING("Casting", Icons.Default.Contacts),
    SCRIPTS("Scripts", Icons.Default.Description),
    SETTINGS("Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    projectId: Long,
    projectViewModel: ProjectViewModel,
    castingViewModel: CastingViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    settingsContent: @Composable (Modifier) -> Unit = {},
    onAddCasting: () -> Unit,
    onCastingClick: (Long) -> Unit,
    onAddAudition: () -> Unit,
    onAuditionClick: (Long) -> Unit,
    onScriptClick: (Long) -> Unit,
    onImportScript: () -> Unit = {},
    auditions: List<Audition> = emptyList(),
    projectScripts: List<ScriptInfo> = emptyList(),
    contacts: List<mx.visionebc.actorstoolkit.data.entity.ProjectContact> = emptyList(),
    onSaveContact: (mx.visionebc.actorstoolkit.data.entity.ProjectContact) -> Unit = {},
    onDeleteContact: (mx.visionebc.actorstoolkit.data.entity.ProjectContact) -> Unit = {},
    initialTab: ProjectTab = ProjectTab.PROJECT
) {
    val project by projectViewModel.selectedProject.collectAsState()
    val castings by castingViewModel.castings.collectAsState()
    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }

    LaunchedEffect(projectId) {
        projectViewModel.loadProject(projectId)
        castingViewModel.setProjectId(projectId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(project?.name ?: "Project", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                modifier = Modifier.height(56.dp)
            ) {
                ProjectTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label, modifier = Modifier.size(18.dp)) },
                        label = { Text(tab.label, style = MaterialTheme.typography.labelSmall, maxLines = 1, fontSize = 9.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer, unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant, unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (project == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }

        when (selectedTab) {
            ProjectTab.PROJECT -> ProjectEditableTab(
                project = project!!, onSave = { projectViewModel.saveProject(it) },
                auditions = auditions, onGoToAuditions = { selectedTab = ProjectTab.AUDITIONS },
                onAuditionClick = onAuditionClick,
                modifier = Modifier.padding(padding)
            )
            ProjectTab.CASTING -> CastingContactsTab(projectId = projectId, contacts = contacts, onSaveContact = onSaveContact, onDeleteContact = onDeleteContact, modifier = Modifier.padding(padding))
            ProjectTab.AUDITIONS -> AuditionsTab(auditions, onAddAudition, onAuditionClick, Modifier.padding(padding))
            ProjectTab.SCRIPTS -> ScriptsTab(projectScripts, onScriptClick, onImportScript, Modifier.padding(padding))
            ProjectTab.SETTINGS -> settingsContent(Modifier.padding(padding))
        }
    }
}

// ══════════════════════════════════════════
// ── Project Tab (Editable) ──
// ══════════════════════════════════════════

@Composable
private fun ProjectEditableTab(project: Project, onSave: (Project) -> Unit, auditions: List<Audition>, onGoToAuditions: () -> Unit, onAuditionClick: (Long) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val dateTimeFormat = remember { SimpleDateFormat("MMM d, yyyy  h:mm a", Locale.getDefault()) }

    var name by remember(project.id) { mutableStateOf(project.name) }
    var director by remember(project.id) { mutableStateOf(project.director) }
    var castingDirector by remember(project.id) { mutableStateOf(project.castingDirector) }
    var notes by remember(project.id) { mutableStateOf(project.notes) }
    var startDate by remember(project.id) { mutableStateOf(project.startDate) }
    var endDate by remember(project.id) { mutableStateOf(project.endDate) }
    var presentationDates by remember(project.id) { mutableStateOf(ProjectJsonConverter.presentationDatesFromJson(project.presentationDatesJson)) }
    var links by remember(project.id) { mutableStateOf(ProjectJsonConverter.linksFromJson(project.linksJson)) }
    var attachments by remember(project.id) { mutableStateOf(ProjectJsonConverter.attachmentsFromJson(project.attachmentsJson)) }
    var team by remember(project.id) { mutableStateOf(ProjectJsonConverter.teamFromJson(project.teamJson)) }
    var locations by remember(project.id) { mutableStateOf(ProjectJsonConverter.locationsFromJson(project.locationsJson)) }
    var cardColor by remember(project.id) { mutableStateOf(project.cardColor) }
    var cardImageUri by remember(project.id) { mutableStateOf(project.cardImageUri) }
    var saveVersion by remember { mutableStateOf(0) }
    var showTeamDialog by remember { mutableStateOf<Int?>(null) }
    var dialogRole by remember { mutableStateOf("") }
    var dialogName by remember { mutableStateOf("") }
    var showLocationDialog by remember { mutableStateOf<Int?>(null) }
    var locType by remember { mutableStateOf("") }
    var locAddress by remember { mutableStateOf("") }
    var locLabel by remember { mutableStateOf("") }
    var locLat by remember { mutableStateOf<Double?>(null) }
    var locLng by remember { mutableStateOf<Double?>(null) }
    var locSearching by remember { mutableStateOf(false) }
    var showMapPicker by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf<Int?>(null) }
    var linkDialogUrl by remember { mutableStateOf("") }
    var linkDialogLabel by remember { mutableStateOf("") }
    var linkDialogNote by remember { mutableStateOf("") }
    var showProjectDatesDialog by remember { mutableStateOf(false) }
    var showColorPickerDialog by remember { mutableStateOf(false) }
    var showPresentationDialog by remember { mutableStateOf<Int?>(null) }
    var presLabel by remember { mutableStateOf("") }
    var presDate by remember { mutableStateOf<Long?>(null) }
    var presEndDate by remember { mutableStateOf<Long?>(null) }
    var presHasTime by remember { mutableStateOf(false) }
    var presRecurrence by remember { mutableStateOf("NONE") }
    var presStartHour by remember { mutableStateOf(-1) }
    var presStartMin by remember { mutableStateOf(0) }
    var presEndHour by remember { mutableStateOf(-1) }
    var presEndMin by remember { mutableStateOf(0) }

    val dayNames = mapOf("MON" to "Monday", "TUE" to "Tuesday", "WED" to "Wednesday", "THU" to "Thursday", "FRI" to "Friday", "SAT" to "Saturday", "SUN" to "Sunday")
    val recurrenceOptions = listOf("NONE", "DAILY") + dayNames.keys.map { "WEEKLY_$it" } + dayNames.keys.map { "BIWEEKLY_$it" }

    fun formatTime(h: Int, m: Int): String {
        val amPm = if (h < 12) "AM" else "PM"
        val h12 = if (h == 0) 12 else if (h > 12) h - 12 else h
        return String.format("%d:%02d %s", h12, m, amPm)
    }

    fun recurrenceLabel(r: String): String = when {
        r == "NONE" -> "Single / Range"
        r == "DAILY" -> "Every day"
        r.startsWith("WEEKLY_") -> "Every ${dayNames[r.removePrefix("WEEKLY_")] ?: r}"
        r.startsWith("BIWEEKLY_") -> "Every other ${dayNames[r.removePrefix("BIWEEKLY_")] ?: r}"
        else -> r
    }

    fun markChanged() { saveVersion++ }

    // Auto-save with debounce (saves 800ms after last change)
    LaunchedEffect(saveVersion) {
        if (saveVersion > 0) {
            kotlinx.coroutines.delay(800)
            if (name.isNotBlank()) {
                onSave(project.copy(
                    name = name.trim(), director = director.trim(), castingDirector = castingDirector.trim(),
                    notes = notes.trim(), startDate = startDate, endDate = endDate,
                    presentationDatesJson = ProjectJsonConverter.presentationDatesToJson(presentationDates),
                    linksJson = ProjectJsonConverter.linksToJson(links),
                    attachmentsJson = ProjectJsonConverter.attachmentsToJson(attachments),
                    teamJson = ProjectJsonConverter.teamToJson(team),
                    locationsJson = ProjectJsonConverter.locationsToJson(locations),
                    cardColor = cardColor, cardImageUri = cardImageUri,
                    updatedAt = System.currentTimeMillis()
                ))
            }
        }
    }

    fun showDatePicker(initial: Long? = null, onPicked: (Long) -> Unit) {
        val cal = Calendar.getInstance()
        if (initial != null) cal.timeInMillis = initial
        DatePickerDialog(context, { _, y, m, d ->
            val picked = Calendar.getInstance(); picked.set(y, m, d, 0, 0, 0); picked.set(Calendar.MILLISECOND, 0)
            onPicked(picked.timeInMillis)
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    fun showDateTimePicker(initial: Long? = null, onPicked: (Long) -> Unit) {
        val cal = Calendar.getInstance()
        if (initial != null) cal.timeInMillis = initial
        DatePickerDialog(context, { _, y, m, d ->
            // After date, show time picker
            android.app.TimePickerDialog(context, { _, h, min ->
                val picked = Calendar.getInstance(); picked.set(y, m, d, h, min, 0); picked.set(Calendar.MILLISECOND, 0)
                onPicked(picked.timeInMillis)
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    fun showDateWithOptionalTime(initial: Long? = null, onPicked: (Long) -> Unit) {
        val cal = Calendar.getInstance()
        if (initial != null) cal.timeInMillis = initial
        DatePickerDialog(context, { _, y, m, d ->
            val picked = Calendar.getInstance(); picked.set(y, m, d, 0, 0, 0); picked.set(Calendar.MILLISECOND, 0)
            // Ask if user wants to add time
            android.app.AlertDialog.Builder(context)
                .setTitle("Add time?")
                .setPositiveButton("Yes") { _, _ ->
                    android.app.TimePickerDialog(context, { _, h, min ->
                        picked.set(Calendar.HOUR_OF_DAY, h); picked.set(Calendar.MINUTE, min)
                        onPicked(picked.timeInMillis)
                    }, 12, 0, false).show()
                }
                .setNegativeButton("No") { _, _ -> onPicked(picked.timeInMillis) }
                .show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    val cardImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            cardImageUri = uri.toString(); markChanged()
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "File"
            attachments = attachments + AttachmentEntry(uri.toString(), fileName); markChanged()
        }
    }

    fun formatDateOptionalTime(millis: Long): String {
        val cal = Calendar.getInstance(); cal.timeInMillis = millis
        return if (cal.get(Calendar.HOUR_OF_DAY) == 0 && cal.get(Calendar.MINUTE) == 0)
            dateFormat.format(Date(millis))
        else dateTimeFormat.format(Date(millis))
    }

    Column(modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // ── Card Appearance ──
            SectionLabel("Card Appearance")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Image
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.size(80.dp).clickable { cardImagePicker.launch(arrayOf("image/*")) }
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (cardImageUri.isNotBlank()) {
                            coil.compose.AsyncImage(
                                model = Uri.parse(cardImageUri),
                                contentDescription = "Card image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Image, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Image", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                // Color picker
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Card Color", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val presetColors = listOf("", "#FF7B4DFF", "#FF4B7BFF", "#FF00D4FF", "#FFFF6B6B", "#FFFF9F43", "#FF00B894", "#FFFFD32A", "#FF6C5CE7", "#FFE17055")
                    val isCustomColor = cardColor.isNotBlank() && cardColor !in presetColors
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Reset button
                        Surface(
                            onClick = { cardColor = ""; markChanged() },
                            shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant,
                            border = if (cardColor.isBlank()) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier.size(32.dp)
                        ) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.FormatColorReset, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) } }
                        // Presets
                        presetColors.drop(1).forEach { c ->
                            val bgColor = try { Color(android.graphics.Color.parseColor(c)) } catch (_: Exception) { MaterialTheme.colorScheme.surfaceVariant }
                            Surface(
                                onClick = { cardColor = c; markChanged() },
                                shape = CircleShape, color = bgColor,
                                border = if (cardColor == c) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier.size(32.dp)
                            ) { if (cardColor == c) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Check, null, Modifier.size(16.dp), tint = Color.White) } }
                        }
                        // Custom color picker circle (rainbow gradient)
                        Surface(
                            onClick = { showColorPickerDialog = true },
                            shape = CircleShape,
                            color = if (isCustomColor) try { Color(android.graphics.Color.parseColor(cardColor)) } catch (_: Exception) { Color.Gray } else Color.Gray,
                            border = if (isCustomColor) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                if (isCustomColor) Icon(Icons.Default.Check, null, Modifier.size(16.dp), tint = Color.White)
                                else Icon(Icons.Default.Palette, null, Modifier.size(16.dp), tint = Color.White)
                            }
                        }
                    }
                }
            }
            if (cardImageUri.isNotBlank()) {
                OutlinedButton(onClick = { cardImageUri = ""; markChanged() }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Close, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Remove Image")
                }
            }

            // ── Name ──
            EditField("Project Name *", name, Icons.Default.Folder) { name = it; markChanged() }

            // ── People ──
            SectionLabel("People")
            EditField("Director", director, Icons.Default.Movie) { director = it; markChanged() }
            EditField("Casting Director", castingDirector, Icons.Default.Person) { castingDirector = it; markChanged() }
            team.forEachIndexed { i, member ->
                if (member.name.isNotBlank() || member.role.isNotBlank()) {
                    TeamMemberDisplay(
                        role = member.role.ifBlank { "Team Member" }, name = member.name,
                        onEdit = { showTeamDialog = i; dialogRole = member.role; dialogName = member.name },
                        onRemove = { team = team.toMutableList().also { it.removeAt(i) }; markChanged() }
                    )
                }
            }
            AddBtn("Add Person") { showTeamDialog = -1; dialogRole = ""; dialogName = "" }

            // ── Project Dates (single line range, date only) ──
            SectionLabel("Project Dates")
            Box(Modifier.fillMaxWidth().clickable { showProjectDatesDialog = true }) {
                val rangeText = buildString {
                    if (startDate != null) append(dateFormat.format(Date(startDate!!)))
                    if (startDate != null && endDate != null) append("  -  ")
                    if (endDate != null) append(dateFormat.format(Date(endDate!!)))
                }
                OutlinedTextField(
                    value = rangeText, onValueChange = {},
                    label = { Text("Project Dates") },
                    leadingIcon = { Icon(Icons.Default.CalendarMonth, null) },
                    trailingIcon = {
                        if (startDate != null || endDate != null) {
                            IconButton(onClick = { startDate = null; endDate = null; markChanged() }, Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, "Clear", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            }
                        }
                    },
                    placeholder = { Text("Tap to set dates") },
                    readOnly = true, enabled = false, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface, disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant, disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant, disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                )
            }

            // ── Important Dates ──
            SectionLabel("Important Dates")
            presentationDates.forEachIndexed { i, entry ->
                val isRecurring = entry.recurrence != "NONE"
                val displayText = if (isRecurring) {
                    val timeStr = if (entry.startHour >= 0 && entry.endHour >= 0)
                        "${formatTime(entry.startHour, entry.startMinute)} - ${formatTime(entry.endHour, entry.endMinute)}"
                    else if (entry.startHour >= 0) formatTime(entry.startHour, entry.startMinute)
                    else ""
                    val periodStr = buildString {
                        append(dateFormat.format(Date(entry.date)))
                        if (entry.endDate != null) append("  -  ${dateFormat.format(Date(entry.endDate))}")
                    }
                    "${recurrenceLabel(entry.recurrence)} $timeStr\n$periodStr"
                } else {
                    val startText = if (entry.hasTime) dateTimeFormat.format(Date(entry.date)) else dateFormat.format(Date(entry.date))
                    if (entry.endDate != null) {
                        val endText = if (entry.hasTime) dateTimeFormat.format(Date(entry.endDate)) else dateFormat.format(Date(entry.endDate))
                        "$startText  -  $endText"
                    } else startText
                }
                val label = entry.label.ifBlank { "Important Date" }
                Box(Modifier.fillMaxWidth().clickable {
                    showPresentationDialog = i; presLabel = entry.label; presDate = entry.date; presEndDate = entry.endDate
                    presHasTime = entry.hasTime; presRecurrence = entry.recurrence
                    presStartHour = entry.startHour; presStartMin = entry.startMinute; presEndHour = entry.endHour; presEndMin = entry.endMinute
                }) {
                    OutlinedTextField(
                        value = displayText, onValueChange = {}, label = { Text(label) },
                        leadingIcon = { Icon(Icons.Default.CalendarMonth, null) },
                        trailingIcon = {
                            IconButton(onClick = { presentationDates = presentationDates.toMutableList().also { it.removeAt(i) }; markChanged() }, Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, "Remove", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            }
                        },
                        readOnly = true, enabled = false, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = false, maxLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface, disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant, disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant, disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
            AddBtn("Add Important Date") { showPresentationDialog = -1; presLabel = ""; presDate = null; presEndDate = null; presHasTime = false; presRecurrence = "NONE"; presStartHour = -1; presStartMin = 0; presEndHour = -1; presEndMin = 0 }

            // ── Audition Dates (read-only summary, + goes to Auditions tab) ──
            SectionLabel("Auditions")
            if (auditions.isNotEmpty()) {
                auditions.forEach { a ->
                    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth().clickable { onAuditionClick(a.id) }) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TheaterComedy, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(a.roleName.ifBlank { "Audition" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (a.auditionDate != null) Text(dateTimeFormat.format(Date(a.auditionDate)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                                Text(a.status, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.onTertiaryContainer)
                            }
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.ChevronRight, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        }
                    }
                }
            }
            OutlinedButton(onClick = onGoToAuditions, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Auditions/Castings")
            }

            // ── Locations ──
            SectionLabel("Locations")
            locations.forEachIndexed { i, loc ->
                val fieldLabel = loc.label.ifBlank { loc.type.ifBlank { "Location ${i + 1}" } }
                Box(Modifier.fillMaxWidth().clickable {
                    showLocationDialog = i; locType = loc.type; locAddress = loc.address; locLabel = loc.label; locLat = loc.latitude; locLng = loc.longitude
                }) {
                    OutlinedTextField(
                        value = loc.address,
                        onValueChange = {},
                        label = { Text(fieldLabel) },
                        leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (loc.latitude != null && loc.longitude != null) {
                                    IconButton(onClick = {
                                        val q = "geo:${loc.latitude},${loc.longitude}?q=${loc.latitude},${loc.longitude}(${Uri.encode(loc.label.ifBlank { loc.address })})"
                                        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(q))) } catch (_: Exception) {}
                                    }, Modifier.size(36.dp)) {
                                        Icon(Icons.Default.Map, "Open in Maps", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                IconButton(onClick = { locations = locations.toMutableList().also { it.removeAt(i) }; markChanged() }, Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Close, "Remove", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                }
                            }
                        },
                        readOnly = true,
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = false,
                        maxLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
            AddBtn("Add Location") { showLocationDialog = -1; locType = ""; locAddress = ""; locLabel = "" }

            // ── Links ──
            SectionLabel("Links")
            links.forEachIndexed { i, link ->
                if (link.url.isNotBlank() || link.label.isNotBlank() || link.note.isNotBlank()) {
                    LinkDisplay(
                        url = link.url, label = link.label.ifBlank { "Link ${i + 1}" }, note = link.note,
                        onEdit = { showLinkDialog = i; linkDialogUrl = link.url; linkDialogLabel = link.label; linkDialogNote = link.note },
                        onRemove = { links = links.toMutableList().also { it.removeAt(i) }; markChanged() }
                    )
                }
            }
            AddBtn("Add Link") { showLinkDialog = -1; linkDialogUrl = ""; linkDialogLabel = ""; linkDialogNote = "" }

            // ── Attachments ──
            SectionLabel("Attachments")
            attachments.forEachIndexed { i, att ->
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AttachFile, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(att.name.ifBlank { "File" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(att.uri, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { attachments = attachments.toMutableList().also { it.removeAt(i) }; markChanged() }, Modifier.size(32.dp)) { Icon(Icons.Default.Close, "Remove", Modifier.size(18.dp)) }
                    }
                }
            }
            AddBtn("Attach File") { filePicker.launch(arrayOf("*/*")) }

            // ── Notes ──
            SectionLabel("Notes")
            OutlinedTextField(value = notes, onValueChange = { notes = it; markChanged() }, modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp), shape = RoundedCornerShape(12.dp), maxLines = 8)

            Spacer(Modifier.height(32.dp))
        }
    }

    // ── Team member dialog ──
    if (showTeamDialog != null) {
        AlertDialog(
            onDismissRequest = { showTeamDialog = null }, shape = RoundedCornerShape(24.dp),
            title = { Text(if (showTeamDialog == -1) "Add Person" else "Edit Person") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = dialogRole, onValueChange = { dialogRole = it }, label = { Text("Role (e.g. Producer, Writer)") }, leadingIcon = { Icon(Icons.Default.Badge, null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                    OutlinedTextField(value = dialogName, onValueChange = { dialogName = it }, label = { Text("Name") }, leadingIcon = { Icon(Icons.Default.Person, null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (dialogName.isNotBlank()) {
                        val entry = TeamMemberEntry(dialogName.trim(), dialogRole.trim())
                        team = if (showTeamDialog == -1) team + entry else team.toMutableList().also { it[showTeamDialog!!] = entry }
                        markChanged()
                    }; showTeamDialog = null
                }, shape = RoundedCornerShape(12.dp), enabled = dialogName.isNotBlank()) { Text("Save") }
            },
            dismissButton = { OutlinedButton(onClick = { showTeamDialog = null }, shape = RoundedCornerShape(12.dp)) { Text("Cancel") } }
        )
    }

    // ── Link dialog ──
    if (showLinkDialog != null) {
        AlertDialog(
            onDismissRequest = { showLinkDialog = null }, shape = RoundedCornerShape(24.dp),
            title = { Text(if (showLinkDialog == -1) "Add Link" else "Edit Link") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = linkDialogLabel, onValueChange = { linkDialogLabel = it }, label = { Text("Title") }, leadingIcon = { Icon(Icons.Default.Label, null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                    OutlinedTextField(value = linkDialogUrl, onValueChange = { linkDialogUrl = it }, label = { Text("Link (URL)") }, leadingIcon = { Icon(Icons.Default.Link, null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                    OutlinedTextField(value = linkDialogNote, onValueChange = { linkDialogNote = it }, label = { Text("Note (optional)") }, leadingIcon = { Icon(Icons.Default.Notes, null) }, modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp), shape = RoundedCornerShape(12.dp), maxLines = 4)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (linkDialogUrl.isNotBlank()) {
                        val entry = LinkEntry(linkDialogUrl.trim(), linkDialogLabel.trim(), linkDialogNote.trim())
                        links = if (showLinkDialog == -1) links + entry else links.toMutableList().also { it[showLinkDialog!!] = entry }
                        markChanged()
                    }; showLinkDialog = null
                }, shape = RoundedCornerShape(12.dp), enabled = linkDialogUrl.isNotBlank()) { Text("Save") }
            },
            dismissButton = { OutlinedButton(onClick = { showLinkDialog = null }, shape = RoundedCornerShape(12.dp)) { Text("Cancel") } }
        )
    }

    // ── Location dialog ──
    if (showLocationDialog != null && !showMapPicker) {
        val scope = rememberCoroutineScope()
        AlertDialog(
            onDismissRequest = { showLocationDialog = null }, shape = RoundedCornerShape(24.dp),
            title = { Text(if (showLocationDialog == -1) "Add Location" else "Edit Location") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = locLabel, onValueChange = { locLabel = it }, label = { Text("Label (e.g. Rehearsal, Main Stage)") }, leadingIcon = { Icon(Icons.Default.Label, null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)

                    // Address with search button
                    OutlinedTextField(
                        value = locAddress, onValueChange = { locAddress = it; locLat = null; locLng = null },
                        label = { Text("Address") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                        trailingIcon = {
                            if (locAddress.isNotBlank()) {
                                IconButton(onClick = {
                                    // Geocode the address
                                    locSearching = true
                                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                        try {
                                            val geocoder = android.location.Geocoder(context, Locale.getDefault())
                                            @Suppress("DEPRECATION")
                                            val results = geocoder.getFromLocationName(locAddress, 1)
                                            if (!results.isNullOrEmpty()) {
                                                locLat = results[0].latitude
                                                locLng = results[0].longitude
                                                // Update address with formatted version if available
                                                val formatted = results[0].getAddressLine(0)
                                                if (!formatted.isNullOrBlank()) locAddress = formatted
                                            }
                                        } catch (_: Exception) {}
                                        locSearching = false
                                    }
                                }) {
                                    if (locSearching) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                    else Icon(Icons.Default.Search, "Search", Modifier.size(20.dp))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), maxLines = 3
                    )

                    OutlinedButton(
                        onClick = { showMapPicker = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Map, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (locLat != null) "Change Pin on Map" else "Pick on Map")
                    }

                    // Map preview + coordinates
                    if (locLat != null && locLng != null) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().clickable {
                                try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:$locLat,$locLng?q=$locLat,$locLng(${Uri.encode(locLabel.ifBlank { locAddress })})"))) } catch (_: Exception) {}
                            }
                        ) {
                            Column {
                                coil.compose.AsyncImage(
                                    model = "https://staticmap.openstreetmap.de/staticmap.php?center=${locLat},${locLng}&zoom=15&size=400x200&markers=${locLat},${locLng},red-pushpin",
                                    contentDescription = "Map preview",
                                    modifier = Modifier.fillMaxWidth().height(120.dp),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Map, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Tap to open in Maps", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.weight(1f))
                                    Text("${String.format("%.4f", locLat)}, ${String.format("%.4f", locLng)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    } else if (locAddress.isNotBlank()) {
                        Text("Tip: Tap the search icon to geocode, or use \"Pick on Map\" to drop a pin.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                val hasCoords = locLat != null && locLng != null
                Button(onClick = {
                    if (locAddress.isNotBlank() || hasCoords) {
                        val addr = if (locAddress.isNotBlank()) locAddress.trim()
                            else String.format("%.5f, %.5f", locLat, locLng)
                        val entry = LocationEntry(type = locType, address = addr, label = locLabel.trim(), latitude = locLat, longitude = locLng)
                        locations = if (showLocationDialog == -1) locations + entry else locations.toMutableList().also { it[showLocationDialog!!] = entry }
                        markChanged()
                    }; showLocationDialog = null
                }, shape = RoundedCornerShape(12.dp), enabled = locAddress.isNotBlank() || hasCoords) { Text("Save") }
            },
            dismissButton = { OutlinedButton(onClick = { showLocationDialog = null }, shape = RoundedCornerShape(12.dp)) { Text("Cancel") } }
        )
    }

    if (showMapPicker) {
        MapPickerDialog(
            initialLat = locLat, initialLng = locLng, initialAddress = locAddress,
            onDismiss = { showMapPicker = false },
            onConfirm = { lat, lng, addr ->
                locLat = lat; locLng = lng
                if (addr.isNotBlank()) locAddress = addr
                showMapPicker = false
            }
        )
    }

    // ── Project dates dialog ──
    if (showProjectDatesDialog) {
        AlertDialog(
            onDismissRequest = { showProjectDatesDialog = false }, shape = RoundedCornerShape(24.dp),
            title = { Text("Project Dates") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Start date
                    Box(Modifier.fillMaxWidth().clickable {
                        showDatePicker(startDate) { startDate = it; markChanged() }
                    }) {
                        OutlinedTextField(
                            value = if (startDate != null) dateFormat.format(Date(startDate!!)) else "",
                            onValueChange = {}, label = { Text("Start Date") },
                            leadingIcon = { Icon(Icons.Default.CalendarMonth, null) },
                            placeholder = { Text("Tap to pick") },
                            readOnly = true, enabled = false, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface, disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant, disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        )
                    }
                    // End date
                    Box(Modifier.fillMaxWidth().clickable {
                        showDatePicker(endDate) { endDate = it; markChanged() }
                    }) {
                        OutlinedTextField(
                            value = if (endDate != null) dateFormat.format(Date(endDate!!)) else "",
                            onValueChange = {}, label = { Text("End Date") },
                            leadingIcon = { Icon(Icons.Default.CalendarMonth, null) },
                            placeholder = { Text("Tap to pick") },
                            readOnly = true, enabled = false, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface, disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant, disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            },
            confirmButton = { Button(onClick = { showProjectDatesDialog = false }, shape = RoundedCornerShape(12.dp)) { Text("Done") } },
            dismissButton = null
        )
    }

    // ── Important date dialog ──
    if (showPresentationDialog != null) {
        val disabledColors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = MaterialTheme.colorScheme.onSurface, disabledBorderColor = MaterialTheme.colorScheme.outline,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant, disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        var showRecurrenceMenu by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showPresentationDialog = null }, shape = RoundedCornerShape(24.dp),
            title = { Text(if (showPresentationDialog == -1) "Add Important Date" else "Edit Important Date") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Title
                    OutlinedTextField(
                        value = presLabel, onValueChange = { presLabel = it },
                        label = { Text("Title (e.g. Rehearsal, Opening Night)") },
                        leadingIcon = { Icon(Icons.Default.Label, null) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true
                    )

                    // Recurrence selector
                    Box {
                        OutlinedButton(onClick = { showRecurrenceMenu = true }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Repeat, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(recurrenceLabel(presRecurrence))
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, null, Modifier.size(20.dp))
                        }
                        DropdownMenu(expanded = showRecurrenceMenu, onDismissRequest = { showRecurrenceMenu = false }) {
                            DropdownMenuItem(text = { Text("Single / Range") }, onClick = { presRecurrence = "NONE"; showRecurrenceMenu = false },
                                leadingIcon = { Icon(Icons.Default.Event, null, Modifier.size(18.dp)) })
                            DropdownMenuItem(text = { Text("Every day") }, onClick = { presRecurrence = "DAILY"; showRecurrenceMenu = false },
                                leadingIcon = { Icon(Icons.Default.Repeat, null, Modifier.size(18.dp)) })
                            HorizontalDivider()
                            dayNames.forEach { (key, name) ->
                                DropdownMenuItem(text = { Text("Every $name") }, onClick = { presRecurrence = "WEEKLY_$key"; showRecurrenceMenu = false },
                                    leadingIcon = { Icon(Icons.Default.DateRange, null, Modifier.size(18.dp)) })
                            }
                            HorizontalDivider()
                            dayNames.forEach { (key, name) ->
                                DropdownMenuItem(text = { Text("Every other $name") }, onClick = { presRecurrence = "BIWEEKLY_$key"; showRecurrenceMenu = false },
                                    leadingIcon = { Icon(Icons.Default.DateRange, null, Modifier.size(18.dp)) })
                            }
                        }
                    }

                    val isRecurring = presRecurrence != "NONE"

                    if (isRecurring) {
                        // Recurring: session time (start hour - end hour)
                        Text("Session Time", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.weight(1f).clickable {
                                android.app.TimePickerDialog(context, { _, h, m -> presStartHour = h; presStartMin = m }, if (presStartHour >= 0) presStartHour else 9, presStartMin, false).show()
                            }) {
                                OutlinedTextField(
                                    value = if (presStartHour >= 0) formatTime(presStartHour, presStartMin) else "",
                                    onValueChange = {}, label = { Text("Start") }, placeholder = { Text("From") },
                                    readOnly = true, enabled = false, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true, colors = disabledColors
                                )
                            }
                            Box(Modifier.weight(1f).clickable {
                                android.app.TimePickerDialog(context, { _, h, m -> presEndHour = h; presEndMin = m }, if (presEndHour >= 0) presEndHour else 17, presEndMin, false).show()
                            }) {
                                OutlinedTextField(
                                    value = if (presEndHour >= 0) formatTime(presEndHour, presEndMin) else "",
                                    onValueChange = {}, label = { Text("End") }, placeholder = { Text("To") },
                                    readOnly = true, enabled = false, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true, colors = disabledColors
                                )
                            }
                        }

                        // Recurring: period (from date - until date)
                        Text("Period", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Box(Modifier.fillMaxWidth().clickable { showDatePicker(presDate) { d -> presDate = d } }) {
                            OutlinedTextField(
                                value = if (presDate != null) dateFormat.format(Date(presDate!!)) else "",
                                onValueChange = {}, label = { Text("From") }, placeholder = { Text("Start date") },
                                leadingIcon = { Icon(Icons.Default.CalendarMonth, null) },
                                readOnly = true, enabled = false, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true, colors = disabledColors
                            )
                        }
                        Box(Modifier.fillMaxWidth().clickable { showDatePicker(presEndDate) { d -> presEndDate = d } }) {
                            OutlinedTextField(
                                value = if (presEndDate != null) dateFormat.format(Date(presEndDate!!)) else "",
                                onValueChange = {}, label = { Text("Until") }, placeholder = { Text("End date") },
                                leadingIcon = { Icon(Icons.Default.CalendarMonth, null) },
                                readOnly = true, enabled = false, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true, colors = disabledColors
                            )
                        }
                    } else {
                        // Single/Range: date with optional time
                        Box(Modifier.fillMaxWidth().clickable { showDateWithOptionalTime(presDate) { d -> presDate = d; presHasTime = true } }) {
                            OutlinedTextField(
                                value = if (presDate != null) { if (presHasTime) dateTimeFormat.format(Date(presDate!!)) else dateFormat.format(Date(presDate!!)) } else "",
                                onValueChange = {}, label = { Text("Date") }, placeholder = { Text("Tap to pick") },
                                leadingIcon = { Icon(Icons.Default.CalendarMonth, null) },
                                readOnly = true, enabled = false, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true, colors = disabledColors
                            )
                        }
                        Box(Modifier.fillMaxWidth().clickable { showDateWithOptionalTime(presEndDate) { d -> presEndDate = d } }) {
                            OutlinedTextField(
                                value = if (presEndDate != null) { if (presHasTime) dateTimeFormat.format(Date(presEndDate!!)) else dateFormat.format(Date(presEndDate!!)) } else "",
                                onValueChange = {}, label = { Text("End Date (optional)") }, placeholder = { Text("For date range") },
                                leadingIcon = { Icon(Icons.Default.CalendarMonth, null) },
                                trailingIcon = { if (presEndDate != null) IconButton(onClick = { presEndDate = null }, Modifier.size(32.dp)) { Icon(Icons.Default.Close, "Clear", Modifier.size(18.dp)) } },
                                readOnly = true, enabled = false, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true, colors = disabledColors
                            )
                        }
                    }
                }
            },
            confirmButton = {
                val isValid = presDate != null && (presRecurrence == "NONE" || (presEndDate != null && presStartHour >= 0))
                Button(onClick = {
                    if (presDate != null) {
                        val entry = ImportantDateEntry(date = presDate!!, endDate = presEndDate, label = presLabel.trim(), hasTime = presHasTime, recurrence = presRecurrence, startHour = presStartHour, startMinute = presStartMin, endHour = presEndHour, endMinute = presEndMin)
                        presentationDates = if (showPresentationDialog == -1) presentationDates + entry else presentationDates.toMutableList().also { it[showPresentationDialog!!] = entry }
                        markChanged()
                    }; showPresentationDialog = null
                }, shape = RoundedCornerShape(12.dp), enabled = isValid) { Text("Save") }
            },
            dismissButton = { OutlinedButton(onClick = { showPresentationDialog = null }, shape = RoundedCornerShape(12.dp)) { Text("Cancel") } }
        )
    }
    // ── Color picker dialog ──
    if (showColorPickerDialog) {
        val palette = listOf(
            // Reds
            listOf("#FFFFCDD2", "#FFEF9A9A", "#FFE57373", "#FFEF5350", "#FFF44336", "#FFE53935", "#FFD32F2F", "#FFC62828", "#FFB71C1C"),
            // Pinks
            listOf("#FFF8BBD0", "#FFF48FB1", "#FFF06292", "#FFEC407A", "#FFE91E63", "#FFD81B60", "#FFC2185B", "#FFAD1457", "#FF880E4F"),
            // Purples
            listOf("#FFE1BEE7", "#FFCE93D8", "#FFBA68C8", "#FFAB47BC", "#FF9C27B0", "#FF8E24AA", "#FF7B1FA2", "#FF6A1B9A", "#FF4A148C"),
            // Deep Purples
            listOf("#FFD1C4E9", "#FFB39DDB", "#FF9575CD", "#FF7E57C2", "#FF673AB7", "#FF5E35B1", "#FF512DA8", "#FF4527A0", "#FF311B92"),
            // Blues
            listOf("#FFBBDEFB", "#FF90CAF9", "#FF64B5F6", "#FF42A5F5", "#FF2196F3", "#FF1E88E5", "#FF1976D2", "#FF1565C0", "#FF0D47A1"),
            // Cyans
            listOf("#FFB2EBF2", "#FF80DEEA", "#FF4DD0E1", "#FF26C6DA", "#FF00BCD4", "#FF00ACC1", "#FF0097A7", "#FF00838F", "#FF006064"),
            // Teals
            listOf("#FFB2DFDB", "#FF80CBC4", "#FF4DB6AC", "#FF26A69A", "#FF009688", "#FF00897B", "#FF00796B", "#FF00695C", "#FF004D40"),
            // Greens
            listOf("#FFC8E6C9", "#FFA5D6A7", "#FF81C784", "#FF66BB6A", "#FF4CAF50", "#FF43A047", "#FF388E3C", "#FF2E7D32", "#FF1B5E20"),
            // Yellows
            listOf("#FFFFF9C4", "#FFFFF59D", "#FFFFF176", "#FFFFEE58", "#FFFFEB3B", "#FFFDD835", "#FFFBC02D", "#FFF9A825", "#FFF57F17"),
            // Oranges
            listOf("#FFFFE0B2", "#FFFFCC80", "#FFFFB74D", "#FFFFA726", "#FFFF9800", "#FFFB8C00", "#FFF57C00", "#FFEF6C00", "#FFE65100"),
            // Browns
            listOf("#FFD7CCC8", "#FFBCAAA4", "#FFA1887F", "#FF8D6E63", "#FF795548", "#FF6D4C41", "#FF5D4037", "#FF4E342E", "#FF3E2723"),
            // Grays
            listOf("#FFF5F5F5", "#FFEEEEEE", "#FFE0E0E0", "#FFBDBDBD", "#FF9E9E9E", "#FF757575", "#FF616161", "#FF424242", "#FF212121")
        )
        var selectedInPicker by remember { mutableStateOf(cardColor) }

        AlertDialog(
            onDismissRequest = { showColorPickerDialog = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Pick a Color") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Preview bar
                    if (selectedInPicker.isNotBlank()) {
                        val previewC = try { Color(android.graphics.Color.parseColor(selectedInPicker)) } catch (_: Exception) { Color.Gray }
                        Box(Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(12.dp)).background(previewC))
                    }
                    // Color grid
                    palette.forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            row.forEach { hex ->
                                val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color.Gray }
                                val isSelected = selectedInPicker == hex
                                Box(
                                    Modifier.size(28.dp).clip(CircleShape).background(c)
                                        .then(if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                                        .clickable { selectedInPicker = hex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) Icon(Icons.Default.Check, null, Modifier.size(14.dp), tint = if (row.indexOf(hex) < 4) Color.Black else Color.White)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { cardColor = selectedInPicker; markChanged(); showColorPickerDialog = false }, shape = RoundedCornerShape(12.dp)) { Text("Apply") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showColorPickerDialog = false }, shape = RoundedCornerShape(12.dp)) { Text("Cancel") }
            }
        )
    }
}

// ── Form helpers ──

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun EditField(label: String, value: String, icon: ImageVector, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, leadingIcon = { Icon(icon, null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
}

@Composable
private fun TeamMemberDisplay(role: String, name: String, onEdit: () -> Unit, onRemove: () -> Unit) {
    Box(Modifier.fillMaxWidth().clickable { onEdit() }) {
        OutlinedTextField(
            value = name,
            onValueChange = {},
            label = { Text(role) },
            leadingIcon = { Icon(Icons.Default.Person, null) },
            trailingIcon = {
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, "Remove", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                }
            },
            readOnly = true,
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
private fun LinkDisplay(url: String, label: String, note: String, onEdit: () -> Unit, onRemove: () -> Unit) {
    Box(Modifier.fillMaxWidth().clickable { onEdit() }) {
        OutlinedTextField(
            value = url,
            onValueChange = {},
            label = { Text(label) },
            leadingIcon = { Icon(Icons.Default.Link, null) },
            supportingText = if (note.isNotBlank()) { { Text(note) } } else null,
            trailingIcon = {
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, "Remove", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                }
            },
            readOnly = true,
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
private fun SmallEditField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), singleLine = true, textStyle = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun DateDisplayField(label: String, date: Long?, formatFn: (Long) -> String, onTap: () -> Unit, onClear: () -> Unit) {
    Box(Modifier.fillMaxWidth().clickable { onTap() }) {
        OutlinedTextField(
            value = if (date != null) formatFn(date) else "",
            onValueChange = {},
            label = { Text(label) },
            leadingIcon = { Icon(Icons.Default.CalendarMonth, null) },
            trailingIcon = {
                if (date != null) {
                    IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, "Clear", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                }
            },
            placeholder = { Text("Tap to set") },
            readOnly = true,
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun DateEntryCard(date: String, fields: List<Pair<String, String>>, onFieldChange: (Int, String) -> Unit, onRemove: () -> Unit) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp)); Text(date, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                IconButton(onClick = onRemove, Modifier.size(32.dp)) { Icon(Icons.Default.Close, "Remove", Modifier.size(18.dp)) }
            }
            fields.forEachIndexed { idx, (label, value) -> SmallEditField(label, value) { onFieldChange(idx, it) } }
        }
    }
}

@Composable
private fun AddBtn(text: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(text)
    }
}

// ══════════════════════════════════════════
// ── Other Tabs ──
// ══════════════════════════════════════════

@Composable
private fun CastingsTab(castings: List<Casting>, onAdd: () -> Unit, onClick: (Long) -> Unit, onDelete: (Casting) -> Unit, modifier: Modifier = Modifier) {
    if (castings.isEmpty()) { EmptyTab(Icons.Default.Groups, "No castings yet", "Add castings to organize characters and scripts", "Add Casting", onAdd, modifier); return }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { FilledTonalButton(onClick = onAdd, shape = RoundedCornerShape(12.dp)) { Icon(Icons.Default.Add, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Add Casting") } } }
        items(castings, key = { it.id }) { c -> CastingCard(c, { onClick(c.id) }, { onDelete(c) }) }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun AuditionsTab(auditions: List<Audition>, onAdd: () -> Unit, onClick: (Long) -> Unit, modifier: Modifier = Modifier) {
    if (auditions.isEmpty()) { EmptyTab(Icons.Default.TheaterComedy, "No auditions yet", "Track audition dates and submissions", "Add Audition", onAdd, modifier); return }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { FilledTonalButton(onClick = onAdd, shape = RoundedCornerShape(12.dp)) { Icon(Icons.Default.Add, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Add Audition") } } }
        items(auditions, key = { it.id }) { a -> AuditionMiniCard(a) { onClick(a.id) } }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun ScriptsTab(scripts: List<ScriptInfo>, onClick: (Long) -> Unit, onImport: () -> Unit, modifier: Modifier = Modifier) {
    val df = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    if (scripts.isEmpty()) { EmptyTab(Icons.Default.Description, "No scripts yet", "Import scripts to practice and rehearse", "Import Script", onImport, modifier); return }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                FilledTonalButton(onClick = onImport, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.NoteAdd, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Import Script")
                }
            }
        }
        items(scripts, key = { it.id }) { s ->
            Card(Modifier.fillMaxWidth().clickable { onClick(s.id) }, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) { Icon(Icons.Default.Description, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) { Text(s.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text(s.fileType.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(df.format(Date(s.updatedAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                    Icon(Icons.Default.ChevronRight, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                }
            }
        }
    }
}

// ── Casting / Contacts Tab ──

@Composable
private fun CastingContactsTab(
    projectId: Long,
    contacts: List<mx.visionebc.actorstoolkit.data.entity.ProjectContact>,
    onSaveContact: (mx.visionebc.actorstoolkit.data.entity.ProjectContact) -> Unit,
    onDeleteContact: (mx.visionebc.actorstoolkit.data.entity.ProjectContact) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var editingContact by remember { mutableStateOf<mx.visionebc.actorstoolkit.data.entity.ProjectContact?>(null) }
    var dName by remember { mutableStateOf("") }
    var dPhone by remember { mutableStateOf("") }
    var dEmail by remember { mutableStateOf("") }
    var dRoles by remember { mutableStateOf(listOf<String>()) }
    var showDeleteConfirm by remember { mutableStateOf<mx.visionebc.actorstoolkit.data.entity.ProjectContact?>(null) }
    var dFromPhone by remember { mutableStateOf(false) }
    var sendLinesContact by remember { mutableStateOf<mx.visionebc.actorstoolkit.data.entity.ProjectContact?>(null) }
    val sendScope = rememberCoroutineScope()

    fun openAdd() { editingContact = null; dName = ""; dPhone = ""; dEmail = ""; dRoles = emptyList(); dFromPhone = false; showDialog = true }
    fun openEdit(c: mx.visionebc.actorstoolkit.data.entity.ProjectContact) { editingContact = c; dName = c.name; dPhone = c.phone; dEmail = c.email; dRoles = c.getRoles(); dFromPhone = false; showDialog = true }

    // Phone contact picker with permission
    val contactPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        if (uri != null) {
            try {
                editingContact = null; dName = ""; dPhone = ""; dEmail = ""; dRoles = emptyList()
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIdx = it.getColumnIndex(android.provider.ContactsContract.Contacts.DISPLAY_NAME)
                        val idIdx = it.getColumnIndex(android.provider.ContactsContract.Contacts._ID)
                        if (nameIdx >= 0) dName = it.getString(nameIdx) ?: ""
                        val contactId = if (idIdx >= 0) it.getString(idIdx) else null
                        if (contactId != null) {
                            // Get phone
                            val phoneCursor = context.contentResolver.query(
                                android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                                "${android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?", arrayOf(contactId), null
                            )
                            phoneCursor?.use { pc -> if (pc.moveToFirst()) { val pi = pc.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER); if (pi >= 0) dPhone = pc.getString(pi) ?: "" } }
                            // Get email
                            val emailCursor = context.contentResolver.query(
                                android.provider.ContactsContract.CommonDataKinds.Email.CONTENT_URI, null,
                                "${android.provider.ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?", arrayOf(contactId), null
                            )
                            emailCursor?.use { ec -> if (ec.moveToFirst()) { val ei = ec.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Email.ADDRESS); if (ei >= 0) dEmail = ec.getString(ei) ?: "" } }
                        }
                    }
                }
                dFromPhone = true
                showDialog = true
            } catch (_: Exception) {}
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) contactPicker.launch(null)
    }

    fun launchContactPicker() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            contactPicker.launch(null)
        } else {
            permissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
        }
    }

    if (contacts.isEmpty()) {
        EmptyTab(Icons.Default.Contacts, "No contacts yet", "Add people involved in this project", "Add Contact", { openAdd() }, modifier)
    } else {
        LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(onClick = { editingContact = null; dName = ""; dPhone = ""; dEmail = ""; dRoles = emptyList(); dFromPhone = false; launchContactPicker() }, shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.ContactPhone, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("From Contacts")
                    }
                    FilledTonalButton(onClick = { openAdd() }, shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.PersonAdd, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Add")
                    }
                }
            }
            items(contacts, key = { it.id }) { contact ->
                ContactCard(contact, onEdit = { openEdit(contact) }, onDelete = { showDeleteConfirm = contact },
                    onCall = { if (contact.phone.isNotBlank()) try { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}"))) } catch (_: Exception) {} },
                    onEmail = { if (contact.email.isNotBlank()) try { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${contact.email}"))) } catch (_: Exception) {} },
                    onSendLines = { sendLinesContact = contact }
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    // Send Lines dialog (contact is pre-selected)
    sendLinesContact?.let { contact ->
        val db = remember { mx.visionebc.actorstoolkit.ActorsToolkitApp.instance.database }
        var scripts by remember { mutableStateOf<List<mx.visionebc.actorstoolkit.data.entity.Script>>(emptyList()) }
        var selectedScriptId by remember { mutableStateOf<Long?>(null) }
        var characters by remember { mutableStateOf<List<String>>(emptyList()) }
        var selectedCharacter by remember { mutableStateOf("") }

        LaunchedEffect(projectId) {
            scripts = db.scriptDao().getByProjectSync(projectId)
            if (scripts.size == 1) selectedScriptId = scripts.first().id
        }
        LaunchedEffect(selectedScriptId) {
            val sid = selectedScriptId
            characters = if (sid != null) {
                db.audioRecordingDao().getRecordingsForScriptSync(sid)
                    .map { it.characterName }.filter { it.isNotBlank() }.distinct()
            } else emptyList()
            if (characters.size == 1) selectedCharacter = characters.first()
        }

        AnimatedDialog(
            onDismissRequest = { sendLinesContact = null },
            title = "Send Lines to ${contact.name}",
            icon = { Icon(Icons.Default.RecordVoiceOver, null, tint = MaterialTheme.colorScheme.primary) },
            confirmButton = {
                Button(
                    onClick = {
                        val sid = selectedScriptId ?: return@Button
                        val ch = selectedCharacter
                        val pid = projectId
                        val contactName = contact.name
                        val contactEmail = contact.email
                        sendLinesContact = null
                        sendScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            mx.visionebc.actorstoolkit.util.DatabaseBackup.sendLinesToContact(
                                context, pid, sid, ch, contactName, contactEmail
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    enabled = selectedScriptId != null && selectedCharacter.isNotBlank()
                ) {
                    Icon(Icons.Default.Send, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Send via…")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { sendLinesContact = null }, shape = RoundedCornerShape(12.dp)) { Text("Cancel") }
            }
        ) {
            Text(
                "Pick the script and the character whose recordings you want to send.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (scripts.isEmpty()) {
                Text("This project has no scripts yet.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                return@AnimatedDialog
            }
            Text("Script", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            scripts.forEach { s ->
                Row(
                    Modifier.fillMaxWidth().clickable { selectedScriptId = s.id; selectedCharacter = "" }.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = selectedScriptId == s.id, onClick = { selectedScriptId = s.id; selectedCharacter = "" })
                    Text(s.title, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (selectedScriptId != null) {
                Text("Character (with recordings)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                if (characters.isEmpty()) {
                    Text(
                        "No recordings yet for this script. Record your lines first.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    characters.forEach { ch ->
                        Row(
                            Modifier.fillMaxWidth().clickable { selectedCharacter = ch }.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedCharacter == ch, onClick = { selectedCharacter = ch })
                            Text(ch, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }

    // Add/Edit dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false }, shape = RoundedCornerShape(24.dp),
            title = { Text(if (editingContact == null) "Add Contact" else "Edit Contact") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Import from contacts button (only in add mode, not already imported)
                    if (editingContact == null && !dFromPhone) {
                        OutlinedButton(onClick = { launchContactPicker() }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.ContactPhone, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Import from Phone Contacts")
                        }
                    }
                    OutlinedTextField(value = dName, onValueChange = { dName = it }, label = { Text("Name") }, leadingIcon = { Icon(Icons.Default.Person, null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                    OutlinedTextField(value = dPhone, onValueChange = { dPhone = it }, label = { Text("Phone") }, leadingIcon = { Icon(Icons.Default.Phone, null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                    OutlinedTextField(value = dEmail, onValueChange = { dEmail = it }, label = { Text("Email") }, leadingIcon = { Icon(Icons.Default.Email, null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)

                    // Roles
                    Text("Roles", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    dRoles.forEachIndexed { i, role ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(value = role, onValueChange = { v -> dRoles = dRoles.toMutableList().also { it[i] = v } }, placeholder = { Text("e.g. Director, Producer") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), singleLine = true, textStyle = MaterialTheme.typography.bodyMedium)
                            IconButton(onClick = { dRoles = dRoles.toMutableList().also { it.removeAt(i) } }, Modifier.size(32.dp)) { Icon(Icons.Default.Close, "Remove", Modifier.size(18.dp)) }
                        }
                    }
                    OutlinedButton(onClick = { dRoles = dRoles + "" }, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Add Role")
                    }

                    // Save to phone contacts option (only when NOT imported from phone)
                    if (editingContact == null && dName.isNotBlank() && !dFromPhone) {
                        OutlinedButton(onClick = {
                            val intent = Intent(android.provider.ContactsContract.Intents.Insert.ACTION).apply {
                                type = android.provider.ContactsContract.RawContacts.CONTENT_TYPE
                                putExtra(android.provider.ContactsContract.Intents.Insert.NAME, dName)
                                if (dPhone.isNotBlank()) putExtra(android.provider.ContactsContract.Intents.Insert.PHONE, dPhone)
                                if (dEmail.isNotBlank()) putExtra(android.provider.ContactsContract.Intents.Insert.EMAIL, dEmail)
                            }
                            try { context.startActivity(intent) } catch (_: Exception) {}
                        }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.PersonAdd, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Also save to Phone Contacts")
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (dName.isNotBlank()) {
                        val contact = if (editingContact != null) {
                            editingContact!!.copy(name = dName.trim(), phone = dPhone.trim(), email = dEmail.trim(), rolesJson = mx.visionebc.actorstoolkit.data.entity.ProjectContact.rolesToJson(dRoles), updatedAt = System.currentTimeMillis())
                        } else {
                            mx.visionebc.actorstoolkit.data.entity.ProjectContact(projectId = projectId, name = dName.trim(), phone = dPhone.trim(), email = dEmail.trim(), rolesJson = mx.visionebc.actorstoolkit.data.entity.ProjectContact.rolesToJson(dRoles))
                        }
                        onSaveContact(contact)
                    }; showDialog = false
                }, shape = RoundedCornerShape(12.dp), enabled = dName.isNotBlank()) { Text("Save") }
            },
            dismissButton = { OutlinedButton(onClick = { showDialog = false }, shape = RoundedCornerShape(12.dp)) { Text("Cancel") } }
        )
    }

    // Delete confirmation
    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null }, shape = RoundedCornerShape(24.dp),
            icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp)) },
            title = { Text("Delete Contact") }, text = { Text("Remove \"${showDeleteConfirm!!.name}\" from this project?") },
            confirmButton = { Button(onClick = { onDeleteContact(showDeleteConfirm!!); showDeleteConfirm = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), shape = RoundedCornerShape(12.dp)) { Text("Delete") } },
            dismissButton = { OutlinedButton(onClick = { showDeleteConfirm = null }, shape = RoundedCornerShape(12.dp)) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ContactCard(
    contact: mx.visionebc.actorstoolkit.data.entity.ProjectContact,
    onEdit: () -> Unit, onDelete: () -> Unit, onCall: () -> Unit, onEmail: () -> Unit,
    onSendLines: () -> Unit = {}
) {
    val roles = remember(contact.rolesJson) { contact.getRoles() }
    Card(Modifier.fillMaxWidth().clickable(onClick = onEdit), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar
                Box(Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    Text(contact.name.take(2).uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(contact.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (roles.isNotEmpty()) {
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            roles.forEach { role ->
                                Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                                    Text(role, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                    }
                }
                IconButton(onClick = onSendLines, Modifier.size(36.dp)) {
                    Icon(Icons.Default.RecordVoiceOver, "Send Lines", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete, Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Delete, "Delete", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                }
            }
            // Phone + Email action row
            if (contact.phone.isNotBlank() || contact.email.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (contact.phone.isNotBlank()) {
                        OutlinedButton(onClick = onCall, shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                            Icon(Icons.Default.Phone, null, Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text(contact.phone, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (contact.email.isNotBlank()) {
                        OutlinedButton(onClick = onEmail, shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                            Icon(Icons.Default.Email, null, Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text(contact.email, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

// ── Shared ──

@Composable
private fun EmptyTab(icon: ImageVector, title: String, subtitle: String, btnText: String? = null, onAction: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(48.dp)) {
            Icon(icon, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(16.dp)); Text(title, style = MaterialTheme.typography.headlineSmall); Spacer(Modifier.height(8.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            if (btnText != null && onAction != null) { Spacer(Modifier.height(24.dp)); FilledTonalButton(onClick = onAction, shape = RoundedCornerShape(12.dp)) { Icon(Icons.Default.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(btnText) } }
        }
    }
}

@Composable
private fun CastingCard(casting: Casting, onClick: () -> Unit, onDelete: () -> Unit) {
    var showDel by remember { mutableStateOf(false) }
    val df = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) { Icon(Icons.Default.Groups, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.secondary) }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) { Text(casting.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis); if (casting.notes.isNotBlank()) Text(casting.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(df.format(Date(casting.updatedAt)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) }
            Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            IconButton(onClick = { showDel = true }, Modifier.size(40.dp)) { Icon(Icons.Outlined.Delete, "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) }
        }
    }
    if (showDel) { AlertDialog(onDismissRequest = { showDel = false }, shape = RoundedCornerShape(24.dp), icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp)) }, title = { Text("Delete Casting") }, text = { Text("Delete \"${casting.name}\"? All characters within will also be deleted.") }, confirmButton = { Button(onClick = { onDelete(); showDel = false }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), shape = RoundedCornerShape(12.dp)) { Text("Delete") } }, dismissButton = { OutlinedButton(onClick = { showDel = false }, shape = RoundedCornerShape(12.dp)) { Text("Cancel") } }) }
}

@Composable
private fun AuditionMiniCard(audition: Audition, onClick: () -> Unit) {
    val df = remember { SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault()) }
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.TheaterComedy, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { Text(audition.projectName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis); if (audition.roleName.isNotBlank()) Text(audition.roleName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); if (audition.auditionDate != null) Text(df.format(Date(audition.auditionDate)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.tertiaryContainer) { Text(audition.status, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.onTertiaryContainer) }
            Spacer(Modifier.width(4.dp)); Icon(Icons.Default.ChevronRight, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        }
    }
}
