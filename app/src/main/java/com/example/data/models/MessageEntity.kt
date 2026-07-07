package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val threadId: String,
    val role: String, // "user", "assistant", "system"
    val partsJson: String, // JSON representation of List<MessagePart>
    val timestamp: Long = System.currentTimeMillis()
)
