package mx.visionebc.actorstoolkit.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Manual") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            HeroBlock()

            Section(
                icon = Icons.Default.Folder,
                title = "Projects",
                intro = "A project holds every casting, audition, script, contact and recording for one production."
            ) {
                Step(
                    number = 1,
                    heading = "Create or import",
                    body = "Tap the + button on the Projects screen. Two bubbles pop up:",
                    bullets = listOf(
                        IconLabel(Icons.Default.Add, "Start New Project — blank slate."),
                        IconLabel(Icons.Default.FileDownload, "Import from File — load a .json or .zip someone shared with you.")
                    )
                )
                Step(
                    number = 2,
                    heading = "Reorder",
                    body = "Each project card has a ☰ handle on the right. Long-press it and drag up or down to change the order."
                )
                Step(
                    number = 3,
                    heading = "Share the project",
                    body = "Tap the share icon on a card and pick exactly what to include: dates & locations, links, team, contacts, auditions, scripts, castings, recordings. Recordings turn the export into a ZIP so audio files travel with it."
                )
            }

            Section(
                icon = Icons.Default.Dashboard,
                title = "Inside a project",
                intro = "Every project has its own bottom navigation: Project, Casting, Auditions, Scripts, Settings."
            ) {
                Step(
                    number = 1,
                    heading = "Switch tabs freely",
                    body = "Your position is remembered. Jump from Scripts to Auditions and back — the project stays loaded."
                )
                Step(
                    number = 2,
                    heading = "Settings anywhere",
                    body = "The floating gear in the top-right corner opens Settings without leaving your current context."
                )
            }

            Section(
                icon = Icons.Default.Groups,
                title = "Castings & Characters",
                intro = "A casting groups characters for a production. Each character can be linked to one or more scripts for practice."
            ) {
                Step(
                    number = 1,
                    heading = "Create a casting",
                    body = "Open a project, go to the Casting tab, tap +. Give it a name (e.g., \"Act 1 Leads\") and optional notes."
                )
                Step(
                    number = 2,
                    heading = "Add characters",
                    body = "Open a casting and tap + to add characters. Each character holds a name and notes — useful for backstory, physical description, or direction."
                )
                Step(
                    number = 3,
                    heading = "Link characters to scripts",
                    body = "From a character's detail screen, tap \"Add Script\" to import a new one just for this character, or \"Pick Script\" to link an existing script already in the project."
                )
                Step(
                    number = 4,
                    heading = "Why link?",
                    body = "Linked scripts appear in both the character's page and the project's Scripts tab. Your practice data stays attached to the character, not just the script."
                )
            }

            Section(
                icon = Icons.Default.TheaterComedy,
                title = "Auditions",
                intro = "Track every audition with role, status, date & time, notes, links, attachments and images."
            ) {
                Step(
                    number = 1,
                    heading = "Add from the project",
                    body = "Open a project, go to the Auditions tab, tap Add. When you save, the app returns to that tab with the new audition visible."
                )
                Step(
                    number = 2,
                    heading = "Date and time",
                    body = "Tap the Audition Date & Time field. Pick a date, then a time — both show in the list."
                )
                Step(
                    number = 3,
                    heading = "Status chips",
                    body = "Submitted, Called, Callback, Booked, Passed. Color-coded so you scan the list fast."
                )
            }

            Section(
                icon = Icons.Default.Description,
                title = "Scripts",
                intro = "Import a PDF, TXT or FDX. The app parses characters and lines automatically."
            ) {
                Step(
                    number = 1,
                    heading = "Import into a project",
                    body = "Open a project, go to the Scripts tab, tap Import. Scripts imported here belong to the project at large."
                )
                Step(
                    number = 2,
                    heading = "Mark your role",
                    body = "The first time you open practice, pick which character you play — that's \"my role\" for this script."
                )
                Step(
                    number = 3,
                    heading = "Characters vs scripts",
                    body = "Scripts live in the project. Characters live in castings. Linking them from a character's detail screen shows the same script in both places without duplication."
                )
            }

            Section(
                icon = Icons.Default.PlayCircle,
                title = "Practice Mode",
                intro = "Rehearse with TTS reading other characters' lines, record your own, overlay blocking and stage diagrams."
            ) {
                Step(
                    number = 1,
                    heading = "Let the phone read other parts",
                    body = "Tap the speaker icon in the top bar. The TTS engine reads every line that isn't yours so you can say yours live."
                )
                Step(
                    number = 2,
                    heading = "Per-character voice",
                    body = "Tap the voice icon in the top bar to open Character Voices. Pick a different TTS voice for each character and preview with ▶."
                )
                Step(
                    number = 3,
                    heading = "Record your own audio",
                    body = "Long-press a line to reveal the record button. Capture your delivery for each line; it plays back during auto-practice."
                )
                Step(
                    number = 4,
                    heading = "Hear yourself (or not)",
                    body = "Settings → Practice → \"Hear my own recordings\". Off means your role's lines stay silent so you say them live; on plays your own takes back."
                )
            }

            Section(
                icon = Icons.Default.Send,
                title = "Send Lines to a Partner",
                intro = "Record your role's lines, then send just those recordings to a scene partner. They import and rehearse with your voice."
            ) {
                Step(
                    number = 1,
                    heading = "Open from Contacts",
                    body = "Inside a project, open the Project tab and scroll to Contacts. On a contact card, tap the voice icon. The contact is pre-selected — you only pick the script and your character."
                )
                Step(
                    number = 2,
                    heading = "Send via anything",
                    body = "Tap \"Send via…\". Android's share sheet opens with every app that accepts a ZIP — WhatsApp, Telegram, Signal, email, Drive, Bluetooth, and so on. If the contact has an email saved, it's pre-filled when you pick an email app."
                )
                Step(
                    number = 3,
                    heading = "How import works",
                    body = "When your partner imports the ZIP, the app matches each project by its internal share ID and merges the recordings into their existing project instead of creating a duplicate. If they don't have the project yet, a new one is created."
                )
            }

            Section(
                icon = Icons.Default.LocationOn,
                title = "Locations",
                intro = "Add rehearsal, casting or filming addresses. Two ways to set coordinates."
            ) {
                Step(
                    number = 1,
                    heading = "Type the address",
                    body = "Tap the search icon inside the address field. The built-in geocoder looks up coordinates and shows a small map preview."
                )
                Step(
                    number = 2,
                    heading = "Pick on the map",
                    body = "Tap \"Pick on Map\" and tap anywhere on the OpenStreetMap view to drop a pin. The address is reverse-geocoded for you."
                )
            }

            Section(
                icon = Icons.Default.Link,
                title = "Links and Contacts",
                intro = "Everything uses the same tap-to-edit dialog pattern."
            ) {
                Step(
                    number = 1,
                    heading = "Links",
                    body = "Each link has a Title, URL and an optional Note. Tap a saved link to edit, or the + button to add one."
                )
                Step(
                    number = 2,
                    heading = "Project contacts",
                    body = "Store the name, phone, email and roles of everyone in the production. They're the pool the \"Send Lines\" dialog uses."
                )
            }

            Section(
                icon = Icons.Default.Palette,
                title = "Themes",
                intro = "Ten themes — pick in Settings → Appearance."
            ) {
                Step(number = 1, heading = "System, Light, Dark", body = "Follow the device, or force one of the basics.")
                Step(number = 2, heading = "Color-led dark", body = "Ocean Blue, Deep Blue, Pink Violet, Forest Green, Golden Yellow.")
                Step(number = 3, heading = "iOS", body = "Clean light palette with Apple's system blue, green, teal and red.")
                Step(number = 4, heading = "Modern", body = "Vivid violet, cyan and pink neon on near-black (OLED-friendly).")
            }

            Section(
                icon = Icons.Default.Backup,
                title = "Backup and Restore",
                intro = "Full-database backup lives separately from per-project shares."
            ) {
                Step(
                    number = 1,
                    heading = "Export",
                    body = "Settings → Backup & Restore → Backup All Projects. Saves a single ZIP with every project, script and recording."
                )
                Step(
                    number = 2,
                    heading = "Restore",
                    body = "Pick the ZIP you saved earlier. A restore overwrites the whole database and restarts the app."
                )
            }

            Section(
                icon = Icons.Default.Share,
                title = "Sharing a project",
                intro = "Share any project as JSON or ZIP with fine-grained control over what's included."
            ) {
                Step(
                    number = 1,
                    heading = "Tap the share icon on a project card",
                    body = "Check what to include: dates & locations, links, team, contacts, auditions, scripts, castings, recordings. Basic info (name, director, notes) always goes."
                )
                Step(
                    number = 2,
                    heading = "JSON or ZIP",
                    body = "Without recordings you get a compact JSON. Turn recordings on and the export becomes a ZIP so the audio files travel with it."
                )
                Step(
                    number = 3,
                    heading = "Smart import",
                    body = "Importing the same project twice doesn't duplicate — the app matches by share ID and merges new data into the existing project."
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "That's it — you're ready to rehearse. Break a leg.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Helpers ───────────────────────────────────────────────

private data class IconLabel(val icon: ImageVector, val text: String)

@Composable
private fun HeroBlock() {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MenuBook, null, Modifier.size(30.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Actors Toolkit",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "Every feature explained, section by section.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun Section(
    icon: ImageVector,
    title: String,
    intro: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                intro,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            content()
        }
    }
}

@Composable
private fun Step(
    number: Int,
    heading: String,
    body: String,
    bullets: List<IconLabel> = emptyList()
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                number.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                heading,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            bullets.forEach { b ->
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        b.icon,
                        null,
                        Modifier.size(16.dp).padding(top = 2.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        b.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
