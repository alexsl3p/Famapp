package com.kinly.famapp.features.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.kinly.famapp.BuildConfig

/** Результат разбора Intent, который возвращает Google Sign-In. */
sealed interface GoogleSignInResult {
    data class Success(val idToken: String) : GoogleSignInResult
    data class Failure(val statusCode: Int, val message: String) : GoogleSignInResult
}

object GoogleSignInHelper {

    fun getClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    /**
     * Разбирает результат входа. В отличие от прежней версии не «глотает» ошибку:
     * возвращает осмысленный статус, чтобы UI мог показать причину, а не молча
     * вернуться на экран входа.
     */
    fun parseResult(data: Intent?): GoogleSignInResult = try {
        val token = GoogleSignIn.getSignedInAccountFromIntent(data)
            .getResult(ApiException::class.java)
            ?.idToken
        if (token != null) {
            GoogleSignInResult.Success(token)
        } else {
            GoogleSignInResult.Failure(
                CommonStatusCodes.ERROR,
                "Google не вернул ID-токен. Проверьте, что в Google Cloud есть Android OAuth-клиент " +
                    "с package com.kinly.famapp и SHA-1 подписи приложения, а в Supabase включён Google-провайдер."
            )
        }
    } catch (e: ApiException) {
        GoogleSignInResult.Failure(e.statusCode, describeStatus(e.statusCode))
    } catch (e: Exception) {
        GoogleSignInResult.Failure(CommonStatusCodes.ERROR, e.message ?: "Неизвестная ошибка входа")
    }

    private fun describeStatus(code: Int): String = when (code) {
        CommonStatusCodes.DEVELOPER_ERROR ->
            "DEVELOPER_ERROR (10): SHA-1 подписи приложения не зарегистрирован в Google Cloud " +
                "или неверный Web client ID."
        CommonStatusCodes.NETWORK_ERROR -> "Нет сети. Проверьте подключение."
        CommonStatusCodes.SIGN_IN_REQUIRED -> "Требуется повторный вход в аккаунт Google."
        12501 -> "Вход отменён."
        12502 -> "Вход уже выполняется."
        else -> "Ошибка Google Sign-In (код $code)."
    }
}
