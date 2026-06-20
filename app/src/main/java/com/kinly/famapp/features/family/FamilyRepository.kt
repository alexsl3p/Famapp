package com.kinly.famapp.features.family

import com.kinly.famapp.data.models.Family
import com.kinly.famapp.data.models.FamilyMember
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

private val rpcJson = Json { ignoreUnknownKeys = true }

@Singleton
class FamilyRepository @Inject constructor(private val supabase: SupabaseClient) {

    suspend fun createFamily(name: String): Family {
        val result = supabase.postgrest.rpc(
            "create_family_with_defaults",
            buildJsonObject { put("p_name", name) }
        )
        return rpcJson.decodeFromString<Family>(result.data)
    }

    suspend fun joinFamily(code: String) {
        supabase.postgrest.rpc(
            "join_family_by_code",
            buildJsonObject { put("p_code", code) }
        )
    }

    suspend fun getFamily(familyId: String): Family? = runCatching {
        supabase.postgrest["families"]
            .select { filter { eq("id", familyId) } }
            .decodeSingleOrNull<Family>()
    }.getOrNull()

    suspend fun getMembers(familyId: String): List<FamilyMember> = runCatching {
        val members = supabase.postgrest["family_members"]
            .select { filter { eq("family_id", familyId) } }
            .decodeList<FamilyMember>()
        val ids = members.map { it.userId }
        val profiles = if (ids.isNotEmpty()) {
            runCatching {
                supabase.postgrest["profiles"]
                    .select { filter { isIn("id", ids) } }
                    .decodeList<com.kinly.famapp.data.models.Profile>()
            }.getOrElse { emptyList() }
        } else emptyList()
        val byId = profiles.associateBy { it.id }
        members.map { m ->
            val p = byId[m.userId]
            if (p != null) m.copy(
                profiles = FamilyMember.EmbeddedProfile(
                    fullName = p.fullName,
                    avatarUrl = p.avatarUrl,
                    color = p.color
                )
            ) else m
        }
    }.getOrElse { emptyList() }

    suspend fun regenerateInviteCode(familyId: String): String? = runCatching {
        val result = supabase.postgrest.rpc(
            "regenerate_invite_code",
            buildJsonObject { put("p_family_id", familyId) }
        )
        result.data.trim('"')
    }.getOrNull()

    suspend fun leaveFamily(familyId: String) {
        runCatching {
            supabase.postgrest.rpc(
                "leave_family",
                buildJsonObject { put("p_family_id", familyId) }
            )
        }
    }

    /** Приглашение по email. Возвращает "ok" | "user_not_found" | "already_member" | "error". */
    suspend fun inviteByEmail(familyId: String, email: String): String = runCatching {
        val result = supabase.postgrest.rpc(
            "invite_to_family",
            buildJsonObject { put("p_family_id", familyId); put("p_email", email) }
        )
        result.data.trim().trim('"')
    }.getOrElse { "error" }

    suspend fun acceptInvite(familyId: String): Boolean = runCatching {
        supabase.postgrest.rpc(
            "accept_family_invite",
            buildJsonObject { put("p_family_id", familyId) }
        )
        true
    }.getOrElse { false }

    suspend fun declineInvite(familyId: String) {
        runCatching {
            supabase.postgrest.rpc(
                "decline_family_invite",
                buildJsonObject { put("p_family_id", familyId) }
            )
        }
    }
}
