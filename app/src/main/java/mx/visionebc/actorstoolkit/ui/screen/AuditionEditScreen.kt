package mx.visionebc.actorstoolkit.ui.screen

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mx.visionebc.actorstoolkit.data.entity.AttachmentEntry
import mx.visionebc.actorstoolkit.data.entity.Audition
import mx.visionebc.actorstoolkit.data.entity.LinkEntry
import mx.visionebc.actorstoolkit.data.entity.ProjectJsonConverter
import mx.visionebc.actorstoolkit.ui.audition.AuditionViewModel
import java.text.SimpleDateFormat
import java.util.*

private val STATUS_OPTIONS = listOf("SUBMITTED", "CALLED", "CALLBACK", "BOOKED", "PASSED")

private fun statusDisplayName(status: String) = when (status) {
    "SUBMITTED" -> "Submitted"; "CALLED" -> "Called"; "CALLBACK" -> "Callback"
    "BOOKED" -> "Booked"; "PASSED" -> "Passed"; else -> status
}

private fun statusColor(status: String): Color = when (status) {
    "SUBMITTED" -> Color(0xFF74B9FF); "CALLED" -> Color(0xFFFFE66D); "CALLBACK" -> Color(0xFFFF9F43)
    "BOOKED" -> Color(0xFF00D4FF); "PASSED" -> Color(0xFFFF6B6B); else -> Color(0xFFB0ADCC)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditionEditScreen(
    auditionId: Long?,
    viewModel: AuditionViewModel,
    projectId: Long? = null,
    projectName: String = "",
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val isEditing = auditionId != null && auditionId != 0L
    val existingAudition by viewModel.selectedAudition.collectAsState()
    val context = LocalContext.current

    var roleName by remember { mutableStateOf("") }
    var auditionDate by remember { mutableStateOf<Long?>(null) }
    var status by remember { mutableStateOf("SUBMITTED") }
    var notes by remember { mutableStateOf("") }
    var links by remember { mutableStateOf(listOf<LinkEntry>()) }
    var attachments by remember { mutableStateOf(listOf<AttachmentEntry>()) }
    var images by remember { mutableStateOf(listOf<AttachmentEntry>()) }
    var loaded by remember { mutableStateOf(false) }

    // Dialog states
    var showLinkDialog by remember { mutableStateOf<Int?>(null) }
    var dialogUrl by remember { mutableStateOf("") }
    var dialogLabel by remember { mutableStateOf("") }
    var dialogNote by remember { mutableStateOf("") }

    val disabledFieldColors = OutlinedTextFieldDefaults.colors(
        disabledTextColor = MaterialTheme.colorScheme.onSurface,
        disabledBorderColor = MaterialTheme.colorScheme.outline,
        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    )

    LaunchedEffect(auditionId) { if (isEditing) viewModel.loadAudition(auditionId!!) }

    LaunchedEffect(existingAudition) {
        if (isEditing && existingAudition != null && !loaded) {
            val a = existingAudition!!
            roleName = a.roleName
            auditionDate = a.auditionDate
            status = a.status
            notes = a.notes
            links = ProjectJsonConverter.linksFromJson(a.linksJson)
            attachments = ProjectJsonConverter.attachmentsFromJson(a.attachmentsJson)
            images = ProjectJsonConverter.attachmentsFromJson(a.imagesJson)
            loaded = true
        }
    }

    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault()) }

    fun showDateTimePicker() {
        val cal = Calendar.getInstance()
        if (auditionDate != null) cal.timeInMillis = auditionDate!!
        DatePickerDialog(context, { _, y, m, d ->
            val timeCal = Calendar.getInstance()
            if (auditionDate != null) timeCal.timeInMillis = auditionDate!!
            TimePickerDialog(context, { _, hour, minute ->
                val picked = Calendar.getInstance()
                picked.set(y, m, d, hour, minute, 0); picked.set(Calendar.MILLISECOND, 0)
                auditionDate = picked.timeInMillis
            }, timeCal.get(Calendar.HOUR_OF_DAY), timeCal.get(Calendar.MINUTE), false).show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            val name = uri.lastPathSegment?.substringAfterLast('/') ?: "File"
            attachments = attachments + AttachmentEntry(uri.toString(), name)
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            val name = uri.lastPathSegment?.substringAfterLast('/') ?: "Image"
            images = images + AttachmentEntry(uri.toString(), name)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Audition" else "New Audition") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Project name as title (non-editable when from project)
            if (projectName.isNotBlank()) {
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.width(12.dp))
                        Text(projectName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            // Role
            OutlinedTextField(
                value = roleName, onValueChange = { roleName = it },
                label = { Text("Role") },
                leadingIcon = { Icon(Icons.Default.TheaterComedy, null) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true
            )

            // Audition Date & Time
            Box(Modifier.fillMaxWidth().clickable { showDateTimePicker() }) {
                OutlinedTextField(
                    value = if (auditionDate != null) dateFormat.format(Date(auditionDate!!)) else "",
                    onValueChange = {}, label = { Text("Audition Date & Time") },
                    leadingIcon = { Icon(Icons.Default.CalendarMonth, null) },
                    trailingIcon = {
                        if (auditionDate != null) IconButton(onClick = { auditionDate = null }, Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, "Clear", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        }
                    },
                    placeholder = { Text("Tap to set date & time") },
                    readOnly = true, enabled = false, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
                    colors = disabledFieldColors
                )
            }

            // Status
            Text("Status", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                STATUS_OPTIONS.forEach { s ->
                    val selected = status == s; val color = statusColor(s)
                    FilterChip(
                        selected = selected, onClick = { status = s },
                        label = { Text(statusDisplayName(s), style = MaterialTheme.typography.labelMedium, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, maxLines = 1) },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = color.copy(alpha = 0.2f), selectedLabelColor = color)
                    )
                }
            }

            // Notes
            OutlinedTextField(
                value = notes, onValueChange = { notes = it },
                label = { Text("Notes") },
                leadingIcon = { Icon(Icons.Default.Notes, null) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp), shape = RoundedCornerShape(12.dp), maxLines = 6
            )

            // Links
            Text("Links", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            links.forEachIndexed { i, link ->
                if (link.url.isNotBlank() || link.label.isNotBlank() || link.note.isNotBlank()) {
                    Box(Modifier.fillMaxWidth().clickable { showLinkDialog = i; dialogUrl = link.url; dialogLabel = link.label; dialogNote = link.note }) {
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
            OutlinedButton(onClick = { showLinkDialog = -1; dialogUrl = ""; dialogLabel = ""; dialogNote = "" }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Add Link")
            }

            // Attachments (files)
            Text("Files", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            attachments.forEachIndexed { i, att ->
                OutlinedTextField(
                    value = att.name.ifBlank { "File" },
                    onValueChange = {},
                    label = { Text("File ${i + 1}") },
                    leadingIcon = { Icon(Icons.Default.AttachFile, null) },
                    trailingIcon = {
                        IconButton(onClick = { attachments = attachments.toMutableList().also { it.removeAt(i) } }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, "Remove", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        }
                    },
                    readOnly = true, enabled = false,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
                    colors = disabledFieldColors
                )
            }
            OutlinedButton(onClick = { filePicker.launch(arrayOf("*/*")) }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.AttachFile, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Attach File")
            }

            // Images
            Text("Images", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            images.forEachIndexed { i, img ->
                OutlinedTextField(
                    value = img.name.ifBlank { "Image" },
                    onValueChange = {},
                    label = { Text("Image ${i + 1}") },
                    leadingIcon = { Icon(Icons.Default.Image, null) },
                    trailingIcon = {
                        IconButton(onClick = { images = images.toMutableList().also { it.removeAt(i) } }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, "Remove", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        }
                    },
                    readOnly = true, enabled = false,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
                    colors = disabledFieldColors
                )
            }
            OutlinedButton(onClick = { imagePicker.launch(arrayOf("image/*")) }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Image, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Add Image")
            }

            Spacer(Modifier.height(8.dp))

            // Save
            Button(
                onClick = {
                    val pName = projectName.ifBlank { if (isEditing && existingAudition != null) existingAudition!!.projectName else "Audition" }
                    val audition = if (isEditing && existingAudition != null) {
                        existingAudition!!.copy(
                            roleName = roleName.trim(),
                            auditionDate = auditionDate,
                            status = status,
                            notes = notes.trim(),
                            linksJson = ProjectJsonConverter.linksToJson(links),
                            attachmentsJson = ProjectJsonConverter.attachmentsToJson(attachments),
                            imagesJson = ProjectJsonConverter.attachmentsToJson(images),
                            updatedAt = System.currentTimeMillis()
                        )
                    } else {
                        Audition(
                            projectId = projectId,
                            projectName = pName,
                            roleName = roleName.trim(),
                            auditionDate = auditionDate,
                            status = status,
                            notes = notes.trim(),
                            linksJson = ProjectJsonConverter.linksToJson(links),
                            attachmentsJson = ProjectJsonConverter.attachmentsToJson(attachments),
                            imagesJson = ProjectJsonConverter.attachmentsToJson(images)
                        )
                    }
                    viewModel.saveAudition(audition) { onSaved() }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp)
            ) {
                Icon(if (isEditing) Icons.Default.Save else Icons.Default.Add, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (isEditing) "Save Changes" else "Add Audition", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // ── Link dialog ──
    if (showLinkDialog != null) {
        AlertDialog(
            onDismissRequest = { showLinkDialog = null }, shape = RoundedCornerShape(24.dp),
            title = { Text(if (showLinkDialog == -1) "Add Link" else "Edit Link") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = dialogLabel, onValueChange = { dialogLabel = it }, label = { Text("Title") }, leadingIcon = { Icon(Icons.Default.Label, null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                    OutlinedTextField(value = dialogUrl, onValueChange = { dialogUrl = it }, label = { Text("Link (URL)") }, leadingIcon = { Icon(Icons.Default.Link, null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                    OutlinedTextField(value = dialogNote, onValueChange = { dialogNote = it }, label = { Text("Note (optional)") }, leadingIcon = { Icon(Icons.Default.Notes, null) }, modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp), shape = RoundedCornerShape(12.dp), maxLines = 4)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (dialogUrl.isNotBlank()) {
                        val entry = LinkEntry(dialogUrl.trim(), dialogLabel.trim(), dialogNote.trim())
                        links = if (showLinkDialog == -1) links + entry else links.toMutableList().also { it[showLinkDialog!!] = entry }
                    }; showLinkDialog = null
                }, shape = RoundedCornerShape(12.dp), enabled = dialogUrl.isNotBlank()) { Text("Save") }
            },
            dismissButton = { OutlinedButton(onClick = { showLinkDialog = null }, shape = RoundedCornerShape(12.dp)) { Text("Cancel") } }
        )
    }
}
