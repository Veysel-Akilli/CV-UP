package com.example.myapplication.data.model

data class DocumentGenerateRequest(
    val document_type: String,
    val input_data: Map<String, Any>
)

data class GeneratedDocumentResponse(
    val document_type: String,
    val input_data: Map<String, Any>,
    val generated_content: String
)
