package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val email: String,
    val displayName: String,
    val passwordHash: String?, // Null for Google accounts
    val authProvider: String, // "email" or "google"
    val avatarUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
