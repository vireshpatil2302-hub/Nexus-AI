package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.models.ThreadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ThreadDao {
    @Query("SELECT * FROM threads ORDER BY updatedAt DESC")
    fun getAllThreads(): Flow<List<ThreadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThread(thread: ThreadEntity)

    @Update
    suspend fun updateThread(thread: ThreadEntity)

    @Query("DELETE FROM threads WHERE id = :id")
    suspend fun deleteThreadById(id: String)

    @Query("SELECT * FROM threads WHERE id = :id")
    suspend fun getThreadById(id: String): ThreadEntity?
}
