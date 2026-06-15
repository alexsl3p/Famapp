package com.kinly.famapp.features.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.kinly.famapp.BuildConfig
import java.security.MessageDigest
import java.util.UUID

class GoogleSignInHelper(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)

    suspend fun signIn(): Pair<String, String> {
        val (rawNonce, hashedNonce) = generateNonce()

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setNonce(hashedNonce)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context = context, request = request)
            val googleIdToken = GoogleIdTokenCredential.createFrom(result.credential.data).idToken
            googleIdToken to rawNonce
        } catch (e: GetCredentialException) {
            throw RuntimeException("Google Sign-In error: ${e.type} — ${e.message}")
        } catch (e: Exception) {
            throw RuntimeException("Ошибка входа: ${e.message}")
        }
    }

    private fun generateNonce(): Pair<String, String> {
        val rawNonce = UUID.randomUUID().toString()
        val bytes = rawNonce.toByteArray()
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        val hashedNonce = digest.joinToString("") { "%02x".format(it) }
        return rawNonce to hashedNonce
    }
}
