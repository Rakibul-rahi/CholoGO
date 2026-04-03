package com.example.chologo.ui.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.chologo.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

suspend fun signInWithGoogleFirebase(context: Context): Result<String> {
    return try {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(context.getString(R.string.default_web_client_id))
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credentialManager = CredentialManager.create(context)
        val result = credentialManager.getCredential(
            context = context,
            request = request
        )

        val credential = result.credential

        if (
            credential is CustomCredential &&
            credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(
                googleIdTokenCredential.idToken,
                null
            )

            val authResult = FirebaseAuth.getInstance()
                .signInWithCredential(firebaseCredential)
                .await()

            val uid = authResult.user?.uid
                ?: return Result.failure(Exception("Google sign-in succeeded but user is null"))

            Result.success(uid)
        } else {
            Result.failure(Exception("Unexpected credential type"))
        }
    } catch (e: GoogleIdTokenParsingException) {
        Result.failure(e)
    } catch (e: Exception) {
        Result.failure(e)
    }
}