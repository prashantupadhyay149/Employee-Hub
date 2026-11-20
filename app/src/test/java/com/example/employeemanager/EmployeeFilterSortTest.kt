package com.example.employeemanager

import com.example.employeemanager.data.model.Document
import com.example.employeemanager.data.model.Employee
import com.example.employeemanager.data.model.EmployeeWithDocuments
import com.example.employeemanager.ui.list.FilterType
import com.example.employeemanager.ui.list.SortType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class EmployeeFilterSortTest {

    private lateinit var sampleEmployees: List<EmployeeWithDocuments>

    @Before
    fun setup() {
        val emp1 = Employee(
            employeeId = 101,
            employeeName = "John Smith",
            departmentId = 7,
            managerId = 26,
            branchId = 1,
            isActive = false
        )
        val emp2 = Employee(
            employeeId = 102,
            employeeName = "Alice Walker",
            departmentId = 3,
            managerId = 12,
            branchId = 2,
            isActive = true
        )
        val emp3 = Employee(
            employeeId = 103,
            employeeName = "Robert Brown",
            departmentId = 7,
            managerId = 26,
            branchId = 1,
            isActive = true
        )

        sampleEmployees = listOf(
            EmployeeWithDocuments(emp1, listOf(
                Document(501, 101, "Address Proof", "JPG", true)
            )),
            EmployeeWithDocuments(emp2, emptyList()),
            EmployeeWithDocuments(emp3, listOf(
                Document(502, 103, "Offer Letter", "PDF", true)
            ))
        )
    }

    @Test
    fun testSearchByName() {
        val query = "alice"
        val filtered = sampleEmployees.filter {
            it.employee.employeeName.contains(query, ignoreCase = true)
        }
        assertEquals(1, filtered.size)
        assertEquals("Alice Walker", filtered[0].employee.employeeName)
    }

    @Test
    fun testSearchById() {
        val query = "101"
        val filtered = sampleEmployees.filter {
            it.employee.employeeId.toString().contains(query)
        }
        assertEquals(1, filtered.size)
        assertEquals("John Smith", filtered[0].employee.employeeName)
    }

    @Test
    fun testFilterActive() {
        val activeOnly = sampleEmployees.filter { it.employee.isActive }
        assertEquals(2, activeOnly.size)
        assertTrue(activeOnly.all { it.employee.isActive })
    }

    @Test
    fun testFilterInactive() {
        val inactiveOnly = sampleEmployees.filter { !it.employee.isActive }
        assertEquals(1, inactiveOnly.size)
        assertEquals(101, inactiveOnly[0].employee.employeeId)
    }

    @Test
    fun testSortByNameAscending() {
        val sorted = sampleEmployees.sortedBy { it.employee.employeeName }
        assertEquals("Alice Walker", sorted[0].employee.employeeName)
        assertEquals("John Smith", sorted[1].employee.employeeName)
        assertEquals("Robert Brown", sorted[2].employee.employeeName)
    }

    @Test
    fun testSortByIdDescending() {
        val sorted = sampleEmployees.sortedByDescending { it.employee.employeeId }
        assertEquals(103, sorted[0].employee.employeeId)
        assertEquals(102, sorted[1].employee.employeeId)
        assertEquals(101, sorted[2].employee.employeeId)
    }
}
