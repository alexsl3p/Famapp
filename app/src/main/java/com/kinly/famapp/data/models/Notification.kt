package com.kinly.famapp.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Notification(
    val id: String,
    @SerialName("family_id") val familyId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("actor_id") val actorId: String? = null,
    val type: String,
    val title: String,
    val body: String? = null,
    @SerialName("entity_type") val entityType: String? = null,
    @SerialName("entity_id") val entityId: String? = null,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
)
