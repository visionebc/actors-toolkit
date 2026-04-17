package mx.visionebc.actorstoolkit.ui.casting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import mx.visionebc.actorstoolkit.data.entity.Casting
import mx.visionebc.actorstoolkit.data.repository.CastingRepository

class CastingViewModel(private val repository: CastingRepository) : ViewModel() {

    private val _projectId = MutableStateFlow(0L)

    val castings: StateFlow<List<Casting>> = _projectId.flatMapLatest { pid ->
        if (pid == 0L) flowOf(emptyList())
        else repository.getCastingsForProject(pid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCasting = MutableStateFlow<Casting?>(null)
    val selectedCasting: StateFlow<Casting?> = _selectedCasting.asStateFlow()

    fun setProjectId(projectId: Long) {
        _projectId.value = projectId
    }

    fun loadCasting(id: Long) {
        viewModelScope.launch {
            _selectedCasting.value = repository.getCasting(id)
        }
    }

    fun saveCasting(casting: Casting, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch {
            if (casting.id == 0L) {
                val id = repository.insertCasting(casting)
                onComplete(id)
            } else {
                repository.updateCasting(casting.copy(updatedAt = System.currentTimeMillis()))
                onComplete(casting.id)
            }
        }
    }

    fun deleteCasting(casting: Casting) {
        viewModelScope.launch {
            repository.deleteCasting(casting)
        }
    }

    class Factory(private val repository: CastingRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CastingViewModel(repository) as T
        }
    }
}
