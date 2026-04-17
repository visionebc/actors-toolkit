package mx.visionebc.actorstoolkit.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "characters",
    foreignKeys = [
        ForeignKey(
            entity = Casting::class,
            parentColumns = ["id"],
            childColumns = ["castingId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Script::class,
            parentColumns = ["id"],
            childColumns = ["scriptId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("castingId"), Index("scriptId")]
)
data class Character(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val castingId: Long? = null,
    val scriptId: Long? = null,
    val name: String,
    val lineCount: Int = 0,
    val isUserRole: Boolean = false,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
