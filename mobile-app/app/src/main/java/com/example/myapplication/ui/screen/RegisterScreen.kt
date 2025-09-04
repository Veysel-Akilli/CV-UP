package com.example.myapplication.ui.screen

import android.util.Patterns
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onRegisterSuccess: () -> Unit,
    onLoginClick: () -> Unit = {}
) {
    var email by rememberSaveable { mutableStateOf("") }
    var fullName by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    val isEmailValid = remember(email) { email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches() }
    val isNameValid = remember(fullName) { fullName.trim().length >= 2 }
    val isPasswordValid = remember(password) { password.length >= 6 }
    val isFormValid = remember(isEmailValid, isNameValid, isPasswordValid, isLoading) {
        isEmailValid && isNameValid && isPasswordValid && !isLoading
    }

    val state = viewModel.registerState
    LaunchedEffect(state) {
        state?.onSuccess {
            isLoading = false
            snackbarHostState.showSnackbar("Kayıt başarılı!")
            delay(600)
            onRegisterSuccess()
        }?.onFailure {
            isLoading = false
            errorMessage = it.message ?: "Kayıt başarısız oldu."
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            errorMessage = null
        }
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(
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
                title = { Text("Kayıt Ol", style = MaterialTheme.typography.titleLarge) }
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
                    Text("Aramıza katıl 🚀", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Hesabını oluştur, hemen kullanmaya başla",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(24.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("E-posta") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        isError = email.isNotBlank() && !isEmailValid,
                        singleLine = true,
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
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Ad Soyad") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        isError = fullName.isNotBlank() && !isNameValid,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    AnimatedVisibility(visible = fullName.isNotBlank() && !isNameValid) {
                        Text(
                            "Lütfen ad soyad girin.",
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
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showPassword) "Şifreyi gizle" else "Şifreyi göster"
                                )
                            }
                        },
                        singleLine = true,
                        isError = password.isNotBlank() && !isPasswordValid,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (isFormValid) {
                                    isLoading = true
                                    viewModel.register(email.trim(), fullName.trim(), password.trim())
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

                    TextButton(onClick = onLoginClick) {
                        Text("Giriş yap", color = Color(0xFF64B5F6))
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            isLoading = true
                            focusManager.clearFocus()
                            viewModel.register(email.trim(), fullName.trim(), password.trim())
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
                            Text("Kaydediliyor…")
                        } else {
                            Text("Kayıt Ol")
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
                ) { CircularProgressIndicator() }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RegisterScreenPreview() {
    MaterialTheme { RegisterScreen(onRegisterSuccess = {}) }
}