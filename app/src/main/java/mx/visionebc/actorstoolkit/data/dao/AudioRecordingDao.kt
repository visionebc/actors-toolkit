package mx.visionebc.actorstoolkit.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import mx.visionebc.actorstoolkit.data.entity.AudioRecording

@Dao
interface AudioRecordingDao {
    @Query("SELECT * FROM audio_recordings WHERE scriptId = :scriptId ORDER BY lineNumber")
    fun getRecordingsForScript(scriptId: Long): Flow<List<AudioRecording>>

    @Query("SELECT * FROM audio_recordings WHERE scriptId = :scriptId ORDER BY lineNumber")
    suspend fun getRecordingsForScriptSync(scriptId: Long): List<AudioRecording>

    @Query("SELECT * FROM audio_recordings WHERE scriptId = :scriptId AND characterName = :character AND lineNumber = :lineNumber LIMIT 1")
    suspend fun getRecording(scriptId: Long, character: String, lineNumber: Int): AudioRecording?

    @Insert
    suspend fun insert(recording: AudioRecording): Long

    @Delete
    suspend fun delete(recording: AudioRecording)

    @Query("DELETE FROM audio_recordings WHERE scriptId = :scriptId")
    suspend fun deleteForScript(scriptId: Long)
}
