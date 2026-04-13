package mx.visionebc.actorstoolkit.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "blocking_marks",
    foreignKeys = [
        ForeignKey(
            entity = Script::class,
            parentColumns = ["id"],
            childColumns = ["scriptId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("scriptId")]
)
data class BlockingMark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scriptId: Long,
    val lineNumber: Int,
    val characterName: String,
    val posX: Float, // Stage X position (0.0 - 1.0)
    val posY: Float, // Stage Y position (0.0 - 1.0)
    val note: String? = null // Movement note
)
