package com.kinly.famapp.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InventoryItem(
    val id: String,
    @SerialName("family_id") val familyId: String,
    @SerialName("product_id") val productId: String,
    @SerialName("location_id") val locationId: String? = null,
    val quantity: Double = 0.0,
    @SerialName("min_quantity") val minQuantity: Double? = null,
    @SerialName("target_quantity") val targetQuantity: Double? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    val status: InventoryStatus get() = when {
        quantity <= 0 -> InventoryStatus.OUT
        minQuantity != null && quantity <= minQuantity -> InventoryStatus.LOW
        expiresAt != null && isExpiringSoon(expiresAt) -> InventoryStatus.EXPIRING
        else -> InventoryStatus.OK
    }
}

enum class InventoryStatus {
    OK, LOW, OUT, EXPIRING;

    val label: String get() = when (this) {
        OK -> "OK"
        LOW -> "Заканчивается"
        OUT -> "Закончилось"
        EXPIRING -> "Истекает"
    }
}

private fun isExpiringSoon(expiresAt: String): Boolean = runCatching {
    val expires = java.time.LocalDate.parse(expiresAt)
    val soon = java.time.LocalDate.now().plusDays(3)
    !expires.isAfter(soon)
}.getOrElse { false }
