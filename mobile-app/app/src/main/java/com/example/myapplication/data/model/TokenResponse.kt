package com.example.myapplication.data.model

data class TokenResponse(val access_token: String, val token_type: String)
data class RegisterRequest(val email: String, val full_name: String?, val password: String)
data class UserResponse(val id: String, val email: String, val full_name: String?, val created_at: String)
