package com.example.employeemanager.data.repository

import com.example.employeemanager.data.db.EmployeeDao
import com.example.employeemanager.data.model.Document
import com.example.employeemanager.data.model.Employee
import com.example.employeemanager.data.model.EmployeeWithDocuments
import com.example.employeemanager.data.model.PolygonPoint
import com.example.employeemanager.data.network.RetrofitClient
import kotlinx.coroutines.flow.Flow

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val exception: Exception? = null) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

class EmployeeRepository(private val dao: EmployeeDao) {

    // ---- Live data from Room ----
    val allEmployeesWithDocuments: Flow<List<EmployeeWithDocuments>> =
        dao.getAllEmployeesWithDocuments()

    val totalCount: Flow<Int> = dao.getTotalCount()
    val activeCount: Flow<Int> = dao.getActiveCount()
    val geofencedCount: Flow<Int> = dao.getGeofencedCount()

    // ---- Sync from API → Room ----
    suspend fun syncFromApi(): Result<Int> {
        return try {
            val response = RetrofitClient.instance.getEmployees()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success) {
                    val employees = mutableListOf<Employee>()
                    val documents = mutableListOf<Document>()

                    body.data.forEach { wrapper ->
                        val details = wrapper.employeeDetails
                        employees.add(
                            Employee(
                                employeeId = details.employeeId,
                                employeeName = details.employeeName,
                                departmentId = details.departmentId,
                                managerId = details.managerId,
                                branchId = details.branchId,
                                isActive = details.isActive,
                                createdDate = details.createdDate,
                                updatedDate = details.updatedDate
                            )
                        )
                        documents.addAll(details.documents)
                    }

                    dao.insertEmployees(employees)
                    dao.insertDocuments(documents)
                    Result.Success(employees.size)
                } else {
                    Result.Error("API returned failure response")
                }
            } else {
                Result.Error("HTTP ${response.code()}: ${response.message()}")
            }
        } catch (e: Exception) {
            Result.Error("Network error: ${e.message}", e)
        }
    }

    // ---- Employee CRUD ----
    suspend fun insertEmployee(employee: Employee) = dao.insertEmployee(employee)
    suspend fun updateEmployee(employee: Employee) = dao.updateEmployee(employee)
    suspend fun deleteEmployee(employee: Employee) = dao.deleteEmployee(employee)
    suspend fun getEmployeeWithDocuments(id: Int) = dao.getEmployeeWithDocuments(id)

    // ---- Documents ----
    suspend fun insertDocument(document: Document) = dao.insertDocument(document)
    suspend fun deleteDocument(document: Document) = dao.deleteDocument(document)
    suspend fun deleteDocumentsForEmployee(employeeId: Int) = dao.deleteDocumentsForEmployee(employeeId)
    suspend fun insertDocuments(docs: List<Document>) = dao.insertDocuments(docs)

    // ---- Polygon Points ----
    suspend fun savePolygonPoints(employeeId: Int, points: List<PolygonPoint>) {
        dao.deletePolygonPoints(employeeId)
        if (points.isNotEmpty()) {
            dao.insertPolygonPoints(points)
        }
    }

    suspend fun getPolygonPoints(employeeId: Int): List<PolygonPoint> =
        dao.getPolygonPoints(employeeId)

    suspend fun clearPolygonPoints(employeeId: Int) = dao.deletePolygonPoints(employeeId)

    suspend fun getPolygonPointCount(employeeId: Int): Int =
        dao.getPolygonPointCount(employeeId)
}
