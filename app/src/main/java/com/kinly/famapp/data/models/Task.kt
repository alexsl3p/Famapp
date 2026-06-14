package com.kinly.famapp.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Task(
    val id: String,
    @SerialName("family_id") val familyId: String,
    val title: String,
    val description: String? = null,
    @SerialName("assigned_to") val assignedTo: String? = null,
    @SerialName("created_by") val createdBy: String,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("due_time") val dueTime: String? = null,
    @SerialName("repeat_type") val repeatType: String = "none",
    @SerialName("is_completed") val isCompleted: Boolean = false,
    @SerialName("completed_by") val completedBy: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)
