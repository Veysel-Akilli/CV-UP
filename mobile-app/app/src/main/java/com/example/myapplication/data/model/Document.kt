package com.example.myapplication.data.model

import com.google.gson.annotations.SerializedName

data class Document(
    val id: Int,
    val title: String,
    val description: String?,
    @SerializedName("file_type")
    val fileType: String?,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("file_size")
    val fileSize: Int
)
