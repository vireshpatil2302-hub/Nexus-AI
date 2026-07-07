package com.example.data.local

import com.example.data.models.MessageEntity
import com.example.data.models.ThreadEntity
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val db: AppDatabase) {
    val allThreads: Flow<List<ThreadEntity>> = db.threadDao().getAllThreads()

    fun getMessagesForThread(threadId: String): Flow<List<MessageEntity>> {
        return db.messageDao().getMessagesForThread(threadId)
    }

    suspend fun insertThread(thread: ThreadEntity) {
        db.threadDao().insertThread(thread)
    }

    suspend fun updateThread(thread: ThreadEntity) {
        db.threadDao().updateThread(thread)
    }

    suspend fun deleteThread(threadId: String) {
        db.threadDao().deleteThreadById(threadId)
        db.messageDao().deleteMessagesForThread(threadId)
    }

    suspend fun insertMessage(message: MessageEntity) {
        db.messageDao().insertMessage(message)
    }

    suspend fun searchMessages(query: String): List<MessageEntity> {
        return db.messageDao().searchMessagesByQuery(query)
    }
}
