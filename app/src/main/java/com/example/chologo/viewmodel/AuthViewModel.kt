package com.example.chologo.viewmodel

import com.example.chologo.repository.UserRepository
import androidx.lifecycle.ViewModel
import com.example.chologo.navigation.Screen
import com.example.chologo.ui.auth.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val destination: String? = null
)

class AuthViewModel : ViewModel() {

    private val repository = UserRepository()

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    fun signup(
        role: UserRole,
        name: String,
        email: String,
        phone: String,
        studentId: String,
        university: String,
        homeLocation: String,
        password: String
    ){
        _uiState.value = AuthUiState(isLoading = true)

        repository.signup(
            role = role,
            name = name,
            email = email,
            phone = phone,
            studentId = studentId,
            university = university,
            homeLocation = homeLocation,
            password = password
        ){ result ->

            result.onSuccess { savedRole ->
                val destination = when {
                    savedRole.equals("passenger", ignoreCase = true) -> Screen.PassengerHome.route
                    savedRole.equals("rider", ignoreCase = true) -> Screen.RiderHome.route
                    else -> null
                }

                _uiState.value = AuthUiState(
                    isLoading = false,
                    destination = destination,
                    errorMessage = if (destination == null) "Invalid user role found" else null
                )
            }.onFailure { e ->
                _uiState.value = AuthUiState(
                    isLoading = false,
                    errorMessage = e.message ?: "Signup failed"
                )
            }
        }
    }

    fun login(email: String, password: String) {
        _uiState.value = AuthUiState(isLoading = true)

        repository.login(
            email = email,
            password = password
        ) { result ->

            result.onSuccess { savedRole ->
                val destination = when {
                    savedRole.equals("passenger", ignoreCase = true) -> Screen.PassengerHome.route
                    savedRole.equals("rider", ignoreCase = true) -> Screen.RiderHome.route
                    else -> null
                }

                _uiState.value = AuthUiState(
                    isLoading = false,
                    destination = destination,
                    errorMessage = if (destination == null) "Invalid user role found" else null
                )
            }.onFailure { e ->
                _uiState.value = AuthUiState(
                    isLoading = false,
                    errorMessage = e.message ?: "Login failed"
                )
            }
        }
    }

    fun clearNavigation() {
        _uiState.value = _uiState.value.copy(destination = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}