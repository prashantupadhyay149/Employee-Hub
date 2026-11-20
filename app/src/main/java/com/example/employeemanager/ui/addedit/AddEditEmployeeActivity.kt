package com.example.employeemanager.ui.addedit

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.employeemanager.R
import com.example.employeemanager.data.db.AppDatabase
import com.example.employeemanager.data.model.Document
import com.example.employeemanager.data.model.Employee
import com.example.employeemanager.data.model.EmployeeWithDocuments
import com.example.employeemanager.data.repository.EmployeeRepository
import com.example.employeemanager.databinding.ActivityAddEditEmployeeBinding
import com.example.employeemanager.databinding.DialogAddDocumentBinding
import com.example.employeemanager.databinding.ItemDocumentRowBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class AddEditEmployeeActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EMPLOYEE_ID = "extra_employee_id"
    }

    private lateinit var binding: ActivityAddEditEmployeeBinding
    private lateinit var repository: EmployeeRepository

    private var existingEmployee: EmployeeWithDocuments? = null
    private val documentList = mutableListOf<Document>()
    private var nextDocId = 9000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditEmployeeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val dao = AppDatabase.getInstance(this).employeeDao()
        repository = EmployeeRepository(dao)

        val employeeId = intent.getIntExtra(EXTRA_EMPLOYEE_ID, -1)
        val isEdit = employeeId != -1

        // Toolbar
        binding.toolbar.title = if (isEdit) getString(R.string.title_edit_employee)
                                 else getString(R.string.title_add_employee)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        if (isEdit) {
            lifecycleScope.launch {
                val emp = repository.getEmployeeWithDocuments(employeeId)
                existingEmployee = emp
                emp?.let { populateFields(it) }
            }
        }

        binding.btnAddDocument.setOnClickListener { showAddDocumentDialog() }

        binding.btnSaveEmployee.setOnClickListener { saveEmployee(isEdit) }
    }

    private fun populateFields(emp: EmployeeWithDocuments) {
        binding.etEmployeeName.setText(emp.employee.employeeName)
        binding.etDepartmentId.setText(emp.employee.departmentId.toString())
        binding.etManagerId.setText(emp.employee.managerId.toString())
        binding.etBranchId.setText(emp.employee.branchId.toString())
        binding.switchIsActive.isChecked = emp.employee.isActive

        documentList.clear()
        documentList.addAll(emp.documents)
        refreshDocumentRows()
    }

    private fun showAddDocumentDialog() {
        val dialogBinding = DialogAddDocumentBinding.inflate(LayoutInflater.from(this))

        // Document type dropdown
        val docTypes = arrayOf("JPG", "PNG", "PDF", "DOC", "DOCX")
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, docTypes)
        dialogBinding.actvDocType.setAdapter(typeAdapter)
        dialogBinding.actvDocType.setText(docTypes[0], false)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnDialogCancel.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnDialogAdd.setOnClickListener {
            val name = dialogBinding.etDocName.text?.toString()?.trim() ?: ""
            if (name.isEmpty()) {
                dialogBinding.tilDocName.error = "Document name required"
                return@setOnClickListener
            }

            val doc = Document(
                documentId = nextDocId++,
                employeeId = existingEmployee?.employee?.employeeId ?: 0,
                documentName = name,
                documentType = dialogBinding.actvDocType.text?.toString() ?: "JPG",
                isVerified = dialogBinding.switchDocVerified.isChecked,
                documentUrl = dialogBinding.etDocUrl.text?.toString()?.trim() ?: "",
                thumbnailUrl = dialogBinding.etThumbnailUrl.text?.toString()?.trim() ?: ""
            )
            documentList.add(doc)
            refreshDocumentRows()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun refreshDocumentRows() {
        binding.layoutDocumentList.removeAllViews()
        if (documentList.isEmpty()) {
            binding.tvNoDocumentsHint.visibility = View.VISIBLE
            return
        }
        binding.tvNoDocumentsHint.visibility = View.GONE

        documentList.forEach { doc ->
            val rowBinding = ItemDocumentRowBinding.inflate(
                LayoutInflater.from(this), binding.layoutDocumentList, false
            )
            rowBinding.tvDocRowName.text = doc.documentName
            rowBinding.tvDocRowType.text = doc.documentType.uppercase()

            if (doc.isVerified) {
                rowBinding.ivDocRowVerified.visibility = View.VISIBLE
                rowBinding.tvDocRowVerifiedLabel.visibility = View.VISIBLE
                rowBinding.tvDocRowVerifiedLabel.text = "Verified"
            } else {
                rowBinding.ivDocRowVerified.visibility = View.GONE
                rowBinding.tvDocRowVerifiedLabel.visibility = View.GONE
            }

            rowBinding.btnRemoveDocument.setOnClickListener {
                documentList.remove(doc)
                refreshDocumentRows()
            }

            // Set icon color by type
            val iconRes = when (doc.documentType.uppercase()) {
                "PDF" -> R.drawable.ic_description
                else -> R.drawable.ic_description
            }
            rowBinding.ivDocIcon.setImageResource(iconRes)

            binding.layoutDocumentList.addView(rowBinding.root)
        }
    }

    private fun saveEmployee(isEdit: Boolean) {
        val name = binding.etEmployeeName.text?.toString()?.trim() ?: ""
        val deptStr = binding.etDepartmentId.text?.toString()?.trim() ?: ""
        val mgrStr = binding.etManagerId.text?.toString()?.trim() ?: ""
        val branchStr = binding.etBranchId.text?.toString()?.trim() ?: ""

        var hasError = false

        if (name.isEmpty()) {
            binding.tilEmployeeName.error = getString(R.string.error_name_required)
            hasError = true
        } else {
            binding.tilEmployeeName.error = null
        }

        if (deptStr.isEmpty()) {
            binding.tilDepartmentId.error = getString(R.string.error_dept_required)
            hasError = true
        } else {
            binding.tilDepartmentId.error = null
        }

        if (mgrStr.isEmpty()) {
            binding.tilManagerId.error = getString(R.string.error_mgr_required)
            hasError = true
        } else {
            binding.tilManagerId.error = null
        }

        if (branchStr.isEmpty()) {
            binding.tilBranchId.error = getString(R.string.error_branch_required)
            hasError = true
        } else {
            binding.tilBranchId.error = null
        }

        if (hasError) return

        val empId = if (isEdit) existingEmployee!!.employee.employeeId
                    else (System.currentTimeMillis() % 100000).toInt()

        val employee = Employee(
            employeeId = empId,
            employeeName = name,
            departmentId = deptStr.toIntOrNull() ?: 0,
            managerId = mgrStr.toIntOrNull() ?: 0,
            branchId = branchStr.toIntOrNull() ?: 0,
            isActive = binding.switchIsActive.isChecked,
            createdDate = existingEmployee?.employee?.createdDate
                ?: java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                    .format(java.util.Date()),
            updatedDate = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date())
        )

        lifecycleScope.launch {
            if (isEdit) {
                repository.updateEmployee(employee)
                repository.deleteDocumentsForEmployee(empId)
            } else {
                repository.insertEmployee(employee)
            }

            // Assign employeeId to documents and save
            val docsWithId = documentList.map { it.copy(employeeId = empId) }
            repository.insertDocuments(docsWithId)

            Toast.makeText(
                this@AddEditEmployeeActivity,
                if (isEdit) "Employee updated!" else "Employee added!",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }
}
