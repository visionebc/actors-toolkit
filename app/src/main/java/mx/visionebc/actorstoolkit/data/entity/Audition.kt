package mx.visionebc.actorstoolkit.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "auditions")
data class Audition(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectName: String,
    val roleName: String = "",
    val castingDirector: String = "",
    val auditionDate: Long? = null,
    val status: String = "SUBMITTED",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
