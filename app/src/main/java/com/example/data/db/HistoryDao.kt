package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.HistoryEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entry: HistoryEntry)

    @Delete
    suspend fun deleteHistory(entry: HistoryEntry)

    @Query("DELETE FROM history WHERE timestamp >= :sinceTimestamp")
    suspend fun clearHistorySince(sinceTimestamp: Long)

    @Query("DELETE FROM history")
    suspend fun clearAllHistory()
}
