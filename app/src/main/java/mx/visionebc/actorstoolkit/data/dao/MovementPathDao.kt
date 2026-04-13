package mx.visionebc.actorstoolkit.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import mx.visionebc.actorstoolkit.data.entity.MovementPath

@Dao
interface MovementPathDao {
    @Query("SELECT * FROM movement_paths WHERE scriptId = :scriptId ORDER BY lineNumber")
    fun getPathsForScript(scriptId: Long): Flow<List<MovementPath>>

    @Query("SELECT * FROM movement_paths WHERE scriptId = :scriptId AND lineNumber = :lineNumber")
    suspend fun getPathsForLine(scriptId: Long, lineNumber: Int): List<MovementPath>

    @Insert
    suspend fun insert(path: MovementPath): Long

    @Delete
    suspend fun delete(path: MovementPath)

    @Query("DELETE FROM movement_paths WHERE scriptId = :scriptId")
    suspend fun deleteForScript(scriptId: Long)
}
