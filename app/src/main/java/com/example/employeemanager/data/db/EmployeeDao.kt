package com.example.employeemanager.data.db

import androidx.room.*
import com.example.employeemanager.data.model.Document
import com.example.employeemanager.data.model.Employee
import com.example.employeemanager.data.model.EmployeeWithDocuments
import com.example.employeemanager.data.model.PolygonPoint
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeeDao {

    // ---- Employee CRUD ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(employee: Employee)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployees(employees: List<Employee>)

    @Update
    suspend fun updateEmployee(employee: Employee)

    @Delete
    suspend fun deleteEmployee(employee: Employee)

    @Query("DELETE FROM employees WHERE employeeId = :id")
    suspend fun deleteEmployeeById(id: Int)

    @Transaction
    @Query("SELECT * FROM employees ORDER BY employeeName ASC")
    fun getAllEmployeesWithDocuments(): Flow<List<EmployeeWithDocuments>>

    @Transaction
    @Query("SELECT * FROM employees WHERE employeeId = :id")
    suspend fun getEmployeeWithDocuments(id: Int): EmployeeWithDocuments?

    @Query("SELECT * FROM employees WHERE employeeId = :id")
    suspend fun getEmployeeById(id: Int): Employee?

    @Query("SELECT COUNT(*) FROM employees")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM employees WHERE isActive = 1")
    fun getActiveCount(): Flow<Int>

    // ---- Document CRUD ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: Document)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocuments(documents: List<Document>)

    @Delete
    suspend fun deleteDocument(document: Document)

    @Query("DELETE FROM documents WHERE employeeId = :employeeId")
    suspend fun deleteDocumentsForEmployee(employeeId: Int)

    // ---- Polygon Points CRUD ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPolygonPoints(points: List<PolygonPoint>)

    @Query("SELECT * FROM polygon_points WHERE employeeId = :employeeId ORDER BY pointOrder ASC")
    suspend fun getPolygonPoints(employeeId: Int): List<PolygonPoint>

    @Query("DELETE FROM polygon_points WHERE employeeId = :employeeId")
    suspend fun deletePolygonPoints(employeeId: Int)

    @Query("SELECT COUNT(*) FROM polygon_points WHERE employeeId = :employeeId")
    suspend fun getPolygonPointCount(employeeId: Int): Int

    @Query("SELECT COUNT(DISTINCT employeeId) FROM polygon_points")
    fun getGeofencedCount(): Flow<Int>
}
