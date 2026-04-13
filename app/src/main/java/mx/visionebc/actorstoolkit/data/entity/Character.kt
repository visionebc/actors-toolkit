package mx.visionebc.actorstoolkit.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "characters",
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
data class Character(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scriptId: Long,
    val name: String,
    val lineCount: Int = 0,
    val isUserRole: Boolean = false
)
