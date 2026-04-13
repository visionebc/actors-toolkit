package mx.visionebc.actorstoolkit.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "movement_paths",
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
data class MovementPath(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scriptId: Long,
    val lineNumber: Int,
    val characterName: String,
    val pathPoints: String // "x1,y1;x2,y2;..." normalized 0.0-1.0
) {
    fun getPoints(): List<Pair<Float, Float>> {
        if (pathPoints.isBlank()) return emptyList()
        return pathPoints.split(";").mapNotNull { seg ->
            val p = seg.split(",")
            if (p.size == 2) {
                p[0].toFloatOrNull()?.let { x ->
                    p[1].toFloatOrNull()?.let { y -> x to y }
                }
            } else null
        }
    }
}
