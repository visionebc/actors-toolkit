package mx.visionebc.actorstoolkit.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import mx.visionebc.actorstoolkit.data.entity.SelfTape

@Dao
interface SelfTapeDao {

    @Query("SELECT * FROM self_tapes ORDER BY createdAt DESC")
    fun getAll(): Flow<List<SelfTape>>

    @Query("SELECT * FROM self_tapes WHERE auditionId = :auditionId ORDER BY createdAt DESC")
    fun getByAudition(auditionId: Long): Flow<List<SelfTape>>

    @Query("SELECT * FROM self_tapes WHERE id = :id")
    suspend fun getById(id: Long): SelfTape?

    @Insert
    suspend fun insert(selfTape: SelfTape): Long

    @Update
    suspend fun update(selfTape: SelfTape)

    @Delete
    suspend fun delete(selfTape: SelfTape)

    @Query("DELETE FROM self_tapes WHERE id = :id")
    suspend fun deleteById(id: Long)
}
