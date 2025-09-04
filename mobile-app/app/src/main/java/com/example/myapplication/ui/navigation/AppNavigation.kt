package com.example.myapplication.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.myapplication.ui.screen.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication.viewmodel.DocumentViewModel

@Composable
fun AppNavigation(navController: NavHostController) {
    var token by rememberSaveable { mutableStateOf("") }

    val cvViewModel: DocumentViewModel = hiltViewModel()

    NavHost(navController = navController, startDestination = "login") {
        composable("register") {
            RegisterScreen(
                onRegisterSuccess = { navController.navigate("login") },
                onLoginClick = { navController.navigate("login") }
            )
        }


        composable("login") {
            LoginScreen(
                onLoginSuccess = { authToken ->
                    token = authToken
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onRegisterClick = { navController.navigate("register") }
            )
        }



        composable("home") {
            if (token.isBlank()) {
                LaunchedEffect(Unit) {
                    navController.navigate("login") { popUpTo("home") { inclusive = true } }
                }
            } else {
                HomeScreen(navController = navController, token = token)
            }
        }



        composable("generate/cv") {
            if (token.isBlank()) {
                LaunchedEffect(Unit) {
                    navController.navigate("login") { popUpTo("generate/cv") { inclusive = true } }
                }
            } else {
                GenerateScreen(
                    documentType = "cv",
                    token = token,
                    navController = navController,
                    viewModel = cvViewModel
                )
            }
        }

        composable("generate/{docType}") { backStackEntry ->
            val docType = backStackEntry.arguments?.getString("docType") ?: ""
            if (token.isBlank()) {
                LaunchedEffect(Unit) {
                    navController.navigate("login") { popUpTo("generate/{docType}") { inclusive = true } }
                }
            } else {
                GenerateScreen(
                    documentType = docType,
                    token = token,
                    navController = navController,
                    viewModel = cvViewModel
                )
            }
        }

        composable(
            route = "cv/section/{section}",
            arguments = listOf(navArgument("section") { type = NavType.StringType })
        ) { backStack ->
            val section = backStack.arguments?.getString("section") ?: "personal"
            if (token.isBlank()) {
                LaunchedEffect(Unit) {
                    navController.navigate("login") { popUpTo("cv/section/{section}") { inclusive = true } }
                }
            } else {
                SectionScreen(
                    section = section,
                    token = token,
                    navController = navController,
                    viewModel = cvViewModel
                )
            }
        }

        composable("cv/review") {
            if (token.isBlank()) {
                LaunchedEffect(Unit) {
                    navController.navigate("login") { popUpTo("cv/review") { inclusive = true } }
                }
            } else {
                ReviewAndCreateScreen(
                    token = token,
                    navController = navController,
                    viewModel = cvViewModel
                )
            }
        }
    }
}
