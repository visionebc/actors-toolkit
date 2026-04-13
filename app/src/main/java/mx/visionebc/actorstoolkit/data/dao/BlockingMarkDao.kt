package mx.visionebc.actorstoolkit.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import mx.visionebc.actorstoolkit.data.entity.BlockingMark

@Dao
interface BlockingMarkDao {
    @Query("SELECT * FROM blocking_marks WHERE scriptId = :scriptId ORDER BY lineNumber")
    fun getBlockingForScript(scriptId: Long): Flow<List<BlockingMark>>

    @Query("SELECT * FROM blocking_marks WHERE scriptId = :scriptId AND lineNumber = :lineNumber")
    suspend fun getBlockingForLine(scriptId: Long, lineNumber: Int): List<BlockingMark>

    @Insert
    suspend fun insert(mark: BlockingMark): Long

    @Update
    suspend fun update(mark: BlockingMark)

    @Delete
    suspend fun delete(mark: BlockingMark)

    @Query("DELETE FROM blocking_marks WHERE scriptId = :scriptId")
    suspend fun deleteForScript(scriptId: Long)
}
