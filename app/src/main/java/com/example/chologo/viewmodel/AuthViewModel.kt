package com.example.chologo.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chologo.data.model.User
import com.example.chologo.navigation.Screen
import com.example.chologo.repository.UserRepository
import com.example.chologo.ui.auth.UserRole
import com.example.chologo.ui.auth.signInWithGoogleFirebase
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val destination: String? = null,
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val userPhone: String = "",
    val userRole: String = "",

    // Rider's vehicle, read back from their profile. Blank for passengers.
    // VehicleType.normalize() turns the blank a pre-car rider account still
    // has into "bike", so nothing downstream needs to special-case it.
    val userVehicleType: String = "",
    val userVehicleModel: String = "",
    val userVehicleNumber: String = "",
    val userVehicleColor: String = ""
)

/**
 * Every auth path (signup, login, Google sign-in, profile completion,
 * refresh) publishes the same profile fields, so they share one copy step
 * instead of five hand-maintained lists that drift apart whenever a field is
 * added.
 */
private fun AuthUiState.withUserProfile(user: User): AuthUiState {
    return copy(
        userId = user.uid,
        userName = user.name,
        userEmail = user.email,
        userPhone = user.phone,
        userRole = user.role,
        userVehicleType = user.vehicleType,
        userVehicleModel = user.vehicleModel,
        userVehicleNumber = user.vehicleNumber,
        userVehicleColor = user.vehicleColor
    )
}

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
        password: String,
        vehicleType: String = "",
        vehicleModel: String = "",
        vehicleNumber: String = "",
        vehicleColor: String = ""
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
            password = password,
            vehicleType = vehicleType,
            vehicleModel = vehicleModel,
            vehicleNumber = vehicleNumber,
            vehicleColor = vehicleColor
        ) { result ->
            result.onSuccess { user ->
                val destination = when {
                    user.role.equals("passenger", ignoreCase = true) -> Screen.PassengerHome.route
                    user.role.equals("rider", ignoreCase = true) -> Screen.RiderHome.route
                    else -> null
                }

                repository.registerFcmTokenForUser(user.uid)

                _uiState.value = _uiState.value.withUserProfile(user).copy(
                    isLoading = false,
                    destination = destination,
                    errorMessage = if (destination == null) "Invalid user role found" else null
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

                repository.registerFcmTokenForUser(user.uid)

                _uiState.value = _uiState.value.withUserProfile(user).copy(
                    isLoading = false,
                    destination = destination,
                    errorMessage = if (destination == null) "Invalid user role found" else null
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Login failed"
                )
            }
        }
    }

    fun signInWithGoogle(context: Context) {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            destination = null
        )

        viewModelScope.launch {
            val result = signInWithGoogleFirebase(context)

            result.onSuccess { uid ->
                repository.getUserByUid(uid) { userResult ->
                    userResult.onSuccess { user ->
                        if (user != null && user.role.isNotBlank()) {
                            val destination = when {
                                user.role.equals("passenger", ignoreCase = true) -> Screen.PassengerHome.route
                                user.role.equals("rider", ignoreCase = true) -> Screen.RiderHome.route
                                else -> Screen.RoleSelection.route
                            }

                            repository.registerFcmTokenForUser(user.uid)

                            _uiState.value = _uiState.value.withUserProfile(user).copy(
                                isLoading = false,
                                destination = destination
                            )
                        } else {
                            // First time signing in with this Google account -
                            // no Firestore profile yet. RoleSelectionScreen
                            // collects the role/phone Google doesn't provide.
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                destination = Screen.RoleSelection.route,
                                userId = uid
                            )
                        }
                    }.onFailure { e ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = e.message ?: "Failed to load profile."
                        )
                    }
                }
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Google sign-in failed."
                )
            }
        }
    }

    fun completeGoogleProfile(
        role: UserRole,
        phone: String,
        vehicleType: String = "",
        vehicleModel: String = "",
        vehicleNumber: String = "",
        vehicleColor: String = ""
    ) {
        val firebaseUser = FirebaseAuth.getInstance().currentUser

        if (firebaseUser == null) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "You're not signed in. Please try Google sign-in again."
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            destination = null
        )

        repository.completeGoogleProfile(
            uid = firebaseUser.uid,
            name = firebaseUser.displayName ?: "",
            email = firebaseUser.email ?: "",
            phone = phone,
            role = role,
            vehicleType = vehicleType,
            vehicleModel = vehicleModel,
            vehicleNumber = vehicleNumber,
            vehicleColor = vehicleColor
        ) { result ->
            result.onSuccess { user ->
                val destination = when {
                    user.role.equals("passenger", ignoreCase = true) -> Screen.PassengerHome.route
                    user.role.equals("rider", ignoreCase = true) -> Screen.RiderHome.route
                    else -> null
                }

                repository.registerFcmTokenForUser(user.uid)

                _uiState.value = _uiState.value.withUserProfile(user).copy(
                    isLoading = false,
                    destination = destination,
                    errorMessage = if (destination == null) "Invalid user role found" else null
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to save your profile."
                )
            }
        }
    }

    fun loadCurrentUser() {
        repository.getCurrentUserData { result ->
            result.onSuccess { user ->
                repository.registerFcmTokenForUser(user.uid)

                _uiState.value = _uiState.value.withUserProfile(user)
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