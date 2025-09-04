package com.example.myapplication.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.*
import com.example.myapplication.data.model.*
import com.example.myapplication.data.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    var loginState by mutableStateOf<Result<TokenResponse>?>(null)
        private set

    var registerState by mutableStateOf<Result<UserResponse>?>(null)
        private set

    fun register(email: String, fullName: String, password: String) {
        viewModelScope.launch {
            registerState = try {
                val response = repository.register(RegisterRequest(email, fullName, password))
                if (response.isSuccessful) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Kayıt başarısız"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            loginState = try {
                val response = repository.login(email, password)
                if (response.isSuccessful) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Giriş başarısız"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
