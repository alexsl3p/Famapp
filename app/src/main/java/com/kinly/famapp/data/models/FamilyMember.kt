package com.kinly.famapp.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FamilyMember(
    val id: String,
    @SerialName("family_id") val familyId: String,
    @SerialName("user_id") val userId: String,
    val nickname: String? = null,
    val color: String? = null,
    @SerialName("joined_at") val joinedAt: String? = null
) {
    val displayName: String get() = nickname ?: "Member"
    val initial: String get() = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "M"
}
