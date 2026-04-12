package com.example.chologo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chologo.data.model.LiveRide
import com.example.chologo.data.model.RideNowRequest
import com.example.chologo.data.model.RideNowStatus
import com.example.chologo.repository.RideNowRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RideNowUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,

    val currentRequestId: String? = null,
    val currentLiveRideId: String? = null,

    val passengerRequest: RideNowRequest? = null,
    val riderLiveRide: LiveRide? = null,

    val availableRequests: List<RideNowRequest> = emptyList(),

    val isRiderLive: Boolean = false,
    val isRequestActive: Boolean = false
)

class RideNowViewModel(
    private val repository: RideNowRepository = RideNowRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RideNowUiState())
    val uiState: StateFlow<RideNowUiState> = _uiState.asStateFlow()

    private var matchingRequestsListener: ListenerRegistration? = null
    private var passengerRequestListener: ListenerRegistration? = null
    private var liveRideListener: ListenerRegistration? = null

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }

    fun createRideNowRequest(
        passengerId: String,
        passengerName: String,
        pickup: String,
        destination: String,
        tripTime: String,
        timeMinutes: Int,
        routeKey: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            val now = Timestamp.now()

            val request = RideNowRequest(
                passengerId = passengerId,
                passengerName = passengerName,
                pickup = pickup,
                destination = destination,
                tripTime = tripTime,
                timeMinutes = timeMinutes,
                routeKey = routeKey,
                status = RideNowStatus.SEARCHING,
                createdAt = now,
                expiresAt = Timestamp(now.seconds + 120, 0)
            )

            val result = repository.createRideNowRequest(request)

            result.onSuccess { requestId ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    currentRequestId = requestId,
                    isRequestActive = true,
                    successMessage = "Ride now request created."
                )

                listenToPassengerRequest(requestId)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to create request."
                )
            }
        }
    }

    fun cancelRideNowRequest() {
        val requestId = _uiState.value.currentRequestId ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            val result = repository.cancelRideNowRequest(requestId)

            result.onSuccess {
                stopPassengerRequestListener()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    currentRequestId = null,
                    passengerRequest = null,
                    isRequestActive = false,
                    successMessage = "Ride now request cancelled."
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to cancel request."
                )
            }
        }
    }

    fun goLiveAsRider(
        riderId: String,
        riderName: String,
        pickup: String,
        destination: String,
        tripDirection: String,
        tripTime: String,
        timeMinutes: Int,
        routeKey: String,
        availableSeats: Int
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            val now = Timestamp.now()

            val liveRide = LiveRide(
                riderId = riderId,
                riderName = riderName,
                pickup = pickup,
                destination = destination,
                tripDirection = tripDirection,
                tripTime = tripTime,
                timeMinutes = timeMinutes,
                routeKey = routeKey,
                availableSeats = availableSeats,
                status = "active",
                isLiveNow = true,
                isAvailable = true,
                currentRequestId = "",
                createdAt = now,
                lastUpdatedAt = now
            )

            val result = repository.goLiveAsRider(liveRide)

            result.onSuccess { rideId ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    currentLiveRideId = rideId,
                    isRiderLive = true,
                    successMessage = "You are now live for Ride Now."
                )

                listenToLiveRide(rideId)
                listenForMatchingRequests(routeKey)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to go live."
                )
            }
        }
    }

    fun stopLiveRide() {
        val currentState = _uiState.value
        val rideId = currentState.currentLiveRideId ?: return

        val hasActiveRequestId =
            !currentState.riderLiveRide?.currentRequestId.isNullOrBlank()

        val requestStatus = currentState.passengerRequest?.status

        val hasLockedTrip =
            requestStatus == RideNowStatus.ACCEPTED ||
                    requestStatus == RideNowStatus.ONGOING

        if (hasActiveRequestId || hasLockedTrip) {
            _uiState.value = currentState.copy(
                errorMessage = "You cannot stop live after accepting a request. Complete the trip first."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            val result = repository.stopLiveRide(rideId)

            result.onSuccess {
                stopMatchingRequestsListener()
                stopLiveRideListener()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    currentLiveRideId = null,
                    riderLiveRide = null,
                    availableRequests = emptyList(),
                    isRiderLive = false,
                    successMessage = "Live ride stopped."
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to stop live ride."
                )
            }
        }
    }

    fun listenForMatchingRequests(routeKey: String) {
        stopMatchingRequestsListener()

        matchingRequestsListener = repository.listenForMatchingRequests(
            routeKey = routeKey,
            onData = { requests ->
                val nowSeconds = Timestamp.now().seconds
                val isBusy = !_uiState.value.riderLiveRide?.currentRequestId.isNullOrBlank()

                val validRequests = requests.filter { request ->
                    val expiresAtSeconds = request.expiresAt?.seconds
                    val isSearching = request.status == RideNowStatus.SEARCHING
                    val isNotExpired = expiresAtSeconds == null || expiresAtSeconds > nowSeconds

                    isSearching && isNotExpired
                }

                _uiState.value = _uiState.value.copy(
                    availableRequests = if (isBusy) emptyList() else validRequests
                )
            },
            onError = { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to load matching requests."
                )
            }
        )
    }

    fun listenToPassengerRequest(requestId: String) {
        stopPassengerRequestListener()

        passengerRequestListener = repository.listenToPassengerRequest(
            requestId = requestId,
            onData = { request ->
                if (request == null) {
                    _uiState.value = _uiState.value.copy(
                        passengerRequest = null,
                        currentRequestId = null,
                        isRequestActive = false
                    )
                    return@listenToPassengerRequest
                }

                val nowSeconds = Timestamp.now().seconds
                val expiresAtSeconds = request.expiresAt?.seconds
                val isExpired =
                    request.status == RideNowStatus.SEARCHING &&
                            expiresAtSeconds != null &&
                            expiresAtSeconds <= nowSeconds

                if (isExpired) {
                    expireRequestAutomatically(request.requestId)

                    _uiState.value = _uiState.value.copy(
                        passengerRequest = request,
                        currentRequestId = request.requestId,
                        isRequestActive = false,
                        successMessage = "Ride now request expired."
                    )
                    return@listenToPassengerRequest
                }

                val active = request.status in listOf(
                    RideNowStatus.SEARCHING,
                    RideNowStatus.ACCEPTED,
                    RideNowStatus.ONGOING
                )

                _uiState.value = _uiState.value.copy(
                    passengerRequest = request,
                    currentRequestId = request.requestId,
                    isRequestActive = active
                )
            },
            onError = { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to listen to request."
                )
            }
        )
    }

    fun listenToLiveRide(rideId: String) {
        stopLiveRideListener()

        liveRideListener = repository.listenToLiveRide(
            rideId = rideId,
            onData = { ride ->
                val isLive = ride?.isLiveNow == true && ride.status == "active"
                val isBusy = !ride?.currentRequestId.isNullOrBlank()

                _uiState.value = _uiState.value.copy(
                    riderLiveRide = ride,
                    currentLiveRideId = ride?.rideId,
                    isRiderLive = isLive,
                    availableRequests = if (isBusy) emptyList() else _uiState.value.availableRequests
                )
            },
            onError = { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to listen to live ride."
                )
            }
        )
    }

    fun acceptRideNowRequest(
        requestId: String,
        riderId: String,
        riderName: String,
        riderPhone: String
    ) {
        val liveRideId = _uiState.value.currentLiveRideId
        if (liveRideId.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "No active live ride found."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            val result = repository.acceptRideNowRequest(
                requestId = requestId,
                liveRideId = liveRideId,
                riderId = riderId,
                riderName = riderName,
                riderPhone = riderPhone
            )

            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    currentRequestId = requestId,
                    availableRequests = emptyList(),
                    successMessage = "Ride now request accepted."
                )

                listenToPassengerRequest(requestId)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to accept request."
                )
            }
        }
    }

    fun startRideNowTrip() {
        val requestId = _uiState.value.currentRequestId
            ?: _uiState.value.passengerRequest?.requestId

        if (requestId.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "No active request found."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            val result = repository.startRideNowTrip(requestId)

            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Trip started."
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to start trip."
                )
            }
        }
    }

    fun completeRideNowTrip() {
        val requestId = _uiState.value.currentRequestId
            ?: _uiState.value.passengerRequest?.requestId
            ?: _uiState.value.riderLiveRide?.currentRequestId

        val liveRideId = _uiState.value.currentLiveRideId
            ?: _uiState.value.riderLiveRide?.rideId

        if (requestId.isNullOrBlank() || liveRideId.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Missing trip info."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            val result = repository.completeRideNowTrip(
                requestId = requestId,
                liveRideId = liveRideId
            )

            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRequestActive = false,
                    currentRequestId = null,
                    passengerRequest = null,
                    availableRequests = emptyList(),
                    successMessage = "Trip completed."
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to complete trip."
                )
            }
        }
    }

    fun checkRequestExpiry() {
        val requestId = _uiState.value.currentRequestId ?: return

        viewModelScope.launch {
            repository.expireRequestIfNeeded(requestId)
        }
    }

    fun loadRequestById(requestId: String) {
        viewModelScope.launch {
            val request = repository.getRideNowRequestById(requestId)

            if (request != null) {
                val nowSeconds = Timestamp.now().seconds
                val expiresAtSeconds = request.expiresAt?.seconds
                val isExpired =
                    request.status == RideNowStatus.SEARCHING &&
                            expiresAtSeconds != null &&
                            expiresAtSeconds <= nowSeconds

                if (isExpired) {
                    expireRequestAutomatically(request.requestId)

                    _uiState.value = _uiState.value.copy(
                        passengerRequest = request,
                        currentRequestId = request.requestId,
                        isRequestActive = false,
                        successMessage = "Ride now request expired."
                    )
                    return@launch
                }
            }

            _uiState.value = _uiState.value.copy(
                passengerRequest = request,
                currentRequestId = request?.requestId,
                isRequestActive = request?.status in listOf(
                    RideNowStatus.SEARCHING,
                    RideNowStatus.ACCEPTED,
                    RideNowStatus.ONGOING
                )
            )

            if (request != null) {
                listenToPassengerRequest(request.requestId)
            }
        }
    }

    fun loadLiveRideById(rideId: String) {
        viewModelScope.launch {
            val ride = repository.getLiveRideById(rideId)

            _uiState.value = _uiState.value.copy(
                riderLiveRide = ride,
                currentLiveRideId = ride?.rideId,
                isRiderLive = ride?.isLiveNow == true && ride.status == "active"
            )

            if (ride != null) {
                listenToLiveRide(ride.rideId)
                listenForMatchingRequests(ride.routeKey)
            }
        }
    }

    private fun expireRequestAutomatically(requestId: String) {
        viewModelScope.launch {
            repository.expireRequestIfNeeded(requestId)
        }
    }

    private fun stopMatchingRequestsListener() {
        matchingRequestsListener?.remove()
        matchingRequestsListener = null
    }

    private fun stopPassengerRequestListener() {
        passengerRequestListener?.remove()
        passengerRequestListener = null
    }

    private fun stopLiveRideListener() {
        liveRideListener?.remove()
        liveRideListener = null
    }

    override fun onCleared() {
        super.onCleared()
        stopMatchingRequestsListener()
        stopPassengerRequestListener()
        stopLiveRideListener()
    }
}