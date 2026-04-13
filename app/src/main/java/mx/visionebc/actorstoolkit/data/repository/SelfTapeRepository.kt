package mx.visionebc.actorstoolkit.data.repository

import mx.visionebc.actorstoolkit.data.AppDatabase
import mx.visionebc.actorstoolkit.data.entity.SelfTape

class SelfTapeRepository(private val db: AppDatabase) {

    private val dao = db.selfTapeDao()
    private val auditionDao = db.auditionDao()

    fun getAllTapes() = dao.getAll()

    fun getTapesByAudition(auditionId: Long) = dao.getByAudition(auditionId)

    suspend fun getTapeById(id: Long) = dao.getById(id)

    suspend fun insertTape(selfTape: SelfTape): Long = dao.insert(selfTape)

    suspend fun updateTape(selfTape: SelfTape) = dao.update(selfTape.copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteTape(selfTape: SelfTape) {
        try {
            val file = java.io.File(selfTape.videoPath)
            if (file.exists()) file.delete()
            if (selfTape.thumbnailPath.isNotBlank()) {
                val thumb = java.io.File(selfTape.thumbnailPath)
                if (thumb.exists()) thumb.delete()
            }
        } catch (_: Exception) {}
        dao.delete(selfTape)
    }

    fun getAllAuditions() = auditionDao.getAllAuditions()
}
