package com.example.chologo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chologo.data.model.MissedRideAnswer
import com.example.chologo.data.model.Ride
import com.example.chologo.data.model.RideRating
import com.example.chologo.data.model.RideReport
import com.example.chologo.data.model.RideRequest
import com.example.chologo.data.model.RideRequestStatus
import com.example.chologo.data.model.VehicleType
import com.example.chologo.data.model.XpRules
import com.example.chologo.data.model.isTimeClose
import com.example.chologo.data.model.needsMissedRideReview
import com.example.chologo.data.model.seatCapacity
import com.example.chologo.data.repository.TomorrowFeedbackRepository
import com.example.chologo.data.repository.TomorrowLegResult
import com.example.chologo.data.repository.TomorrowRideRepository
import com.example.chologo.data.repository.XpRepository
import com.example.chologo.repository.UserRepository
import com.google.firebase.Timestamp
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
    val availableSeats: Int,
    val totalSeats: Int,
    val vehicleType: String,
    val vehicleModel: String,
    val vehicleNumber: String,
    val vehicleColor: String
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
    val matchedRidesForPassenger: List<TomorrowMatchedRide> = emptyList(),

    // ---- Missed trips (either side) ----
    // Every matched leg of this user's that is still sitting in an
    // unfinished lifecycle status, across all dates. Callers narrow this
    // to the genuinely overdue ones at render time with
    // needsMissedRideReview(), since whether a leg counts as "missed"
    // depends on the clock, not on the snapshot.
    val unfinishedLegs: List<RideRequest> = emptyList()
) {
    /** Overdue legs needing the "did this ride happen?" review, right now. */
    fun missedLegs(nowMillis: Long = System.currentTimeMillis()): List<RideRequest> {
        return unfinishedLegs
            .filter { it.needsMissedRideReview(nowMillis) }
            .sortedWith(compareBy({ it.rideDate }, { it.timeMinutes }))
    }
}

class TomorrowRideViewModel(
    private val repository: TomorrowRideRepository = TomorrowRideRepository(),
    private val feedbackRepository: TomorrowFeedbackRepository = TomorrowFeedbackRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val xpRepository: XpRepository = XpRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(TomorrowRideUiState())
    val uiState: StateFlow<TomorrowRideUiState> = _uiState.asStateFlow()

    private var ridesListener: ListenerRegistration? = null
    private var pendingRequestsListener: ListenerRegistration? = null
    private var acceptedRequestsListener: ListenerRegistration? = null
    private var requestsListener: ListenerRegistration? = null
    private var activeRidesListener: ListenerRegistration? = null
    private var unfinishedLegsListener: ListenerRegistration? = null

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
        campusSeats: Int,
        homeDestination: String,
        homeTripTime: String,
        homeTimeMinutes: Int,
        homeSeats: Int,
        vehicleType: String,
        vehicleModel: String = "",
        vehicleNumber: String = "",
        vehicleColor: String = "",
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
                timeMinutes = campusTimeMinutes,
                vehicleType = vehicleType,
                vehicleModel = vehicleModel,
                vehicleNumber = vehicleNumber,
                vehicleColor = vehicleColor,
                requestedSeats = campusSeats
            )

            val homeResult = repository.upsertRiderRide(
                riderId = riderId,
                riderName = riderName,
                rideDate = rideDate,
                tripDirection = "to_home",
                pickup = "AUST Gate",
                destination = homeDestination,
                tripTime = homeTripTime,
                timeMinutes = homeTimeMinutes,
                vehicleType = vehicleType,
                vehicleModel = vehicleModel,
                vehicleNumber = vehicleNumber,
                vehicleColor = vehicleColor,
                requestedSeats = homeSeats
            )

            handlePlanSaveOutcome(
                results = listOf(campusResult, homeResult),
                userId = riderId,
                isRider = true,
                rideDate = rideDate,
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
        riderPhone: String
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
                // No XP here, on purpose. Accepting used to pay the rider
                // 10 XP the instant they tapped it, which two accounts
                // could farm without limit: accept, cancel, let the
                // passenger resubmit, accept again. Nothing pays out until
                // a trip is actually finished, and the ledger caps even
                // that at the two legs a real commuter makes in a day.
                _uiState.value = _uiState.value.copy(
                    successMessage = "Passenger accepted!"
                )

                // Fire-and-forget push to the passenger - a separate child
                // launch so a notify failure can never affect the accept
                // flow's own success/error state.
                viewModelScope.launch {
                    repository.notifyPassengerAccepted(match.requestId)
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

    fun startTripAsRider(requestId: String, riderId: String) {
        setProcessing(requestId, true)

        viewModelScope.launch {
            val result = repository.riderStartTrip(requestId, riderId)

            setProcessing(requestId, false)

            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    successMessage = "Waiting for passenger to confirm the trip started."
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to start trip."
                )
            }
        }
    }

    fun requestTripCompletionAsRider(requestId: String, riderId: String) {
        setProcessing(requestId, true)

        viewModelScope.launch {
            val result = repository.riderRequestTripCompletion(requestId, riderId)

            setProcessing(requestId, false)

            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    successMessage = "Waiting for passenger to confirm completion."
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to complete trip."
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
                        availableSeats = ride.availableSeats,
                        totalSeats = ride.seatCapacity(),
                        vehicleType = VehicleType.normalize(ride.vehicleType),
                        vehicleModel = ride.vehicleModel,
                        vehicleNumber = ride.vehicleNumber,
                        vehicleColor = ride.vehicleColor
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
        passengerPhone: String,
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
                        passengerPhone = passengerPhone,
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
                        passengerPhone = passengerPhone,
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
                results = results,
                userId = userId,
                isRider = false,
                rideDate = rideDate,
                onXpAwarded = onXpAwarded
            )

            // Fire-and-forget push to any rider whose saved ride matches -
            // covers both a brand-new request and a resubmitted/edited one;
            // the server's own dedup marker handles repeat-safety, not us.
            results.mapNotNull { it.getOrNull() }
                .filterIsInstance<TomorrowLegResult.Saved>()
                .forEach { saved ->
                    viewModelScope.launch {
                        repository.notifyMatchingRiders(saved.docId)
                    }
                }
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

    fun confirmTripStarted(requestId: String) {
        setProcessing(requestId, true)

        viewModelScope.launch {
            val result = repository.passengerConfirmTripStarted(requestId)

            setProcessing(requestId, false)

            result.onSuccess {
                _uiState.value = _uiState.value.copy(successMessage = "Trip started confirmed.")
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to confirm trip start."
                )
            }
        }
    }

    fun rejectTripStarted(requestId: String) {
        setProcessing(requestId, true)

        viewModelScope.launch {
            val result = repository.passengerRejectTripStarted(requestId)

            setProcessing(requestId, false)

            result.onSuccess {
                _uiState.value = _uiState.value.copy(successMessage = "Trip start rejected.")
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to reject trip start."
                )
            }
        }
    }

    fun confirmTripCompleted(requestId: String) {
        setProcessing(requestId, true)

        viewModelScope.launch {
            val result = repository.passengerConfirmTripCompleted(requestId)

            setProcessing(requestId, false)

            result.onSuccess {
                _uiState.value = _uiState.value.copy(successMessage = "Trip completed.")
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to confirm trip completion."
                )
            }
        }
    }

    fun submitTomorrowRating(
        request: RideRequest,
        ratedBy: String,
        ratedTo: String,
        stars: Int,
        comment: String
    ) {
        viewModelScope.launch {
            val rating = RideRating(
                requestId = request.requestId,
                rideId = request.matchedRideId,
                passengerId = request.userId,
                riderId = request.matchedRiderId,
                ratedBy = ratedBy,
                ratedTo = ratedTo,
                stars = stars,
                comment = comment,
                createdAt = Timestamp.now()
            )

            val result = feedbackRepository.submitRideRating(rating)

            result.onSuccess {
                _uiState.value = _uiState.value.copy(successMessage = "Rating submitted.")
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to submit rating."
                )
            }
        }
    }

    /** The mirror of [submitTomorrowRating]: the rider rating the passenger. */
    fun submitTomorrowPassengerRating(
        request: RideRequest,
        ratedBy: String,
        ratedTo: String,
        stars: Int,
        comment: String
    ) {
        viewModelScope.launch {
            val rating = RideRating(
                requestId = request.requestId,
                rideId = request.matchedRideId,
                passengerId = request.userId,
                riderId = request.matchedRiderId,
                ratedBy = ratedBy,
                ratedTo = ratedTo,
                stars = stars,
                comment = comment,
                createdAt = Timestamp.now()
            )

            val result = feedbackRepository.submitPassengerRating(rating)

            result.onSuccess {
                _uiState.value = _uiState.value.copy(successMessage = "Rating submitted.")
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to submit rating."
                )
            }
        }
    }

    fun submitTomorrowReport(
        request: RideRequest,
        reportedBy: String,
        reportedUserId: String,
        reason: String,
        details: String
    ) {
        viewModelScope.launch {
            val report = RideReport(
                requestId = request.requestId,
                rideId = request.matchedRideId,
                passengerId = request.userId,
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
                _uiState.value = _uiState.value.copy(successMessage = "Report submitted.")
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to submit report."
                )
            }
        }
    }

    // =====================================================================
    // Missed trips (either side)
    // =====================================================================

    /**
     * Starts watching for legs that were matched but never driven to an
     * end - the "we both just rode and forgot to touch the app" case.
     *
     * Deliberately not scoped to a rideDate, unlike every other listener
     * here. That scoping is why a missed ride is otherwise invisible: the
     * Tomorrow tab only ever asks for tomorrow's key, so yesterday's
     * unfinished 8:00 AM leg is never loaded by anyone and stays
     * "accepted" forever.
     *
     * Called by the Ride History screen, not by the Tomorrow tabs. A trip
     * that already happened is history, and asking about it in the tab for
     * planning the *next* one buried the planning form under a prompt
     * about something the user considered finished days ago.
     *
     * Also re-offers every completed trip to the XP ledger. That has to
     * happen here rather than at the moment a trip finishes, because only
     * one side is present then - the passenger is the one who confirms -
     * so an award-on-the-spot design always leaves the other party owed
     * with nowhere to record the debt. Re-offering is free to repeat: the
     * ledger's derived IDs reject anything already paid.
     */
    fun startMissedRideReview(userId: String, isRider: Boolean) {
        if (userId.isBlank()) return
        startMissedRideSession(userId, isRider)
    }

    private fun startMissedRideSession(userId: String, isRider: Boolean) {
        unfinishedLegsListener?.remove()

        val onData: (List<RideRequest>) -> Unit = { legs ->
            _uiState.value = _uiState.value.copy(unfinishedLegs = legs)
        }

        val onError: (Exception) -> Unit = { e ->
            _uiState.value = _uiState.value.copy(
                errorMessage = e.message ?: "Failed to load unfinished trips."
            )
        }

        unfinishedLegsListener = if (isRider) {
            repository.listenRiderUnfinishedLegs(userId, onData, onError)
        } else {
            repository.listenPassengerUnfinishedLegs(userId, onData, onError)
        }

        // Trip XP is claimed by re-offering every completed trip to the
        // ledger, which also covers legs reconstructed through the
        // missed-ride review: a reconciled leg reaches "completed" like
        // any other, and the ledger's derived IDs stop the two paths ever
        // both paying for it.
        viewModelScope.launch {
            xpRepository.claimTripXpFor(userId, isRider)
        }
    }

    /**
     * Records this user's answer to "did this ride actually happen?".
     *
     * The repository derives the outcome from both answers, so what comes
     * back is either the leg's unchanged status (this was the first
     * answer - the other side still has to validate) or one of the three
     * terminal outcomes. Only a leg both sides vouched for pays out.
     */
    fun answerMissedRide(
        request: RideRequest,
        userId: String,
        isRider: Boolean,
        didHappen: Boolean,
        onXpAwarded: () -> Unit = {}
    ) {
        val tripXpLabel = XpRules.amountFor(
            XpRules.REASON_TOMORROW_TRIP,
            if (isRider) XpRules.ROLE_RIDER else XpRules.ROLE_PASSENGER
        )

        setProcessing(request.requestId, true)

        viewModelScope.launch {
            val result = repository.submitMissedRideAnswer(
                requestId = request.requestId,
                userId = userId,
                isRider = isRider,
                answer = if (didHappen) MissedRideAnswer.YES else MissedRideAnswer.NO
            )

            setProcessing(request.requestId, false)

            result.onSuccess { resolvedStatus ->
                _uiState.value = _uiState.value.copy(
                    successMessage = when (resolvedStatus) {
                        RideRequestStatus.COMPLETED ->
                            "Trip confirmed by both of you (+$tripXpLabel XP)"

                        RideRequestStatus.NOT_COMPLETED ->
                            "Both of you said this trip didn't happen. Closed."

                        RideRequestStatus.UNVERIFIED ->
                            "Your answers don't match, so this trip is marked " +
                                    "not verified. It doesn't count for either of you."

                        else ->
                            "Answer saved. Waiting for the other person to confirm."
                    }
                )

                // The leg is now "completed" like any other finished
                // trip, so its XP is claimed through the ledger by the
                // same sweep that handles the normal path.
                if (resolvedStatus == RideRequestStatus.COMPLETED) {
                    xpRepository.claimTripXpFor(userId, isRider)
                    onXpAwarded()
                }
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to save your answer."
                )
            }
        }
    }

    // =====================================================================
    // Shared helpers
    // =====================================================================

    /**
     * Turns a list of per-leg upsert results into one combined message,
     * and offers the day's planning XP to the ledger.
     *
     * The award is keyed on the travel date, not on whether a leg happened
     * to be newly created, which is what makes it un-farmable: planning
     * any given day is worth its 2 XP exactly once, however many times the
     * plan is edited - or deleted and rebuilt from scratch, which the old
     * "5 XP per newly created leg" rule paid for every single time, since
     * a still-pending plan is deletable.
     */
    private fun handlePlanSaveOutcome(
        results: List<Result<TomorrowLegResult>>,
        userId: String,
        isRider: Boolean,
        rideDate: String,
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
        val savedLegs = legResults.filterIsInstance<TomorrowLegResult.Saved>()

        if (blockedReasons.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = blockedReasons.joinToString(" ")
            )
            return
        }

        // Report the save immediately. The XP write must NEVER gate this:
        // a Firestore write's Task only resolves on server acknowledgement,
        // so awaiting the ledger before clearing isLoading left the Save
        // button reading "Saving..." forever on a slow or offline
        // connection - on a plan that had, in fact, already saved. That is
        // what "I can't create a new tomorrow plan" turned out to be.
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            successMessage = "Tomorrow plan saved."
        )

        if (savedLegs.isEmpty()) return

        // XP is a background afterthought. The ledger decides whether this
        // day of planning is worth anything - it pays the first save for a
        // travel date and nothing after - and the message is only amended
        // once the points are genuinely banked.
        viewModelScope.launch {
            val awarded = xpRepository.awardXp(
                userId = userId,
                reason = XpRules.REASON_PLAN_SAVED,
                role = if (isRider) XpRules.ROLE_RIDER else XpRules.ROLE_PASSENGER,
                sourceId = savedLegs.first().docId,
                dedupeKey = XpRules.planDedupeKey(rideDate)
            )

            if (!awarded) return@launch

            _uiState.value = _uiState.value.copy(
                successMessage = "Tomorrow plan saved (+${XpRules.PLAN_SAVED_XP} XP)"
            )

            onXpAwarded()
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
        unfinishedLegsListener?.remove()
    }

}