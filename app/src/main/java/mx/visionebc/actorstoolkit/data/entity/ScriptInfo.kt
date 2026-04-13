package mx.visionebc.actorstoolkit.data.entity

/**
 * Lightweight projection of Script that excludes rawContent.
 * Used for list views and detail views where the full text isn't needed.
 */
data class ScriptInfo(
    val id: Long,
    val title: String,
    val fileName: String,
    val fileType: String,
    val createdAt: Long,
    val updatedAt: Long,
    val lastPracticedAt: Long?,
    val practiceCount: Int
)
