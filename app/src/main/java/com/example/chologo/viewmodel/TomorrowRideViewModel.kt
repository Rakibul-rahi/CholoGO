package com.example.chologo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chologo.data.model.Ride
import com.example.chologo.data.model.RideRequest
import com.example.chologo.data.model.isTimeClose
import com.example.chologo.data.repository.TomorrowLegResult
import com.example.chologo.data.repository.TomorrowRideRepository
import com.example.chologo.repository.UserRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Shown to a RIDER: a passenger request matching one of their saved rides. */
data class TomorrowMatchedRequest(
    val rideId: String,
    val requestId: String,
    val passengerName: String,
    val tripDirection: String,
    val pickup: String,
    val destination: String,
    val tripTime: String,
    val timeMinutes: Int
)

/** Shown to a PASSENGER: a rider's ride matching one of their saved requests. */
data class TomorrowMatchedRide(
    val rideId: String,
    val requestId: String,
    val riderName: String,
    val tripDirection: String,
    val pickup: String,
    val destination: String,
    val tripTime: String,
    val availableSeats: Int
)

data class TomorrowRideUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,

    // ---- Rider side ----
    val savedRides: List<Ride> = emptyList(),
    val pendingRequestsForDate: List<RideRequest> = emptyList(),
    val acceptedRequestsForRider: List<RideRequest> = emptyList(),
    val matchedRequestsForRider: List<TomorrowMatchedRequest> = emptyList(),
    val processingRequestIds: Set<String> = emptySet(),

    // ---- Passenger side ----
    val savedRequests: List<RideRequest> = emptyList(),
    val activeRidesForDate: List<Ride> = emptyList(),
    val matchedRidesForPassenger: List<TomorrowMatchedRide> = emptyList()
)

class TomorrowRideViewModel(
    private val repository: TomorrowRideRepository = TomorrowRideRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(TomorrowRideUiState())
    val uiState: StateFlow<TomorrowRideUiState> = _uiState.asStateFlow()

    private var ridesListener: ListenerRegistration? = null
    private var pendingRequestsListener: ListenerRegistration? = null
    private var acceptedRequestsListener: ListenerRegistration? = null
    private var requestsListener: ListenerRegistration? = null
    private var activeRidesListener: ListenerRegistration? = null

    private var currentRiderId: String? = null
    private var currentUserId: String? = null

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }

    // =====================================================================
    // Rider side
    // =====================================================================

    /**
     * Starts live listeners for this rider's saved rides and all pending
     * passenger requests for the date. Safe to call repeatedly.
     */
    fun startRiderSession(riderId: String, rideDate: String) {
        if (riderId.isBlank()) return
        currentRiderId = riderId

        ridesListener?.remove()
        ridesListener = repository.listenRiderRides(
            riderId = riderId,
            rideDate = rideDate,
            onData = { rides ->
                _uiState.value = _uiState.value.copy(savedRides = rides)
                recomputeRiderMatches()
            },
            onError = { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to load your saved rides."
                )
            }
        )

        pendingRequestsListener?.remove()
        pendingRequestsListener = repository.listenPendingRequests(
            rideDate = rideDate,
            onData = { requests ->
                _uiState.value = _uiState.value.copy(pendingRequestsForDate = requests)
                recomputeRiderMatches()
            },
            onError = { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to load passenger requests."
                )
            }
        )

        acceptedRequestsListener?.remove()
        acceptedRequestsListener = repository.listenAcceptedRequestsForRider(
            riderId = riderId,
            rideDate = rideDate,
            onData = { requests ->
                _uiState.value = _uiState.value.copy(acceptedRequestsForRider = requests)
            },
            onError = { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to load accepted requests."
                )
            }
        )
    }

    private fun recomputeRiderMatches() {
        val riderId = currentRiderId ?: return
        val rides = _uiState.value.savedRides
        val requests = _uiState.value.pendingRequestsForDate

        if (rides.isEmpty() || requests.isEmpty()) {
            _uiState.value = _uiState.value.copy(matchedRequestsForRider = emptyList())
            return
        }

        val matched = mutableListOf<TomorrowMatchedRequest>()

        rides.forEach { ride ->
            requests.filter { request ->
                ride.status == "active" &&
                        ride.availableSeats > 0 &&
                        ride.routeKey == request.routeKey &&
                        isTimeClose(ride.timeMinutes, request.timeMinutes) &&
                        !request.rejectedByRiderIds.contains(riderId)
            }.forEach { request ->
                matched.add(
                    TomorrowMatchedRequest(
                        rideId = ride.rideId,
                        requestId = request.requestId,
                        passengerName = request.passengerName.ifBlank { "Passenger" },
                        tripDirection = request.tripDirection,
                        pickup = request.pickup,
                        destination = request.destination,
                        tripTime = request.tripTime,
                        timeMinutes = request.timeMinutes
                    )
                )
            }
        }

        _uiState.value = _uiState.value.copy(
            matchedRequestsForRider = matched.distinctBy { "${it.rideId}_${it.requestId}" }
        )
    }

    /**
     * Saves (creates or updates) a rider's campus + return legs for
     * tomorrow. XP is only awarded if at least one leg was genuinely new -
     * re-saving an unchanged plan, or updating an existing open leg, does
     * not grant XP again. Any leg that's already matched with a passenger
     * is left untouched and reported back as blocked rather than silently
     * overwritten.
     */
    fun saveRiderPlan(
        riderId: String,
        riderName: String,
        rideDate: String,
        campusPickup: String,
        campusTripTime: String,
        campusTimeMinutes: Int,
        homeDestination: String,
        homeTripTime: String,
        homeTimeMinutes: Int,
        onXpAwarded: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val campusResult = repository.upsertRiderRide(
                riderId = riderId,
                riderName = riderName,
                rideDate = rideDate,
                tripDirection = "to_campus",
                pickup = campusPickup,
                destination = "AUST Gate",
                tripTime = campusTripTime,
                timeMinutes = campusTimeMinutes
            )

            val homeResult = repository.upsertRiderRide(
                riderId = riderId,
                riderName = riderName,
                rideDate = rideDate,
                tripDirection = "to_home",
                pickup = "AUST Gate",
                destination = homeDestination,
                tripTime = homeTripTime,
                timeMinutes = homeTimeMinutes
            )

            handlePlanSaveOutcome(
                listOf(campusResult, homeResult),
                xpAmount = 5L,
                onXpAwarded = onXpAwarded
            )
        }
    }

    fun removeRiderRide(rideId: String) {
        val riderId = currentRiderId ?: return

        viewModelScope.launch {
            val result = repository.deleteRide(rideId, riderId)

            result.onSuccess {
                _uiState.value = _uiState.value.copy(successMessage = "Ride removed.")
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to remove ride."
                )
            }
        }
    }

    fun acceptRequest(
        match: TomorrowMatchedRequest,
        riderId: String,
        riderName: String,
        riderPhone: String,
        onXpAwarded: () -> Unit = {}
    ) {
        setProcessing(match.requestId, true)

        viewModelScope.launch {
            val result = repository.acceptRequest(
                rideId = match.rideId,
                requestId = match.requestId,
                riderId = riderId,
                riderName = riderName,
                riderPhone = riderPhone
            )

            setProcessing(match.requestId, false)

            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    successMessage = "Passenger accepted! (+10 XP)"
                )

                userRepository.addXpToCurrentUser(10L) { xpResult ->
                    xpResult.onSuccess { onXpAwarded() }
                }
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to accept request."
                )
            }
        }
    }

    fun declineRequest(match: TomorrowMatchedRequest, riderId: String) {
        setProcessing(match.requestId, true)

        viewModelScope.launch {
            val result = repository.declineRequest(match.requestId, riderId)

            setProcessing(match.requestId, false)

            result.onSuccess {
                _uiState.value = _uiState.value.copy(successMessage = "Request declined.")
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to decline request."
                )
            }
        }
    }

    fun cancelAcceptedRideAsRider(
        request: RideRequest,
        riderId: String,
        reason: String
    ) {
        setProcessing(request.requestId, true)

        viewModelScope.launch {
            val result = repository.riderCancelAcceptedRide(
                rideId = request.matchedRideId,
                requestId = request.requestId,
                riderId = riderId,
                reason = reason
            )

            setProcessing(request.requestId, false)

            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    successMessage = "Ride cancelled and seat freed up."
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to cancel ride."
                )
            }
        }
    }

    // =====================================================================
    // Passenger side
    // =====================================================================

    fun startPassengerSession(userId: String, rideDate: String) {
        if (userId.isBlank()) return
        currentUserId = userId

        requestsListener?.remove()
        requestsListener = repository.listenPassengerRequests(
            userId = userId,
            rideDate = rideDate,
            onData = { requests ->
                _uiState.value = _uiState.value.copy(savedRequests = requests)
                recomputePassengerMatches()
            },
            onError = { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to load your saved requests."
                )
            }
        )

        activeRidesListener?.remove()
        activeRidesListener = repository.listenActiveRidesForDate(
            rideDate = rideDate,
            onData = { rides ->
                _uiState.value = _uiState.value.copy(activeRidesForDate = rides)
                recomputePassengerMatches()
            },
            onError = { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to load available rides."
                )
            }
        )
    }

    private fun recomputePassengerMatches() {
        val requests = _uiState.value.savedRequests.filter { it.status == "pending" }
        val rides = _uiState.value.activeRidesForDate

        if (requests.isEmpty() || rides.isEmpty()) {
            _uiState.value = _uiState.value.copy(matchedRidesForPassenger = emptyList())
            return
        }

        val matched = mutableListOf<TomorrowMatchedRide>()

        requests.forEach { request ->
            rides.filter { ride ->
                ride.availableSeats > 0 &&
                        ride.routeKey == request.routeKey &&
                        isTimeClose(ride.timeMinutes, request.timeMinutes)
            }.forEach { ride ->
                matched.add(
                    TomorrowMatchedRide(
                        rideId = ride.rideId,
                        requestId = request.requestId,
                        riderName = ride.riderName.ifBlank { "Rider" },
                        tripDirection = ride.tripDirection,
                        pickup = ride.pickup,
                        destination = ride.destination,
                        tripTime = ride.tripTime,
                        availableSeats = ride.availableSeats
                    )
                )
            }
        }

        _uiState.value = _uiState.value.copy(
            matchedRidesForPassenger = matched.distinctBy { "${it.rideId}_${it.requestId}" }
        )
    }

    /**
     * Saves (creates or updates) a passenger's requested legs for tomorrow.
     * Only the directions the passenger actually wants are touched - unlike
     * the rider side, both legs are not mandatory here.
     */
    fun savePassengerPlan(
        userId: String,
        passengerName: String,
        rideDate: String,
        wantCampus: Boolean,
        campusPickup: String,
        campusTripTime: String,
        campusHour: Int,
        campusMinute: Int,
        campusTimeMinutes: Int,
        wantHome: Boolean,
        homeDestination: String,
        homeTripTime: String,
        homeHour: Int,
        homeMinute: Int,
        homeTimeMinutes: Int,
        onXpAwarded: () -> Unit = {}
    ) {
        if (!wantCampus && !wantHome) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Please select at least one trip direction."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val results = mutableListOf<Result<TomorrowLegResult>>()

            if (wantCampus) {
                results.add(
                    repository.upsertPassengerRequest(
                        userId = userId,
                        passengerName = passengerName,
                        rideDate = rideDate,
                        tripDirection = "to_campus",
                        pickup = campusPickup,
                        destination = "AUST Gate",
                        tripTime = campusTripTime,
                        hour = campusHour,
                        minute = campusMinute,
                        timeMinutes = campusTimeMinutes
                    )
                )
            }

            if (wantHome) {
                results.add(
                    repository.upsertPassengerRequest(
                        userId = userId,
                        passengerName = passengerName,
                        rideDate = rideDate,
                        tripDirection = "to_home",
                        pickup = "AUST Gate",
                        destination = homeDestination,
                        tripTime = homeTripTime,
                        hour = homeHour,
                        minute = homeMinute,
                        timeMinutes = homeTimeMinutes
                    )
                )
            }

            handlePlanSaveOutcome(
                results,
                xpAmount = 5L,
                onXpAwarded = onXpAwarded
            )
        }
    }

    fun cancelAcceptedRequestAsPassenger(
        request: RideRequest,
        userId: String,
        reason: String
    ) {
        setProcessing(request.requestId, true)

        viewModelScope.launch {
            val result = repository.requestPassengerCancellation(
                requestId = request.requestId,
                userId = userId,
                reason = reason
            )

            setProcessing(request.requestId, false)

            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    successMessage = "Ride cancelled and seat freed up."
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to cancel request."
                )
            }
        }
    }

    fun removePassengerRequest(requestId: String) {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            val result = repository.deleteRequest(requestId, userId)

            result.onSuccess {
                _uiState.value = _uiState.value.copy(successMessage = "Request removed.")
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to remove request."
                )
            }
        }
    }

    // =====================================================================
    // Shared helpers
    // =====================================================================

    /**
     * Turns a list of per-leg upsert results into one combined message.
     * Any Blocked leg surfaces as an error (even if other legs saved fine)
     * since the user needs to know a leg was left untouched. XP is only
     * awarded once, and only if at least one leg was genuinely newly
     * created - never for edits to an existing open leg.
     */
    private fun handlePlanSaveOutcome(
        results: List<Result<TomorrowLegResult>>,
        xpAmount: Long,
        onXpAwarded: () -> Unit
    ) {
        val failures = results.mapNotNull { it.exceptionOrNull() }
        if (failures.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = failures.first().message ?: "Failed to save plan."
            )
            return
        }

        val legResults = results.mapNotNull { it.getOrNull() }
        val blockedReasons = legResults.filterIsInstance<TomorrowLegResult.Blocked>()
            .map { it.reason }
        val anyNewlyCreated = legResults.filterIsInstance<TomorrowLegResult.Saved>()
            .any { it.isNew }

        if (blockedReasons.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = blockedReasons.joinToString(" ")
            )
        } else {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                successMessage = if (anyNewlyCreated) {
                    "Tomorrow plan saved (+$xpAmount XP)"
                } else {
                    "Tomorrow plan updated."
                }
            )
        }

        if (anyNewlyCreated) {
            userRepository.addXpToCurrentUser(xpAmount) { xpResult ->
                xpResult.onSuccess { onXpAwarded() }
            }
        }
    }

    private fun setProcessing(requestId: String, isProcessing: Boolean) {
        val current = _uiState.value.processingRequestIds
        _uiState.value = _uiState.value.copy(
            processingRequestIds = if (isProcessing) {
                current + requestId
            } else {
                current - requestId
            }
        )
    }

    override fun onCleared() {
        super.onCleared()
        ridesListener?.remove()
        pendingRequestsListener?.remove()
        acceptedRequestsListener?.remove()
        requestsListener?.remove()
        activeRidesListener?.remove()
    }
}