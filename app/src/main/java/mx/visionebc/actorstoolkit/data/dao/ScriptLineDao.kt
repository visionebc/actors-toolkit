package mx.visionebc.actorstoolkit.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import mx.visionebc.actorstoolkit.data.entity.ScriptLine

@Dao
interface ScriptLineDao {
    @Query("SELECT * FROM script_lines WHERE scriptId = :scriptId ORDER BY lineNumber")
    fun getLinesForScript(scriptId: Long): Flow<List<ScriptLine>>

    @Query("SELECT * FROM script_lines WHERE scriptId = :scriptId ORDER BY lineNumber")
    suspend fun getLinesForScriptSync(scriptId: Long): List<ScriptLine>

    @Query("SELECT * FROM script_lines WHERE scriptId = :scriptId AND character = :character ORDER BY lineNumber")
    fun getLinesForCharacter(scriptId: Long, character: String): Flow<List<ScriptLine>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(lines: List<ScriptLine>)

    @Update
    suspend fun update(line: ScriptLine)

    @Query("UPDATE script_lines SET isMemorized = :memorized WHERE id = :lineId")
    suspend fun setMemorized(lineId: Long, memorized: Boolean)

    @Query("UPDATE script_lines SET isSkipped = :skipped WHERE id = :lineId")
    suspend fun setSkipped(lineId: Long, skipped: Boolean)

    @Query("UPDATE script_lines SET editedDialogue = :editedDialogue WHERE id = :lineId")
    suspend fun updateEditedDialogue(lineId: Long, editedDialogue: String?)

    @Query("UPDATE script_lines SET ignoredWords = :ignoredWords WHERE id = :lineId")
    suspend fun updateIgnoredWords(lineId: Long, ignoredWords: String?)

    @Query("DELETE FROM script_lines WHERE scriptId = :scriptId")
    suspend fun deleteForScript(scriptId: Long)
}
