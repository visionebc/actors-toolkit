package mx.visionebc.actorstoolkit.data.repository

import kotlinx.coroutines.flow.Flow
import mx.visionebc.actorstoolkit.data.AppDatabase
import mx.visionebc.actorstoolkit.data.entity.Audition

class AuditionRepository(private val db: AppDatabase) {

    fun getAllAuditions(): Flow<List<Audition>> = db.auditionDao().getAllAuditions()

    fun getAuditionsByStatus(status: String): Flow<List<Audition>> =
        db.auditionDao().getAuditionsByStatus(status)

    suspend fun getAudition(id: Long): Audition? = db.auditionDao().getById(id)

    suspend fun insertAudition(audition: Audition): Long = db.auditionDao().insert(audition)

    suspend fun updateAudition(audition: Audition) = db.auditionDao().update(audition)

    suspend fun deleteAudition(audition: Audition) = db.auditionDao().delete(audition)

    suspend fun deleteAuditionById(id: Long) = db.auditionDao().deleteById(id)
}
