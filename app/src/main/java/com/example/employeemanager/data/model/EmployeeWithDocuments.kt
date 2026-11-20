package com.example.employeemanager.data.model

import androidx.room.Embedded
import androidx.room.Relation
import com.google.gson.annotations.SerializedName

/** Room 1-to-many join result */
data class EmployeeWithDocuments(
    @Embedded val employee: Employee,
    @Relation(
        parentColumn = "employeeId",
        entityColumn = "employeeId"
    )
    val documents: List<Document>
)

/** API response wrapper per employee */
data class EmployeeDetailsWrapper(
    @SerializedName("employee_details")
    val employeeDetails: EmployeeDetails
)

data class EmployeeDetails(
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
    val updatedDate: String = "",

    @SerializedName("Documents")
    val documents: List<Document> = emptyList()
)

/** Root API response */
data class ApiResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("status")
    val status: Int,

    @SerializedName("message")
    val message: String,

    @SerializedName("data")
    val data: List<EmployeeDetailsWrapper>
)
