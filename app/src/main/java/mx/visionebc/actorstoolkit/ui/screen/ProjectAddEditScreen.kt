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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mx.visionebc.actorstoolkit.data.entity.*
import mx.visionebc.actorstoolkit.ui.project.ProjectViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectAddEditScreen(
    projectId: Long?,
    viewModel: ProjectViewModel,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val isEditing = projectId != null && projectId != 0L
    val existingProject by viewModel.selectedProject.collectAsState()
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val dateTimeFormat = remember { SimpleDateFormat("MMM d, yyyy  h:mm a", Locale.getDefault()) }

    // Form state
    var name by remember { mutableStateOf("") }
    var director by remember { mutableStateOf("") }
    var castingDirector by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }
    var presentationDates by remember { mutableStateOf(listOf<ImportantDateEntry>()) }
    var links by remember { mutableStateOf(listOf<LinkEntry>()) }
    var attachments by remember { mutableStateOf(listOf<AttachmentEntry>()) }
    var team by remember { mutableStateOf(listOf<TeamMemberEntry>()) }
    var locations by remember { mutableStateOf(listOf<LocationEntry>()) }
    var cardColor by remember { mutableStateOf("") }
    var cardImageUri by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }

    // Dialog states
    var showTeamDialog by remember { mutableStateOf<Int?>(null) }
    var dialogRole by remember { mutableStateOf("") }
    var dialogName by remember { mutableStateOf("") }
    var showLinkDialog by remember { mutableStateOf<Int?>(null) }
    var linkDialogUrl by remember { mutableStateOf("") }
    var linkDialogLabel by remember { mutableStateOf("") }
    var linkDialogNote by remember { mutableStateOf("") }
    var showLocationDialog by remember { mutableStateOf<Int?>(null) }
    var locType by remember { mutableStateOf("") }
    var locAddress by remember { mutableStateOf("") }
    var locLabel by remember { mutableStateOf("") }
    var locLat by remember { mutableStateOf<Double?>(null) }
    var locLng by remember { mutableStateOf<Double?>(null) }
    var locSearching by remember { mutableStateOf(false) }
    var showMapPicker by remember { mutableStateOf(false) }
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

    fun formatDateOptionalTime(millis: Long): String {
        val cal = Calendar.getInstance(); cal.timeInMillis = millis
        return if (cal.get(Calendar.HOUR_OF_DAY) == 0 && cal.get(Calendar.MINUTE) == 0)
            dateFormat.format(Date(millis))
        else dateTimeFormat.format(Date(millis))
    }

    LaunchedEffect(projectId) {
        if (isEditing) viewModel.loadProject(projectId!!)
    }

    LaunchedEffect(existingProject) {
        if (isEditing && existingProject != null && !loaded) {
            val p = existingProject!!
            name = p.name
            director = p.director
            castingDirector = p.castingDirector
            notes = p.notes
            startDate = p.startDate
            endDate = p.endDate
            presentationDates = ProjectJsonConverter.presentationDatesFromJson(p.presentationDatesJson)
            links = ProjectJsonConverter.linksFromJson(p.linksJson)
            attachments = ProjectJsonConverter.attachmentsFromJson(p.attachmentsJson)
            team = ProjectJsonConverter.teamFromJson(p.teamJson)
            locations = ProjectJsonConverter.locationsFromJson(p.locationsJson)
            cardColor = p.cardColor
            cardImageUri = p.cardImageUri
            loaded = true
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

    fun showDateWithOptionalTime(initial: Long? = null, onPicked: (Long) -> Unit) {
        val cal = Calendar.getInstance()
        if (initial != null) cal.timeInMillis = initial
        DatePickerDialog(context, { _, y, m, d ->
            val picked = Calendar.getInstance(); picked.set(y, m, d, 0, 0, 0); picked.set(Calendar.MILLISECOND, 0)
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
            cardImageUri = uri.toString()
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "File"
            attachments = attachments + AttachmentEntry(uri.toString(), fileName)
        }
    }

    val disabledFieldColors = OutlinedTextFieldDefaults.colors(
        disabledTextColor = MaterialTheme.colorScheme.onSurface,
        disabledBorderColor = MaterialTheme.colorScheme.outline,
        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    )

    fun doSave() {
        if (name.isBlank()) return
        val project = if (isEditing && existingProject != null) {
            existingProject!!.copy(
                name = name.trim(), director = director.trim(), castingDirector = castingDirector.trim(),
                notes = notes.trim(), startDate = startDate, endDate = endDate,
                presentationDatesJson = ProjectJsonConverter.presentationDatesToJson(presentationDates),
                linksJson = ProjectJsonConverter.linksToJson(links),
                attachmentsJson = ProjectJsonConverter.attachmentsToJson(attachments),
                teamJson = ProjectJsonConverter.teamToJson(team),
                locationsJson = ProjectJsonConverter.locationsToJson(locations),
                cardColor = cardColor, cardImageUri = cardImageUri,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            Project(
                name = name.trim(), director = director.trim(), castingDirector = castingDirector.trim(),
                notes = notes.trim(), startDate = startDate, endDate = endDate,
                presentationDatesJson = ProjectJsonConverter.presentationDatesToJson(presentationDates),
                linksJson = ProjectJsonConverter.linksToJson(links),
                attachmentsJson = ProjectJsonConverter.attachmentsToJson(attachments),
                teamJson = ProjectJsonConverter.teamToJson(team),
                locationsJson = ProjectJsonConverter.locationsToJson(locations),
                cardColor = cardColor, cardImageUri = cardImageUri
            )
        }
        viewModel.saveProject(project) { onSaved() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Project" else "New Project") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
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
            // ── Card Appearance ──
            SectionLabel("Card Appearance")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Card Color", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val presetColors = listOf("", "#FF7B4DFF", "#FF4B7BFF", "#FF00D4FF", "#FFFF6B6B", "#FFFF9F43", "#FF00B894", "#FFFFD32A", "#FF6C5CE7", "#FFE17055")
                    val isCustomColor = cardColor.isNotBlank() && cardColor !in presetColors
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            onClick = { cardColor = "" },
                            shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant,
                            border = if (cardColor.isBlank()) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier.size(32.dp)
                        ) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.FormatColorReset, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) } }
                        presetColors.drop(1).forEach { c ->
                            val bgColor = try { Color(android.graphics.Color.parseColor(c)) } catch (_: Exception) { MaterialTheme.colorScheme.surfaceVariant }
                            Surface(
                                onClick = { cardColor = c },
                                shape = CircleShape, color = bgColor,
                                border = if (cardColor == c) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier.size(32.dp)
                            ) { if (cardColor == c) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Check, null, Modifier.size(16.dp), tint = Color.White) } }
                        }
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
                OutlinedButton(onClick = { cardImageUri = "" }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Close, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Remove Image")
                }
            }

            // ── Name ──
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Project Name *") },
                leadingIcon = { Icon(Icons.Default.Folder, null) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true
            )

            // ── People ──
            SectionLabel("People")
            OutlinedTextField(
                value = director, onValueChange = { director = it },
                label = { Text("Director") },
                leadingIcon = { Icon(Icons.Default.Movie, null) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true
            )
            OutlinedTextField(
                value = castingDirector, onValueChange = { castingDirector = it },
                label = { Text("Casting Director") },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true
            )
            team.forEachIndexed { i, member ->
                if (member.name.isNotBlank() || member.role.isNotBlank()) {
                    Box(Modifier.fillMaxWidth().clickable { showTeamDialog = i; dialogRole = member.role; dialogName = member.name }) {
                        OutlinedTextField(
                            value = member.name,
                            onValueChange = {},
                            label = { Text(member.role.ifBlank { "Team Member" }) },
                            leadingIcon = { Icon(Icons.Default.Person, null) },
                            trailingIcon = {
                                IconButton(onClick = { team = team.toMutableList().also { it.removeAt(i) } }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Close, "Remove", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                }
                            },
                            readOnly = true, enabled = false,
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
                            colors = disabledFieldColors
                        )
                    }
                }
            }
            OutlinedButton(onClick = { showTeamDialog = -1; dialogRole = ""; dialogName = "" }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Add Person")
            }

            // ── Project Dates ──
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
                            IconButton(onClick = { startDate = null; endDate = null }, Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, "Clear", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            }
                        }
                    },
                    placeholder = { Text("Tap to set dates") },
                    readOnly = true, enabled = false, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
                    colors = disabledFieldColors
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
                            IconButton(onClick = { presentationDates = presentationDates.toMutableList().also { it.removeAt(i) } }, Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, "Remove", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            }
                        },
                        readOnly = true, enabled = false, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = false, maxLines = 2,
                        colors = disabledFieldColors
                    )
                }
            }
            OutlinedButton(onClick = { showPresentationDialog = -1; presLabel = ""; presDate = null; presEndDate = null; presHasTime = false; presRecurrence = "NONE"; presStartHour = -1; presStartMin = 0; presEndHour = -1; presEndMin = 0 }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Add Important Date")
            }

            // ── Locations ──
            SectionLabel("Locations")
            locations.forEachIndexed { i, loc ->
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth().clickable {
                    showLocationDialog = i; locType = loc.type; locAddress = loc.address; locLabel = loc.label; locLat = loc.latitude; locLng = loc.longitude
                }) {
                    Column {
                        if (loc.latitude != null && loc.longitude != null) {
                            coil.compose.AsyncImage(
                                model = "https://staticmap.openstreetmap.de/staticmap.php?center=${loc.latitude},${loc.longitude}&zoom=15&size=400x100&markers=${loc.latitude},${loc.longitude},red-pushpin",
                                contentDescription = "Map",
                                modifier = Modifier.fillMaxWidth().height(80.dp),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                if (loc.label.isNotBlank()) Text(loc.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(loc.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = {
                                val q = if (loc.latitude != null) "geo:${loc.latitude},${loc.longitude}?q=${loc.latitude},${loc.longitude}(${Uri.encode(loc.label.ifBlank { loc.address })})" else "geo:0,0?q=${Uri.encode(loc.address)}"
                                try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(q))) } catch (_: Exception) {}
                            }, Modifier.size(36.dp)) {
                                Icon(Icons.Default.Map, "Open in Maps", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { locations = locations.toMutableList().also { it.removeAt(i) } }, Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, "Remove", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
            OutlinedButton(onClick = { showLocationDialog = -1; locType = ""; locAddress = ""; locLabel = ""; locLat = null; locLng = null }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Add Location")
            }

            // ── Links ──
            SectionLabel("Links")
            links.forEachIndexed { i, link ->
                if (link.url.isNotBlank() || link.label.isNotBlank() || link.note.isNotBlank()) {
                    Box(Modifier.fillMaxWidth().clickable {
                        showLinkDialog = i; linkDialogUrl = link.url; linkDialogLabel = link.label; linkDialogNote = link.note
                    }) {
                        OutlinedTextField(
                            value = link.url,
                            onValueChange = {},
                            label = { Text(link.label.ifBlank { "Link ${i + 1}" }) },
                            leadingIcon = { Icon(Icons.Default.Link, null) },
                            supportingText = if (link.note.isNotBlank()) { { Text(link.note) } } else null,
                            trailingIcon = {
                                IconButton(onClick = { links = links.toMutableList().also { it.removeAt(i) } }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Close, "Remove", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                }
                            },
                            readOnly = true, enabled = false,
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
                            colors = disabledFieldColors
                        )
                    }
                }
            }
            OutlinedButton(
                onClick = { showLinkDialog = -1; linkDialogUrl = ""; linkDialogLabel = ""; linkDialogNote = "" },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Add Link")
            }

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
                        IconButton(onClick = { attachments = attachments.toMutableList().also { it.removeAt(i) } }, Modifier.size(32.dp)) { Icon(Icons.Default.Close, "Remove", Modifier.size(18.dp)) }
                    }
                }
            }
            OutlinedButton(onClick = { filePicker.launch(arrayOf("*/*")) }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Attach File")
            }

            // ── Notes ──
            SectionLabel("Notes")
            OutlinedTextField(value = notes, onValueChange = { notes = it }, modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp), shape = RoundedCornerShape(12.dp), maxLines = 8)

            Spacer(Modifier.height(8.dp))

            // ── Save ──
            Button(
                onClick = { doSave() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = name.isNotBlank()
            ) {
                Icon(if (isEditing) Icons.Default.Save else Icons.Default.Add, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (isEditing) "Save Changes" else "Create Project", style = MaterialTheme.typography.titleMedium)
            }

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
                    OutlinedTextField(
                        value = linkDialogLabel, onValueChange = { linkDialogLabel = it },
                        label = { Text("Title") },
                        leadingIcon = { Icon(Icons.Default.Label, null) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true
                    )
                    OutlinedTextField(
                        value = linkDialogUrl, onValueChange = { linkDialogUrl = it },
                        label = { Text("Link (URL)") },
                        leadingIcon = { Icon(Icons.Default.Link, null) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true
                    )
                    OutlinedTextField(
                        value = linkDialogNote, onValueChange = { linkDialogNote = it },
                        label = { Text("Note (optional)") },
                        leadingIcon = { Icon(Icons.Default.Notes, null) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp), shape = RoundedCornerShape(12.dp), maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (linkDialogUrl.isNotBlank()) {
                        val entry = LinkEntry(linkDialogUrl.trim(), linkDialogLabel.trim(), linkDialogNote.trim())
                        links = if (showLinkDialog == -1) links + entry else links.toMutableList().also { it[showLinkDialog!!] = entry }
                    }; showLinkDialog = null
                }, shape = RoundedCornerShape(12.dp), enabled = linkDialogUrl.isNotBlank()) { Text("Save") }
            },
            dismissButton = { OutlinedButton(onClick = { showLinkDialog = null }, shape = RoundedCornerShape(12.dp)) { Text("Cancel") } }
        )
    }

    // ── Map picker ──
    if (showMapPicker) {
        val scope = rememberCoroutineScope()
        MapPickerDialog(
            initialLat = locLat, initialLng = locLng,
            onDismiss = { showMapPicker = false },
            onConfirm = { lat, lng ->
                locLat = lat; locLng = lng
                showMapPicker = false
                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val geocoder = android.location.Geocoder(context, Locale.getDefault())
                        @Suppress("DEPRECATION")
                        val results = geocoder.getFromLocation(lat, lng, 1)
                        if (!results.isNullOrEmpty()) {
                            val formatted = results[0].getAddressLine(0)
                            if (!formatted.isNullOrBlank()) locAddress = formatted
                        }
                    } catch (_: Exception) {}
                }
            }
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
                    OutlinedTextField(value = locLabel, onValueChange = { locLabel = it }, label = { Text("Label (e.g. Main Stage)") }, leadingIcon = { Icon(Icons.Default.Label, null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)

                    OutlinedTextField(
                        value = locAddress, onValueChange = { locAddress = it; locLat = null; locLng = null },
                        label = { Text("Address") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                        trailingIcon = {
                            if (locAddress.isNotBlank()) {
                                IconButton(onClick = {
                                    locSearching = true
                                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                        try {
                                            val geocoder = android.location.Geocoder(context, Locale.getDefault())
                                            @Suppress("DEPRECATION")
                                            val results = geocoder.getFromLocationName(locAddress, 1)
                                            if (!results.isNullOrEmpty()) {
                                                locLat = results[0].latitude
                                                locLng = results[0].longitude
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
                        Text(
                            "Tip: Tap the search icon to find coordinates, or use \"Pick on Map\" to drop a pin.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                    }; showLocationDialog = null
                }, shape = RoundedCornerShape(12.dp), enabled = locAddress.isNotBlank() || hasCoords) { Text("Save") }
            },
            dismissButton = { OutlinedButton(onClick = { showLocationDialog = null }, shape = RoundedCornerShape(12.dp)) { Text("Cancel") } }
        )
    }

    // ── Project dates dialog ──
    if (showProjectDatesDialog) {
        AlertDialog(
            onDismissRequest = { showProjectDatesDialog = false }, shape = RoundedCornerShape(24.dp),
            title = { Text("Project Dates") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.fillMaxWidth().clickable {
                        showDatePicker(startDate) { startDate = it }
                    }) {
                        OutlinedTextField(
                            value = if (startDate != null) dateFormat.format(Date(startDate!!)) else "",
                            onValueChange = {}, label = { Text("Start Date") },
                            leadingIcon = { Icon(Icons.Default.CalendarMonth, null) },
                            placeholder = { Text("Tap to pick") },
                            readOnly = true, enabled = false, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
                            colors = disabledFieldColors
                        )
                    }
                    Box(Modifier.fillMaxWidth().clickable {
                        showDatePicker(endDate) { endDate = it }
                    }) {
                        OutlinedTextField(
                            value = if (endDate != null) dateFormat.format(Date(endDate!!)) else "",
                            onValueChange = {}, label = { Text("End Date") },
                            leadingIcon = { Icon(Icons.Default.CalendarMonth, null) },
                            placeholder = { Text("Tap to pick") },
                            readOnly = true, enabled = false, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
                            colors = disabledFieldColors
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
        var showRecurrenceMenu by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showPresentationDialog = null }, shape = RoundedCornerShape(24.dp),
            title = { Text(if (showPresentationDialog == -1) "Add Important Date" else "Edit Important Date") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = presLabel, onValueChange = { presLabel = it },
                        label = { Text("Title (e.g. Rehearsal, Opening Night)") },
                        leadingIcon = { Icon(Icons.Default.Label, null) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true
                    )

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
                        Text("Session Time", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.weight(1f).clickable {
                                android.app.TimePickerDialog(context, { _, h, m -> presStartHour = h; presStartMin = m }, if (presStartHour >= 0) presStartHour else 9, presStartMin, false).show()
                            }) {
                                OutlinedTextField(
                                    value = if (presStartHour >= 0) formatTime(presStartHour, presStartMin) else "",
                                    onValueChange = {}, label = { Text("Start") }, placeholder = { Text("From") },
                                    readOnly = true, enabled = false, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true, colors = disabledFieldColors
                                )
                            }
                            Box(Modifier.weight(1f).clickable {
                                android.app.TimePickerDialog(context, { _, h, m -> presEndHour = h; presEndMin = m }, if (presEndHour >= 0) presEndHour else 17, presEndMin, false).show()
                            }) {
                                OutlinedTextField(
                                    value = if (presEndHour >= 0) formatTime(presEndHour, presEndMin) else "",
                                    onValueChange = {}, label = { Text("End") }, placeholder = { Text("To") },
                                    readOnly = true, enabled = false, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true, colors = disabledFieldColors
                                )
                            }
                        }

                        Text("Period", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Box(Modifier.fillMaxWidth().clickable { showDatePicker(presDate) { d -> presDate = d } }) {
                            OutlinedTextField(
                                value = if (presDate != null) dateFormat.format(Date(presDate!!)) else "",
                                onValueChange = {}, label = { Text("From") }, placeholder = { Text("Start date") },
                                leadingIcon = { Icon(Icons.Default.CalendarMonth, null) },
                                readOnly = true, enabled = false, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true, colors = disabledFieldColors
                            )
                        }
                        Box(Modifier.fillMaxWidth().clickable { showDatePicker(presEndDate) { d -> presEndDate = d } }) {
                            OutlinedTextField(
                                value = if (presEndDate != null) dateFormat.format(Date(presEndDate!!)) else "",
                                onValueChange = {}, label = { Text("Until") }, placeholder = { Text("End date") },
                                leadingIcon = { Icon(Icons.Default.CalendarMonth, null) },
                                readOnly = true, enabled = false, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true, colors = disabledFieldColors
                            )
                        }
                    } else {
                        Box(Modifier.fillMaxWidth().clickable { showDateWithOptionalTime(presDate) { d -> presDate = d; presHasTime = true } }) {
                            OutlinedTextField(
                                value = if (presDate != null) { if (presHasTime) dateTimeFormat.format(Date(presDate!!)) else dateFormat.format(Date(presDate!!)) } else "",
                                onValueChange = {}, label = { Text("Date") }, placeholder = { Text("Tap to pick") },
                                leadingIcon = { Icon(Icons.Default.CalendarMonth, null) },
                                readOnly = true, enabled = false, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true, colors = disabledFieldColors
                            )
                        }
                        Box(Modifier.fillMaxWidth().clickable { showDateWithOptionalTime(presEndDate) { d -> presEndDate = d } }) {
                            OutlinedTextField(
                                value = if (presEndDate != null) { if (presHasTime) dateTimeFormat.format(Date(presEndDate!!)) else dateFormat.format(Date(presEndDate!!)) } else "",
                                onValueChange = {}, label = { Text("End Date (optional)") }, placeholder = { Text("For date range") },
                                leadingIcon = { Icon(Icons.Default.CalendarMonth, null) },
                                trailingIcon = { if (presEndDate != null) IconButton(onClick = { presEndDate = null }, Modifier.size(32.dp)) { Icon(Icons.Default.Close, "Clear", Modifier.size(18.dp)) } },
                                readOnly = true, enabled = false, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true, colors = disabledFieldColors
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
                    }; showPresentationDialog = null
                }, shape = RoundedCornerShape(12.dp), enabled = isValid) { Text("Save") }
            },
            dismissButton = { OutlinedButton(onClick = { showPresentationDialog = null }, shape = RoundedCornerShape(12.dp)) { Text("Cancel") } }
        )
    }

    // ── Color picker dialog ──
    if (showColorPickerDialog) {
        val palette = listOf(
            listOf("#FFFFCDD2", "#FFEF9A9A", "#FFE57373", "#FFEF5350", "#FFF44336", "#FFE53935", "#FFD32F2F", "#FFC62828", "#FFB71C1C"),
            listOf("#FFF8BBD0", "#FFF48FB1", "#FFF06292", "#FFEC407A", "#FFE91E63", "#FFD81B60", "#FFC2185B", "#FFAD1457", "#FF880E4F"),
            listOf("#FFE1BEE7", "#FFCE93D8", "#FFBA68C8", "#FFAB47BC", "#FF9C27B0", "#FF8E24AA", "#FF7B1FA2", "#FF6A1B9A", "#FF4A148C"),
            listOf("#FFD1C4E9", "#FFB39DDB", "#FF9575CD", "#FF7E57C2", "#FF673AB7", "#FF5E35B1", "#FF512DA8", "#FF4527A0", "#FF311B92"),
            listOf("#FFBBDEFB", "#FF90CAF9", "#FF64B5F6", "#FF42A5F5", "#FF2196F3", "#FF1E88E5", "#FF1976D2", "#FF1565C0", "#FF0D47A1"),
            listOf("#FFB2EBF2", "#FF80DEEA", "#FF4DD0E1", "#FF26C6DA", "#FF00BCD4", "#FF00ACC1", "#FF0097A7", "#FF00838F", "#FF006064"),
            listOf("#FFB2DFDB", "#FF80CBC4", "#FF4DB6AC", "#FF26A69A", "#FF009688", "#FF00897B", "#FF00796B", "#FF00695C", "#FF004D40"),
            listOf("#FFC8E6C9", "#FFA5D6A7", "#FF81C784", "#FF66BB6A", "#FF4CAF50", "#FF43A047", "#FF388E3C", "#FF2E7D32", "#FF1B5E20"),
            listOf("#FFFFF9C4", "#FFFFF59D", "#FFFFF176", "#FFFFEE58", "#FFFFEB3B", "#FFFDD835", "#FFFBC02D", "#FFF9A825", "#FFF57F17"),
            listOf("#FFFFE0B2", "#FFFFCC80", "#FFFFB74D", "#FFFFA726", "#FFFF9800", "#FFFB8C00", "#FFF57C00", "#FFEF6C00", "#FFE65100"),
            listOf("#FFD7CCC8", "#FFBCAAA4", "#FFA1887F", "#FF8D6E63", "#FF795548", "#FF6D4C41", "#FF5D4037", "#FF4E342E", "#FF3E2723"),
            listOf("#FFF5F5F5", "#FFEEEEEE", "#FFE0E0E0", "#FFBDBDBD", "#FF9E9E9E", "#FF757575", "#FF616161", "#FF424242", "#FF212121")
        )
        var selectedInPicker by remember { mutableStateOf(cardColor) }

        AlertDialog(
            onDismissRequest = { showColorPickerDialog = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Pick a Color") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (selectedInPicker.isNotBlank()) {
                        val previewC = try { Color(android.graphics.Color.parseColor(selectedInPicker)) } catch (_: Exception) { Color.Gray }
                        Box(Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(12.dp)).background(previewC))
                    }
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
                Button(onClick = { cardColor = selectedInPicker; showColorPickerDialog = false }, shape = RoundedCornerShape(12.dp)) { Text("Apply") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showColorPickerDialog = false }, shape = RoundedCornerShape(12.dp)) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
}
