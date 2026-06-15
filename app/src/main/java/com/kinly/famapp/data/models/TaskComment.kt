package com.kinly.famapp.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TaskComment(
    val id: String,
    @SerialName("task_id") val taskId: String,
    @SerialName("family_id") val familyId: String,
    @SerialName("author_id") val authorId: String? = null,
    val body: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)
