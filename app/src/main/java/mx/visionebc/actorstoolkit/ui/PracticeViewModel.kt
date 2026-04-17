package mx.visionebc.actorstoolkit.ui

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import mx.visionebc.actorstoolkit.data.entity.AudioRecording
import mx.visionebc.actorstoolkit.data.entity.BlockingMark
import mx.visionebc.actorstoolkit.data.entity.Character
import mx.visionebc.actorstoolkit.data.entity.MovementPath
import mx.visionebc.actorstoolkit.data.entity.ScriptLine
import mx.visionebc.actorstoolkit.data.entity.StageItem
import mx.visionebc.actorstoolkit.data.repository.ScriptRepository
import java.io.File
import java.io.IOException

enum class PracticeMode {
    READ_THROUGH,
    HIDE_MY_LINES,
    MEMORIZATION
}

data class PracticeState(
    val scriptTitle: String = "",
    val lines: List<ScriptLine> = emptyList(),
    val characters: List<Character> = emptyList(),
    val userRole: Character? = null,
    val currentLineIndex: Int = 0,
    val mode: PracticeMode = PracticeMode.READ_THROUGH,
    val revealedLines: Set<Long> = emptySet(),
    val isLoading: Boolean = true,
    val showRoleSelector: Boolean = false,
    val ttsEnabled: Boolean = true,
    val progress: Float = 0f,
    val error: String? = null,
    val isAutoPlaying: Boolean = false,
    val editingLine: ScriptLine? = null,
    val showBlocking: Boolean = false,
    val blockingMarks: List<BlockingMark> = emptyList(),
    val stageItems: List<StageItem> = emptyList(),
    val movementPaths: List<MovementPath> = emptyList(),
    // Voice recording state
    val isRecording: Boolean = false,
    val recordingLineIndex: Int = -1,
    val isPlayingRecording: Boolean = false,
    val playingLineIndex: Int = -1,
    val recordings: Map<Int, AudioRecording> = emptyMap(), // lineNumber -> recording
    val characterVoices: Map<String, String> = emptyMap() // characterName -> TTS voice name
)

class PracticeViewModel(private val repository: ScriptRepository) : ViewModel() {

    private val _state = MutableStateFlow(PracticeState())
    val state: StateFlow<PracticeState> = _state

    // Set from the screen via setPlayOwnRecordings(); defaults to false ("say them live")
    private var playOwnRecordings: Boolean = false
    fun setPlayOwnRecordings(value: Boolean) { playOwnRecordings = value }

    private fun shouldPlayRecording(lineCharacter: String): Boolean {
        val myRole = _state.value.userRole?.name
        return playOwnRecordings || lineCharacter != myRole
    }

    var tts: TextToSpeech? = null
    private var autoPlayJob: Job? = null
    private val _ttsFinished = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    // Audio recording
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentRecordingPath: String? = null
    private var recordingStartTime: Long = 0L
    private var appContext: Context? = null
    private var currentScriptId: Long = 0L

    fun setAppContext(context: Context) {
        appContext = context.applicationContext
    }

    fun loadScript(scriptId: Long) {
        Log.d("PracticeVM", "loadScript called with id=$scriptId")
        currentScriptId = scriptId
        viewModelScope.launch {
            try {
                _state.value = PracticeState(isLoading = true)
                Log.d("PracticeVM", "Loading script info...")

                val scriptInfo = repository.getScriptInfo(scriptId)
                if (scriptInfo == null) {
                    Log.w("PracticeVM", "Script not found for id=$scriptId")
                    _state.value = PracticeState(
                        isLoading = false,
                        error = "Script not found (id=$scriptId)"
                    )
                    return@launch
                }
                Log.d("PracticeVM", "Script found: ${scriptInfo.title}")

                val lines = repository.getLinesSync(scriptId)
                Log.d("PracticeVM", "Loaded ${lines.size} lines")

                val characters = repository.getCharactersSync(scriptId)
                Log.d("PracticeVM", "Loaded ${characters.size} characters")

                val userRole = try {
                    repository.getUserRole(scriptId)
                } catch (e: Exception) {
                    Log.w("PracticeVM", "Failed to get user role", e)
                    null
                }

                // Load blocking data
                val blockingMarks = try {
                    val result = mutableListOf<BlockingMark>()
                    repository.getBlockingMarks(scriptId).first().let { result.addAll(it) }
                    result
                } catch (e: Exception) {
                    Log.w("PracticeVM", "Failed to load blocking marks", e)
                    emptyList()
                }

                val stageItems = try {
                    repository.getStageItemsSync(scriptId)
                } catch (e: Exception) {
                    Log.w("PracticeVM", "Failed to load stage items", e)
                    emptyList()
                }

                val movementPaths = try {
                    val result = mutableListOf<MovementPath>()
                    repository.getMovementPaths(scriptId).first().let { result.addAll(it) }
                    result
                } catch (e: Exception) {
                    Log.w("PracticeVM", "Failed to load movement paths", e)
                    emptyList()
                }

                // Load existing audio recordings
                val recordingsMap = loadRecordingsMap(scriptId)

                // Load per-character voice mapping
                val voicesMap = try {
                    val script = repository.getScript(scriptId)
                    parseVoicesJson(script?.characterVoicesJson ?: "{}")
                } catch (_: Exception) { emptyMap() }

                _state.value = PracticeState(
                    scriptTitle = scriptInfo.title,
                    lines = lines,
                    characters = characters,
                    userRole = userRole,
                    isLoading = false,
                    showRoleSelector = userRole == null && characters.isNotEmpty(),
                    error = null,
                    blockingMarks = blockingMarks,
                    stageItems = stageItems,
                    movementPaths = movementPaths,
                    recordings = recordingsMap,
                    characterVoices = voicesMap
                )

                try {
                    repository.updatePracticeStats(scriptId)
                } catch (e: Exception) {
                    Log.w("PracticeVM", "Failed to update practice stats", e)
                }
            } catch (e: Exception) {
                Log.e("PracticeVM", "Failed to load script", e)
                _state.value = PracticeState(
                    isLoading = false,
                    error = "Error loading script: ${e.javaClass.simpleName}: ${e.message}"
                )
            }
        }
    }

    private suspend fun loadRecordingsMap(scriptId: Long): Map<Int, AudioRecording> {
        return try {
            val recordings = mutableMapOf<Int, AudioRecording>()
            repository.getAudioRecordings(scriptId).first().forEach { rec ->
                recordings[rec.lineNumber] = rec
            }
            recordings
        } catch (e: Exception) {
            Log.w("PracticeVM", "Failed to load recordings", e)
            emptyMap()
        }
    }

    // ── Voice Recording ─────────────────────────────

    fun startRecording(lineIndex: Int) {
        val ctx = appContext ?: return
        val current = _state.value
        if (current.isRecording || current.isAutoPlaying) return
        if (lineIndex !in current.lines.indices) return

        val line = current.lines[lineIndex]
        val dir = File(ctx.filesDir, "recordings")
        if (!dir.exists()) dir.mkdirs()
        val filePath = File(dir, "script_${currentScriptId}_line_${line.lineNumber}_${System.currentTimeMillis()}.m4a").absolutePath
        currentRecordingPath = filePath
        recordingStartTime = System.currentTimeMillis()

        try {
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(ctx)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(filePath)
                prepare()
                start()
            }
            _state.value = current.copy(
                isRecording = true,
                recordingLineIndex = lineIndex
            )
            Log.d("PracticeVM", "Recording started for line ${line.lineNumber}")
        } catch (e: IOException) {
            Log.e("PracticeVM", "Failed to start recording", e)
            mediaRecorder?.release()
            mediaRecorder = null
            currentRecordingPath = null
        }
    }

    fun stopRecording() {
        val current = _state.value
        if (!current.isRecording) return

        val durationMs = System.currentTimeMillis() - recordingStartTime
        val filePath = currentRecordingPath

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("PracticeVM", "Error stopping recorder", e)
        }
        mediaRecorder = null

        _state.value = current.copy(
            isRecording = false,
            recordingLineIndex = -1
        )

        // Save to database
        if (filePath != null && current.recordingLineIndex in current.lines.indices) {
            val line = current.lines[current.recordingLineIndex]
            viewModelScope.launch {
                try {
                    // Delete old recording for this line if exists
                    val existingRec = current.recordings[line.lineNumber]
                    if (existingRec != null) {
                        repository.deleteAudioRecording(existingRec)
                        // Delete old file
                        try { File(existingRec.filePath).delete() } catch (_: Exception) {}
                    }

                    val recording = AudioRecording(
                        scriptId = currentScriptId,
                        characterName = line.character,
                        lineNumber = line.lineNumber,
                        filePath = filePath,
                        durationMs = durationMs,
                        recordedBy = "local"
                    )
                    val id = repository.insertAudioRecording(recording)
                    val saved = recording.copy(id = id)

                    val updatedRecordings = _state.value.recordings.toMutableMap()
                    updatedRecordings[line.lineNumber] = saved
                    _state.value = _state.value.copy(recordings = updatedRecordings)
                    Log.d("PracticeVM", "Recording saved: line ${line.lineNumber}, ${durationMs}ms")
                } catch (e: Exception) {
                    Log.e("PracticeVM", "Failed to save recording", e)
                }
            }
        }
        currentRecordingPath = null
    }

    fun playRecording(lineIndex: Int) {
        val current = _state.value
        if (current.isRecording || current.isPlayingRecording) {
            stopPlayback()
            if (current.playingLineIndex == lineIndex) return // toggle off
        }
        if (lineIndex !in current.lines.indices) return

        val line = current.lines[lineIndex]
        val recording = current.recordings[line.lineNumber] ?: return
        val file = File(recording.filePath)
        if (!file.exists()) {
            Log.w("PracticeVM", "Recording file not found: ${recording.filePath}")
            // Remove stale recording
            viewModelScope.launch {
                try {
                    repository.deleteAudioRecording(recording)
                    val updated = _state.value.recordings.toMutableMap()
                    updated.remove(line.lineNumber)
                    _state.value = _state.value.copy(recordings = updated)
                } catch (_: Exception) {}
            }
            return
        }

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(recording.filePath)
                prepare()
                setOnCompletionListener {
                    _state.value = _state.value.copy(
                        isPlayingRecording = false,
                        playingLineIndex = -1
                    )
                    release()
                    mediaPlayer = null
                }
                start()
            }
            _state.value = current.copy(
                isPlayingRecording = true,
                playingLineIndex = lineIndex
            )
        } catch (e: Exception) {
            Log.e("PracticeVM", "Failed to play recording", e)
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null
        _state.value = _state.value.copy(
            isPlayingRecording = false,
            playingLineIndex = -1
        )
    }

    fun deleteRecording(lineIndex: Int) {
        val current = _state.value
        if (lineIndex !in current.lines.indices) return
        val line = current.lines[lineIndex]
        val recording = current.recordings[line.lineNumber] ?: return

        // Stop playback if playing this one
        if (current.playingLineIndex == lineIndex) stopPlayback()

        viewModelScope.launch {
            try {
                repository.deleteAudioRecording(recording)
                try { File(recording.filePath).delete() } catch (_: Exception) {}
                val updated = _state.value.recordings.toMutableMap()
                updated.remove(line.lineNumber)
                _state.value = _state.value.copy(recordings = updated)
                Log.d("PracticeVM", "Deleted recording for line ${line.lineNumber}")
            } catch (e: Exception) {
                Log.e("PracticeVM", "Failed to delete recording", e)
            }
        }
    }

    fun hasRecording(lineIndex: Int): Boolean {
        val current = _state.value
        if (lineIndex !in current.lines.indices) return false
        return current.recordings.containsKey(current.lines[lineIndex].lineNumber)
    }

    // ── Existing methods ─────────────────────────────

    fun selectRole(character: Character) {
        viewModelScope.launch {
            try {
                val sid = character.scriptId ?: return@launch
                repository.setUserRole(sid, character.id)
                _state.value = _state.value.copy(
                    userRole = character.copy(isUserRole = true),
                    showRoleSelector = false
                )
            } catch (e: Exception) {
                Log.e("PracticeVM", "Failed to select role", e)
            }
        }
    }

    fun setMode(mode: PracticeMode) {
        stopAutoPlay()
        _state.value = _state.value.copy(
            mode = mode,
            revealedLines = emptySet(),
            currentLineIndex = 0
        )
    }

    fun toggleBlocking() {
        _state.value = _state.value.copy(showBlocking = !_state.value.showBlocking)
    }

    fun getCurrentBlockingMarks(): List<BlockingMark> {
        val current = _state.value
        if (current.lines.isEmpty() || current.blockingMarks.isEmpty()) return emptyList()

        val currentLine = current.lines[current.currentLineIndex]
        val lineMarks = current.blockingMarks.filter { it.lineNumber == currentLine.lineNumber }
        if (lineMarks.isNotEmpty()) return lineMarks

        val inherited = mutableListOf<BlockingMark>()
        val seenChars = mutableSetOf<String>()
        for (i in current.currentLineIndex downTo 0) {
            val ln = current.lines[i].lineNumber
            val marks = current.blockingMarks.filter { it.lineNumber == ln }
            for (m in marks) {
                if (m.characterName !in seenChars) {
                    seenChars.add(m.characterName)
                    inherited.add(m)
                }
            }
            if (seenChars.size >= current.characters.size) break
        }
        return inherited
    }

    fun getCurrentMovementPaths(): List<MovementPath> {
        val current = _state.value
        if (current.lines.isEmpty()) return emptyList()
        val lineNum = current.lines[current.currentLineIndex].lineNumber
        return current.movementPaths.filter { it.lineNumber == lineNum }
    }

    private fun findNextUnskippedIndex(fromIndex: Int): Int {
        val current = _state.value
        var idx = fromIndex
        while (idx < current.lines.size && current.lines[idx].isSkipped) {
            idx++
        }
        return idx
    }

    private fun findPrevUnskippedIndex(fromIndex: Int): Int {
        val current = _state.value
        var idx = fromIndex
        while (idx >= 0 && current.lines[idx].isSkipped) {
            idx--
        }
        return idx
    }

    fun nextLine() {
        val current = _state.value
        if (current.currentLineIndex < current.lines.size - 1) {
            val nextIdx = findNextUnskippedIndex(current.currentLineIndex + 1)
            if (nextIdx < current.lines.size) {
                _state.value = current.copy(
                    currentLineIndex = nextIdx,
                    progress = if (current.lines.isNotEmpty()) nextIdx.toFloat() / current.lines.size.toFloat() else 0f
                )
                val nextLine = current.lines[nextIdx]
                if (current.ttsEnabled && nextLine.character != current.userRole?.name && nextLine.speakableDialogue.isNotBlank()) {
                    // Use recorded audio if available, otherwise TTS
                    val recording = current.recordings[nextLine.lineNumber]
                    if (recording != null && File(recording.filePath).exists() && shouldPlayRecording(nextLine.character)) {
                        playRecording(nextIdx)
                    } else {
                        speakLine(nextLine.speakableDialogue, nextLine.character)
                    }
                }
            }
        }
    }

    fun previousLine() {
        val current = _state.value
        if (current.currentLineIndex > 0) {
            val prevIdx = findPrevUnskippedIndex(current.currentLineIndex - 1)
            if (prevIdx >= 0) {
                _state.value = current.copy(
                    currentLineIndex = prevIdx,
                    progress = if (current.lines.isNotEmpty()) prevIdx.toFloat() / current.lines.size.toFloat() else 0f
                )
            }
        }
    }

    fun revealLine(lineId: Long) {
        val current = _state.value
        _state.value = current.copy(
            revealedLines = current.revealedLines + lineId
        )
    }

    fun toggleTts() {
        _state.value = _state.value.copy(ttsEnabled = !_state.value.ttsEnabled)
    }

    fun toggleSkipLine(line: ScriptLine) {
        viewModelScope.launch {
            try {
                val newSkipped = !line.isSkipped
                repository.setSkipped(line.id, newSkipped)
                val current = _state.value
                val updatedLines = current.lines.map {
                    if (it.id == line.id) it.copy(isSkipped = newSkipped) else it
                }
                _state.value = current.copy(lines = updatedLines)
            } catch (e: Exception) {
                Log.e("PracticeVM", "Failed to toggle skip", e)
            }
        }
    }

    fun openEditLine(line: ScriptLine) {
        _state.value = _state.value.copy(editingLine = line)
    }

    fun closeEditLine() {
        _state.value = _state.value.copy(editingLine = null)
    }

    fun saveEditedDialogue(lineId: Long, newDialogue: String?) {
        viewModelScope.launch {
            try {
                val current = _state.value
                val originalLine = current.lines.find { it.id == lineId }
                val toSave = if (newDialogue.isNullOrBlank() || newDialogue.trim() == originalLine?.dialogue?.trim()) {
                    null
                } else {
                    newDialogue.trim()
                }

                repository.updateEditedDialogue(lineId, toSave)
                val updatedLines = current.lines.map {
                    if (it.id == lineId) it.copy(editedDialogue = toSave) else it
                }
                _state.value = current.copy(lines = updatedLines, editingLine = null)
            } catch (e: Exception) {
                Log.e("PracticeVM", "Failed to save edited dialogue", e)
            }
        }
    }

    fun toggleIgnoredWord(lineId: Long, wordIndex: Int) {
        viewModelScope.launch {
            try {
                val current = _state.value
                val line = current.lines.find { it.id == lineId } ?: return@launch
                val currentIgnored = line.ignoredWordIndices.toMutableSet()

                if (wordIndex in currentIgnored) {
                    currentIgnored.remove(wordIndex)
                } else {
                    currentIgnored.add(wordIndex)
                }

                val newIgnoredStr = if (currentIgnored.isEmpty()) null
                else currentIgnored.sorted().joinToString(",")

                repository.updateIgnoredWords(lineId, newIgnoredStr)
                val updatedLines = current.lines.map {
                    if (it.id == lineId) it.copy(ignoredWords = newIgnoredStr) else it
                }
                val updatedEditingLine = if (current.editingLine?.id == lineId) {
                    current.editingLine.copy(ignoredWords = newIgnoredStr)
                } else current.editingLine

                _state.value = current.copy(lines = updatedLines, editingLine = updatedEditingLine)
            } catch (e: Exception) {
                Log.e("PracticeVM", "Failed to toggle ignored word", e)
            }
        }
    }

    fun updateUserNotes(lineId: Long, notes: String?) {
        viewModelScope.launch {
            try {
                val trimmed = notes?.trim()?.ifBlank { null }
                repository.updateUserNotes(lineId, trimmed)
                val current = _state.value
                val updatedLines = current.lines.map {
                    if (it.id == lineId) it.copy(userNotes = trimmed) else it
                }
                _state.value = current.copy(lines = updatedLines)
            } catch (e: Exception) {
                Log.e("PracticeVM", "Failed to update user notes", e)
            }
        }
    }

    fun resetLineEdits(lineId: Long) {
        viewModelScope.launch {
            try {
                repository.updateEditedDialogue(lineId, null)
                repository.updateIgnoredWords(lineId, null)
                val current = _state.value
                val updatedLines = current.lines.map {
                    if (it.id == lineId) it.copy(editedDialogue = null, ignoredWords = null) else it
                }
                val updatedEditingLine = if (current.editingLine?.id == lineId) {
                    current.editingLine.copy(editedDialogue = null, ignoredWords = null)
                } else current.editingLine
                _state.value = current.copy(lines = updatedLines, editingLine = updatedEditingLine)
            } catch (e: Exception) {
                Log.e("PracticeVM", "Failed to reset line edits", e)
            }
        }
    }

    fun markMemorized(lineId: Long, memorized: Boolean) {
        viewModelScope.launch {
            try {
                repository.setMemorized(lineId, memorized)
            } catch (e: Exception) {
                Log.e("PracticeVM", "Failed to mark memorized", e)
            }
        }
    }

    fun goToLine(index: Int) {
        val current = _state.value
        if (index in current.lines.indices) {
            _state.value = current.copy(
                currentLineIndex = index,
                progress = if (current.lines.isNotEmpty()) index.toFloat() / current.lines.size.toFloat() else 0f
            )
        }
    }

    fun playAll() {
        stopAutoPlay()
        _state.value = _state.value.copy(isAutoPlaying = true)

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                _ttsFinished.tryEmit(Unit)
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _ttsFinished.tryEmit(Unit)
            }
        })

        autoPlayJob = viewModelScope.launch {
            val current = _state.value
            var idx = current.currentLineIndex

            if (idx < current.lines.size && current.lines[idx].isSkipped) {
                idx = findNextUnskippedIndex(idx)
            }

            while (idx < _state.value.lines.size && _state.value.isAutoPlaying) {
                val lines = _state.value.lines
                val line = lines[idx]

                if (line.isSkipped) {
                    idx = findNextUnskippedIndex(idx + 1)
                    continue
                }

                _state.value = _state.value.copy(
                    currentLineIndex = idx,
                    progress = if (lines.isNotEmpty()) idx.toFloat() / lines.size.toFloat() else 0f
                )

                val isUserLine = line.character == _state.value.userRole?.name
                val textToSpeak = line.speakableDialogue

                // Check for voice recording first
                val recording = _state.value.recordings[line.lineNumber]
                if (recording != null && File(recording.filePath).exists() && shouldPlayRecording(line.character)) {
                    // Play the recorded audio
                    val completionFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
                    try {
                        mediaPlayer?.release()
                        mediaPlayer = MediaPlayer().apply {
                            setDataSource(recording.filePath)
                            prepare()
                            setOnCompletionListener {
                                completionFlow.tryEmit(Unit)
                                release()
                                mediaPlayer = null
                                _state.value = _state.value.copy(
                                    isPlayingRecording = false,
                                    playingLineIndex = -1
                                )
                            }
                            start()
                        }
                        _state.value = _state.value.copy(
                            isPlayingRecording = true,
                            playingLineIndex = idx
                        )
                        completionFlow.first()
                        delay(500)
                    } catch (e: Exception) {
                        Log.e("PracticeVM", "Failed to play recording in autoplay", e)
                        delay(1000)
                    }
                } else if (_state.value.ttsEnabled && !isUserLine && textToSpeak.isNotBlank()) {
                    speakLine(textToSpeak, line.character)
                    _ttsFinished.first()
                    delay(500)
                } else if (isUserLine) {
                    delay(3000)
                } else {
                    val readTime = (textToSpeak.length * 50L).coerceIn(1500, 8000)
                    delay(readTime)
                }

                idx = findNextUnskippedIndex(idx + 1)
            }

            _state.value = _state.value.copy(isAutoPlaying = false)
        }
    }

    fun stopAutoPlay() {
        autoPlayJob?.cancel()
        autoPlayJob = null
        tts?.stop()
        stopPlayback()
        _state.value = _state.value.copy(isAutoPlaying = false)
    }

    private fun speakLine(text: String, character: String? = null) {
        val engine = tts ?: return
        val voiceName = character?.let { _state.value.characterVoices[it] }
        if (!voiceName.isNullOrBlank()) {
            try {
                val v = engine.voices?.firstOrNull { it.name == voiceName }
                if (v != null) engine.voice = v
            } catch (_: Exception) {}
        } else {
            try { engine.voice = engine.defaultVoice } catch (_: Exception) {}
        }
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "line")
    }

    private fun parseVoicesJson(json: String): Map<String, String> = try {
        val obj = org.json.JSONObject(json)
        val map = mutableMapOf<String, String>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            map[k] = obj.optString(k, "")
        }
        map
    } catch (_: Exception) { emptyMap() }

    private fun voicesMapToJson(map: Map<String, String>): String {
        val obj = org.json.JSONObject()
        map.forEach { (k, v) -> if (v.isNotBlank()) obj.put(k, v) }
        return obj.toString()
    }

    fun setCharacterVoice(character: String, voiceName: String?) {
        val current = _state.value.characterVoices.toMutableMap()
        if (voiceName.isNullOrBlank()) current.remove(character) else current[character] = voiceName
        _state.value = _state.value.copy(characterVoices = current)
        val sid = currentScriptId
        if (sid > 0L) {
            viewModelScope.launch {
                try { repository.updateCharacterVoices(sid, voicesMapToJson(current)) } catch (_: Exception) {}
            }
        }
    }

    /** Sample a voice by name with a short phrase so the user can preview it. */
    fun previewVoice(voiceName: String, sample: String = "Hello, this is my voice for rehearsal.") {
        val engine = tts ?: return
        try {
            val v = engine.voices?.firstOrNull { it.name == voiceName }
            if (v != null) engine.voice = v
            engine.speak(sample, TextToSpeech.QUEUE_FLUSH, null, "preview")
        } catch (_: Exception) {}
    }

    /** List available TTS voices (name, locale tag). */
    fun availableVoices(): List<Pair<String, String>> {
        val engine = tts ?: return emptyList()
        return try {
            engine.voices.orEmpty()
                .filter { !it.isNetworkConnectionRequired }
                .map { it.name to it.locale.toLanguageTag() }
                .sortedBy { it.second }
        } catch (_: Exception) { emptyList() }
    }

    fun stopTts() {
        stopAutoPlay()
        tts?.stop()
    }

    override fun onCleared() {
        stopAutoPlay()
        stopPlayback()
        if (mediaRecorder != null) {
            try { mediaRecorder?.release() } catch (_: Exception) {}
            mediaRecorder = null
        }
        tts?.shutdown()
        super.onCleared()
    }

    class Factory(private val repository: ScriptRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PracticeViewModel(repository) as T
        }
    }
}
