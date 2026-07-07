package com.example

import com.example.data.models.MessageEntity
import com.example.data.models.MessagePart
import com.example.data.models.ThreadEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchEngineUnitTest {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val partsListAdapter = moshi.adapter<List<MessagePart>>(
        Types.newParameterizedType(List::class.java, MessagePart::class.java)
    )

    @Test
    fun testSnippetExtractionFromTextPart() {
        val query = "quantum"
        val parts = listOf(
            MessagePart(type = "text", text = "We are researching quantum computers today.")
        )
        val partsJson = partsListAdapter.toJson(parts)

        val snippet = getSnippetFromPartsJson(partsJson, query)
        assertNotNull(snippet)
        assertTrue("Snippet should contain the query", snippet!!.contains(query, ignoreCase = true))
    }

    @Test
    fun testSnippetExtractionFromCodePart() {
        val query = "fun bubbleSort"
        val parts = listOf(
            MessagePart(type = "tool-write_code", code = "fun bubbleSort(arr: IntArray) { }", task = "implement sorting")
        )
        val partsJson = partsListAdapter.toJson(parts)

        val snippet = getSnippetFromPartsJson(partsJson, query)
        assertNotNull(snippet)
        assertTrue("Snippet should contain the query in the code part", snippet!!.contains("bubbleSort"))
    }

    @Test
    fun testSnippetExtractionNoMatch() {
        val query = "nonexistent"
        val parts = listOf(
            MessagePart(type = "text", text = "This is a simple chat conversation.")
        )
        val partsJson = partsListAdapter.toJson(parts)

        val snippet = getSnippetFromPartsJson(partsJson, query)
        assertNull(snippet)
    }

    private fun getSnippetFromPartsJson(partsJson: String, query: String): String? {
        return try {
            val parts = partsListAdapter.fromJson(partsJson) ?: return null
            for (part in parts) {
                val textToSearch = listOfNotNull(part.text, part.answer, part.code, part.prompt, part.plan)
                    .joinToString(" ")

                val index = textToSearch.indexOf(query, ignoreCase = true)
                if (index >= 0) {
                    val start = maxOf(0, index - 20)
                    val end = minOf(textToSearch.length, index + query.length + 30)
                    var snippet = textToSearch.substring(start, end)
                    if (start > 0) snippet = "...$snippet"
                    if (end < textToSearch.length) snippet = "$snippet..."
                    return snippet
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}
