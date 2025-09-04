package com.example.myapplication.data.api

import com.example.myapplication.data.model.Document
import com.example.myapplication.data.model.DocumentGenerateRequest
import com.example.myapplication.data.model.GeneratedDocumentResponse
import com.example.myapplication.data.model.RegisterRequest
import com.example.myapplication.data.model.TokenResponse
import com.example.myapplication.data.model.UserResponse
import com.example.myapplication.data.model.ExperienceBulletsRequest
import com.example.myapplication.data.model.ExperienceBulletsResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("api/v1/auth/login")
    @FormUrlEncoded
    suspend fun login(
        @Field("username") email: String,
        @Field("password") password: String
    ): Response<TokenResponse>

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<UserResponse>

    @GET("api/v1/users/me")
    suspend fun getCurrentUser(@Header("Authorization") token: String): Response<UserResponse>

    @POST("api/v1/document-requests/generate")
    suspend fun generateDocument(
        @Header("Authorization") token: String,
        @Body request: DocumentGenerateRequest
    ): Response<GeneratedDocumentResponse>

    @GET("api/v1/documents/")
    suspend fun getUserDocuments(
        @Header("Authorization") token: String
    ): Response<List<Document>>

    @GET("api/v1/documents/{id}")
    suspend fun getDocumentById(
        @Header("Authorization") token: String,
        @Path("id") documentId: Int
    ): Response<Document>

    @Multipart
    @POST("api/v1/documents/upload")
    suspend fun uploadDocument(
        @Header("Authorization") token: String,
        @Part file: MultipartBody.Part,
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody,
        @Part("is_public") isPublic: RequestBody = "false".toRequestBody("text/plain".toMediaTypeOrNull())
    ): Response<Document>


    @POST("api/v1/document-requests/experience-bullets")
    suspend fun generateExperienceBullets(
        @Header("Authorization") token: String,
        @Body req: ExperienceBulletsRequest
    ): Response<ExperienceBulletsResponse>
}
