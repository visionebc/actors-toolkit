package mx.visionebc.actorstoolkit.ui.character

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import mx.visionebc.actorstoolkit.data.AppDatabase
import mx.visionebc.actorstoolkit.data.entity.Character
import mx.visionebc.actorstoolkit.data.entity.CharacterScript
import mx.visionebc.actorstoolkit.data.entity.ScriptInfo
class CharacterViewModel(private val db: AppDatabase) : ViewModel() {

    private val _castingId = MutableStateFlow(0L)
    private val _characterId = MutableStateFlow(0L)

    val characters: StateFlow<List<Character>> = _castingId.flatMapLatest { cid ->
        if (cid == 0L) flowOf(emptyList())
        else db.characterDao().getCharactersForCasting(cid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCharacter = MutableStateFlow<Character?>(null)
    val selectedCharacter: StateFlow<Character?> = _selectedCharacter.asStateFlow()

    val scripts: StateFlow<List<ScriptInfo>> = _characterId.flatMapLatest { chId ->
        if (chId == 0L) flowOf(emptyList())
        else db.characterScriptDao().getScriptInfosForCharacter(chId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableScripts: StateFlow<List<ScriptInfo>> = _characterId.flatMapLatest { chId ->
        if (chId == 0L) flowOf(emptyList())
        else db.characterScriptDao().getAvailableScripts(chId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setCastingId(castingId: Long) { _castingId.value = castingId }
    fun setCharacterId(characterId: Long) { _characterId.value = characterId }

    fun loadCharacter(id: Long) {
        viewModelScope.launch {
            _selectedCharacter.value = db.characterDao().getById(id)
        }
    }

    fun saveCharacter(character: Character, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch {
            if (character.id == 0L) {
                val id = db.characterDao().insert(character)
                onComplete(id)
            } else {
                db.characterDao().update(character.copy(updatedAt = System.currentTimeMillis()))
                onComplete(character.id)
            }
        }
    }

    fun deleteCharacter(character: Character) {
        viewModelScope.launch { db.characterDao().deleteById(character.id) }
    }

    fun linkScript(characterId: Long, scriptId: Long) {
        viewModelScope.launch {
            db.characterScriptDao().link(CharacterScript(characterId, scriptId))
        }
    }

    fun unlinkScript(characterId: Long, scriptId: Long) {
        viewModelScope.launch {
            db.characterScriptDao().unlink(characterId, scriptId)
        }
    }

    class Factory(private val db: AppDatabase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CharacterViewModel(db) as T
        }
    }
}
