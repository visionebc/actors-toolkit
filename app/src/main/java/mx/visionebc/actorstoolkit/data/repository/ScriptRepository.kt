package mx.visionebc.actorstoolkit.data.repository

import kotlinx.coroutines.flow.Flow
import mx.visionebc.actorstoolkit.data.AppDatabase
import mx.visionebc.actorstoolkit.data.entity.*

class ScriptRepository(private val db: AppDatabase) {

    // Scripts
    fun getAllScripts(): Flow<List<Script>> = db.scriptDao().getAllScripts()
    fun getAllScriptInfos(): Flow<List<ScriptInfo>> = db.scriptDao().getAllScriptInfos()
    suspend fun getScript(id: Long): Script? = db.scriptDao().getScriptById(id)
    suspend fun getScriptInfo(id: Long): ScriptInfo? = db.scriptDao().getScriptInfoById(id)
    suspend fun insertScript(script: Script): Long = db.scriptDao().insert(script)
    suspend fun updateScript(script: Script) = db.scriptDao().update(script)
    suspend fun deleteScript(script: Script) = db.scriptDao().delete(script)
    suspend fun deleteScriptById(id: Long) = db.scriptDao().deleteById(id)
    suspend fun updatePracticeStats(scriptId: Long) = db.scriptDao().updatePracticeStats(scriptId, System.currentTimeMillis())
    suspend fun updateCharacterVoices(scriptId: Long, json: String) = db.scriptDao().updateCharacterVoices(scriptId, json)

    // Lines
    fun getLines(scriptId: Long): Flow<List<ScriptLine>> = db.scriptLineDao().getLinesForScript(scriptId)
    suspend fun getLinesSync(scriptId: Long): List<ScriptLine> = db.scriptLineDao().getLinesForScriptSync(scriptId)
    fun getLinesForCharacter(scriptId: Long, character: String): Flow<List<ScriptLine>> =
        db.scriptLineDao().getLinesForCharacter(scriptId, character)
    suspend fun insertLines(lines: List<ScriptLine>) = db.scriptLineDao().insertAll(lines)
    suspend fun setMemorized(lineId: Long, memorized: Boolean) = db.scriptLineDao().setMemorized(lineId, memorized)
    suspend fun setSkipped(lineId: Long, skipped: Boolean) = db.scriptLineDao().setSkipped(lineId, skipped)
    suspend fun updateEditedDialogue(lineId: Long, editedDialogue: String?) = db.scriptLineDao().updateEditedDialogue(lineId, editedDialogue)
    suspend fun updateIgnoredWords(lineId: Long, ignoredWords: String?) = db.scriptLineDao().updateIgnoredWords(lineId, ignoredWords)
    suspend fun updateUserNotes(lineId: Long, notes: String?) = db.scriptLineDao().updateUserNotes(lineId, notes)

    // Characters
    fun getCharacters(scriptId: Long): Flow<List<Character>> = db.characterDao().getCharactersForScript(scriptId)
    suspend fun getCharactersSync(scriptId: Long): List<Character> = db.characterDao().getCharactersForScriptSync(scriptId)
    suspend fun getUserRole(scriptId: Long): Character? = db.characterDao().getUserRole(scriptId)
    suspend fun insertCharacters(characters: List<Character>) = db.characterDao().insertAll(characters)
    suspend fun setUserRole(scriptId: Long, characterId: Long) {
        db.characterDao().clearUserRoles(scriptId)
        db.characterDao().setUserRole(characterId)
    }

    // Audio
    fun getAudioRecordings(scriptId: Long): Flow<List<AudioRecording>> =
        db.audioRecordingDao().getRecordingsForScript(scriptId)
    suspend fun getAudioRecording(scriptId: Long, character: String, lineNumber: Int): AudioRecording? =
        db.audioRecordingDao().getRecording(scriptId, character, lineNumber)
    suspend fun insertAudioRecording(recording: AudioRecording): Long =
        db.audioRecordingDao().insert(recording)
    suspend fun deleteAudioRecording(recording: AudioRecording) =
        db.audioRecordingDao().delete(recording)

    // Blocking
    fun getBlockingMarks(scriptId: Long): Flow<List<BlockingMark>> =
        db.blockingMarkDao().getBlockingForScript(scriptId)
    suspend fun getBlockingForLine(scriptId: Long, lineNumber: Int): List<BlockingMark> =
        db.blockingMarkDao().getBlockingForLine(scriptId, lineNumber)
    suspend fun insertBlockingMark(mark: BlockingMark): Long =
        db.blockingMarkDao().insert(mark)
    suspend fun updateBlockingMark(mark: BlockingMark) =
        db.blockingMarkDao().update(mark)
    suspend fun deleteBlockingMark(mark: BlockingMark) =
        db.blockingMarkDao().delete(mark)

    // Stage Items
    fun getStageItems(scriptId: Long): Flow<List<StageItem>> =
        db.stageItemDao().getItemsForScript(scriptId)
    suspend fun getStageItemsSync(scriptId: Long): List<StageItem> =
        db.stageItemDao().getItemsForScriptSync(scriptId)
    suspend fun insertStageItem(item: StageItem): Long =
        db.stageItemDao().insert(item)
    suspend fun updateStageItem(item: StageItem) =
        db.stageItemDao().update(item)
    suspend fun deleteStageItem(item: StageItem) =
        db.stageItemDao().delete(item)

    // Movement Paths
    fun getMovementPaths(scriptId: Long): Flow<List<MovementPath>> =
        db.movementPathDao().getPathsForScript(scriptId)
    suspend fun getMovementPathsForLine(scriptId: Long, lineNumber: Int): List<MovementPath> =
        db.movementPathDao().getPathsForLine(scriptId, lineNumber)
    suspend fun insertMovementPath(path: MovementPath): Long =
        db.movementPathDao().insert(path)
    suspend fun deleteMovementPath(path: MovementPath) =
        db.movementPathDao().delete(path)
}
