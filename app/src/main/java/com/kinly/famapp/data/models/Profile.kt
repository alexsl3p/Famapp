package com.kinly.famapp.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val color: String? = null,
    val email: String? = null,
    @SerialName("active_family_id") val activeFamilyId: String? = null
) {
    val displayName: String get() = fullName ?: email?.substringBefore("@") ?: "User"
    val initial: String get() = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
}
