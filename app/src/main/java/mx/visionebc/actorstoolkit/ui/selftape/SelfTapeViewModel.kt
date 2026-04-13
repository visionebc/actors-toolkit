package mx.visionebc.actorstoolkit.ui.selftape

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.visionebc.actorstoolkit.data.entity.Audition
import mx.visionebc.actorstoolkit.data.entity.SelfTape
import mx.visionebc.actorstoolkit.data.repository.SelfTapeRepository
import java.io.File
import java.io.FileOutputStream

class SelfTapeViewModel(private val repository: SelfTapeRepository) : ViewModel() {

    val allTapes: StateFlow<List<SelfTape>> = repository.getAllTapes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditions: StateFlow<List<Audition>> = repository.getAllAuditions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTape = MutableStateFlow<SelfTape?>(null)
    val selectedTape: StateFlow<SelfTape?> = _selectedTape.asStateFlow()

    fun loadTape(id: Long) {
        viewModelScope.launch {
            _selectedTape.value = repository.getTapeById(id)
        }
    }

    fun saveTape(
        context: Context,
        videoPath: String,
        title: String,
        auditionId: Long?,
        scriptId: Long?,
        notes: String,
        onSaved: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val durationMs = getVideoDuration(videoPath)
            val thumbnailPath = extractThumbnail(context, videoPath)

            val tape = SelfTape(
                title = title,
                videoPath = videoPath,
                thumbnailPath = thumbnailPath,
                durationMs = durationMs,
                auditionId = auditionId,
                scriptId = scriptId,
                notes = notes
            )
            val id = repository.insertTape(tape)
            onSaved(id)
        }
    }

    fun updateTape(tape: SelfTape) {
        viewModelScope.launch {
            repository.updateTape(tape)
            _selectedTape.value = repository.getTapeById(tape.id)
        }
    }

    fun updateTrimPoints(tapeId: Long, startMs: Long, endMs: Long) {
        viewModelScope.launch {
            val tape = repository.getTapeById(tapeId) ?: return@launch
            repository.updateTape(tape.copy(trimStartMs = startMs, trimEndMs = endMs))
            _selectedTape.value = repository.getTapeById(tapeId)
        }
    }

    fun deleteTape(tape: SelfTape, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteTape(tape)
            onDeleted()
        }
    }

    private suspend fun getVideoDuration(path: String): Long = withContext(Dispatchers.IO) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(path)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            duration?.toLongOrNull() ?: 0L
        } catch (_: Exception) { 0L }
    }

    private suspend fun extractThumbnail(context: Context, videoPath: String): String = withContext(Dispatchers.IO) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoPath)
            val bitmap = retriever.getFrameAtTime(1_000_000) // 1 second in
            retriever.release()
            if (bitmap != null) {
                val thumbDir = File(context.filesDir, "thumbnails")
                thumbDir.mkdirs()
                val thumbFile = File(thumbDir, "thumb_${System.currentTimeMillis()}.jpg")
                FileOutputStream(thumbFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                }
                bitmap.recycle()
                thumbFile.absolutePath
            } else ""
        } catch (_: Exception) { "" }
    }

    class Factory(private val repository: SelfTapeRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SelfTapeViewModel(repository) as T
        }
    }
}
