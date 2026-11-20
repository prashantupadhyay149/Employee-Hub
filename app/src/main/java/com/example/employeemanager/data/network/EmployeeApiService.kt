package com.example.employeemanager.data.network

import com.example.employeemanager.data.model.ApiResponse
import retrofit2.Response
import retrofit2.http.GET

interface EmployeeApiService {
    @GET("08f6af34-d99f-4ab2-bd09-6c9e953829d4")
    suspend fun getEmployees(): Response<ApiResponse>
}
