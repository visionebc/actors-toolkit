package mx.visionebc.actorstoolkit.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import mx.visionebc.actorstoolkit.data.entity.StageItem

@Dao
interface StageItemDao {
    @Query("SELECT * FROM stage_items WHERE scriptId = :scriptId")
    fun getItemsForScript(scriptId: Long): Flow<List<StageItem>>

    @Query("SELECT * FROM stage_items WHERE scriptId = :scriptId")
    suspend fun getItemsForScriptSync(scriptId: Long): List<StageItem>

    @Insert
    suspend fun insert(item: StageItem): Long

    @Update
    suspend fun update(item: StageItem)

    @Delete
    suspend fun delete(item: StageItem)

    @Query("DELETE FROM stage_items WHERE scriptId = :scriptId")
    suspend fun deleteAllForScript(scriptId: Long)
}
