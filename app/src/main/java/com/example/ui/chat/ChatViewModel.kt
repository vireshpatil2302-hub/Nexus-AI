package com.example.ui.chat

import android.app.Application
import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.FunctionDeclaration
import com.example.data.api.GenerateContentRequest
import com.example.data.api.ImageConfig
import com.example.data.api.GenerationConfig
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import com.example.data.api.Schema
import com.example.data.api.Tool
import com.example.data.local.AppDatabase
import com.example.data.local.ChatRepository
import com.example.data.models.MessageEntity
import com.example.data.models.MessagePart
import com.example.data.models.ThreadEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val repository = ChatRepository(db)

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val partsListAdapter = moshi.adapter<List<MessagePart>>(
        Types.newParameterizedType(List::class.java, MessagePart::class.java)
    )

    // UI state for threads and active selection
    val allThreads: StateFlow<List<ThreadEntity>> = repository.allThreads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeThreadId = MutableStateFlow<String?>(null)
    val activeThreadId: StateFlow<String?> = _activeThreadId.asStateFlow()

    private val _messages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val messages: StateFlow<List<MessageEntity>> = _messages.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchMatch>>(emptyList())
    val searchResults: StateFlow<List<SearchMatch>> = _searchResults.asStateFlow()

    init {
        // Automatically select or create first thread
        viewModelScope.launch {
            allThreads.collect { threads ->
                if (_activeThreadId.value == null) {
                    if (threads.isNotEmpty()) {
                        selectThread(threads.first().id)
                    } else {
                        createNewThread()
                    }
                }
            }
        }
    }

    fun selectThread(threadId: String) {
        _activeThreadId.value = threadId
        viewModelScope.launch {
            repository.getMessagesForThread(threadId).collect { msgs ->
                _messages.value = msgs
            }
        }
    }

    fun createNewThread(title: String = "New Chat") {
        viewModelScope.launch {
            val newId = UUID.randomUUID().toString()
            val thread = ThreadEntity(
                id = newId,
                title = title,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            repository.insertThread(thread)
            _activeThreadId.value = newId
            selectThread(newId)
        }
    }

    fun deleteThread(threadId: String) {
        viewModelScope.launch {
            repository.deleteThread(threadId)
            if (_activeThreadId.value == threadId) {
                _activeThreadId.value = null
                val threads = allThreads.value.filter { it.id != threadId }
                if (threads.isNotEmpty()) {
                    selectThread(threads.first().id)
                } else {
                    createNewThread()
                }
            }
        }
    }

    fun renameThread(threadId: String, newTitle: String) {
        viewModelScope.launch {
            val title = if (newTitle.length > 50) newTitle.take(50) + "..." else newTitle
            val existing = allThreads.value.find { it.id == threadId }
            val thread = existing?.copy(title = title, updatedAt = System.currentTimeMillis()) ?: ThreadEntity(
                id = threadId,
                title = title,
                updatedAt = System.currentTimeMillis()
            )
            repository.insertThread(thread)
        }
    }

    fun toggleAgent(threadId: String, agentKey: String, enabled: Boolean) {
        viewModelScope.launch {
            val existing = allThreads.value.find { it.id == threadId } ?: return@launch
            val updated = when (agentKey) {
                "researcher" -> existing.copy(researcherEnabled = enabled, updatedAt = System.currentTimeMillis())
                "coder" -> existing.copy(coderEnabled = enabled, updatedAt = System.currentTimeMillis())
                "image" -> existing.copy(imageGenEnabled = enabled, updatedAt = System.currentTimeMillis())
                "video" -> existing.copy(videoGenEnabled = enabled, updatedAt = System.currentTimeMillis())
                "planner" -> existing.copy(plannerEnabled = enabled, updatedAt = System.currentTimeMillis())
                else -> existing
            }
            repository.insertThread(updated)
        }
    }

    fun sendMessage(text: String) {
        val threadId = _activeThreadId.value ?: return
        if (text.isBlank() || _isSending.value) return

        _isSending.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                // 1. Insert user message
                val userParts = listOf(MessagePart(type = "text", text = text))
                val userMessage = MessageEntity(
                    id = UUID.randomUUID().toString(),
                    threadId = threadId,
                    role = "user",
                    partsJson = partsListAdapter.toJson(userParts),
                    timestamp = System.currentTimeMillis()
                )
                repository.insertMessage(userMessage)

                // Update thread timestamp
                val currentThread = allThreads.value.find { it.id == threadId }
                if (currentThread != null) {
                    // Update title if it was default
                    val newTitle = if (currentThread.title == "New Chat") text else currentThread.title
                    repository.insertThread(currentThread.copy(title = newTitle, updatedAt = System.currentTimeMillis()))
                }

                // 2. Create AI assistant message skeleton
                val assistantMessageId = UUID.randomUUID().toString()
                val assistantParts = mutableListOf<MessagePart>()
                saveAssistantMessage(assistantMessageId, threadId, assistantParts)

                // 3. Orchestration reasoning loop
                runOrchestrator(threadId, assistantMessageId, assistantParts)

            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error sending message", e)
                _error.value = e.localizedMessage ?: "Unknown error occurred"
            } finally {
                _isSending.value = false
            }
        }
    }

    private suspend fun runOrchestrator(
        threadId: String,
        assistantMessageId: String,
        assistantParts: MutableList<MessagePart>
    ) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty()) {
            assistantParts.add(
                MessagePart(
                    type = "text",
                    text = "API Key is missing! Please configure GEMINI_API_KEY in the Secrets panel in AI Studio."
                )
            )
            saveAssistantMessage(assistantMessageId, threadId, assistantParts)
            return
        }

        // Fetch history
        val historyEntities = repository.getMessagesForThread(threadId).first()
        val contents = historyEntities.map { entity ->
            val partsList = try {
                partsListAdapter.fromJson(entity.partsJson) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
            Content(
                role = if (entity.role == "user") "user" else "model",
                parts = partsList.mapNotNull { part ->
                    when (part.type) {
                        "text" -> Part(text = part.text)
                        else -> {
                            if (part.state == "done") {
                                val summary = when (part.type) {
                                    "tool-web_research" -> "[Tool Research Result for: ${part.query}]\n${part.answer}"
                                    "tool-write_code" -> "[Tool Code Generated: ${part.filename}]\n```${part.language}\n${part.code}\n```"
                                    "tool-generate_image" -> "[Tool Image Generated: ${part.prompt}]"
                                    "tool-plan_build" -> "[Tool Plan Spec]:\n${part.plan}"
                                    else -> null
                                }
                                summary?.let { Part(text = it) }
                            } else null
                        }
                    }
                }
            )
        }

        val activeThread = allThreads.value.find { it.id == threadId }
        val researcherEnabled = activeThread?.researcherEnabled ?: true
        val coderEnabled = activeThread?.coderEnabled ?: true
        val imageGenEnabled = activeThread?.imageGenEnabled ?: true
        val videoGenEnabled = activeThread?.videoGenEnabled ?: true
        val plannerEnabled = activeThread?.plannerEnabled ?: true

        val systemPrompt = Content(
            parts = listOf(
                Part(
                    text = """
                        You are Nexus, a multi-agent AI orchestrator. You coordinate a team of specialist tools to fulfill user requests end-to-end.
                        
                        Available specialists (tools):
                        ${if (researcherEnabled) "- web_research(query): deep research and analysis on a topic." else ""}
                        ${if (coderEnabled) "- write_code(language, task, filename): produce production-quality code." else ""}
                        ${if (imageGenEnabled) "- generate_image(prompt): create a visual from a prompt." else ""}
                        ${if (videoGenEnabled) "- generate_video(prompt): create a short video clip (stubbed)." else ""}
                        ${if (plannerEnabled) "- plan_build(kind, brief): draft a structured spec for an app or website." else ""}
                        
                        Rules:
                        1. When the user asks for anything creative, technical, or research-heavy, PICK the right tool(s) from the AVAILABLE list above and CALL them using Function Calling. Do not just describe what you would do.
                        2. If a specialist tool/agent is NOT in the available list, you CANNOT use it. Instead, explain politely that the specific agent is currently disabled in the orchestration panel.
                        3. You may call multiple tools in sequence to fully answer a request.
                        4. After tools return, write a short, well-formatted summary to the user in markdown — do NOT re-paste raw tool output.
                        5. If the request is a simple conversation or doesn't need tools, answer directly.
                    """.trimIndent()
                )
            )
        )

        val activeDecls = mutableListOf<FunctionDeclaration>()
        if (researcherEnabled) {
            activeDecls.add(
                FunctionDeclaration(
                    name = "web_research",
                    description = "Research a topic in depth. Use for factual questions, technology comparisons, or anything the user wants investigated.",
                    parameters = Schema(
                        type = "OBJECT",
                        properties = mapOf(
                            "query" to Schema(type = "STRING", description = "The research question or topic.")
                        ),
                        required = listOf("query")
                    )
                )
            )
        }
        if (coderEnabled) {
            activeDecls.add(
                FunctionDeclaration(
                    name = "write_code",
                    description = "Write or explain production-quality code. Returns the code and filename.",
                    parameters = Schema(
                        type = "OBJECT",
                        properties = mapOf(
                            "language" to Schema(type = "STRING", description = "Programming language, e.g. 'typescript', 'python'."),
                            "task" to Schema(type = "STRING", description = "What the code should do."),
                            "filename" to Schema(type = "STRING", description = "Suggested filename with extension.")
                        ),
                        required = listOf("language", "task")
                    )
                )
            )
        }
        if (imageGenEnabled) {
            activeDecls.add(
                FunctionDeclaration(
                    name = "generate_image",
                    description = "Generate an image from a text prompt.",
                    parameters = Schema(
                        type = "OBJECT",
                        properties = mapOf(
                            "prompt" to Schema(type = "STRING", description = "Detailed visual description.")
                        ),
                        required = listOf("prompt")
                    )
                )
            )
        }
        if (videoGenEnabled) {
            activeDecls.add(
                FunctionDeclaration(
                    name = "generate_video",
                    description = "Generate a short video clip from a prompt.",
                    parameters = Schema(
                        type = "OBJECT",
                        properties = mapOf(
                            "prompt" to Schema(type = "STRING", description = "Detailed description of the video.")
                        ),
                        required = listOf("prompt")
                    )
                )
            )
        }
        if (plannerEnabled) {
            activeDecls.add(
                FunctionDeclaration(
                    name = "plan_build",
                    description = "Draft a structured build spec for an app or website.",
                    parameters = Schema(
                        type = "OBJECT",
                        properties = mapOf(
                            "kind" to Schema(type = "STRING", description = "The kind of project: 'app' or 'website'."),
                            "brief" to Schema(type = "STRING", description = "What the user wants to build.")
                        ),
                        required = listOf("kind", "brief")
                    )
                )
            )
        }

        val toolsList = if (activeDecls.isNotEmpty()) {
            listOf(Tool(functionDeclarations = activeDecls))
        } else {
            emptyList()
        }

        val request = GenerateContentRequest(
            contents = contents,
            tools = toolsList,
            systemInstruction = systemPrompt,
            generationConfig = GenerationConfig(temperature = 0.5f)
        )

        try {
            val response = RetrofitClient.service.generateContent("gemini-3.1-pro-preview", apiKey, request)
            val candidate = response.candidates?.firstOrNull()
            val modelContent = candidate?.content
            val firstPart = modelContent?.parts?.firstOrNull()

            if (firstPart?.functionCall != null) {
                // Handle Function Call!
                val call = firstPart.functionCall
                executeTool(threadId, assistantMessageId, assistantParts, call.name, call.args ?: emptyMap(), apiKey)
            } else {
                // Standard Text response
                val text = firstPart?.text ?: "No response received."
                assistantParts.add(MessagePart(type = "text", text = text))
                saveAssistantMessage(assistantMessageId, threadId, assistantParts)
            }
        } catch (e: Exception) {
            Log.e("ChatViewModel", "API call failed", e)
            assistantParts.add(MessagePart(type = "text", text = "Error contacting Gemini: ${e.localizedMessage}"))
            saveAssistantMessage(assistantMessageId, threadId, assistantParts)
        }
    }

    private suspend fun executeTool(
        threadId: String,
        assistantMessageId: String,
        assistantParts: MutableList<MessagePart>,
        name: String,
        args: Map<String, String>,
        apiKey: String
    ) {
        when (name) {
            "web_research" -> {
                val query = args["query"] ?: "research query"
                val chipIdx = assistantParts.size
                assistantParts.add(MessagePart(type = "tool-web_research", state = "thinking", query = query))
                saveAssistantMessage(assistantMessageId, threadId, assistantParts)

                try {
                    // Call Deep Research Specialist
                    val response = RetrofitClient.service.generateContent(
                        "gemini-3.1-pro-preview", apiKey, GenerateContentRequest(
                            contents = listOf(Content(parts = listOf(Part(text = query)))),
                            systemInstruction = Content(
                                parts = listOf(
                                    Part(
                                        text = "You are a research analyst. Produce a highly informative, well-structured factual markdown answer with key insights, sections, and clear bullet points."
                                    )
                                )
                            )
                        )
                    )
                    val resultText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        ?: "No research results."
                    assistantParts[chipIdx] = MessagePart(
                        type = "tool-web_research",
                        state = "done",
                        query = query,
                        answer = resultText
                    )
                } catch (e: Exception) {
                    assistantParts[chipIdx] = MessagePart(
                        type = "tool-web_research",
                        state = "error",
                        query = query,
                        answer = "Research failed: ${e.localizedMessage}"
                    )
                }
                saveAssistantMessage(assistantMessageId, threadId, assistantParts)
            }

            "write_code" -> {
                val language = args["language"] ?: "kotlin"
                val task = args["task"] ?: "sample task"
                val filename = args["filename"] ?: "solution.kt"
                val chipIdx = assistantParts.size
                assistantParts.add(
                    MessagePart(
                        type = "tool-write_code",
                        state = "thinking",
                        language = language,
                        task = task,
                        filename = filename
                    )
                )
                saveAssistantMessage(assistantMessageId, threadId, assistantParts)

                try {
                    // Call Coder Specialist
                    val response = RetrofitClient.service.generateContent(
                        "gemini-3.1-pro-preview", apiKey, GenerateContentRequest(
                            contents = listOf(
                                Content(
                                    parts = listOf(
                                        Part(
                                            text = "Language: $language\nTask: $task\n\nRespond with ONLY the raw code block. No markdown wrapper blocks (no triple backticks), no intro, no outro."
                                        )
                                    )
                                )
                            ),
                            systemInstruction = Content(parts = listOf(Part(text = "You are an expert coder. Write clean, complete, idiomatic, fully functional code.")))
                        )
                    )
                    val resultCode = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        ?: "// No code received"
                    assistantParts[chipIdx] = MessagePart(
                        type = "tool-write_code",
                        state = "done",
                        language = language,
                        task = task,
                        filename = filename,
                        code = resultCode
                    )
                } catch (e: Exception) {
                    assistantParts[chipIdx] = MessagePart(
                        type = "tool-write_code",
                        state = "error",
                        language = language,
                        task = task,
                        filename = filename,
                        code = "// Error: ${e.localizedMessage}"
                    )
                }
                saveAssistantMessage(assistantMessageId, threadId, assistantParts)
            }

            "generate_image" -> {
                val prompt = args["prompt"] ?: "futuristic artwork"
                val chipIdx = assistantParts.size
                assistantParts.add(MessagePart(type = "tool-generate_image", state = "thinking", prompt = prompt))
                saveAssistantMessage(assistantMessageId, threadId, assistantParts)

                try {
                    // Call Imagen Specialist (gemini-2.5-flash-image)
                    val response = RetrofitClient.service.generateContent(
                        "gemini-2.5-flash-image", apiKey, GenerateContentRequest(
                            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                            generationConfig = GenerationConfig(
                                imageConfig = ImageConfig(aspectRatio = "1:1", imageSize = "1K"),
                                responseModalities = listOf("TEXT", "IMAGE")
                            )
                        )
                    )
                    val part = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull { it.inlineData != null }
                    if (part?.inlineData != null) {
                        val base64Data = part.inlineData.data
                        val path = saveBase64ImageToCache(base64Data)
                        assistantParts[chipIdx] = MessagePart(
                            type = "tool-generate_image",
                            state = "done",
                            prompt = prompt,
                            url = path
                        )
                    } else {
                        // Fallback placeholder/avatar generation using generate_image tool inside workspace
                        assistantParts[chipIdx] = MessagePart(
                            type = "tool-generate_image",
                            state = "error",
                            prompt = prompt,
                            message = "No visual output in the payload."
                        )
                    }
                } catch (e: Exception) {
                    assistantParts[chipIdx] = MessagePart(
                        type = "tool-generate_image",
                        state = "error",
                        prompt = prompt,
                        message = "Image generation failed: ${e.localizedMessage}"
                    )
                }
                saveAssistantMessage(assistantMessageId, threadId, assistantParts)
            }

            "generate_video" -> {
                val prompt = args["prompt"] ?: "video scene"
                assistantParts.add(
                    MessagePart(
                        type = "tool-generate_video",
                        state = "stubbed",
                        prompt = prompt,
                        message = "Video generation is coming soon. Prompt captured: \"$prompt\" (5s)."
                    )
                )
                saveAssistantMessage(assistantMessageId, threadId, assistantParts)
            }

            "plan_build" -> {
                val kind = args["kind"] ?: "app"
                val brief = args["brief"] ?: "simple draft"
                val chipIdx = assistantParts.size
                assistantParts.add(MessagePart(type = "tool-plan_build", state = "thinking", kind = kind, task = brief))
                saveAssistantMessage(assistantMessageId, threadId, assistantParts)

                try {
                    // Call Planner Specialist
                    val response = RetrofitClient.service.generateContent(
                        "gemini-3.1-pro-preview", apiKey, GenerateContentRequest(
                            contents = listOf(Content(parts = listOf(Part(text = "Kind: $kind\nBrief: $brief")))),
                            systemInstruction = Content(
                                parts = listOf(
                                    Part(
                                        text = "You are a senior product engineer scoping a new build. Produce a complete markdown spec with Overview, Pages/Screens, Key Components, Data Model, and Tech Notes."
                                    )
                                )
                            )
                        )
                    )
                    val resultText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        ?: "No spec drafted."
                    assistantParts[chipIdx] = MessagePart(
                        type = "tool-plan_build",
                        state = "done",
                        kind = kind,
                        plan = resultText
                    )
                } catch (e: Exception) {
                    assistantParts[chipIdx] = MessagePart(
                        type = "tool-plan_build",
                        state = "error",
                        kind = kind,
                        plan = "Planning failed: ${e.localizedMessage}"
                    )
                }
                saveAssistantMessage(assistantMessageId, threadId, assistantParts)
            }
        }

        // Generate final summary
        try {
            val summaryResponse = RetrofitClient.service.generateContent(
                "gemini-3.5-flash", apiKey, GenerateContentRequest(
                    contents = listOf(
                        Content(
                            parts = listOf(
                                Part(
                                    text = "Summarize the execution and outcomes of the following tools that were run for the user's task. Write a direct, helpful, clean reply in markdown:\n" +
                                            assistantParts.joinToString("\n") { p ->
                                                when (p.type) {
                                                    "tool-web_research" -> "Research Summary: ${p.answer}"
                                                    "tool-write_code" -> "Code was written for ${p.task} in file ${p.filename}."
                                                    "tool-generate_image" -> "Image generated for prompt: ${p.prompt}."
                                                    "tool-plan_build" -> "App blueprint: ${p.plan}"
                                                    else -> ""
                                                }
                                            }
                                )
                            )
                        )
                    ),
                    systemInstruction = Content(parts = listOf(Part(text = "You are Nexus. Write a concise, elegant, friendly summary in markdown.")))
                )
            )
            val summaryText = summaryResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (summaryText != null) {
                assistantParts.add(MessagePart(type = "text", text = summaryText))
                saveAssistantMessage(assistantMessageId, threadId, assistantParts)
            }
        } catch (e: Exception) {
            assistantParts.add(MessagePart(type = "text", text = "Finished executing the specialists successfully! Let me know what to do next."))
            saveAssistantMessage(assistantMessageId, threadId, assistantParts)
        }
    }

    private suspend fun saveAssistantMessage(
        messageId: String,
        threadId: String,
        parts: List<MessagePart>
    ) {
        val message = MessageEntity(
            id = messageId,
            threadId = threadId,
            role = "assistant",
            partsJson = partsListAdapter.toJson(parts),
            timestamp = System.currentTimeMillis()
        )
        repository.insertMessage(message)
    }

    private suspend fun saveBase64ImageToCache(base64Str: String): String? = withContext(Dispatchers.IO) {
        try {
            val bytes = Base64.decode(base64Str, Base64.DEFAULT)
            val context = getApplication<Application>().applicationContext
            val file = File(context.cacheDir, "nexus_art_${UUID.randomUUID()}.png")
            file.writeBytes(bytes)
            file.absolutePath
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Failed to save cached image", e)
            null
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            if (query.isBlank()) {
                _searchResults.value = emptyList()
                return@launch
            }

            // 1. Find all matching threads (by title)
            val threads = allThreads.value
            val matchingThreads = threads.filter { it.title.contains(query, ignoreCase = true) }

            // 2. Find matching messages
            val matchingMessages = try {
                repository.searchMessages(query)
            } catch (e: Exception) {
                emptyList()
            }

            // 3. Map to SearchMatch
            val matches = mutableListOf<SearchMatch>()

            // Add thread matches first
            matchingThreads.forEach { t ->
                matches.add(SearchMatch(thread = t))
            }

            // Add message matches (avoiding duplicates but enriching with snippet)
            matchingMessages.forEach { msg ->
                val thread = threads.find { it.id == msg.threadId }
                if (thread != null) {
                    val snippet = getSnippetFromMessage(msg, query)
                    val existingIndex = matches.indexOfFirst { it.thread.id == thread.id }
                    if (existingIndex >= 0) {
                        if (matches[existingIndex].matchedSnippet == null && snippet != null) {
                            matches[existingIndex] = matches[existingIndex].copy(
                                matchedMessage = msg,
                                matchedSnippet = snippet
                            )
                        }
                    } else {
                        matches.add(SearchMatch(
                            thread = thread,
                            matchedMessage = msg,
                            matchedSnippet = snippet
                        ))
                    }
                }
            }

            _searchResults.value = matches
        }
    }

    private fun getSnippetFromMessage(msg: MessageEntity, query: String): String? {
        return try {
            val parts = partsListAdapter.fromJson(msg.partsJson) ?: return null
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

data class SearchMatch(
    val thread: ThreadEntity,
    val matchedMessage: MessageEntity? = null,
    val matchedSnippet: String? = null
)
