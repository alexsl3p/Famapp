package com.kinly.famapp.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InventoryEvent(
    val id: String,
    @SerialName("family_id") val familyId: String,
    @SerialName("product_id") val productId: String,
    @SerialName("event_type") val eventType: String,
    val quantity: Double? = null,
    val price: Double? = null,
    @SerialName("event_at") val eventAt: String? = null,
    @SerialName("created_by") val createdBy: String? = null
)
