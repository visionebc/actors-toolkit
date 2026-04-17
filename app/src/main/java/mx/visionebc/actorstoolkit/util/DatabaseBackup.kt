package mx.visionebc.actorstoolkit.util

import android.content.Context
import android.net.Uri
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import mx.visionebc.actorstoolkit.data.AppDatabase
import mx.visionebc.actorstoolkit.data.entity.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object DatabaseBackup {

    private const val DB_NAME = "actors_toolkit.db"
    private const val AUTHORITY_SUFFIX = ".fileprovider"

    private fun getAuthority(context: Context) = "${context.packageName}$AUTHORITY_SUFFIX"

    // ── Configurable backup (ZIP) ──

    suspend fun exportBackup(context: Context, includeRecordings: Boolean, includeDocuments: Boolean, projectIds: List<Long>? = null): Uri? {
        return try {
            val db = AppDatabase.getInstance(context)
            // Checkpoint WAL to ensure all data is in the main db file
            try {
                db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
            } catch (_: Exception) {}

            val shareDir = File(context.cacheDir, "share")
            shareDir.mkdirs()
            val zipFile = File(shareDir, "actors_toolkit_backup.zip")

            ZipOutputStream(FileOutputStream(zipFile)).use { zip ->
                // Always include database
                val dbFile = context.getDatabasePath(DB_NAME)
                if (dbFile.exists()) {
                    zip.putNextEntry(ZipEntry("database/$DB_NAME"))
                    FileInputStream(dbFile).use { it.copyTo(zip) }
                    zip.closeEntry()
                }

                // Include audio recordings
                if (includeRecordings) {
                    val recordingsDir = File(context.filesDir, "recordings")
                    if (recordingsDir.exists() && recordingsDir.isDirectory) {
                        recordingsDir.listFiles()?.forEach { file ->
                            if (file.isFile) {
                                zip.putNextEntry(ZipEntry("recordings/${file.name}"))
                                FileInputStream(file).use { it.copyTo(zip) }
                                zip.closeEntry()
                            }
                        }
                    }
                }

                // Include documents (attachments from projects and auditions)
                if (includeDocuments) {
                    val addedUris = mutableSetOf<String>()

                    // Project attachments + card images
                    val allProjects = db.projectDao().getAllSync()
                    val projects = if (projectIds != null) allProjects.filter { it.id in projectIds } else allProjects
                    for (project in projects) {
                        if (project.cardImageUri.isNotBlank()) {
                            addUriToZip(context, zip, project.cardImageUri, "documents/card_images", addedUris)
                        }
                        val attachments = ProjectJsonConverter.attachmentsFromJson(project.attachmentsJson)
                        for (att in attachments) {
                            if (att.uri.isNotBlank()) {
                                addUriToZip(context, zip, att.uri, "documents/project_attachments", addedUris)
                            }
                        }
                    }

                    // Audition attachments + images
                    val allAuditions = db.auditionDao().getAllSync()
                    val auditions = if (projectIds != null) allAuditions.filter { it.projectId != null && it.projectId in projectIds } else allAuditions
                    for (audition in auditions) {
                        val attachments = try {
                            val arr = JSONArray(audition.attachmentsJson)
                            (0 until arr.length()).map { arr.getJSONObject(it).optString("uri", "") }
                        } catch (_: Exception) { emptyList() }
                        for (uri in attachments) {
                            if (uri.isNotBlank()) addUriToZip(context, zip, uri, "documents/audition_attachments", addedUris)
                        }

                        val images = try {
                            val arr = JSONArray(audition.imagesJson)
                            (0 until arr.length()).map { arr.getString(it) }
                        } catch (_: Exception) { emptyList() }
                        for (uri in images) {
                            if (uri.isNotBlank()) addUriToZip(context, zip, uri, "documents/audition_images", addedUris)
                        }
                    }
                }

                // Write a manifest so we know what's included
                val manifest = JSONObject()
                manifest.put("version", 2)
                manifest.put("includesRecordings", includeRecordings)
                manifest.put("includesDocuments", includeDocuments)
                manifest.put("isSelective", projectIds != null)
                manifest.put("exportDate", System.currentTimeMillis())
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(manifest.toString(2).toByteArray())
                zip.closeEntry()
            }

            FileProvider.getUriForFile(context, getAuthority(context), zipFile)
        } catch (e: Exception) {
            Log.e("DatabaseBackup", "Export failed", e)
            null
        }
    }

    private fun addUriToZip(context: Context, zip: ZipOutputStream, uriStr: String, folder: String, addedUris: MutableSet<String>) {
        if (uriStr in addedUris) return
        try {
            val uri = Uri.parse(uriStr)
            val fileName = uri.lastPathSegment?.replace("/", "_")?.replace("\\", "_") ?: return
            // Use a hash prefix to avoid name collisions
            val safeFileName = "${uriStr.hashCode().toUInt()}_$fileName"
            context.contentResolver.openInputStream(uri)?.use { input ->
                zip.putNextEntry(ZipEntry("$folder/$safeFileName"))
                input.copyTo(zip)
                zip.closeEntry()
                addedUris.add(uriStr)
            }
        } catch (e: Exception) {
            Log.w("DatabaseBackup", "Could not backup file: $uriStr", e)
        }
    }

    // ── Import full backup (ZIP or legacy .db) ──

    fun importFullBackup(context: Context, inputUri: Uri): Boolean {
        return try {
            // Peek at the file to determine type
            val isZip = context.contentResolver.openInputStream(inputUri)?.use { input ->
                val header = ByteArray(4)
                input.read(header)
                // ZIP magic bytes: PK\x03\x04
                header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() && header[2] == 0x03.toByte() && header[3] == 0x04.toByte()
            } ?: false

            if (isZip) {
                importFromZip(context, inputUri)
            } else {
                importDatabaseOnly(context, inputUri)
            }
        } catch (_: Exception) { false }
    }

    private fun importFromZip(context: Context, inputUri: Uri): Boolean {
        return try {
            AppDatabase.getInstance(context).close()

            context.contentResolver.openInputStream(inputUri)?.use { input ->
                ZipInputStream(input).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        when {
                            entry.name == "database/$DB_NAME" -> {
                                val dbFile = context.getDatabasePath(DB_NAME)
                                context.getDatabasePath("$DB_NAME-wal").delete()
                                context.getDatabasePath("$DB_NAME-shm").delete()
                                FileOutputStream(dbFile).use { zip.copyTo(it) }
                            }
                            entry.name.startsWith("recordings/") && !entry.isDirectory -> {
                                val fileName = entry.name.removePrefix("recordings/")
                                val recordingsDir = File(context.filesDir, "recordings")
                                if (!recordingsDir.exists()) recordingsDir.mkdirs()
                                FileOutputStream(File(recordingsDir, fileName)).use { zip.copyTo(it) }
                            }
                            // Documents are restored but won't auto-link since their URIs may differ on a new device
                            // The DB backup already has the metadata
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            }
            true
        } catch (_: Exception) { false }
    }

    private fun importDatabaseOnly(context: Context, inputUri: Uri): Boolean {
        return try {
            val dbFile = context.getDatabasePath(DB_NAME)
            AppDatabase.getInstance(context).close()
            context.getDatabasePath("$DB_NAME-wal").delete()
            context.getDatabasePath("$DB_NAME-shm").delete()
            context.contentResolver.openInputStream(inputUri)?.use { input ->
                FileOutputStream(dbFile).use { output -> input.copyTo(output) }
            }
            true
        } catch (_: Exception) { false }
    }

    // ── Share via Android share sheet ──

    fun shareFile(context: Context, uri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Actors Toolkit Backup")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Save backup to...")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    // ── Selective project backup (as JSON with full hierarchy) ──

    suspend fun exportSelectedProjects(context: Context, projectIds: List<Long>): Uri? {
        return try {
            val db = AppDatabase.getInstance(context)
            val root = JSONObject()
            root.put("version", 2)
            root.put("exportDate", System.currentTimeMillis())

            val projectsArr = JSONArray()
            for (pid in projectIds) {
                projectsArr.put(exportFullProject(db, pid) ?: continue)
            }
            root.put("projects", projectsArr)

            val shareDir = File(context.cacheDir, "share")
            shareDir.mkdirs()
            val file = File(shareDir, "actors_toolkit_projects.json")
            file.writeText(root.toString(2))

            FileProvider.getUriForFile(context, getAuthority(context), file)
        } catch (_: Exception) { null }
    }

    private suspend fun exportFullProject(
        db: AppDatabase,
        projectId: Long,
        options: ShareOptions = ShareOptions(),
        audioOut: MutableList<Pair<String, File>>? = null
    ): JSONObject? {
        val project = db.projectDao().getById(projectId) ?: return null
        val pObj = projectToJson(project, options)

        // Contacts
        if (options.contacts) {
            val contactsArr = JSONArray()
            db.projectContactDao().getByProjectIdSync(projectId).forEach { c ->
                contactsArr.put(JSONObject().apply {
                    put("name", c.name); put("phone", c.phone); put("email", c.email); put("rolesJson", c.rolesJson)
                })
            }
            pObj.put("contacts", contactsArr)
        }

        // Auditions
        if (options.auditions) {
            val auditionsArr = JSONArray()
            db.auditionDao().getByProjectIdSync(projectId).forEach { a ->
                auditionsArr.put(auditionToJson(a, options))
            }
            pObj.put("auditions", auditionsArr)
        }

        // Scripts + Castings both need the castings list
        val castings = if (options.scripts || options.castingsAndCharacters) db.castingDao().getByProjectIdSync(projectId) else emptyList()
        val exportedScriptIds = mutableListOf<Long>()
        val scriptIdToIndex: Map<Long, Int>

        if (options.scripts) {
            // Direct project scripts
            val directScripts = db.scriptDao().getByProjectSync(projectId)
            for (s in directScripts) {
                if (s.id !in exportedScriptIds) exportedScriptIds.add(s.id)
            }
            // Via castings -> characters -> character_scripts
            for (casting in castings) {
                val characters = db.characterDao().getCharactersForCastingSync(casting.id)
                for (ch in characters) {
                    val scriptIds = db.characterScriptDao().getScriptIdsForCharacter(ch.id)
                    for (sid in scriptIds) {
                        if (sid !in exportedScriptIds) exportedScriptIds.add(sid)
                    }
                }
            }
            scriptIdToIndex = exportedScriptIds.withIndex().associate { (idx, id) -> id to idx }

            val scriptsArr = JSONArray()
            for (scriptId in exportedScriptIds) {
                val script = db.scriptDao().getScriptById(scriptId) ?: continue
                val sObj = JSONObject().apply {
                    put("title", script.title); put("fileName", script.fileName)
                    put("fileType", script.fileType); put("rawContent", script.rawContent)
                    put("practiceCount", script.practiceCount)
                }
                val linesArr = JSONArray()
                db.scriptLineDao().getLinesForScriptSync(scriptId).forEach { line ->
                    linesArr.put(JSONObject().apply {
                        put("lineNumber", line.lineNumber); put("character", line.character)
                        put("dialogue", line.dialogue)
                        if (line.stageDirection != null) put("stageDirection", line.stageDirection)
                        if (line.editedDialogue != null) put("editedDialogue", line.editedDialogue)
                        if (line.ignoredWords != null) put("ignoredWords", line.ignoredWords)
                        if (line.userNotes != null) put("userNotes", line.userNotes)
                        put("isMemorized", line.isMemorized); put("isSkipped", line.isSkipped)
                    })
                }
                sObj.put("lines", linesArr)

                if (options.recordings) {
                    val recArr = JSONArray()
                    db.audioRecordingDao().getRecordingsForScriptSync(scriptId).forEach { rec ->
                        val src = File(rec.filePath)
                        val relPath = if (audioOut != null && src.exists()) {
                            val ext = src.extension.ifBlank { "m4a" }
                            val rel = "audio/script_${scriptIdToIndex[scriptId] ?: 0}/rec_${rec.id}.$ext"
                            audioOut.add(rel to src)
                            rel
                        } else ""
                        recArr.put(JSONObject().apply {
                            put("characterName", rec.characterName)
                            put("lineNumber", rec.lineNumber)
                            put("durationMs", rec.durationMs)
                            put("recordedBy", rec.recordedBy)
                            put("createdAt", rec.createdAt)
                            put("relativePath", relPath)
                        })
                    }
                    sObj.put("recordings", recArr)
                }
                scriptsArr.put(sObj)
            }
            pObj.put("scripts", scriptsArr)
        } else {
            scriptIdToIndex = emptyMap()
        }

        // Castings -> Characters (with script index references)
        if (options.castingsAndCharacters) {
            val castingsArr = JSONArray()
            for (casting in castings) {
                val cObj = JSONObject().apply {
                    put("name", casting.name); put("notes", casting.notes)
                }
                val charsArr = JSONArray()
                val characters = db.characterDao().getCharactersForCastingSync(casting.id)
                for (ch in characters) {
                    val chObj = JSONObject().apply {
                        put("name", ch.name); put("notes", ch.notes)
                        put("isUserRole", ch.isUserRole); put("lineCount", ch.lineCount)
                    }
                    val linkedIndices = JSONArray()
                    if (options.scripts) {
                        val scriptIds = db.characterScriptDao().getScriptIdsForCharacter(ch.id)
                        for (sid in scriptIds) {
                            val idx = scriptIdToIndex[sid]
                            if (idx != null) linkedIndices.put(idx)
                        }
                    }
                    chObj.put("linkedScriptIndices", linkedIndices)
                    charsArr.put(chObj)
                }
                cObj.put("characters", charsArr)
                castingsArr.put(cObj)
            }
            pObj.put("castings", castingsArr)
        }

        return pObj
    }

    suspend fun importProjectsFromJson(context: Context, inputUri: Uri): Int {
        return try {
            val db = AppDatabase.getInstance(context)
            val bytes = context.contentResolver.openInputStream(inputUri)?.use { it.readBytes() } ?: return 0

            var audioBaseDir: File? = null
            val json: String = if (bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()
                && bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()) {
                // ZIP: extract to temp dir
                val tmp = File(context.cacheDir, "import_${System.currentTimeMillis()}")
                tmp.mkdirs()
                var projectJson: String? = null
                java.util.zip.ZipInputStream(java.io.ByteArrayInputStream(bytes)).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            val safe = entry.name.replace("..", "_")
                            val out = File(tmp, safe)
                            out.parentFile?.mkdirs()
                            if (safe == "project.json") {
                                projectJson = zis.bufferedReader().readText()
                            } else {
                                out.outputStream().use { zis.copyTo(it) }
                            }
                        }
                        zis.closeEntry(); entry = zis.nextEntry
                    }
                }
                audioBaseDir = tmp
                projectJson ?: return 0
            } else {
                bytes.toString(Charsets.UTF_8)
            }

            val root = JSONObject(json)
            val projectsArr = root.getJSONArray("projects")
            var count = 0

            for (i in 0 until projectsArr.length()) {
                val pObj = projectsArr.getJSONObject(i)
                val incomingShareId = pObj.optString("shareId", "")
                val existingProject = if (incomingShareId.isNotBlank()) db.projectDao().getByShareId(incomingShareId) else null
                val isMerge = existingProject != null

                val newPid: Long = if (isMerge) {
                    existingProject!!.id
                } else {
                    val project = Project(
                        name = pObj.getString("name"),
                        director = pObj.optString("director", ""),
                        castingDirector = pObj.optString("castingDirector", ""),
                        notes = pObj.optString("notes", ""),
                        direction = pObj.optString("direction", ""),
                        startDate = if (pObj.isNull("startDate")) null else pObj.getLong("startDate"),
                        endDate = if (pObj.isNull("endDate")) null else pObj.getLong("endDate"),
                        auditionDatesJson = pObj.optString("auditionDatesJson", "[]"),
                        presentationDatesJson = pObj.optString("presentationDatesJson", "[]"),
                        linksJson = pObj.optString("linksJson", "[]"),
                        attachmentsJson = pObj.optString("attachmentsJson", "[]"),
                        teamJson = pObj.optString("teamJson", "[]"),
                        locationsJson = pObj.optString("locationsJson", "[]"),
                        cardColor = pObj.optString("cardColor", ""),
                        cardImageUri = pObj.optString("cardImageUri", ""),
                        shareId = incomingShareId.ifBlank { java.util.UUID.randomUUID().toString() }
                    )
                    db.projectDao().insert(project)
                }

                // Contacts (skip in merge mode to avoid duplicates)
                val contactsArr = pObj.optJSONArray("contacts")
                if (contactsArr != null && !isMerge) {
                    for (j in 0 until contactsArr.length()) {
                        val c = contactsArr.getJSONObject(j)
                        db.projectContactDao().insert(ProjectContact(
                            projectId = newPid, name = c.getString("name"),
                            phone = c.optString("phone", ""), email = c.optString("email", ""),
                            rolesJson = c.optString("rolesJson", "[]")
                        ))
                    }
                }

                // Auditions (skip in merge mode)
                val auditionsArr = pObj.optJSONArray("auditions")
                if (auditionsArr != null && !isMerge) {
                    for (j in 0 until auditionsArr.length()) {
                        val a = auditionsArr.getJSONObject(j)
                        db.auditionDao().insert(Audition(
                            projectId = newPid, projectName = a.optString("projectName", pObj.optString("name", "")),
                            roleName = a.optString("roleName", ""), castingDirector = a.optString("castingDirector", ""),
                            auditionDate = if (a.isNull("auditionDate")) null else a.getLong("auditionDate"),
                            status = a.optString("status", "SUBMITTED"), notes = a.optString("notes", ""),
                            linksJson = a.optString("linksJson", "[]"), attachmentsJson = a.optString("attachmentsJson", "[]"),
                            imagesJson = a.optString("imagesJson", "[]")
                        ))
                    }
                }

                // Scripts: reuse existing by title when merging; otherwise insert fresh
                val scriptIdMap = mutableMapOf<Int, Long>() // JSON index -> DB id
                val scriptsArr = pObj.optJSONArray("scripts")
                if (scriptsArr != null) {
                    for (j in 0 until scriptsArr.length()) {
                        val s = scriptsArr.getJSONObject(j)
                        val title = s.getString("title")
                        val existingScript = if (isMerge) db.scriptDao().getByProjectAndTitle(newPid, title) else null
                        val scriptId: Long = if (existingScript != null) {
                            existingScript.id
                        } else {
                            val newId = db.scriptDao().insert(Script(
                                title = title, fileName = s.optString("fileName", ""),
                                fileType = s.optString("fileType", "txt"), rawContent = s.optString("rawContent", ""),
                                practiceCount = s.optInt("practiceCount", 0),
                                projectId = newPid
                            ))
                            val linesArr = s.optJSONArray("lines")
                            if (linesArr != null) {
                                val lines = (0 until linesArr.length()).map { k ->
                                    val l = linesArr.getJSONObject(k)
                                    ScriptLine(
                                        scriptId = newId, lineNumber = l.getInt("lineNumber"),
                                        character = l.getString("character"), dialogue = l.getString("dialogue"),
                                        stageDirection = l.optString("stageDirection", null),
                                        editedDialogue = l.optString("editedDialogue", null),
                                        ignoredWords = l.optString("ignoredWords", null),
                                        userNotes = l.optString("userNotes", null),
                                        isMemorized = l.optBoolean("isMemorized", false),
                                        isSkipped = l.optBoolean("isSkipped", false)
                                    )
                                }
                                db.scriptLineDao().insertAll(lines)
                            }
                            newId
                        }
                        scriptIdMap[j] = scriptId

                        // Recordings: replace on conflict (scriptId, character, lineNumber)
                        val recordingsArr = s.optJSONArray("recordings")
                        if (recordingsArr != null && audioBaseDir != null) {
                            val recDir = File(context.filesDir, "recordings")
                            if (!recDir.exists()) recDir.mkdirs()
                            for (k in 0 until recordingsArr.length()) {
                                val r = recordingsArr.getJSONObject(k)
                                val rel = r.optString("relativePath", "")
                                if (rel.isBlank()) continue
                                val src = File(audioBaseDir, rel)
                                if (!src.exists()) continue
                                val charName = r.optString("characterName", "")
                                val lineNum = r.optInt("lineNumber", 0)
                                val existingRec = db.audioRecordingDao().getRecording(scriptId, charName, lineNum)
                                if (existingRec != null) {
                                    try { File(existingRec.filePath).delete() } catch (_: Exception) {}
                                    db.audioRecordingDao().delete(existingRec)
                                }
                                val ext = src.extension.ifBlank { "m4a" }
                                val dst = File(recDir, "import_${scriptId}_${lineNum}_${System.currentTimeMillis()}_$k.$ext")
                                try {
                                    src.inputStream().use { input -> dst.outputStream().use { input.copyTo(it) } }
                                    db.audioRecordingDao().insert(AudioRecording(
                                        scriptId = scriptId,
                                        characterName = charName,
                                        lineNumber = lineNum,
                                        filePath = dst.absolutePath,
                                        durationMs = r.optLong("durationMs", 0),
                                        recordedBy = r.optString("recordedBy", "imported"),
                                        createdAt = r.optLong("createdAt", System.currentTimeMillis())
                                    ))
                                } catch (_: Exception) {}
                            }
                        }
                    }
                }

                // Castings -> Characters -> CharacterScript links (skip in merge mode)
                val castingsArr = pObj.optJSONArray("castings")
                if (castingsArr != null && !isMerge) {
                    for (j in 0 until castingsArr.length()) {
                        val cObj = castingsArr.getJSONObject(j)
                        val newCastingId = db.castingDao().insert(Casting(
                            projectId = newPid, name = cObj.getString("name"),
                            notes = cObj.optString("notes", "")
                        ))

                        val charsArr = cObj.optJSONArray("characters")
                        if (charsArr != null) {
                            for (k in 0 until charsArr.length()) {
                                val chObj = charsArr.getJSONObject(k)
                                val newCharId = db.characterDao().insert(Character(
                                    castingId = newCastingId, name = chObj.getString("name"),
                                    notes = chObj.optString("notes", ""),
                                    isUserRole = chObj.optBoolean("isUserRole", false),
                                    lineCount = chObj.optInt("lineCount", 0)
                                ))

                                // Link scripts to character
                                val linkedIndices = chObj.optJSONArray("linkedScriptIndices")
                                if (linkedIndices != null) {
                                    for (si in 0 until linkedIndices.length()) {
                                        val scriptIdx = linkedIndices.getInt(si)
                                        val newScriptId = scriptIdMap[scriptIdx]
                                        if (newScriptId != null) {
                                            db.characterScriptDao().link(CharacterScript(newCharId, newScriptId))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                count++
            }
            count
        } catch (e: Exception) {
            Log.e("DatabaseBackup", "Import failed", e)
            0
        }
    }

    // ── Share project via Android share sheet ──

    data class ShareOptions(
        val datesAndLocations: Boolean = true,
        val linksAndAttachments: Boolean = true,
        val team: Boolean = true,
        val contacts: Boolean = true,
        val auditions: Boolean = true,
        val scripts: Boolean = true,
        val castingsAndCharacters: Boolean = true,
        val recordings: Boolean = false
    )

    suspend fun shareProject(context: Context, projectId: Long, options: ShareOptions = ShareOptions()) {
        try {
            val db = AppDatabase.getInstance(context)
            val project = db.projectDao().getById(projectId) ?: return

            val root = JSONObject()
            root.put("version", 2)
            root.put("exportDate", System.currentTimeMillis())

            val audioFiles = mutableListOf<Pair<String, File>>()
            val pObj = exportFullProject(db, projectId, options, audioFiles) ?: return

            val projectsArr = JSONArray()
            projectsArr.put(pObj)
            root.put("projects", projectsArr)

            val shareDir = File(context.cacheDir, "share")
            shareDir.mkdirs()
            val fileName = project.name.replace(Regex("[^a-zA-Z0-9 ]"), "").replace(" ", "_").take(30)

            val (shareFile, mime) = if (options.recordings && audioFiles.isNotEmpty()) {
                val zip = File(shareDir, "${fileName}.zip")
                java.util.zip.ZipOutputStream(zip.outputStream().buffered()).use { zos ->
                    zos.putNextEntry(java.util.zip.ZipEntry("project.json"))
                    zos.write(root.toString(2).toByteArray(Charsets.UTF_8))
                    zos.closeEntry()
                    for ((rel, src) in audioFiles) {
                        if (!src.exists()) continue
                        zos.putNextEntry(java.util.zip.ZipEntry(rel))
                        src.inputStream().buffered().use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
                zip to "application/zip"
            } else {
                val json = File(shareDir, "${fileName}.json")
                json.writeText(root.toString(2))
                json to "application/json"
            }

            val uri = FileProvider.getUriForFile(context, getAuthority(context), shareFile)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Actors Toolkit - ${project.name}")
                putExtra(Intent.EXTRA_TEXT, "Project: ${project.name}\nDirector: ${project.director}\nShared from Actors Toolkit")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Share Project")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                context.startActivity(chooser)
            }
        } catch (_: Exception) {}
    }

    // ── Helpers ──

    // ── Send lines (rehearsal pack for a specific contact & role) ──

    suspend fun sendLinesToContact(
        context: Context,
        projectId: Long,
        scriptId: Long,
        characterName: String,
        contactName: String = "",
        contactEmail: String = "",
        contactPhone: String = ""
    ): Boolean {
        try {
            val db = AppDatabase.getInstance(context)
            val project = db.projectDao().getById(projectId) ?: return false
            val script = db.scriptDao().getScriptById(scriptId) ?: return false

            val audioFiles = mutableListOf<Pair<String, File>>()

            val pObj = JSONObject().apply {
                put("shareId", project.shareId)
                put("name", project.name)
                put("cardColor", project.cardColor)
            }

            val sObj = JSONObject().apply {
                put("title", script.title)
                put("fileName", script.fileName)
                put("fileType", script.fileType)
                put("rawContent", script.rawContent)
                put("practiceCount", 0)
            }
            val linesArr = JSONArray()
            db.scriptLineDao().getLinesForScriptSync(scriptId).forEach { line ->
                linesArr.put(JSONObject().apply {
                    put("lineNumber", line.lineNumber); put("character", line.character)
                    put("dialogue", line.dialogue)
                    if (line.stageDirection != null) put("stageDirection", line.stageDirection)
                    put("isMemorized", false); put("isSkipped", false)
                })
            }
            sObj.put("lines", linesArr)

            val recArr = JSONArray()
            db.audioRecordingDao().getRecordingsForScriptSync(scriptId)
                .filter { it.characterName == characterName }
                .forEach { rec ->
                    val src = File(rec.filePath)
                    if (!src.exists()) return@forEach
                    val ext = src.extension.ifBlank { "m4a" }
                    val rel = "audio/role/rec_${rec.id}.$ext"
                    audioFiles.add(rel to src)
                    recArr.put(JSONObject().apply {
                        put("characterName", rec.characterName)
                        put("lineNumber", rec.lineNumber)
                        put("durationMs", rec.durationMs)
                        put("recordedBy", rec.recordedBy)
                        put("createdAt", rec.createdAt)
                        put("relativePath", rel)
                    })
                }
            sObj.put("recordings", recArr)

            pObj.put("scripts", JSONArray().put(sObj))

            val root = JSONObject().apply {
                put("version", 2)
                put("exportDate", System.currentTimeMillis())
                put("projects", JSONArray().put(pObj))
            }

            val shareDir = File(context.cacheDir, "share")
            shareDir.mkdirs()
            val safeName = (project.name + "_" + characterName).replace(Regex("[^a-zA-Z0-9 ]"), "").replace(" ", "_").take(40)
            val zip = File(shareDir, "${safeName}_lines.zip")
            java.util.zip.ZipOutputStream(zip.outputStream().buffered()).use { zos ->
                zos.putNextEntry(java.util.zip.ZipEntry("project.json"))
                zos.write(root.toString(2).toByteArray(Charsets.UTF_8))
                zos.closeEntry()
                for ((rel, src) in audioFiles) {
                    zos.putNextEntry(java.util.zip.ZipEntry(rel))
                    src.inputStream().buffered().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }

            val uri = FileProvider.getUriForFile(context, getAuthority(context), zip)
            val forName = if (contactName.isNotBlank()) " for $contactName" else ""
            val bodyText = buildString {
                append("Rehearsal lines for $characterName in \"${project.name}\"")
                if (contactName.isNotBlank()) append(" — hi $contactName!")
                append("\n\nImport this file in Actors Toolkit to rehearse with my voice.")
            }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "${project.name} — $characterName lines$forName")
                putExtra(Intent.EXTRA_TEXT, bodyText)
                if (contactEmail.isNotBlank()) putExtra(Intent.EXTRA_EMAIL, arrayOf(contactEmail))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Send via")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                context.startActivity(chooser)
            }
            return true
        } catch (_: Exception) {
            return false
        }
    }

    private fun projectToJson(project: Project, options: ShareOptions = ShareOptions()): JSONObject {
        return JSONObject().apply {
            put("shareId", project.shareId)
            put("name", project.name)
            put("director", project.director)
            put("castingDirector", project.castingDirector)
            put("notes", project.notes)
            put("direction", project.direction)
            if (options.datesAndLocations) {
                put("startDate", project.startDate ?: JSONObject.NULL)
                put("endDate", project.endDate ?: JSONObject.NULL)
                put("auditionDatesJson", project.auditionDatesJson)
                put("presentationDatesJson", project.presentationDatesJson)
                put("locationsJson", project.locationsJson)
            }
            if (options.linksAndAttachments) {
                put("linksJson", project.linksJson)
                put("attachmentsJson", project.attachmentsJson)
            }
            if (options.team) {
                put("teamJson", project.teamJson)
            }
            put("cardColor", project.cardColor)
            put("cardImageUri", project.cardImageUri)
        }
    }

    private fun auditionToJson(a: Audition, options: ShareOptions = ShareOptions()): JSONObject {
        return JSONObject().apply {
            put("projectName", a.projectName); put("roleName", a.roleName); put("castingDirector", a.castingDirector)
            put("auditionDate", a.auditionDate ?: JSONObject.NULL); put("status", a.status); put("notes", a.notes)
            if (options.linksAndAttachments) {
                put("linksJson", a.linksJson); put("attachmentsJson", a.attachmentsJson); put("imagesJson", a.imagesJson)
            }
        }
    }
}
