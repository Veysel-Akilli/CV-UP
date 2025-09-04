package com.example.myapplication.data.repository

import com.example.myapplication.data.api.ApiService
import com.example.myapplication.data.model.Document
import com.example.myapplication.data.model.DocumentGenerateRequest
import com.example.myapplication.data.model.ExperienceBulletsRequest
import com.example.myapplication.data.model.ExperienceBulletsResponse
import com.example.myapplication.data.model.GeneratedDocumentResponse
import javax.inject.Inject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class DocumentRepository @Inject constructor(
    private val api: ApiService
) {

    suspend fun generateDocument(
        token: String,
        request: DocumentGenerateRequest
    ): GeneratedDocumentResponse {
        val response = api.generateDocument("Bearer $token", request)
        if (response.isSuccessful) {
            return response.body() ?: throw Exception("Boş yanıt")
        } else {
            val error = response.errorBody()?.string().orEmpty()
            throw Exception("Sunucu hatası: ${response.code()} - $error")
        }
    }

    suspend fun getUserDocuments(token: String): List<Document> {
        val response = api.getUserDocuments("Bearer $token")
        if (response.isSuccessful) {
            return response.body() ?: emptyList()
        } else {
            val error = response.errorBody()?.string().orEmpty()
            throw Exception("Belgeler alınamadı: ${response.code()} - $error")
        }
    }

    suspend fun uploadDocument(
        token: String,
        file: File,
        title: String,
        description: String
    ): Document {
        val requestFile = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
        val multipartBody = MultipartBody.Part.createFormData("file", file.name, requestFile)
        val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
        val descBody = description.toRequestBody("text/plain".toMediaTypeOrNull())

        val response = api.uploadDocument("Bearer $token", multipartBody, titleBody, descBody)
        if (response.isSuccessful) {
            return response.body() ?: throw Exception("Yanıt boş")
        } else {
            val error = response.errorBody()?.string().orEmpty()
            throw Exception("Yükleme başarısız: ${response.code()} - $error")
        }
    }

    suspend fun generateExperienceBullets(
        token: String,
        req: ExperienceBulletsRequest
    ): ExperienceBulletsResponse {
        val resp = api.generateExperienceBullets("Bearer $token", req)
        if (resp.isSuccessful) {
            return resp.body() ?: ExperienceBulletsResponse(bullets = emptyList(), text = null)
        } else {
            val msg = resp.errorBody()?.string().orEmpty()
            throw Exception("ExperienceBullets API error: ${resp.code()} - $msg")
        }
    }
}
