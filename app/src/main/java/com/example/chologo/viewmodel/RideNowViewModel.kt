package com.example.chologo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chologo.data.model.LiveRide
import com.example.chologo.data.model.RideHistory
import com.example.chologo.data.model.RideNowRequest
import com.example.chologo.data.model.RideNowStatus
import com.example.chologo.data.model.RideRating
import com.example.chologo.data.model.RideReport
import com.example.chologo.data.repository.RideNowFeedbackRepository
import com.example.chologo.data.repository.RideNowLiveRepository
import com.example.chologo.data.repository.RideNowRequestRepository
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

    val rideHistory: List<RideHistory> = emptyList(),

    val isRiderLive: Boolean = false,
    val isRequestActive: Boolean = false
)

class RideNowViewModel(
    private val liveRepository: RideNowLiveRepository = RideNowLiveRepository(),
    private val requestRepository: RideNowRequestRepository = RideNowRequestRepository(),
    private val feedbackRepository: RideNowFeedbackRepository = RideNowFeedbackRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RideNowUiState())
    val uiState: StateFlow<RideNowUiState> = _uiState.asStateFlow()

    private var matchingRequestsListener: ListenerRegistration? = null
    private var passengerRequestListener: ListenerRegistration? = null
    private var liveRideListener: ListenerRegistration? = null

    private var passengerRideHistoryListener: ListenerRegistration? = null
    private var riderRideHistoryListener: ListenerRegistration? = null

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
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

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

            val result = requestRepository.createRideNowRequest(request)

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

    /**
     * Cancels the passenger's current Ride Now request.
     *
     * IMPORTANT: once a request has been matched to a rider (status is past
     * SEARCHING and matchedRideId is set), cancelling MUST also release the
     * rider's LiveRide document (isAvailable / currentRequestId), otherwise
     * the rider gets permanently stuck showing "You are Live" while every
     * future accept attempt fails with "This rider is no longer available."
     * That's why this branches to cancelAcceptedRideNowTrip in that case
     * instead of always calling the simple cancelRideNowRequest.
     */
    fun cancelRideNowRequest() {
        val request = _uiState.value.passengerRequest
        val requestId = _uiState.value.currentRequestId ?: request?.requestId ?: return

        val isMatchedToRider = request != null &&
                request.status != RideNowStatus.SEARCHING &&
                request.matchedRideId.isNotBlank()

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val result = if (isMatchedToRider) {
                requestRepository.cancelAcceptedRideNowTrip(
                    requestId = requestId,
                    liveRideId = request!!.matchedRideId
                )
            } else {
                requestRepository.cancelRideNowRequest(requestId)
            }

            result.onSuccess {
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
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            // Self-heal before creating a new LiveRide: clear out any
            // leftover LiveRide documents for this rider that are still
            // marked "active" in Firestore, regardless of what this
            // ViewModel's local state currently thinks. This covers cases
            // where the app was killed/restarted (losing local state) while
            // Firestore still held a stale document - without this, "Go
            // Live" alone can't fix a broken document since it only ever
            // creates a new one, never touches old ones. Failure here is
            // intentionally non-fatal: if it fails (e.g. offline), we still
            // attempt to go live rather than blocking the rider entirely.
            liveRepository.forceReleaseStaleLiveRides(riderId)

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

            val result = liveRepository.goLiveAsRider(liveRide)

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

        // IMPORTANT: only block on passengerRequest.status, which comes from
        // a live Firestore listener and reflects genuine current state.
        // We deliberately do NOT block on riderLiveRide.currentRequestId
        // being non-blank, because that field can go stale (e.g. left
        // pointing at an already-finished request due to a data bug, or any
        // future edge case) with nothing to ever clear it. Trusting a raw
        // flag like that created a permanent deadlock: the rider couldn't
        // stop live because of a "locked" trip that no longer actually
        // existed, and couldn't go live again because they could never stop.
        // Trusting the actively-listened request status instead means this
        // self-heals: if there's no genuinely active tracked request, Stop
        // Live always works, and stopping resets currentRequestId to "" on
        // the LiveRide doc anyway (see RideNowLiveRepository.stopLiveRide).
        val requestStatus = currentState.passengerRequest?.status
        val hasLockedTrip =
            requestStatus == RideNowStatus.ACCEPTED ||
                    requestStatus == RideNowStatus.START_PENDING_CONFIRMATION ||
                    requestStatus == RideNowStatus.ONGOING ||
                    requestStatus == RideNowStatus.END_PENDING_CONFIRMATION

        if (hasLockedTrip) {
            _uiState.value = currentState.copy(
                errorMessage = "You cannot stop live after accepting a request. Complete the trip first."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val result = liveRepository.stopLiveRide(rideId)

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

        matchingRequestsListener = requestRepository.listenForMatchingRequests(
            routeKey = routeKey,
            onData = { requests ->
                val nowSeconds = Timestamp.now().seconds
                val isBusy = !_uiState.value.riderLiveRide?.currentRequestId.isNullOrBlank()

                val validRequests = requests.filter { request ->
                    val expiresAtSeconds = request.expiresAt?.seconds
                    request.status == RideNowStatus.SEARCHING &&
                            (expiresAtSeconds == null || expiresAtSeconds > nowSeconds)
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

        passengerRequestListener = requestRepository.listenToPassengerRequest(
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

                val active = request.status in activeRideNowStatuses()

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

        liveRideListener = liveRepository.listenToLiveRide(
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

                val activeRequestId = ride?.currentRequestId
                if (!activeRequestId.isNullOrBlank()) {
                    listenToPassengerRequest(activeRequestId)
                }
            },
            onError = { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to listen to live ride."
                )
            }
        )
    }

    fun listenPassengerRideHistory(passengerId: String) {
        stopPassengerRideHistoryListener()

        passengerRideHistoryListener = requestRepository.listenPassengerRideHistory(
            passengerId = passengerId,
            onData = { history ->
                _uiState.value = _uiState.value.copy(
                    rideHistory = history
                )
            },
            onError = { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to load passenger ride history."
                )
            }
        )
    }

    fun listenRiderRideHistory(riderId: String) {
        stopRiderRideHistoryListener()

        riderRideHistoryListener = requestRepository.listenRiderRideHistory(
            riderId = riderId,
            onData = { history ->
                _uiState.value = _uiState.value.copy(
                    rideHistory = history
                )
            },
            onError = { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to load rider ride history."
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
            _uiState.value = _uiState.value.copy(errorMessage = "No active live ride found.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val result = requestRepository.acceptRideNowRequest(
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

                // Stop listening for other requests immediately instead of
                // waiting on the LiveRide snapshot listener to catch up on
                // currentRequestId. Closes the brief window where a rider
                // could still see and try to accept a second request.
                stopMatchingRequestsListener()

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
            ?: _uiState.value.riderLiveRide?.currentRequestId

        if (requestId.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "No active request found.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val result = requestRepository.startRideNowTrip(requestId)

            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Start request sent. Waiting for passenger confirmation."
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
            _uiState.value = _uiState.value.copy(errorMessage = "Missing trip info.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val result = requestRepository.completeRideNowTrip(
                requestId = requestId,
                liveRideId = liveRideId
            )

            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Completion request sent. Waiting for passenger confirmation."
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
            requestRepository.expireRequestIfNeeded(requestId)
        }
    }

    fun listenPassengerActiveRide(passengerId: String) {
        stopPassengerRequestListener()

        passengerRequestListener = requestRepository.listenPassengerActiveRide(
            passengerId = passengerId,
            onData = { request ->
                _uiState.value = _uiState.value.copy(
                    passengerRequest = request,
                    currentRequestId = request?.requestId,
                    isRequestActive = request != null
                )
            },
            onError = { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to listen active ride."
                )
            }
        )
    }

    fun confirmRideStarted(requestId: String) {
        viewModelScope.launch {
            val result = requestRepository.passengerConfirmRideNowStarted(requestId)

            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    successMessage = "Ride started confirmed."
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to confirm ride start."
                )
            }
        }
    }

    fun rejectRideStarted(requestId: String) {
        viewModelScope.launch {
            val result = requestRepository.passengerRejectRideNowStarted(requestId)

            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    successMessage = "Ride start rejected."
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to reject ride start."
                )
            }
        }
    }

    fun confirmRideCompleted(
        requestId: String,
        liveRideId: String
    ) {
        viewModelScope.launch {
            val result = requestRepository.passengerConfirmRideNowCompleted(
                requestId = requestId,
                liveRideId = liveRideId
            )

            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isRequestActive = false,
                    currentRequestId = null,
                    successMessage = "Ride completed."
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to confirm ride completion."
                )
            }
        }
    }

    /**
     * Reports an issue on the current Ride Now trip.
     *
     * liveRideId is now required so the repository can release the matched
     * rider's LiveRide (isAvailable / currentRequestId) in the same
     * transaction that marks the request ISSUE_REPORTED. Without this, a
     * report filed mid-trip would leave the rider permanently locked out
     * of new matches, the same way an unreleased cancel used to.
     *
     * If the caller doesn't have the liveRideId handy, it falls back to
     * whatever this ViewModel already knows about the current trip.
     */
    fun reportRideIssue(
        requestId: String,
        liveRideId: String? = null
    ) {
        val resolvedLiveRideId = liveRideId
            ?: _uiState.value.passengerRequest?.matchedRideId?.takeIf { it.isNotBlank() }
            ?: _uiState.value.currentLiveRideId
            ?: _uiState.value.riderLiveRide?.rideId

        if (resolvedLiveRideId.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Missing trip info.")
            return
        }

        viewModelScope.launch {
            val result = requestRepository.passengerReportRideNowIssue(
                requestId = requestId,
                liveRideId = resolvedLiveRideId
            )

            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    successMessage = "Issue reported."
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to report issue."
                )
            }
        }
    }

    fun loadRequestById(requestId: String) {
        viewModelScope.launch {
            val request = requestRepository.getRideNowRequestById(requestId)

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

            val isActive = request?.status in activeRideNowStatuses()

            _uiState.value = _uiState.value.copy(
                passengerRequest = request,
                currentRequestId = request?.requestId,
                isRequestActive = isActive
            )

            if (request != null) {
                listenToPassengerRequest(request.requestId)
            }
        }
    }

    fun submitRideRating(
        ratedBy: String,
        ratedTo: String,
        stars: Int,
        comment: String
    ) {
        val request = _uiState.value.passengerRequest ?: return

        viewModelScope.launch {
            val rating = RideRating(
                requestId = request.requestId,
                rideId = request.matchedRideId,
                passengerId = request.passengerId,
                riderId = request.matchedRiderId,
                ratedBy = ratedBy,
                ratedTo = ratedTo,
                stars = stars,
                comment = comment,
                createdAt = Timestamp.now()
            )

            val result = feedbackRepository.submitRideRating(rating)

            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    successMessage = "Rating submitted."
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to submit rating."
                )
            }
        }
    }

    fun submitRideReport(
        reportedBy: String,
        reportedUserId: String,
        reason: String,
        details: String
    ) {
        val request = _uiState.value.passengerRequest ?: return

        viewModelScope.launch {
            val report = RideReport(
                requestId = request.requestId,
                rideId = request.matchedRideId,
                passengerId = request.passengerId,
                riderId = request.matchedRiderId,
                reportedBy = reportedBy,
                reportedUserId = reportedUserId,
                reason = reason,
                details = details,
                status = "pending",
                createdAt = Timestamp.now()
            )

            val result = feedbackRepository.submitRideReport(report)

            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    successMessage = "Report submitted."
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to submit report."
                )
            }
        }
    }

    fun loadLiveRideById(rideId: String) {
        viewModelScope.launch {
            val ride = liveRepository.getLiveRideById(rideId)

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
            requestRepository.expireRequestIfNeeded(requestId)
        }
    }

    private fun activeRideNowStatuses(): List<String> {
        return listOf(
            RideNowStatus.SEARCHING,
            RideNowStatus.NOTIFIED,
            RideNowStatus.ACCEPTED,
            RideNowStatus.START_PENDING_CONFIRMATION,
            RideNowStatus.ONGOING,
            RideNowStatus.END_PENDING_CONFIRMATION
        )
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

    private fun stopPassengerRideHistoryListener() {
        passengerRideHistoryListener?.remove()
        passengerRideHistoryListener = null
    }

    private fun stopRiderRideHistoryListener() {
        riderRideHistoryListener?.remove()
        riderRideHistoryListener = null
    }

    override fun onCleared() {
        super.onCleared()
        stopMatchingRequestsListener()
        stopPassengerRequestListener()
        stopLiveRideListener()
        stopPassengerRideHistoryListener()
        stopRiderRideHistoryListener()
    }
}