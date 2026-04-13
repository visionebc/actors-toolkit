package mx.visionebc.actorstoolkit.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import mx.visionebc.actorstoolkit.data.entity.Audition

@Dao
interface AuditionDao {

    @Query("SELECT * FROM auditions ORDER BY auditionDate DESC, createdAt DESC")
    fun getAllAuditions(): Flow<List<Audition>>

    @Query("SELECT * FROM auditions WHERE status = :status ORDER BY auditionDate DESC")
    fun getAuditionsByStatus(status: String): Flow<List<Audition>>

    @Query("SELECT * FROM auditions WHERE id = :id")
    suspend fun getById(id: Long): Audition?

    @Insert
    suspend fun insert(audition: Audition): Long

    @Update
    suspend fun update(audition: Audition)

    @Delete
    suspend fun delete(audition: Audition)

    @Query("DELETE FROM auditions WHERE id = :id")
    suspend fun deleteById(id: Long)
}
