package mx.visionebc.actorstoolkit.data.repository

import kotlinx.coroutines.flow.Flow
import mx.visionebc.actorstoolkit.data.AppDatabase
import mx.visionebc.actorstoolkit.data.entity.Project

class ProjectRepository(private val db: AppDatabase) {

    fun getAllProjects(): Flow<List<Project>> = db.projectDao().getAllProjects()

    suspend fun getProject(id: Long): Project? = db.projectDao().getById(id)

    suspend fun insertProject(project: Project): Long = db.projectDao().insert(project)

    suspend fun updateProject(project: Project) = db.projectDao().update(project)

    suspend fun deleteProject(project: Project) = db.projectDao().delete(project)

    suspend fun deleteProjectById(id: Long) = db.projectDao().deleteById(id)

    suspend fun updateSortOrders(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id ->
            db.projectDao().updateSortOrder(id, index)
        }
    }
}
