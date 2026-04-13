package mx.visionebc.actorstoolkit.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import mx.visionebc.actorstoolkit.data.entity.Script
import mx.visionebc.actorstoolkit.data.entity.ScriptInfo

@Dao
interface ScriptDao {
    @Query("SELECT * FROM scripts ORDER BY updatedAt DESC")
    fun getAllScripts(): Flow<List<Script>>

    @Query("SELECT id, title, fileName, fileType, createdAt, updatedAt, lastPracticedAt, practiceCount FROM scripts ORDER BY updatedAt DESC")
    fun getAllScriptInfos(): Flow<List<ScriptInfo>>

    @Query("SELECT * FROM scripts WHERE id = :id")
    suspend fun getScriptById(id: Long): Script?

    @Query("SELECT id, title, fileName, fileType, createdAt, updatedAt, lastPracticedAt, practiceCount FROM scripts WHERE id = :id")
    suspend fun getScriptInfoById(id: Long): ScriptInfo?

    @Insert
    suspend fun insert(script: Script): Long

    @Update
    suspend fun update(script: Script)

    @Delete
    suspend fun delete(script: Script)

    @Query("DELETE FROM scripts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE scripts SET lastPracticedAt = :time, practiceCount = practiceCount + 1 WHERE id = :scriptId")
    suspend fun updatePracticeStats(scriptId: Long, time: Long)
}
