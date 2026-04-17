package mx.visionebc.actorstoolkit.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "script_lines",
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
data class ScriptLine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scriptId: Long,
    val lineNumber: Int,
    val character: String,
    val dialogue: String,
    val stageDirection: String? = null,
    val isMemorized: Boolean = false,
    val isSkipped: Boolean = false,
    val editedDialogue: String? = null,
    val ignoredWords: String? = null,
    val userNotes: String? = null
) {
    /** Returns the user-edited dialogue if set, otherwise the original */
    val effectiveDialogue: String get() = editedDialogue ?: dialogue

    /** Returns the set of ignored word indices */
    val ignoredWordIndices: Set<Int> get() {
        if (ignoredWords.isNullOrBlank()) return emptySet()
        return ignoredWords.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
    }

    /** Returns the dialogue text for TTS with ignored words removed */
    val speakableDialogue: String get() {
        val ignored = ignoredWordIndices
        if (ignored.isEmpty()) return effectiveDialogue
        return effectiveDialogue.split("\\s+".toRegex())
            .filterIndexed { index, _ -> index !in ignored }
            .joinToString(" ")
    }
}
