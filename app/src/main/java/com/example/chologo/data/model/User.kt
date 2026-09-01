package com.example.chologo.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "",

    // Rider-only. Blank for passengers, and blank on every rider account
    // created before cars existed - VehicleType.normalize() reads that as
    // "bike", which is what those riders have always been.
    val vehicleType: String = "",
    val vehicleModel: String = "",
    val vehicleNumber: String = "",
    val vehicleColor: String = "",

    val university: String = "AUST",
    val studentId: String = "",
    val homeLocation: String = "",

    /**
     * Dead data. XP used to live here as a plain counter, which meant the
     * security rules had to let the owner write it - and a rule that lets
     * you add 10 to your own XP lets you add 10 a thousand times, from any
     * Firestore client, with no app involved. It is now the sum of the
     * user's rows in xp_events (see XpRepository); nothing reads or writes
     * this field, and no rule permits writing it. Kept only so older
     * documents still deserialize.
     */
    val xp: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val ratingAverage: Double = 0.0,
    val ratingCount: Int = 0,
    val reportCount: Int = 0,
    val fcmTokens: List<String> = emptyList()
)