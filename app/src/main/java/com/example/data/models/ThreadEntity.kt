package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "threads")
data class ThreadEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val researcherEnabled: Boolean = true,
    val coderEnabled: Boolean = true,
    val imageGenEnabled: Boolean = true,
    val videoGenEnabled: Boolean = true,
    val plannerEnabled: Boolean = true
)
