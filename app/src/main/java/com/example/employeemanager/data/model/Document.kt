package com.example.employeemanager.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(
    tableName = "documents",
    foreignKeys = [ForeignKey(
        entity = Employee::class,
        parentColumns = ["employeeId"],
        childColumns = ["employeeId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("employeeId")]
)
data class Document(
    @PrimaryKey
    @SerializedName("DocumentId")
    val documentId: Int = 0,

    @SerializedName("EmployeeId")
    val employeeId: Int,

    @SerializedName("DocumentName")
    val documentName: String,

    @SerializedName("DocumentType")
    val documentType: String,

    @SerializedName("IsVerified")
    val isVerified: Boolean,

    @SerializedName("DocumentUrl")
    val documentUrl: String = "",

    @SerializedName("ThumbnailUrl")
    val thumbnailUrl: String = "",

    @SerializedName("CreatedDate")
    val createdDate: String = "",

    @SerializedName("UpdatedDate")
    val updatedDate: String = ""
)
