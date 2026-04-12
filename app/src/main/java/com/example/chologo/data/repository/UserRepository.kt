package com.example.chologo.repository

import com.example.chologo.data.model.User
import com.example.chologo.ui.auth.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun signup(
        role: UserRole,
        name: String,
        email: String,
        phone: String,
        studentId: String,
        university: String,
        homeLocation: String,
        password: String,
        onResult: (Result<User>) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid

                if (uid == null) {
                    onResult(Result.failure(Exception("User ID not found")))
                    return@addOnSuccessListener
                }

                val roleString = role.name.lowercase()

                val user = User(
                    uid = uid,
                    name = name,
                    email = email,
                    phone = phone,
                    role = roleString,
                    university = university,
                    studentId = studentId,
                    homeLocation = homeLocation,
                    xp = 0L
                )

                db.collection("users")
                    .document(uid)
                    .set(user)
                    .addOnSuccessListener {
                        onResult(Result.success(user))
                    }
                    .addOnFailureListener { e ->
                        onResult(Result.failure(e))
                    }
            }
            .addOnFailureListener { e ->
                onResult(Result.failure(e))
            }
    }

    fun login(
        email: String,
        password: String,
        onResult: (Result<User>) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid

                if (uid == null) {
                    onResult(Result.failure(Exception("User ID not found")))
                    return@addOnSuccessListener
                }

                db.collection("users")
                    .document(uid)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        if (!snapshot.exists()) {
                            onResult(Result.failure(Exception("User data not found in Firestore")))
                            return@addOnSuccessListener
                        }

                        val user = snapshot.toObject(User::class.java)

                        if (user == null) {
                            onResult(Result.failure(Exception("Failed to parse user data")))
                        } else {
                            onResult(Result.success(user))
                        }
                    }
                    .addOnFailureListener { e ->
                        onResult(Result.failure(e))
                    }
            }
            .addOnFailureListener { e ->
                onResult(Result.failure(e))
            }
    }

    fun getCurrentUserData(
        onResult: (Result<User>) -> Unit
    ) {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            onResult(Result.failure(Exception("No logged in user")))
            return
        }

        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    onResult(Result.failure(Exception("User data not found")))
                    return@addOnSuccessListener
                }

                val user = snapshot.toObject(User::class.java)

                if (user != null) {
                    onResult(Result.success(user))
                } else {
                    onResult(Result.failure(Exception("Failed to parse user data")))
                }
            }
            .addOnFailureListener { e ->
                onResult(Result.failure(e))
            }
    }

    fun addXpToUser(
        userId: String,
        amount: Long,
        onResult: (Result<Unit>) -> Unit
    ) {
        val userRef = db.collection("users").document(userId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val currentXp = snapshot.getLong("xp") ?: 0L
            val newXp = currentXp + amount
            transaction.update(userRef, "xp", newXp)
        }.addOnSuccessListener {
            onResult(Result.success(Unit))
        }.addOnFailureListener { e ->
            onResult(Result.failure(e))
        }
    }

    fun addXpToCurrentUser(
        amount: Long,
        onResult: (Result<Unit>) -> Unit
    ) {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            onResult(Result.failure(Exception("No logged in user")))
            return
        }

        addXpToUser(uid, amount, onResult)
    }

    fun logout() {
        auth.signOut()
    }
}