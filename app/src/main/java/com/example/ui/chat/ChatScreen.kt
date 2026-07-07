package com.example.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.models.MessageEntity
import com.example.data.models.MessagePart
import com.example.data.models.ThreadEntity
import com.example.ui.theme.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.launch
import java.io.File

val CosmicPrimary: Color @Composable get() = LocalCosmicColors.current.primary
val CosmicSecondary: Color @Composable get() = LocalCosmicColors.current.secondary
val CosmicTertiary: Color @Composable get() = LocalCosmicColors.current.tertiary
val CosmicMuted: Color @Composable get() = LocalCosmicColors.current.muted
val CosmicBorder: Color @Composable get() = LocalCosmicColors.current.border
val CosmicSurface: Color @Composable get() = LocalCosmicColors.current.surface
val CosmicBackground: Color @Composable get() = LocalCosmicColors.current.background

fun getActiveAgentTask(messages: List<MessageEntity>, isSending: Boolean): String? {
    if (!isSending) return null
    val lastMsg = messages.lastOrNull() ?: return "Orchestrating tools..."
    if (lastMsg.role != "assistant") return "Analyzing request..."
    
    val json = lastMsg.partsJson
    return when {
        json.contains("\"state\":\"thinking\"") && json.contains("\"type\":\"tool-generate_image\"") -> "Designer Agent generating image..."
        json.contains("\"state\":\"thinking\"") && json.contains("\"type\":\"tool-generate_video\"") -> "Video Generator Agent generating video..."
        json.contains("\"state\":\"thinking\"") && json.contains("\"type\":\"tool-web_research\"") -> "Researcher Agent conducting deep search..."
        json.contains("\"state\":\"thinking\"") && json.contains("\"type\":\"tool-write_code\"") -> "Coder Agent synthesizing clean code..."
        json.contains("\"state\":\"thinking\"") && json.contains("\"type\":\"tool-plan_build\"") -> "Spec Planner Agent drafting spec..."
        else -> "Nexus Orchestrator thinking..."
    }
}

@Composable
fun RealTimeAgentProgress(
    stages: List<String>,
    color: Color,
    modifier: Modifier = Modifier
) {
    var currentStageIdx by remember { mutableStateOf(0) }
    var progressVal by remember { mutableStateOf(0f) }

    LaunchedEffect(stages) {
        val delayPerStage = 1000L
        while (currentStageIdx < stages.size - 1) {
            kotlinx.coroutines.delay(delayPerStage)
            currentStageIdx++
        }
    }

    LaunchedEffect(stages) {
        val steps = 100
        val totalDuration = (stages.size - 1) * 1000f
        val delayMs = (totalDuration / steps).toLong()
        for (i in 1..steps) {
            kotlinx.coroutines.delay(delayMs)
            progressVal = i / 100f
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = color,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stages[currentStageIdx],
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "${(progressVal * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progressVal },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            stages.forEachIndexed { idx, stage ->
                val isCompleted = idx < currentStageIdx
                val isCurrent = idx == currentStageIdx
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isCompleted) Color(0xFF4CAF50) else if (isCurrent) color else Color.Transparent)
                            .border(1.dp, if (isCompleted) Color(0xFF4CAF50) else if (isCurrent) color else CosmicMuted.copy(alpha = 0.3f), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stage,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isCompleted) CosmicMuted.copy(alpha = 0.4f) else if (isCurrent) MaterialTheme.colorScheme.onSurface else CosmicMuted.copy(alpha = 0.3f),
                        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier,
    onLogout: (() -> Unit)? = null,
    darkTheme: Boolean = false,
    onToggleDarkTheme: () -> Unit = {}
) {
    val activeThreadId by viewModel.activeThreadId.collectAsState()
    val threads by viewModel.allThreads.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val error by viewModel.error.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val activeTaskText = if (isSending) getActiveAgentTask(messages, isSending) else null

    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var renameTargetThread by remember { mutableStateOf<ThreadEntity?>(null) }
    var renameTitleInput by remember { mutableStateOf("") }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to end your current secure session on this Nexus terminal?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout?.invoke()
                    }
                ) {
                    Text("Confirm Log Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showRenameDialog && renameTargetThread != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Chat", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                OutlinedTextField(
                    value = renameTitleInput,
                    onValueChange = { renameTitleInput = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renameThread(renameTargetThread!!.id, renameTitleInput)
                        showRenameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save", color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        if (isWideScreen) {
            // Tablet layout: Side-by-side split screen (Sidebar list on left, Chat main on right)
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    SidebarContent(
                        threads = threads,
                        activeThreadId = activeThreadId,
                        isSending = isSending,
                        activeTaskText = activeTaskText,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        searchResults = searchResults,
                        onThreadSelected = { viewModel.selectThread(it) },
                        onNewThread = { viewModel.createNewThread() },
                        onDeleteThread = { viewModel.deleteThread(it) },
                        onRenameRequest = { thread ->
                            renameTargetThread = thread
                            renameTitleInput = thread.title
                            showRenameDialog = true
                        },
                        onToggleAgent = { agentKey, enabled ->
                            activeThreadId?.let { viewModel.toggleAgent(it, agentKey, enabled) }
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    ChatContent(
                        messages = messages,
                        isSending = isSending,
                        error = error,
                        activeThreadTitle = threads.find { it.id == activeThreadId }?.title ?: "Nexus Workstation",
                        onSendMessage = { viewModel.sendMessage(it) },
                        onMenuClick = { /* No-op: Sidebar is already visible */ },
                        showMenuButton = false,
                        onLogoutClick = { showLogoutDialog = true },
                        darkTheme = darkTheme,
                        onToggleDarkTheme = onToggleDarkTheme
                    )
                }
            }
        } else {
            // Mobile layout: Sliding Drawer with hamburger menu
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.width(300.dp)
                    ) {
                        SidebarContent(
                            threads = threads,
                            activeThreadId = activeThreadId,
                            isSending = isSending,
                            activeTaskText = activeTaskText,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { viewModel.setSearchQuery(it) },
                            searchResults = searchResults,
                            onThreadSelected = {
                                viewModel.selectThread(it)
                                scope.launch { drawerState.close() }
                            },
                            onNewThread = {
                                viewModel.createNewThread()
                                scope.launch { drawerState.close() }
                            },
                            onDeleteThread = { viewModel.deleteThread(it) },
                            onRenameRequest = { thread ->
                                renameTargetThread = thread
                                renameTitleInput = thread.title
                                showRenameDialog = true
                            },
                            onToggleAgent = { agentKey, enabled ->
                                activeThreadId?.let { viewModel.toggleAgent(it, agentKey, enabled) }
                            }
                        )
                    }
                }
            ) {
                ChatContent(
                    messages = messages,
                    isSending = isSending,
                    error = error,
                    activeThreadTitle = threads.find { it.id == activeThreadId }?.title ?: "Nexus Workstation",
                    onSendMessage = { viewModel.sendMessage(it) },
                    onMenuClick = { scope.launch { drawerState.open() } },
                    showMenuButton = true,
                    onLogoutClick = { showLogoutDialog = true },
                    darkTheme = darkTheme,
                    onToggleDarkTheme = onToggleDarkTheme
                )
            }
        }
    }
}

@Composable
fun SidebarContent(
    threads: List<ThreadEntity>,
    activeThreadId: String?,
    isSending: Boolean,
    activeTaskText: String?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchResults: List<SearchMatch>,
    onThreadSelected: (String) -> Unit,
    onNewThread: () -> Unit,
    onDeleteThread: (String) -> Unit,
    onRenameRequest: (ThreadEntity) -> Unit,
    onToggleAgent: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Brand Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0F0C1B))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.nexus_logo),
                    contentDescription = "Nexus Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Nexus",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // New Chat Button
        Button(
            onClick = onNewThread,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Icon", tint = MaterialTheme.colorScheme.onPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("New Chat", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
        }

        // Search Engine Input Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search chats & messages...", style = MaterialTheme.typography.bodyMedium, color = CosmicMuted) },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search Icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear Search",
                            tint = CosmicMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("search_engine_input"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.05f),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        )

        Text(
            text = if (searchQuery.isEmpty()) "Conversations" else "Search Results",
            style = MaterialTheme.typography.bodySmall,
            color = CosmicMuted,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Threads List or Search Results List
        if (searchQuery.isNotEmpty() && searchResults.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "No results",
                        tint = CosmicMuted.copy(alpha = 0.4f),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No matches found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CosmicMuted
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (searchQuery.isEmpty()) {
                    items(threads) { thread ->
                        val isActive = thread.id == activeThreadId
                        val isWorkingOnThisThread = isActive && isSending
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable { onThreadSelected(thread.id) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isWorkingOnThisThread) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.ChatBubbleOutline,
                                    contentDescription = "Chat icon",
                                    tint = if (isActive) MaterialTheme.colorScheme.primary else CosmicMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = thread.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                if (isWorkingOnThisThread && activeTaskText != null) {
                                    Text(
                                        text = activeTaskText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }

                            if (isActive) {
                                IconButton(
                                    onClick = { onRenameRequest(thread) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit name",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { onDeleteThread(thread.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(searchResults) { match ->
                        val thread = match.thread
                        val isActive = thread.id == activeThreadId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable { onThreadSelected(thread.id) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.ChatBubbleOutline,
                                contentDescription = "Chat icon",
                                tint = if (isActive) MaterialTheme.colorScheme.primary else CosmicMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = thread.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                if (match.matchedSnippet != null) {
                                    Text(
                                        text = match.matchedSnippet,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        maxLines = 2,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }

                            if (isActive) {
                                IconButton(
                                    onClick = { onRenameRequest(thread) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit name",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { onDeleteThread(thread.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(16.dp))

        AgentOrchestrationPanel(
            activeThread = threads.find { it.id == activeThreadId },
            onToggleAgent = onToggleAgent
        )
    }
}

@Composable
fun AgentOrchestrationPanel(
    activeThread: ThreadEntity?,
    onToggleAgent: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }
    
    if (activeThread == null) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(12.dp),
            modifier = modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Tune,
                    contentDescription = "Tune Icon",
                    tint = CosmicMuted.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Select a conversation to configure agent workflow.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CosmicMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        return
    }

    val activeCount = listOf(
        activeThread.researcherEnabled,
        activeThread.coderEnabled,
        activeThread.imageGenEnabled,
        activeThread.videoGenEnabled,
        activeThread.plannerEnabled
    ).count { it }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Expandable Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp)
            ) {
                Icon(
                    Icons.Default.Tune,
                    contentDescription = "Tune Icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Agent Orchestrator",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Custom Workflow: $activeCount/5 active",
                        style = MaterialTheme.typography.labelSmall,
                        color = CosmicMuted
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Toggle Expand",
                    tint = CosmicMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val agents = listOf(
                        Triple("researcher", "Researcher", "Deep Web Search & Fact Retrieval"),
                        Triple("coder", "Coder", "Production Code Generation"),
                        Triple("image", "Image Generator", "Diffusion Visual & Artwork"),
                        Triple("video", "Video Generator", "Motion Frame Sequence"),
                        Triple("planner", "Spec Planner", "Structured Specs & Blueprint")
                    )

                    agents.forEach { (key, name, desc) ->
                        val isEnabled = when (key) {
                            "researcher" -> activeThread.researcherEnabled
                            "coder" -> activeThread.coderEnabled
                            "image" -> activeThread.imageGenEnabled
                            "video" -> activeThread.videoGenEnabled
                            "planner" -> activeThread.plannerEnabled
                            else -> true
                        }

                        val icon = when (key) {
                            "researcher" -> Icons.Default.Search
                            "coder" -> Icons.Default.Code
                            "image" -> Icons.Default.Image
                            "video" -> Icons.Default.Videocam
                            "planner" -> Icons.Default.Assignment
                            else -> Icons.Default.Build
                        }

                        val color = when (key) {
                            "researcher" -> CosmicSecondary
                            "coder" -> CosmicPrimary
                            "image" -> CosmicTertiary
                            "video" -> CosmicSecondary
                            "planner" -> CosmicPrimary
                            else -> MaterialTheme.colorScheme.primary
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isEnabled) color.copy(alpha = 0.05f) else Color.Transparent)
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (isEnabled) color.copy(alpha = 0.15f) else CosmicMuted.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = name,
                                    tint = if (isEnabled) color else CosmicMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isEnabled) MaterialTheme.colorScheme.onSurface else CosmicMuted
                                )
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CosmicMuted.copy(alpha = 0.8f),
                                    fontSize = 10.sp
                                )
                            }
                            Switch(
                                checked = isEnabled,
                                onCheckedChange = { onToggleAgent(key, it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = color,
                                    uncheckedThumbColor = CosmicMuted,
                                    uncheckedTrackColor = CosmicMuted.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier
                                    .scale(0.75f)
                                    .testTag("${key}_agent_toggle")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatContent(
    messages: List<MessageEntity>,
    isSending: Boolean,
    error: String?,
    activeThreadTitle: String,
    onSendMessage: (String) -> Unit,
    onMenuClick: () -> Unit,
    showMenuButton: Boolean,
    onLogoutClick: () -> Unit,
    darkTheme: Boolean = false,
    onToggleDarkTheme: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var textInput by remember { mutableStateOf("") }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size, isSending) {
        if (messages.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                ) {
                    if (showMenuButton) {
                        IconButton(onClick = onMenuClick) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Menu icon",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Text(
                        text = activeThreadTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = onToggleDarkTheme,
                        modifier = Modifier.testTag("chat_theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (darkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onLogoutClick,
                        modifier = Modifier.testTag("chat_logout_button")
                    ) {
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = "Logout Secure Vault",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), thickness = 1.dp)
                if (isSending) {
                    val activeTask = getActiveAgentTask(messages, isSending)
                    if (activeTask != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = activeTask,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), thickness = 1.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    if (error != null) {
                        Text(
                            text = "Error: $error",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = { Text("Ask Nexus anything...", color = CosmicMuted) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(max = 120.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        IconButton(
                        onClick = {
                            if (textInput.isNotBlank() && !isSending) {
                                onSendMessage(textInput)
                                textInput = ""
                            }
                        },
                        enabled = textInput.isNotBlank() && !isSending,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (textInput.isNotBlank() && !isSending) MaterialTheme.colorScheme.primary else CosmicBorder
                            )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Message",
                            tint = if (textInput.isNotBlank() && !isSending) MaterialTheme.colorScheme.onPrimary else CosmicMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Text(
                    text = "Nexus can call research, code, image, video, and spec planning agents.",
                    style = MaterialTheme.typography.labelSmall,
                    color = CosmicMuted,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }
        }
    },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        if (messages.isEmpty() && !isSending) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 500.dp)
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                        .verticalScroll(androidx.compose.foundation.rememberScrollState())
                ) {
                    // Bento Grid Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp, start = 8.dp, end = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "AGENT ALPHA",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Nexus AI",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = (-0.5).sp
                            )
                        }
                        // Account Avatar Pill
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .border(1.5.dp, Color.White, CircleShape)
                                .clickable { onLogoutClick() }
                                .testTag("bento_logout_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = "Profile",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    // Bento Grid Boxes
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Card 1: Active Build (Coder suggestion) - spans full width
                        Card(
                            onClick = { onSendMessage("Write a Kotlin script to calculate Fibonacci") },
                            shape = RoundedCornerShape(28.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(145.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.4f))
                                            .padding(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Code,
                                            contentDescription = "Coder Icon",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = "ACTIVE BUILD",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                                        letterSpacing = 1.sp
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Coder Agent Workstation",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Write a Kotlin script to calculate Fibonacci sequence...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        // Card 2 & Column of Card 3/4
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Left Bento Box: Media Engine (Videocam suggestion)
                            Card(
                                onClick = { onSendMessage("Generate a neon cyberpunk laboratory at dusk") },
                                shape = RoundedCornerShape(28.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier
                                    .weight(1.1f)
                                    .height(185.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(28.dp))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Icon(
                                        Icons.Default.Videocam,
                                        contentDescription = "Video Engine",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Media Engine",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Generate neon cyberpunk lab at dusk...",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }

                            // Right Stack: Deep Research & Workflows
                            Column(
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(185.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                              ) {
                                // Right Bento 1: Deep Research (Quantum Suggestion)
                                Card(
                                    onClick = { onSendMessage("Research quantum computing breakthroughs in 2026") },
                                    shape = RoundedCornerShape(28.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8DEF8)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Search,
                                            contentDescription = "Research Icon",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Deep Research",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1D1B20)
                                        )
                                    }
                                }

                                // Right Bento 2: Workflows
                                Card(
                                    onClick = { onSendMessage("List all automated workflows and multi-agent chains") },
                                    shape = RoundedCornerShape(28.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFD8E4)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.FlashOn,
                                            contentDescription = "Workflows Icon",
                                            tint = Color(0xFF31111D),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Workflows",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF31111D)
                                        )
                                    }
                                }
                            }
                        }

                        // Card 3: Omni-Prompt Card (Product spec suggestion) - spans full width
                        Card(
                            onClick = { onSendMessage("Draft a product spec for a local coffee app") },
                            shape = RoundedCornerShape(28.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(28.dp))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(bottom = 10.dp)
                                ) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = "Magic Button",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "OMNI-PROMPT",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 1.sp
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "\"Draft a product spec for a local coffee app...\"",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowUpward,
                                            contentDescription = "Submit",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val moshi = remember { Moshi.Builder().add(KotlinJsonAdapterFactory()).build() }
            val adapter = remember {
                moshi.adapter<List<MessagePart>>(
                    Types.newParameterizedType(List::class.java, MessagePart::class.java)
                )
            }

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(messages) { message ->
                    val parts = try {
                        adapter.fromJson(message.partsJson) ?: emptyList()
                    } catch (e: Exception) {
                        emptyList()
                    }
                    MessageRow(message = message, parts = parts)
                }

                if (isSending && (messages.isEmpty() || messages.last().role == "user")) {
                    item {
                        ThinkingPlaceholder()
                    }
                }
            }
        }
    }
}

@Composable
fun MessageRow(message: MessageEntity, parts: List<MessagePart>) {
    val isUser = message.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = "AI",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .widthIn(max = 640.dp)
        ) {
            Text(
                text = if (isUser) "You" else "Nexus AI Workstation",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = CosmicMuted,
                modifier = Modifier.padding(bottom = 4.dp, start = if (isUser) 0.dp else 4.dp)
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    )
                    .border(
                        1.dp,
                        if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    parts.forEach { part ->
                        when (part.type) {
                            "text" -> {
                                MarkdownText(
                                    text = part.text ?: "",
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            "tool-web_research" -> {
                                ToolResearchCard(part = part)
                            }
                            "tool-write_code" -> {
                                ToolCodeCard(part = part)
                            }
                            "tool-generate_image" -> {
                                ToolImageCard(part = part)
                            }
                            "tool-generate_video" -> {
                                ToolVideoCard(part = part)
                            }
                            "tool-plan_build" -> {
                                ToolPlanCard(part = part)
                            }
                        }
                    }
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "User",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun ThinkingPlaceholder() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = "AI",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = "Nexus AI Workstation",
                style = MaterialTheme.typography.labelSmall,
                color = CosmicMuted,
                modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Orchestrating agents...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CosmicMuted
                    )
                }
            }
        }
    }
}

// --- Agent Specialty Custom Visual Components ---

@Composable
fun ToolResearchCard(part: MessagePart) {
    var expanded by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
            .clickable { expanded = !expanded }
            .animateContentSize()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search icon",
                    tint = CosmicSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Researcher Agent",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Query: \"${part.query}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = CosmicMuted
                    )
                }
                if (part.state == "thinking") {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = CosmicSecondary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand icon",
                        tint = CosmicMuted,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(arrowRotation)
                    )
                }
            }

            AnimatedVisibility(visible = expanded || part.state == "thinking") {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(8.dp))
                    if (part.state == "thinking") {
                        RealTimeAgentProgress(
                            stages = listOf(
                                "Structuring deep research vectors...",
                                "Polling web index pages...",
                                "Parsing search results and content blocks...",
                                "Synthesizing factual consensus insights...",
                                "Polishing final research brief..."
                            ),
                            color = CosmicSecondary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else {
                        MarkdownText(
                            text = part.answer ?: "No response",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ToolCodeCard(part: MessagePart) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF13111C)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
            .animateContentSize()
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(12.dp)
            ) {
                Icon(
                    Icons.Default.Code,
                    contentDescription = "Code icon",
                    tint = CosmicPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Coder Agent",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "File: ${part.filename} (${part.language})",
                        style = MaterialTheme.typography.bodySmall,
                        color = CosmicMuted
                    )
                }
                if (part.state == "thinking") {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = CosmicPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand icon",
                        tint = CosmicMuted,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(arrowRotation)
                    )
                }
            }

            AnimatedVisibility(visible = expanded || part.state == "thinking") {
                Column(modifier = Modifier.padding(12.dp)) {
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(8.dp))
                    if (part.state == "thinking") {
                        RealTimeAgentProgress(
                            stages = listOf(
                                "Analyzing software requirements...",
                                "Structuring technical components...",
                                "Synthesizing clean, idiomatic code...",
                                "Performing static AST checks...",
                                "Polishing code documentation..."
                            ),
                            color = CosmicPrimary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = "OUTPUT SNIPPET",
                                style = MaterialTheme.typography.labelSmall,
                                color = CosmicPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = {
                                    part.code?.let { clipboardManager.setText(AnnotatedString(it)) }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy code",
                                    tint = CosmicMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        SelectionContainer {
                            Text(
                                text = part.code ?: "",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFFA5D6A7)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF07050A), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ToolImageCard(part: MessagePart) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Image,
                    contentDescription = "Image icon",
                    tint = CosmicTertiary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Designer Agent",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Visual generation",
                        style = MaterialTheme.typography.bodySmall,
                        color = CosmicMuted
                    )
                }
                if (part.state == "thinking") {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = CosmicTertiary,
                        strokeWidth = 2.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Prompt: \"${part.prompt}\"",
                style = MaterialTheme.typography.bodySmall,
                color = CosmicMuted,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (part.state == "thinking") {
                RealTimeAgentProgress(
                    stages = listOf(
                        "Formulating visual aesthetic prompt vectors...",
                        "Initializing latent noise space distributions...",
                        "Executing multi-pass diffusion denoising...",
                        "Upscaling and detailing visual canvas...",
                        "Finalizing color balance and rendering pipeline..."
                    ),
                    color = CosmicTertiary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else if (part.state == "done" && part.url != null) {
                AsyncImage(
                    model = File(part.url),
                    contentDescription = part.prompt,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 400.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Text(
                    text = part.message ?: "Failed to produce visual assets.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun ToolVideoCard(part: MessagePart) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Videocam,
                    contentDescription = "Video icon",
                    tint = CosmicSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Video Generator Agent",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Dynamic clip production",
                        style = MaterialTheme.typography.bodySmall,
                        color = CosmicMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Prompt: \"${part.prompt}\"",
                style = MaterialTheme.typography.bodySmall,
                color = CosmicMuted,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (part.state == "thinking") {
                RealTimeAgentProgress(
                    stages = listOf(
                        "Decomposing cinematic prompt context...",
                        "Synthesizing keyframe spatial layouts...",
                        "Generating temporally coherent flow frames...",
                        "Interpolating and enhancing frame rate...",
                        "Rendering final high-fidelity MP4 output..."
                    ),
                    color = CosmicSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(Color(0xFF13111C), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            Icons.Default.HourglassEmpty,
                            contentDescription = "Wait",
                            tint = CosmicSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = part.message ?: "Video generation complete.",
                            style = MaterialTheme.typography.bodySmall,
                            color = CosmicMuted,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ToolPlanCard(part: MessagePart) {
    var expanded by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
            .clickable { expanded = !expanded }
            .animateContentSize()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Layers,
                    contentDescription = "Layers icon",
                    tint = CosmicTertiary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Spec Planner Agent",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Project outline (${part.kind})",
                        style = MaterialTheme.typography.bodySmall,
                        color = CosmicMuted
                    )
                }
                if (part.state == "thinking") {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = CosmicTertiary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand icon",
                        tint = CosmicMuted,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(arrowRotation)
                    )
                }
            }

            AnimatedVisibility(visible = expanded || part.state == "thinking") {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(8.dp))
                    if (part.state == "thinking") {
                        Text(
                            text = "Drafting comprehensive blueprint spec...",
                            style = MaterialTheme.typography.bodySmall,
                            color = CosmicMuted
                        )
                    } else {
                        MarkdownText(
                            text = part.plan ?: "No spec generated.",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

// --- Inline Markdown Parser ---

@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    Column(modifier = modifier) {
        val lines = text.split("\n")
        lines.forEach { line ->
            when {
                line.startsWith("### ") -> {
                    Text(
                        text = line.substring(4),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                line.startsWith("## ") -> {
                    Text(
                        text = line.substring(3),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }
                line.startsWith("# ") -> {
                    Text(
                        text = line.substring(2),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
                    )
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    Row(modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp)) {
                        Text(text = "• ", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = parseInlineMarkdown(line.substring(2)),
                            style = MaterialTheme.typography.bodyLarge,
                            color = color
                        )
                    }
                }
                else -> {
                    Text(
                        text = parseInlineMarkdown(line),
                        style = MaterialTheme.typography.bodyLarge,
                        color = color,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun parseInlineMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        while (cursor < text.length) {
            val boldStart = text.indexOf("**", cursor)
            val codeStart = text.indexOf("`", cursor)

            val nextToken = when {
                boldStart != -1 && codeStart != -1 -> if (boldStart < codeStart) "bold" else "code"
                boldStart != -1 -> "bold"
                codeStart != -1 -> "code"
                else -> null
            }

            if (nextToken == null) {
                append(text.substring(cursor))
                break
            }

            if (nextToken == "bold") {
                append(text.substring(cursor, boldStart))
                val boldEnd = text.indexOf("**", boldStart + 2)
                if (boldEnd != -1) {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = CosmicPrimary)) {
                        append(text.substring(boldStart + 2, boldEnd))
                    }
                    cursor = boldEnd + 2
                } else {
                    append("**")
                    cursor = boldStart + 2
                }
            } else {
                append(text.substring(cursor, codeStart))
                val codeEnd = text.indexOf("`", codeStart + 1)
                if (codeEnd != -1) {
                    withStyle(style = SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0xFF231F32), color = CosmicSecondary)) {
                        append(text.substring(codeStart + 1, codeEnd))
                    }
                    cursor = codeEnd + 1
                } else {
                    append("`")
                    cursor = codeStart + 1
                }
            }
        }
    }
}
