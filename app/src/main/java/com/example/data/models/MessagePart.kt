package com.example.data.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MessagePart(
    val type: String, // "text", "tool-web_research", "tool-write_code", "tool-generate_image", "tool-generate_video", "tool-plan_build"
    val text: String? = null,
    val state: String? = null, // "thinking", "done", "error", "stubbed"
    val query: String? = null,
    val answer: String? = null,
    val language: String? = null,
    val code: String? = null,
    val filename: String? = null,
    val task: String? = null,
    val prompt: String? = null,
    val url: String? = null,
    val message: String? = null,
    val plan: String? = null,
    val kind: String? = null
)
