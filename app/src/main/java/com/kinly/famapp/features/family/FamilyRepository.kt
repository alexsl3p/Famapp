package com.kinly.famapp.features.family

import com.kinly.famapp.data.models.Family
import com.kinly.famapp.data.models.FamilyMember
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FamilyRepository @Inject constructor(private val supabase: SupabaseClient) {

    suspend fun createFamily(name: String): Family {
        return supabase.postgrest.rpc(
            "create_family_with_defaults",
            buildJsonObject { put("p_name", name) }
        ).decodeSingle<Family>()
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
        supabase.postgrest["family_members"]
            .select { filter { eq("family_id", familyId) } }
            .decodeList<FamilyMember>()
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
}
