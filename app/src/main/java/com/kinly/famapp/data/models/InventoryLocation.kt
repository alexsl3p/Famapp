package com.kinly.famapp.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InventoryLocation(
    val id: String,
    @SerialName("family_id") val familyId: String,
    val name: String,
    val icon: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)
