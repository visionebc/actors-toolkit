package mx.visionebc.actorstoolkit.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import mx.visionebc.actorstoolkit.data.entity.Casting

@Dao
interface CastingDao {

    @Query("SELECT * FROM castings WHERE projectId = :projectId ORDER BY updatedAt DESC")
    fun getByProjectId(projectId: Long): Flow<List<Casting>>

    @Query("SELECT * FROM castings WHERE projectId = :projectId ORDER BY updatedAt DESC")
    suspend fun getByProjectIdSync(projectId: Long): List<Casting>

    @Query("SELECT * FROM castings WHERE id = :id")
    suspend fun getById(id: Long): Casting?

    @Insert
    suspend fun insert(casting: Casting): Long

    @Update
    suspend fun update(casting: Casting)

    @Delete
    suspend fun delete(casting: Casting)

    @Query("DELETE FROM castings WHERE id = :id")
    suspend fun deleteById(id: Long)
}
