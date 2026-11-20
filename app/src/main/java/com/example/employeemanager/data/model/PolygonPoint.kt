package com.example.employeemanager.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "polygon_points",
    foreignKeys = [ForeignKey(
        entity = Employee::class,
        parentColumns = ["employeeId"],
        childColumns = ["employeeId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("employeeId")]
)
data class PolygonPoint(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val employeeId: Int,
    val latitude: Double,
    val longitude: Double,
    val pointOrder: Int
)
