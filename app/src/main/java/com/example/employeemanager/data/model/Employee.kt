package com.example.employeemanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "employees")
data class Employee(
    @PrimaryKey
    @SerializedName("EmployeeId")
    val employeeId: Int,

    @SerializedName("EmployeeName")
    val employeeName: String,

    @SerializedName("DepartmentId")
    val departmentId: Int,

    @SerializedName("ManagerId")
    val managerId: Int,

    @SerializedName("BranchId")
    val branchId: Int,

    @SerializedName("IsActive")
    val isActive: Boolean,

    @SerializedName("CreatedDate")
    val createdDate: String = "",

    @SerializedName("UpdatedDate")
    val updatedDate: String = ""
)
