package mx.visionebc.actorstoolkit.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "audio_recordings",
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
data class AudioRecording(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scriptId: Long,
    val characterName: String,
    val lineNumber: Int,
    val filePath: String,
    val durationMs: Long = 0,
    val recordedBy: String = "local", // "local" or imported user name
    val createdAt: Long = System.currentTimeMillis()
)
