package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchAnalysisDao {
    @Query("SELECT * FROM match_analyses ORDER BY timestamp DESC")
    fun getAllAnalyses(): Flow<List<MatchAnalysisEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysis(analysis: MatchAnalysisEntity): Long

    @Query("DELETE FROM match_analyses WHERE id = :id")
    suspend fun deleteAnalysisById(id: Int)

    @Query("DELETE FROM match_analyses")
    suspend fun clearAllAnalyses()
}
