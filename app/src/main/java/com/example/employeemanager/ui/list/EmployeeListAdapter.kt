package com.example.employeemanager.ui.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.employeemanager.R
import com.example.employeemanager.data.model.EmployeeWithDocuments
import com.example.employeemanager.databinding.ItemEmployeeBinding
import java.text.SimpleDateFormat
import java.util.Locale

class EmployeeListAdapter(
    private val onEdit: (EmployeeWithDocuments) -> Unit,
    private val onDelete: (EmployeeWithDocuments) -> Unit,
    private val onMapArea: (EmployeeWithDocuments) -> Unit
) : ListAdapter<EmployeeWithDocuments, EmployeeListAdapter.EmployeeViewHolder>(DIFF_CALLBACK) {

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<EmployeeWithDocuments>() {
            override fun areItemsTheSame(a: EmployeeWithDocuments, b: EmployeeWithDocuments) =
                a.employee.employeeId == b.employee.employeeId
            override fun areContentsTheSame(a: EmployeeWithDocuments, b: EmployeeWithDocuments) =
                a == b
        }
    }

    inner class EmployeeViewHolder(private val binding: ItemEmployeeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: EmployeeWithDocuments) {
            val emp = item.employee

            // Avatar initials (up to 2 chars)
            val initials = emp.employeeName.trim().split(" ")
                .take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .joinToString("")
            binding.tvAvatarInitials.text = initials.ifEmpty { "?" }

            // Name & ID
            binding.tvEmployeeName.text = emp.employeeName
            binding.tvEmployeeIdBadge.text = "Employee ID: #${emp.employeeId}"

            // Status pill
            if (emp.isActive) {
                binding.tvStatusPill.text = "Active"
                binding.tvStatusPill.setBackgroundResource(R.drawable.bg_pill_active)
                binding.tvStatusPill.setTextColor(itemView.context.getColor(R.color.status_active))
            } else {
                binding.tvStatusPill.text = "Inactive"
                binding.tvStatusPill.setBackgroundResource(R.drawable.bg_pill_inactive)
                binding.tvStatusPill.setTextColor(itemView.context.getColor(R.color.status_inactive))
            }

            // Department / Manager / Branch chips
            binding.tvDepartment.text = "Dept #${emp.departmentId}"
            binding.tvManager.text = "Mgr #${emp.managerId}"
            binding.tvBranch.text = "Branch #${emp.branchId}"

            // Creation date
            try {
                val inputFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val outputFmt = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val parsedDate = inputFmt.parse(emp.createdDate)
                binding.tvDates.text = "Joined: ${parsedDate?.let { outputFmt.format(it) } ?: ""}"
            } catch (e: Exception) {
                binding.tvDates.text = ""
            }

            // Map area status chip
            binding.tvMapStatusChip.text = "📍 Tap to Define Area"

            // Documents
            if (item.documents.isEmpty()) {
                binding.rvDocuments.visibility = View.GONE
                binding.tvNoDocuments.visibility = View.VISIBLE
                binding.tvDocumentsLabel.text = "Documents (0)"
            } else {
                binding.rvDocuments.visibility = View.VISIBLE
                binding.tvNoDocuments.visibility = View.GONE
                binding.tvDocumentsLabel.text = "Documents (${item.documents.size})"
                val docAdapter = DocumentPreviewAdapter()
                binding.rvDocuments.layoutManager =
                    LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
                binding.rvDocuments.adapter = docAdapter
                docAdapter.submitList(item.documents)
            }

            // Actions
            binding.btnEditEmployee.setOnClickListener { onEdit(item) }
            binding.btnDeleteEmployee.setOnClickListener { onDelete(item) }
            binding.btnMapArea.setOnClickListener { onMapArea(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmployeeViewHolder {
        val binding = ItemEmployeeBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return EmployeeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EmployeeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
