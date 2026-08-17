package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "match_analyses")
data class MatchAnalysisEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sportType: String,
    val teamHome: String,
    val teamAway: String,
    val rawInput: String,
    val analysisJson: String,
    val timestamp: Long = System.currentTimeMillis()
)
