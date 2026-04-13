package mx.visionebc.actorstoolkit.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import mx.visionebc.actorstoolkit.data.entity.Character

@Dao
interface CharacterDao {
    @Query("SELECT * FROM characters WHERE scriptId = :scriptId ORDER BY lineCount DESC")
    fun getCharactersForScript(scriptId: Long): Flow<List<Character>>

    @Query("SELECT * FROM characters WHERE scriptId = :scriptId ORDER BY lineCount DESC")
    suspend fun getCharactersForScriptSync(scriptId: Long): List<Character>

    @Query("SELECT * FROM characters WHERE scriptId = :scriptId AND isUserRole = 1 LIMIT 1")
    suspend fun getUserRole(scriptId: Long): Character?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(characters: List<Character>)

    @Update
    suspend fun update(character: Character)

    @Query("UPDATE characters SET isUserRole = 0 WHERE scriptId = :scriptId")
    suspend fun clearUserRoles(scriptId: Long)

    @Query("UPDATE characters SET isUserRole = 1 WHERE id = :characterId")
    suspend fun setUserRole(characterId: Long)

    @Query("DELETE FROM characters WHERE scriptId = :scriptId")
    suspend fun deleteForScript(scriptId: Long)
}
