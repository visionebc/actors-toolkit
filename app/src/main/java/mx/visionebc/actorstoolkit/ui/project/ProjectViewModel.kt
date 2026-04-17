package mx.visionebc.actorstoolkit.ui.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import mx.visionebc.actorstoolkit.data.entity.Project
import mx.visionebc.actorstoolkit.data.repository.ProjectRepository

class ProjectViewModel(private val repository: ProjectRepository) : ViewModel() {

    val projects: StateFlow<List<Project>> = repository.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedProject = MutableStateFlow<Project?>(null)
    val selectedProject: StateFlow<Project?> = _selectedProject.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredProjects: StateFlow<List<Project>> = combine(projects, _searchQuery) { list, query ->
        if (query.isBlank()) list
        else list.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.castingDirector.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun loadProject(id: Long) {
        viewModelScope.launch {
            _selectedProject.value = repository.getProject(id)
        }
    }

    fun saveProject(project: Project, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch {
            if (project.id == 0L) {
                val id = repository.insertProject(project)
                onComplete(id)
            } else {
                repository.updateProject(project.copy(updatedAt = System.currentTimeMillis()))
                onComplete(project.id)
            }
        }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch {
            repository.deleteProject(project)
        }
    }

    fun reorderProjects(orderedIds: List<Long>) {
        viewModelScope.launch {
            repository.updateSortOrders(orderedIds)
        }
    }

    fun cloneProject(project: Project, newName: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val clone = project.copy(
                id = 0,
                name = newName,
                createdAt = now,
                updatedAt = now
            )
            repository.insertProject(clone)
        }
    }

    class Factory(private val repository: ProjectRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProjectViewModel(repository) as T
        }
    }
}
