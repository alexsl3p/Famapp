package com.kinly.famapp.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val id: String,
    @SerialName("family_id") val familyId: String,
    val name: String,
    val brand: String? = null,
    val barcode: String? = null,
    @SerialName("barcode_normalized") val barcodeNormalized: String? = null,
    @SerialName("package_size") val packageSize: String? = null,
    @SerialName("default_unit") val defaultUnit: String? = null,
    @SerialName("product_type") val productType: String = "other",
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("product_url") val productUrl: String? = null,
    val notes: String? = null,
    val source: String? = null,
    @SerialName("created_by") val createdBy: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    val displayName: String get() = if (brand != null) "$name ($brand)" else name
}
