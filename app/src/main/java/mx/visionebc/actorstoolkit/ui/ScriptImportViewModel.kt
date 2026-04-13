package mx.visionebc.actorstoolkit.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mx.visionebc.actorstoolkit.data.entity.Script
import mx.visionebc.actorstoolkit.data.repository.ScriptRepository
import mx.visionebc.actorstoolkit.util.PdfReader
import mx.visionebc.actorstoolkit.util.ScriptParser

data class ImportState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val importedScriptId: Long? = null,
    val previewText: String = "",
    val title: String = ""
)

class ScriptImportViewModel(private val repository: ScriptRepository) : ViewModel() {

    private val _state = MutableStateFlow(ImportState())
    val state: StateFlow<ImportState> = _state

    fun loadFile(context: Context, uri: Uri, fileName: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val fileType = when {
                    fileName.endsWith(".pdf", true) -> "pdf"
                    fileName.endsWith(".fdx", true) -> "fdx"
                    else -> "txt"
                }

                val rawContent = when (fileType) {
                    "pdf" -> PdfReader.extractText(context, uri)
                    else -> {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            stream.bufferedReader().readText()
                        } ?: throw IllegalArgumentException("Cannot read file")
                    }
                }

                val title = fileName
                    .substringBeforeLast(".")
                    .replace("_", " ")
                    .replace("-", " ")
                    .replaceFirstChar { it.uppercase() }

                _state.value = _state.value.copy(
                    isLoading = false,
                    previewText = rawContent.take(2000),
                    title = title
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to read file: ${e.message}"
                )
            }
        }
    }

    fun updateTitle(title: String) {
        _state.value = _state.value.copy(title = title)
    }

    fun importScript(context: Context, uri: Uri, fileName: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val fileType = when {
                    fileName.endsWith(".pdf", true) -> "pdf"
                    fileName.endsWith(".fdx", true) -> "fdx"
                    else -> "txt"
                }

                val rawContent = when (fileType) {
                    "pdf" -> PdfReader.extractText(context, uri)
                    else -> {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            stream.bufferedReader().readText()
                        } ?: throw IllegalArgumentException("Cannot read file")
                    }
                }

                // Save script to database
                val scriptId = repository.insertScript(
                    Script(
                        title = _state.value.title,
                        fileName = fileName,
                        fileType = fileType,
                        rawContent = rawContent
                    )
                )

                // Parse and save lines/characters
                val parsed = if (fileType == "fdx") {
                    ScriptParser.parseFdx(scriptId, rawContent)
                } else {
                    ScriptParser.parse(scriptId, rawContent)
                }

                repository.insertLines(parsed.lines)
                repository.insertCharacters(parsed.characters)

                _state.value = _state.value.copy(
                    isLoading = false,
                    importedScriptId = scriptId
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Import failed: ${e.message}"
                )
            }
        }
    }

    fun reset() {
        _state.value = ImportState()
    }

    class Factory(private val repository: ScriptRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ScriptImportViewModel(repository) as T
        }
    }
}
