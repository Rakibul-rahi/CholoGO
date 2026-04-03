package com.example.chologo.repository

import com.example.chologo.model.User
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
        onResult: (Result<String>) -> Unit
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
                    homeLocation = homeLocation
                )

                db.collection("users")
                    .document(uid)
                    .set(user)
                    .addOnSuccessListener {
                        onResult(Result.success(roleString))
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
        onResult: (Result<String>) -> Unit
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

                        val role = snapshot.getString("role")

                        if (role.isNullOrBlank()) {
                            onResult(Result.failure(Exception("Role not found")))
                        } else {
                            onResult(Result.success(role))
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
}