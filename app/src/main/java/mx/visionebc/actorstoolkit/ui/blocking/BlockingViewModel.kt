package mx.visionebc.actorstoolkit.ui.blocking

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import mx.visionebc.actorstoolkit.data.entity.BlockingMark
import mx.visionebc.actorstoolkit.data.entity.Character
import mx.visionebc.actorstoolkit.data.entity.MovementPath
import mx.visionebc.actorstoolkit.data.entity.ScriptLine
import mx.visionebc.actorstoolkit.data.entity.StageItem
import mx.visionebc.actorstoolkit.data.repository.ScriptRepository

data class MovementArrow(
    val characterName: String,
    val fromX: Float,
    val fromY: Float,
    val toX: Float,
    val toY: Float
)

data class BlockingState(
    val scriptTitle: String = "",
    val lines: List<ScriptLine> = emptyList(),
    val characters: List<Character> = emptyList(),
    val marks: List<BlockingMark> = emptyList(),
    val stageItems: List<StageItem> = emptyList(),
    val movementPaths: List<MovementPath> = emptyList(),
    val currentLineIndex: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedCharacter: String? = null,
    val isPlacingMark: Boolean = false,
    val showPropsPanel: Boolean = false,
    val draggingMarkId: Long? = null,
    val draggingItemId: Long? = null,
    val isPlaying: Boolean = false,
    val showMovement: Boolean = true,
    val isDrawingMode: Boolean = false,
    val currentDrawingPoints: List<Pair<Float, Float>> = emptyList()
)

class BlockingViewModel(private val repository: ScriptRepository) : ViewModel() {

    private val _state = MutableStateFlow(BlockingState())
    val state: StateFlow<BlockingState> = _state


    fun loadScript(scriptId: Long) {
        viewModelScope.launch {
            try {
                _state.value = BlockingState(isLoading = true)

                val scriptInfo = repository.getScriptInfo(scriptId)
                if (scriptInfo == null) {
                    _state.value = BlockingState(isLoading = false, error = "Script not found")
                    return@launch
                }

                val lines = repository.getLinesSync(scriptId)
                val characters = repository.getCharactersSync(scriptId)

                val marks = mutableListOf<BlockingMark>()
                repository.getBlockingMarks(scriptId).first().let { marks.addAll(it) }

                val items = mutableListOf<StageItem>()
                repository.getStageItems(scriptId).first().let { items.addAll(it) }

                val paths = mutableListOf<MovementPath>()
                repository.getMovementPaths(scriptId).first().let { paths.addAll(it) }

                _state.value = BlockingState(
                    scriptTitle = scriptInfo.title,
                    lines = lines,
                    characters = characters,
                    marks = marks,
                    stageItems = items,
                    movementPaths = paths,
                    currentLineIndex = 0,
                    isLoading = false,
                    selectedCharacter = characters.firstOrNull()?.name
                )
            } catch (e: Exception) {
                Log.e("BlockingVM", "Failed to load script", e)
                _state.value = BlockingState(isLoading = false, error = e.message)
            }
        }
    }

    fun selectCharacter(name: String) {
        _state.value = _state.value.copy(selectedCharacter = name, isPlacingMark = true)
    }

    fun togglePlacingMark() {
        _state.value = _state.value.copy(isPlacingMark = !_state.value.isPlacingMark)
    }

    fun goToLine(index: Int) {
        val current = _state.value
        if (index in current.lines.indices) {
            _state.value = current.copy(currentLineIndex = index)
        }
    }

    fun nextLine() {
        val current = _state.value
        if (current.currentLineIndex < current.lines.size - 1) {
            _state.value = current.copy(currentLineIndex = current.currentLineIndex + 1)
        }
    }

    fun previousLine() {
        val current = _state.value
        if (current.currentLineIndex > 0) {
            _state.value = current.copy(currentLineIndex = current.currentLineIndex - 1)
        }
    }

    fun getCurrentMarks(): List<BlockingMark> {
        val current = _state.value
        if (current.lines.isEmpty()) return emptyList()

        val currentLine = current.lines[current.currentLineIndex]
        val lineMarks = current.marks.filter { it.lineNumber == currentLine.lineNumber }

        if (lineMarks.isNotEmpty()) return lineMarks

        val inherited = mutableListOf<BlockingMark>()
        val seenChars = mutableSetOf<String>()
        for (i in current.currentLineIndex downTo 0) {
            val ln = current.lines[i].lineNumber
            val marksAtLine = current.marks.filter { it.lineNumber == ln }
            for (m in marksAtLine) {
                if (m.characterName !in seenChars) {
                    seenChars.add(m.characterName)
                    inherited.add(m)
                }
            }
            if (seenChars.size >= current.characters.size) break
        }
        return inherited
    }

    /** Get movement paths for the current line */
    fun getCurrentPaths(): List<MovementPath> {
        val current = _state.value
        if (current.lines.isEmpty()) return emptyList()
        val lineNum = current.lines[current.currentLineIndex].lineNumber
        return current.movementPaths.filter { it.lineNumber == lineNum }
    }

    /** Auto-generated arrows for characters WITHOUT freehand paths */
    fun getMovementArrows(): List<MovementArrow> {
        val current = _state.value
        if (current.lines.isEmpty() || current.marks.isEmpty()) return emptyList()

        val currentLineIdx = current.currentLineIndex
        val currentPaths = getCurrentPaths()
        val charsWithPaths = currentPaths.map { it.characterName }.toSet()
        val arrows = mutableListOf<MovementArrow>()

        for (char in current.characters) {
            // Skip characters that have freehand paths on this line
            if (char.name in charsWithPaths) continue

            var currentMark: BlockingMark? = null
            for (i in currentLineIdx downTo 0) {
                val ln = current.lines[i].lineNumber
                val mark = current.marks.find { it.lineNumber == ln && it.characterName == char.name }
                if (mark != null) {
                    currentMark = mark
                    break
                }
            }

            if (currentMark != null) {
                val markLineIdx = current.lines.indexOfFirst { it.lineNumber == currentMark!!.lineNumber }
                if (markLineIdx > 0) {
                    var previousMark: BlockingMark? = null
                    for (i in markLineIdx - 1 downTo 0) {
                        val ln = current.lines[i].lineNumber
                        val mark = current.marks.find { it.lineNumber == ln && it.characterName == char.name }
                        if (mark != null) {
                            previousMark = mark
                            break
                        }
                    }
                    if (previousMark != null &&
                        (previousMark.posX != currentMark.posX || previousMark.posY != currentMark.posY)
                    ) {
                        arrows.add(
                            MovementArrow(
                                characterName = char.name,
                                fromX = previousMark.posX,
                                fromY = previousMark.posY,
                                toX = currentMark.posX,
                                toY = currentMark.posY
                            )
                        )
                    }
                }
            }
        }
        return arrows
    }

    fun addMark(posX: Float, posY: Float) {
        val current = _state.value
        val charName = current.selectedCharacter ?: return
        if (current.lines.isEmpty()) return

        val lineNumber = current.lines[current.currentLineIndex].lineNumber
        val scriptId = current.lines[current.currentLineIndex].scriptId

        viewModelScope.launch {
            try {
                val existing = current.marks.filter {
                    it.lineNumber == lineNumber && it.characterName == charName
                }
                existing.forEach { repository.deleteBlockingMark(it) }

                val mark = BlockingMark(
                    scriptId = scriptId,
                    lineNumber = lineNumber,
                    characterName = charName,
                    posX = posX.coerceIn(0f, 1f),
                    posY = posY.coerceIn(0f, 1f)
                )
                val id = repository.insertBlockingMark(mark)

                val updatedMarks = current.marks
                    .filter { !(it.lineNumber == lineNumber && it.characterName == charName) } +
                    mark.copy(id = id)

                _state.value = current.copy(marks = updatedMarks)
            } catch (e: Exception) {
                Log.e("BlockingVM", "Failed to add mark", e)
            }
        }
    }

    fun updateMarkPosition(markId: Long, posX: Float, posY: Float) {
        val current = _state.value
        val mark = current.marks.find { it.id == markId } ?: return

        val updated = mark.copy(posX = posX.coerceIn(0f, 1f), posY = posY.coerceIn(0f, 1f))
        _state.value = current.copy(
            marks = current.marks.map { if (it.id == markId) updated else it }
        )

        viewModelScope.launch {
            try {
                repository.updateBlockingMark(updated)
            } catch (e: Exception) {
                Log.e("BlockingVM", "Failed to update mark", e)
            }
        }
    }

    fun deleteMark(markId: Long) {
        val current = _state.value
        val mark = current.marks.find { it.id == markId } ?: return

        viewModelScope.launch {
            try {
                repository.deleteBlockingMark(mark)
                _state.value = current.copy(marks = current.marks.filter { it.id != markId })
            } catch (e: Exception) {
                Log.e("BlockingVM", "Failed to delete mark", e)
            }
        }
    }

    // --- Stage Items (Props/Furniture) ---

    fun togglePropsPanel() {
        _state.value = _state.value.copy(showPropsPanel = !_state.value.showPropsPanel)
    }

    fun addStageItem(itemType: String, posX: Float = 0.5f, posY: Float = 0.5f) {
        val current = _state.value
        if (current.lines.isEmpty()) return
        val scriptId = current.lines[0].scriptId

        viewModelScope.launch {
            try {
                val item = StageItem(
                    scriptId = scriptId,
                    itemType = itemType,
                    posX = posX,
                    posY = posY
                )
                val id = repository.insertStageItem(item)
                _state.value = current.copy(
                    stageItems = current.stageItems + item.copy(id = id),
                    showPropsPanel = false
                )
            } catch (e: Exception) {
                Log.e("BlockingVM", "Failed to add stage item", e)
            }
        }
    }

    fun updateStageItemPosition(itemId: Long, posX: Float, posY: Float) {
        val current = _state.value
        val item = current.stageItems.find { it.id == itemId } ?: return

        val updated = item.copy(posX = posX.coerceIn(0f, 1f), posY = posY.coerceIn(0f, 1f))
        _state.value = current.copy(
            stageItems = current.stageItems.map { if (it.id == itemId) updated else it }
        )

        viewModelScope.launch {
            try {
                repository.updateStageItem(updated)
            } catch (e: Exception) {
                Log.e("BlockingVM", "Failed to update stage item", e)
            }
        }
    }

    fun deleteStageItem(itemId: Long) {
        val current = _state.value
        val item = current.stageItems.find { it.id == itemId } ?: return

        viewModelScope.launch {
            try {
                repository.deleteStageItem(item)
                _state.value = current.copy(stageItems = current.stageItems.filter { it.id != itemId })
            } catch (e: Exception) {
                Log.e("BlockingVM", "Failed to delete stage item", e)
            }
        }
    }

    fun cloneStageItem(itemId: Long): Long? {
        val current = _state.value
        val item = current.stageItems.find { it.id == itemId } ?: return null
        if (current.lines.isEmpty()) return null
        val scriptId = current.lines[0].scriptId

        // Offset the clone slightly so it's visible
        val clone = StageItem(
            scriptId = scriptId,
            itemType = item.itemType,
            posX = (item.posX + 0.05f).coerceIn(0f, 1f),
            posY = (item.posY + 0.05f).coerceIn(0f, 1f),
            scaleX = item.scaleX,
            scaleY = item.scaleY,
            rotation = item.rotation
        )

        var newId = 0L
        viewModelScope.launch {
            try {
                newId = repository.insertStageItem(clone)
                _state.value = _state.value.copy(
                    stageItems = _state.value.stageItems + clone.copy(id = newId)
                )
            } catch (e: Exception) {
                Log.e("BlockingVM", "Failed to clone stage item", e)
            }
        }
        return newId
    }

    fun rotateStageItem(itemId: Long) {
        val current = _state.value
        val item = current.stageItems.find { it.id == itemId } ?: return
        val updated = item.copy(rotation = (item.rotation + 45f) % 360f)
        _state.value = current.copy(
            stageItems = current.stageItems.map { if (it.id == itemId) updated else it }
        )
        viewModelScope.launch {
            try {
                repository.updateStageItem(updated)
            } catch (e: Exception) {
                Log.e("BlockingVM", "Failed to rotate stage item", e)
            }
        }
    }

    fun resizeStageItem(itemId: Long, increase: Boolean) {
        val current = _state.value
        val item = current.stageItems.find { it.id == itemId } ?: return
        val delta = if (increase) 0.3f else -0.3f
        val newScale = (item.scaleX + delta).coerceIn(0.5f, 5f)
        val updated = item.copy(scaleX = newScale, scaleY = newScale)
        _state.value = current.copy(
            stageItems = current.stageItems.map { if (it.id == itemId) updated else it }
        )
        viewModelScope.launch {
            try {
                repository.updateStageItem(updated)
            } catch (e: Exception) {
                Log.e("BlockingVM", "Failed to resize stage item", e)
            }
        }
    }

    /** Direct pinch-to-resize and two-finger-rotate on a prop */
    fun transformStageItem(itemId: Long, zoomDelta: Float, rotationDeltaRad: Float) {
        val current = _state.value
        val item = current.stageItems.find { it.id == itemId } ?: return
        val newScale = (item.scaleX * zoomDelta).coerceIn(0.3f, 8f)
        val newRotation = item.rotation + Math.toDegrees(rotationDeltaRad.toDouble()).toFloat()
        val updated = item.copy(scaleX = newScale, scaleY = newScale, rotation = newRotation)
        _state.value = current.copy(
            stageItems = current.stageItems.map { if (it.id == itemId) updated else it }
        )
        viewModelScope.launch {
            try {
                repository.updateStageItem(updated)
            } catch (_: Exception) {}
        }
    }

    fun toggleMovement() {
        _state.value = _state.value.copy(showMovement = !_state.value.showMovement)
    }

    // --- Drawing Mode ---

    fun toggleDrawingMode() {
        _state.value = _state.value.copy(
            isDrawingMode = !_state.value.isDrawingMode,
            isPlacingMark = false,
            currentDrawingPoints = emptyList()
        )
    }

    fun updateDrawingPoints(points: List<Pair<Float, Float>>) {
        _state.value = _state.value.copy(currentDrawingPoints = points)
    }

    fun finishDrawing(points: List<Pair<Float, Float>>) {
        val current = _state.value
        val charName = current.selectedCharacter ?: return
        if (current.lines.isEmpty() || points.size < 3) {
            _state.value = current.copy(currentDrawingPoints = emptyList())
            return
        }

        val lineNumber = current.lines[current.currentLineIndex].lineNumber
        val scriptId = current.lines[current.currentLineIndex].scriptId
        val pathStr = points.joinToString(";") { "${it.first},${it.second}" }

        viewModelScope.launch {
            try {
                val path = MovementPath(
                    scriptId = scriptId,
                    lineNumber = lineNumber,
                    characterName = charName,
                    pathPoints = pathStr
                )
                val id = repository.insertMovementPath(path)
                _state.value = _state.value.copy(
                    currentDrawingPoints = emptyList(),
                    movementPaths = _state.value.movementPaths + path.copy(id = id)
                )
            } catch (e: Exception) {
                Log.e("BlockingVM", "Failed to save path", e)
                _state.value = _state.value.copy(currentDrawingPoints = emptyList())
            }
        }
    }

    fun clearPathsForCurrentLine() {
        val current = _state.value
        val charName = current.selectedCharacter ?: return
        if (current.lines.isEmpty()) return
        val lineNumber = current.lines[current.currentLineIndex].lineNumber

        val toDelete = current.movementPaths.filter {
            it.lineNumber == lineNumber && it.characterName == charName
        }

        viewModelScope.launch {
            try {
                toDelete.forEach { repository.deleteMovementPath(it) }
                _state.value = _state.value.copy(
                    movementPaths = _state.value.movementPaths.filter { path ->
                        !(path.lineNumber == lineNumber && path.characterName == charName)
                    }
                )
            } catch (e: Exception) {
                Log.e("BlockingVM", "Failed to clear paths", e)
            }
        }
    }

    fun deleteMovementPath(pathId: Long) {
        val current = _state.value
        val path = current.movementPaths.find { it.id == pathId } ?: return
        viewModelScope.launch {
            try {
                repository.deleteMovementPath(path)
                _state.value = _state.value.copy(
                    movementPaths = _state.value.movementPaths.filter { it.id != pathId }
                )
            } catch (e: Exception) {
                Log.e("BlockingVM", "Failed to delete path", e)
            }
        }
    }

    // --- Playback ---

    fun togglePlayback() {
        if (_state.value.isPlaying) {
            stopPlayback()
        } else {
            startPlayback()
        }
    }

    private fun startPlayback() {
        _state.value = _state.value.copy(isPlaying = true)
        // Playback timing is now controlled by TTS callbacks in BlockingScreen
    }

    fun stopPlayback() {
        _state.value = _state.value.copy(isPlaying = false)
    }

    override fun onCleared() {
        _state.value = _state.value.copy(isPlaying = false)
        super.onCleared()
    }

    class Factory(private val repository: ScriptRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BlockingViewModel(repository) as T
        }
    }
}
