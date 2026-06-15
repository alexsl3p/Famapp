package com.kinly.famapp.features.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.kinly.famapp.BuildConfig

object GoogleSignInHelper {

    fun getClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun extractIdToken(data: Intent?): String? = try {
        GoogleSignIn.getSignedInAccountFromIntent(data)
            .getResult(ApiException::class.java)
            ?.idToken
    } catch (e: ApiException) {
        null
    } catch (e: Exception) {
        null
    }
}
