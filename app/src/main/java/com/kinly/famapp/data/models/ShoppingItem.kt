package com.kinly.famapp.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShoppingItem(
    val id: String,
    @SerialName("family_id") val familyId: String,
    @SerialName("list_id") val listId: String,
    val title: String,
    val quantity: String? = null,
    val unit: String? = null,
    @SerialName("estimated_price") val estimatedPrice: Double? = null,
    @SerialName("created_by") val createdBy: String,
    @SerialName("is_checked") val isChecked: Boolean = false,
    @SerialName("checked_by") val checkedBy: String? = null,
    @SerialName("checked_at") val checkedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)
