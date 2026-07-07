package com.example.data.models

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

class PartTypeConverters {
    private val moshi = Moshi.Builder().build()
    private val listMyData = Types.newParameterizedType(List::class.java, MessagePart::class.java)
    private val adapter = moshi.adapter<List<MessagePart>>(listMyData)

    @TypeConverter
    fun fromString(value: String?): List<MessagePart>? {
        if (value == null) return emptyList()
        return try {
            adapter.fromJson(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromList(list: List<MessagePart>?): String? {
        if (list == null) return "[]"
        return try {
            adapter.toJson(list)
        } catch (e: Exception) {
            "[]"
        }
    }
}
