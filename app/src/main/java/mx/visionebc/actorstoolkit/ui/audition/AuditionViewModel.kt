package mx.visionebc.actorstoolkit.ui.audition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import mx.visionebc.actorstoolkit.data.entity.Audition
import mx.visionebc.actorstoolkit.data.repository.AuditionRepository

class AuditionViewModel(private val repository: AuditionRepository) : ViewModel() {

    val auditions: StateFlow<List<Audition>> = repository.getAllAuditions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedAudition = MutableStateFlow<Audition?>(null)
    val selectedAudition: StateFlow<Audition?> = _selectedAudition.asStateFlow()

    private val _filterStatus = MutableStateFlow<String?>(null)
    val filterStatus: StateFlow<String?> = _filterStatus.asStateFlow()

    val filteredAuditions: StateFlow<List<Audition>> = combine(auditions, _filterStatus) { list, filter ->
        if (filter == null) list
        else list.filter { it.status == filter }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(status: String?) {
        _filterStatus.value = status
    }

    fun loadAudition(id: Long) {
        viewModelScope.launch {
            _selectedAudition.value = repository.getAudition(id)
        }
    }

    fun saveAudition(audition: Audition, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch {
            if (audition.id == 0L) {
                val id = repository.insertAudition(audition)
                onComplete(id)
            } else {
                repository.updateAudition(audition.copy(updatedAt = System.currentTimeMillis()))
                onComplete(audition.id)
            }
        }
    }

    fun deleteAudition(audition: Audition) {
        viewModelScope.launch {
            repository.deleteAudition(audition)
        }
    }

    fun updateStatus(audition: Audition, newStatus: String) {
        viewModelScope.launch {
            repository.updateAudition(audition.copy(status = newStatus, updatedAt = System.currentTimeMillis()))
        }
    }

    class Factory(private val repository: AuditionRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuditionViewModel(repository) as T
        }
    }
}
