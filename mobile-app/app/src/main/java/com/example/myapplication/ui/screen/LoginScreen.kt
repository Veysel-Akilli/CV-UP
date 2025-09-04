package com.example.myapplication.ui.screen

import android.util.Patterns
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onLoginSuccess: (String) -> Unit,
    onRegisterClick: () -> Unit = {}
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    val isEmailValid = remember(email) {
        email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    val isPasswordValid = remember(password) { password.length >= 6 }
    val isFormValid = remember(isEmailValid, isPasswordValid, isLoading) {
        isEmailValid && isPasswordValid && !isLoading
    }

    val state = viewModel.loginState
    LaunchedEffect(state) {
        state?.onSuccess {
            isLoading = false
            onLoginSuccess(it.access_token)
        }?.onFailure {
            isLoading = false
            errorMessage = it.message ?: "Bilinmeyen bir hata oluştu."
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            errorMessage = null
        }
    }

    val gradient = Brush.verticalGradient(
        listOf(
            Color(0xFF0F2027),
            Color(0xFF203A43),
            Color(0xFF2C5364)
        )
    )
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color(0xFF64B5F6),
        unfocusedBorderColor = Color(0xFF90CAF9),
        focusedLabelColor = Color(0xFFBBDEFB),
        cursorColor = Color(0xFF64B5F6)
    )
    val buttonColors = ButtonDefaults.buttonColors(
        containerColor = Color(0xFF1565C0),
        contentColor = Color.White,
        disabledContainerColor = Color(0xFF1565C0).copy(alpha = 0.4f),
        disabledContentColor = Color.White.copy(alpha = 0.7f)
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Giriş Yap", style = MaterialTheme.typography.titleLarge) }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(padding)
        ) {
            Card(
                modifier = Modifier
                    .padding(24.dp)
                    .align(Alignment.Center),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Hoş geldin 👋", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Hesabına giriş yaparak devam et",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(24.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("E-posta") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        singleLine = true,
                        isError = email.isNotBlank() && !isEmailValid,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    AnimatedVisibility(visible = email.isNotBlank() && !isEmailValid) {
                        Text(
                            "Lütfen geçerli bir e-posta girin.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Şifre") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Şifreyi gizle" else "Şifreyi göster"
                                )
                            }
                        },
                        singleLine = true,
                        isError = password.isNotBlank() && !isPasswordValid,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (isFormValid) {
                                    isLoading = true
                                    viewModel.login(email.trim(), password.trim())
                                }
                            }
                        ),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    AnimatedVisibility(visible = password.isNotBlank() && !isPasswordValid) {
                        Text(
                            "Şifre en az 6 karakter olmalı.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onRegisterClick) {
                            Text("Kayıt ol", color = Color(0xFF64B5F6))
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            isLoading = true
                            focusManager.clearFocus()
                            viewModel.login(email.trim(), password.trim())
                        },
                        enabled = isFormValid,
                        colors = buttonColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Giriş yapılıyor…")
                        } else {
                            Text("Giriş Yap")
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}


@Composable
private fun LoginScreenPreviewOnly() {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(Modifier.padding(24.dp)) {
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("E-posta") })
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Şifre") })
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewLoginCard() {
    MaterialTheme { LoginScreenPreviewOnly() }
}
