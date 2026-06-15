package com.kinly.famapp.features.auth

import com.kinly.famapp.data.models.Profile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(private val supabase: SupabaseClient) {

    val sessionFlow = supabase.auth.sessionStatus

    suspend fun signInWithGoogle(idToken: String, rawNonce: String): Result<Unit> = runCatching {
        supabase.auth.signInWith(IDToken) {
            provider = Google
            this.idToken = idToken
            if (rawNonce.isNotEmpty()) nonce = rawNonce
        }
    }

    suspend fun signOut() {
        runCatching { supabase.auth.signOut() }
    }

    fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    suspend fun getProfile(): Profile? = runCatching {
        val userId = currentUserId() ?: return null
        supabase.postgrest["profiles"]
            .select { filter { eq("id", userId) } }
            .decodeSingleOrNull<Profile>()
    }.getOrNull()

    suspend fun updateActiveFamilyId(familyId: String) {
        val userId = currentUserId() ?: return
        runCatching {
            supabase.postgrest["profiles"].update(
                { set("active_family_id", familyId) }
            ) {
                filter { eq("id", userId) }
            }
        }
    }

    suspend fun updateName(name: String) {
        val userId = currentUserId() ?: return
        supabase.postgrest["profiles"].update(
            { set("full_name", name) }
        ) {
            filter { eq("id", userId) }
        }
    }

    suspend fun updateAvatarUrl(url: String) {
        val userId = currentUserId() ?: return
        supabase.postgrest["profiles"].update(
            { set("avatar_url", url) }
        ) {
            filter { eq("id", userId) }
        }
    }
}
