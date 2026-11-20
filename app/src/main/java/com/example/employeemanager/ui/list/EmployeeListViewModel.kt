package com.example.employeemanager.ui.list

import androidx.lifecycle.*
import com.example.employeemanager.data.model.EmployeeWithDocuments
import com.example.employeemanager.data.repository.EmployeeRepository
import com.example.employeemanager.data.repository.Result
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class FilterType { ALL, ACTIVE, INACTIVE, GEOFENCED }
enum class SortType { NAME_ASC, NAME_DESC, ID_ASC, ID_DESC }

class EmployeeListViewModel(private val repository: EmployeeRepository) : ViewModel() {

    private val _filter = MutableStateFlow(FilterType.ALL)
    private val _sort = MutableStateFlow(SortType.NAME_ASC)
    private val _search = MutableStateFlow("")
    private val _syncResult = MutableLiveData<Result<Int>>()
    val syncResult: LiveData<Result<Int>> = _syncResult

    private val _geofencedIds = MutableStateFlow<Set<Int>>(emptySet())

    val totalCount: LiveData<Int> = repository.totalCount.asLiveData()
    val activeCount: LiveData<Int> = repository.activeCount.asLiveData()
    val geofencedCount: LiveData<Int> = repository.geofencedCount.asLiveData()

    val employees: StateFlow<List<EmployeeWithDocuments>> = combine(
        repository.allEmployeesWithDocuments,
        _filter,
        _sort,
        _search,
        _geofencedIds
    ) { all, filter, sort, query, geofencedIds ->
        // Apply search
        var list = if (query.isBlank()) all else all.filter { emp ->
            emp.employee.employeeName.contains(query, ignoreCase = true) ||
                    emp.employee.employeeId.toString().contains(query) ||
                    emp.employee.departmentId.toString().contains(query)
        }

        // Apply filter
        list = when (filter) {
            FilterType.ALL -> list
            FilterType.ACTIVE -> list.filter { it.employee.isActive }
            FilterType.INACTIVE -> list.filter { !it.employee.isActive }
            FilterType.GEOFENCED -> list.filter { geofencedIds.contains(it.employee.employeeId) }
        }

        // Apply sort
        list = when (sort) {
            SortType.NAME_ASC -> list.sortedBy { it.employee.employeeName }
            SortType.NAME_DESC -> list.sortedByDescending { it.employee.employeeName }
            SortType.ID_ASC -> list.sortedBy { it.employee.employeeId }
            SortType.ID_DESC -> list.sortedByDescending { it.employee.employeeId }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        syncFromApi()
        loadGeofencedIds()
    }

    private fun loadGeofencedIds() {
        // Reactive approach: watch geofenced count changes to reload IDs
        // For simplicity we refresh geofenced IDs after sync
    }

    fun updateGeofencedIds(ids: Set<Int>) {
        _geofencedIds.value = ids
    }

    fun syncFromApi() {
        viewModelScope.launch {
            _syncResult.value = Result.Loading
            _syncResult.value = repository.syncFromApi()
        }
    }

    fun setFilter(filter: FilterType) { _filter.value = filter }
    fun setSort(sort: SortType) { _sort.value = sort }
    fun setSearch(query: String) { _search.value = query }

    suspend fun deleteEmployee(emp: EmployeeWithDocuments) {
        repository.deleteEmployee(emp.employee)
    }

    suspend fun restoreEmployee(emp: EmployeeWithDocuments) {
        repository.insertEmployee(emp.employee)
        repository.insertDocuments(emp.documents)
    }

    class Factory(private val repository: EmployeeRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            EmployeeListViewModel(repository) as T
    }
}
