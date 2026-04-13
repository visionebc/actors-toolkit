package mx.visionebc.actorstoolkit.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stage_items",
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
data class StageItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scriptId: Long,
    val itemType: String,   // "chair", "table", "sofa", "door", "window", "stairs", "bed", "desk", "podium", "plant", "lamp", "custom"
    val label: String = "",
    val posX: Float,        // 0.0 - 1.0 normalized
    val posY: Float,        // 0.0 - 1.0 normalized
    val rotation: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f
)
