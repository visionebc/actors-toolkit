package mx.visionebc.actorstoolkit.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import mx.visionebc.actorstoolkit.data.entity.CharacterScript
import mx.visionebc.actorstoolkit.data.entity.ScriptInfo

@Dao
interface CharacterScriptDao {

    @Query("""
        SELECT s.id, s.title, s.fileName, s.fileType, s.createdAt, s.updatedAt, s.lastPracticedAt, s.practiceCount
        FROM scripts s INNER JOIN character_scripts cs ON s.id = cs.scriptId
        WHERE cs.characterId = :characterId ORDER BY cs.assignedAt DESC
    """)
    fun getScriptInfosForCharacter(characterId: Long): Flow<List<ScriptInfo>>

    @Query("""
        SELECT s.id, s.title, s.fileName, s.fileType, s.createdAt, s.updatedAt, s.lastPracticedAt, s.practiceCount
        FROM scripts s WHERE s.id NOT IN (
            SELECT scriptId FROM character_scripts WHERE characterId = :characterId
        ) ORDER BY s.updatedAt DESC
    """)
    fun getAvailableScripts(characterId: Long): Flow<List<ScriptInfo>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun link(characterScript: CharacterScript)

    @Query("DELETE FROM character_scripts WHERE characterId = :characterId AND scriptId = :scriptId")
    suspend fun unlink(characterId: Long, scriptId: Long)

    @Query("SELECT COUNT(*) FROM character_scripts WHERE characterId = :characterId")
    suspend fun countForCharacter(characterId: Long): Int

    @Query("SELECT scriptId FROM character_scripts WHERE characterId = :characterId")
    suspend fun getScriptIdsForCharacter(characterId: Long): List<Long>

    @Query("""
        SELECT DISTINCT s.id, s.title, s.fileName, s.fileType, s.createdAt, s.updatedAt, s.lastPracticedAt, s.practiceCount
        FROM scripts s
        INNER JOIN character_scripts cs ON s.id = cs.scriptId
        INNER JOIN characters c ON cs.characterId = c.id
        INNER JOIN castings ca ON c.castingId = ca.id
        WHERE ca.projectId = :projectId
        ORDER BY s.updatedAt DESC
    """)
    fun getScriptInfosForProject(projectId: Long): Flow<List<ScriptInfo>>

    @Query("""
        SELECT DISTINCT s.id
        FROM scripts s
        INNER JOIN character_scripts cs ON s.id = cs.scriptId
        INNER JOIN characters c ON cs.characterId = c.id
        INNER JOIN castings ca ON c.castingId = ca.id
        WHERE ca.projectId = :projectId
    """)
    suspend fun getScriptIdsForProject(projectId: Long): List<Long>
}
