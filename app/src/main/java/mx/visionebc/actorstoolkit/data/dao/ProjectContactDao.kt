package mx.visionebc.actorstoolkit.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import mx.visionebc.actorstoolkit.data.entity.ProjectContact

@Dao
interface ProjectContactDao {

    @Query("SELECT * FROM project_contacts WHERE projectId = :projectId ORDER BY name ASC")
    fun getByProjectId(projectId: Long): Flow<List<ProjectContact>>

    @Query("SELECT * FROM project_contacts WHERE projectId = :projectId ORDER BY name ASC")
    suspend fun getByProjectIdSync(projectId: Long): List<ProjectContact>

    @Query("SELECT * FROM project_contacts WHERE id = :id")
    suspend fun getById(id: Long): ProjectContact?

    @Insert
    suspend fun insert(contact: ProjectContact): Long

    @Update
    suspend fun update(contact: ProjectContact)

    @Delete
    suspend fun delete(contact: ProjectContact)
}
