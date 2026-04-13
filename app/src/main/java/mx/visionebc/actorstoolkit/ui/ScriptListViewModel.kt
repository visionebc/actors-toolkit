package mx.visionebc.actorstoolkit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import mx.visionebc.actorstoolkit.data.entity.ScriptInfo
import mx.visionebc.actorstoolkit.data.repository.ScriptRepository

class ScriptListViewModel(private val repository: ScriptRepository) : ViewModel() {

    val scripts: StateFlow<List<ScriptInfo>> = repository.getAllScriptInfos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteScript(scriptInfo: ScriptInfo) {
        viewModelScope.launch {
            repository.deleteScriptById(scriptInfo.id)
        }
    }

    class Factory(private val repository: ScriptRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ScriptListViewModel(repository) as T
        }
    }
}
