package mx.visionebc.actorstoolkit.data.repository

import kotlinx.coroutines.flow.Flow
import mx.visionebc.actorstoolkit.data.AppDatabase
import mx.visionebc.actorstoolkit.data.entity.Casting

class CastingRepository(private val db: AppDatabase) {

    fun getCastingsForProject(projectId: Long): Flow<List<Casting>> =
        db.castingDao().getByProjectId(projectId)

    suspend fun getCasting(id: Long): Casting? = db.castingDao().getById(id)

    suspend fun insertCasting(casting: Casting): Long = db.castingDao().insert(casting)

    suspend fun updateCasting(casting: Casting) = db.castingDao().update(casting)

    suspend fun deleteCasting(casting: Casting) = db.castingDao().delete(casting)
}
