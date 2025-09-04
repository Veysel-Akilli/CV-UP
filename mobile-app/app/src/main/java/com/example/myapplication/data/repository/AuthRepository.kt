package com.example.myapplication.data.repository

import com.example.myapplication.data.api.ApiService
import com.example.myapplication.data.model.RegisterRequest

class AuthRepository(private val api: ApiService) {
    suspend fun login(email: String, password: String) = api.login(email, password)
    suspend fun register(request: RegisterRequest) = api.register(request)
}
