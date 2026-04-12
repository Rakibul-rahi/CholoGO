package com.example.chologo.viewmodel

import androidx.lifecycle.ViewModel
import com.example.chologo.navigation.Screen
import com.example.chologo.repository.UserRepository
import com.example.chologo.ui.auth.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val destination: String? = null,
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val userPhone: String = "",
    val userRole: String = ""
)

class AuthViewModel : ViewModel() {

    private val repository = UserRepository()

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signup(
        role: UserRole,
        name: String,
        email: String,
        phone: String,
        studentId: String,
        university: String,
        homeLocation: String,
        password: String
    ) {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            destination = null
        )

        repository.signup(
            role = role,
            name = name,
            email = email,
            phone = phone,
            studentId = studentId,
            university = university,
            homeLocation = homeLocation,
            password = password
        ) { result ->
            result.onSuccess { user ->
                val destination = when {
                    user.role.equals("passenger", ignoreCase = true) -> Screen.PassengerHome.route
                    user.role.equals("rider", ignoreCase = true) -> Screen.RiderHome.route
                    else -> null
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    destination = destination,
                    errorMessage = if (destination == null) "Invalid user role found" else null,
                    userId = user.uid,
                    userName = user.name,
                    userEmail = user.email,
                    userPhone = user.phone,
                    userRole = user.role
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Signup failed"
                )
            }
        }
    }

    fun login(email: String, password: String) {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            destination = null
        )

        repository.login(
            email = email,
            password = password
        ) { result ->
            result.onSuccess { user ->
                val destination = when {
                    user.role.equals("passenger", ignoreCase = true) -> Screen.PassengerHome.route
                    user.role.equals("rider", ignoreCase = true) -> Screen.RiderHome.route
                    else -> null
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    destination = destination,
                    errorMessage = if (destination == null) "Invalid user role found" else null,
                    userId = user.uid,
                    userName = user.name,
                    userEmail = user.email,
                    userPhone = user.phone,
                    userRole = user.role
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Login failed"
                )
            }
        }
    }

    fun loadCurrentUser() {
        repository.getCurrentUserData { result ->
            result.onSuccess { user ->
                _uiState.value = _uiState.value.copy(
                    userId = user.uid,
                    userName = user.name,
                    userEmail = user.email,
                    userPhone = user.phone,
                    userRole = user.role
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message
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

    fun logout() {
        repository.logout()
        _uiState.value = AuthUiState()
    }
}