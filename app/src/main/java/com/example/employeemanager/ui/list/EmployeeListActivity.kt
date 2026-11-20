package com.example.employeemanager.ui.list

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.employeemanager.R
import com.example.employeemanager.data.db.AppDatabase
import com.example.employeemanager.data.model.Employee
import com.example.employeemanager.data.model.EmployeeWithDocuments
import com.example.employeemanager.data.repository.EmployeeRepository
import com.example.employeemanager.data.repository.Result
import com.example.employeemanager.databinding.ActivityEmployeeListBinding
import com.example.employeemanager.ui.addedit.AddEditEmployeeActivity
import com.example.employeemanager.ui.map.MapPolygonActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class EmployeeListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmployeeListBinding
    private lateinit var viewModel: EmployeeListViewModel
    private lateinit var adapter: EmployeeListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmployeeListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val dao = AppDatabase.getInstance(this).employeeDao()
        val repository = EmployeeRepository(dao)
        viewModel = ViewModelProvider(this, EmployeeListViewModel.Factory(repository))
            .get(EmployeeListViewModel::class.java)

        setupRecyclerView()
        setupSearch()
        setupFilters()
        setupFab()
        setupSortButton()
        observeViewModel()

        // Load geofenced IDs on start
        lifecycleScope.launch {
            refreshGeofencedIds()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh geofenced status when returning from Map screen
        lifecycleScope.launch { refreshGeofencedIds() }
    }

    private suspend fun refreshGeofencedIds() {
        val dao = AppDatabase.getInstance(this).employeeDao()
        val repository = EmployeeRepository(dao)
        // Get all employees then check polygon points
        val allIds = mutableSetOf<Int>()
        viewModel.employees.value.forEach { emp ->
            if (repository.getPolygonPointCount(emp.employee.employeeId) >= 3) {
                allIds.add(emp.employee.employeeId)
            }
        }
        viewModel.updateGeofencedIds(allIds)
    }

    private fun setupRecyclerView() {
        adapter = EmployeeListAdapter(
            onEdit = { emp -> openAddEditScreen(emp.employee) },
            onDelete = { emp -> showDeleteConfirmation(emp) },
            onMapArea = { emp -> openMapScreen(emp.employee) }
        )
        binding.rvEmployees.apply {
            layoutManager = LinearLayoutManager(this@EmployeeListActivity)
            adapter = this@EmployeeListActivity.adapter
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.syncFromApi()
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearch(s?.toString() ?: "")
                binding.ivClearSearch.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.ivClearSearch.setOnClickListener {
            binding.etSearch.setText("")
            binding.ivClearSearch.visibility = View.GONE
        }
    }

    private fun setupFilters() {
        binding.chipGroupFilters.setOnCheckedStateChangeListener { group, checkedIds ->
            val filter = when {
                checkedIds.contains(R.id.chipActive) -> FilterType.ACTIVE
                checkedIds.contains(R.id.chipInactive) -> FilterType.INACTIVE
                checkedIds.contains(R.id.chipGeofenced) -> FilterType.GEOFENCED
                else -> FilterType.ALL
            }
            viewModel.setFilter(filter)
        }
    }

    private fun setupFab() {
        binding.fabAddEmployee.setOnClickListener {
            openAddEditScreen(null)
        }
    }

    private fun setupSortButton() {
        binding.btnSortEmployees.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.menu_sort, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.sort_name_asc -> viewModel.setSort(SortType.NAME_ASC)
                    R.id.sort_name_desc -> viewModel.setSort(SortType.NAME_DESC)
                    R.id.sort_id_asc -> viewModel.setSort(SortType.ID_ASC)
                    R.id.sort_id_desc -> viewModel.setSort(SortType.ID_DESC)
                }
                true
            }
            popup.show()
        }
    }

    private fun observeViewModel() {
        // Employee list
        lifecycleScope.launch {
            viewModel.employees.collectLatest { employees ->
                adapter.submitList(employees)
                val isEmpty = employees.isEmpty()
                binding.layoutEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
                binding.swipeRefreshLayout.visibility = if (isEmpty) View.GONE else View.VISIBLE
            }
        }

        // Stats
        viewModel.totalCount.observe(this) { count ->
            binding.tvStatTotal.text = count.toString()
            binding.tvEmployeeCount.text = "$count total employees"
        }
        viewModel.activeCount.observe(this) { count ->
            binding.tvStatActive.text = count.toString()
        }
        viewModel.geofencedCount.observe(this) { count ->
            binding.tvStatGeofenced.text = count.toString()
        }

        // Sync result
        viewModel.syncResult.observe(this) { result ->
            binding.swipeRefreshLayout.isRefreshing = false
            binding.progressBar.visibility = View.GONE
            when (result) {
                is Result.Loading -> binding.progressBar.visibility = View.VISIBLE
                is Result.Success -> {
                    Snackbar.make(binding.root, getString(R.string.sync_success), Snackbar.LENGTH_SHORT).show()
                }
                is Result.Error -> {
                    // Show offline message - data served from Room DB
                    Snackbar.make(binding.root, getString(R.string.sync_offline), Snackbar.LENGTH_LONG).show()
                }
            }
        }

        // Refresh button
        binding.btnRefresh.setOnClickListener { viewModel.syncFromApi() }
    }

    private fun openAddEditScreen(employee: Employee?) {
        val intent = Intent(this, AddEditEmployeeActivity::class.java).apply {
            employee?.let { putExtra(AddEditEmployeeActivity.EXTRA_EMPLOYEE_ID, it.employeeId) }
        }
        startActivity(intent)
    }

    private fun openMapScreen(employee: Employee) {
        val intent = Intent(this, MapPolygonActivity::class.java).apply {
            putExtra(MapPolygonActivity.EXTRA_EMPLOYEE_ID, employee.employeeId)
            putExtra(MapPolygonActivity.EXTRA_EMPLOYEE_NAME, employee.employeeName)
        }
        startActivity(intent)
    }

    private fun showDeleteConfirmation(emp: EmployeeWithDocuments) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.dialog_delete_title))
            .setMessage(
                getString(
                    R.string.dialog_delete_message,
                    emp.employee.employeeName,
                    emp.employee.employeeId
                )
            )
            .setNegativeButton(getString(R.string.btn_cancel)) { d, _ -> d.dismiss() }
            .setPositiveButton(getString(R.string.btn_confirm_delete)) { _, _ ->
                lifecycleScope.launch {
                    viewModel.deleteEmployee(emp)
                    Snackbar.make(binding.root, getString(R.string.employee_deleted), Snackbar.LENGTH_LONG)
                        .setAction(getString(R.string.action_undo)) {
                            lifecycleScope.launch { viewModel.restoreEmployee(emp) }
                        }.show()
                }
            }
            .show()
    }
}
