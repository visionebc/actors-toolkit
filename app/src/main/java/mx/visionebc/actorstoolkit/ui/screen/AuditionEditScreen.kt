package mx.visionebc.actorstoolkit.ui.screen

import android.app.DatePickerDialog
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
import androidx.compose.ui.unit.dp
import mx.visionebc.actorstoolkit.data.entity.Audition
import mx.visionebc.actorstoolkit.ui.audition.AuditionViewModel
import java.text.SimpleDateFormat
import java.util.*

private val STATUS_OPTIONS = listOf("SUBMITTED", "CALLED", "CALLBACK", "BOOKED", "PASSED")

private fun statusDisplayName(status: String) = when (status) {
    "SUBMITTED" -> "Submitted"
    "CALLED" -> "Called"
    "CALLBACK" -> "Callback"
    "BOOKED" -> "Booked"
    "PASSED" -> "Passed"
    else -> status
}

private fun statusColor(status: String): Color = when (status) {
    "SUBMITTED" -> Color(0xFF74B9FF)
    "CALLED" -> Color(0xFFFFE66D)
    "CALLBACK" -> Color(0xFFFF9F43)
    "BOOKED" -> Color(0xFF00D4FF)
    "PASSED" -> Color(0xFFFF6B6B)
    else -> Color(0xFFB0ADCC)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditionEditScreen(
    auditionId: Long?,
    viewModel: AuditionViewModel,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val isEditing = auditionId != null && auditionId != 0L
    val existingAudition by viewModel.selectedAudition.collectAsState()
    val context = LocalContext.current

    var projectName by remember { mutableStateOf("") }
    var roleName by remember { mutableStateOf("") }
    var castingDirector by remember { mutableStateOf("") }
    var auditionDate by remember { mutableStateOf<Long?>(null) }
    var status by remember { mutableStateOf("SUBMITTED") }
    var notes by remember { mutableStateOf("") }
    var showStatusMenu by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(auditionId) {
        if (isEditing) {
            viewModel.loadAudition(auditionId!!)
        }
    }

    LaunchedEffect(existingAudition) {
        if (isEditing && existingAudition != null && !loaded) {
            val a = existingAudition!!
            projectName = a.projectName
            roleName = a.roleName
            castingDirector = a.castingDirector
            auditionDate = a.auditionDate
            status = a.status
            notes = a.notes
            loaded = true
        }
    }

    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    fun showDatePicker() {
        val cal = Calendar.getInstance()
        if (auditionDate != null) cal.timeInMillis = auditionDate!!
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val picked = Calendar.getInstance()
                picked.set(year, month, day, 0, 0, 0)
                picked.set(Calendar.MILLISECOND, 0)
                auditionDate = picked.timeInMillis
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Audition" else "New Audition") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Project Name
            OutlinedTextField(
                value = projectName,
                onValueChange = { projectName = it },
                label = { Text("Project / Show Name") },
                leadingIcon = { Icon(Icons.Default.Movie, null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Role Name
            OutlinedTextField(
                value = roleName,
                onValueChange = { roleName = it },
                label = { Text("Role") },
                leadingIcon = { Icon(Icons.Default.TheaterComedy, null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Casting Director
            OutlinedTextField(
                value = castingDirector,
                onValueChange = { castingDirector = it },
                label = { Text("Casting Director") },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Audition Date
            Surface(
                onClick = { showDatePicker() },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Audition Date",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            if (auditionDate != null) dateFormat.format(Date(auditionDate!!))
                            else "Tap to set date",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (auditionDate != null) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    if (auditionDate != null) {
                        IconButton(onClick = { auditionDate = null }) {
                            Icon(Icons.Default.Clear, "Clear date", Modifier.size(20.dp))
                        }
                    }
                }
            }

            // Status selector
            Text(
                "Status",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                STATUS_OPTIONS.forEach { s ->
                    val selected = status == s
                    val color = statusColor(s)
                    FilterChip(
                        selected = selected,
                        onClick = { status = s },
                        label = {
                            Text(
                                statusDisplayName(s),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = color.copy(alpha = 0.2f),
                            selectedLabelColor = color
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                leadingIcon = { Icon(Icons.Default.Notes, null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                shape = RoundedCornerShape(12.dp),
                maxLines = 6
            )

            Spacer(Modifier.height(8.dp))

            // Save button
            Button(
                onClick = {
                    if (projectName.isBlank()) return@Button
                    val audition = if (isEditing && existingAudition != null) {
                        existingAudition!!.copy(
                            projectName = projectName.trim(),
                            roleName = roleName.trim(),
                            castingDirector = castingDirector.trim(),
                            auditionDate = auditionDate,
                            status = status,
                            notes = notes.trim(),
                            updatedAt = System.currentTimeMillis()
                        )
                    } else {
                        Audition(
                            projectName = projectName.trim(),
                            roleName = roleName.trim(),
                            castingDirector = castingDirector.trim(),
                            auditionDate = auditionDate,
                            status = status,
                            notes = notes.trim()
                        )
                    }
                    viewModel.saveAudition(audition) { onSaved() }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = projectName.isNotBlank()
            ) {
                Icon(
                    if (isEditing) Icons.Default.Save else Icons.Default.Add,
                    null,
                    Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isEditing) "Save Changes" else "Add Audition",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
