package mx.visionebc.actorstoolkit.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "self_tapes",
    foreignKeys = [
        ForeignKey(
            entity = Audition::class,
            parentColumns = ["id"],
            childColumns = ["auditionId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["auditionId"])]
)
data class SelfTape(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val videoPath: String,
    val thumbnailPath: String = "",
    val durationMs: Long = 0,
    val auditionId: Long? = null,
    val scriptId: Long? = null,
    val trimStartMs: Long = 0,
    val trimEndMs: Long = 0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
