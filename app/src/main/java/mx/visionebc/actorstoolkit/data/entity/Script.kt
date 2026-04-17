package mx.visionebc.actorstoolkit.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scripts")
data class Script(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val fileName: String,
    val fileType: String, // "pdf", "txt", "fdx"
    val rawContent: String, // Full text content
    val projectId: Long? = null,
    val characterVoicesJson: String = "{}", // JSON map: characterName -> TTS voice name
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastPracticedAt: Long? = null,
    val practiceCount: Int = 0
)
