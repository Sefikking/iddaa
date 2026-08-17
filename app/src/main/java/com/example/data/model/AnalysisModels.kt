package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MatchAnalysis(
    val teamHome: String,
    val teamAway: String,
    val homeTier: String,
    val awayTier: String,
    val winProbabilityHome: Int,
    val winProbabilityAway: Int,
    val positiveHighlights: List<Highlight>,
    val negativeHighlights: List<Highlight>,
    val homeLeaguePerformance: List<String>,
    val awayLeaguePerformance: List<String>,
    val recentMatchesHome: List<RecentMatch>,
    val recentMatchesAway: List<RecentMatch>,
    val h2hMatches: List<H2HMatch>,
    val standingsHomePos: Int,
    val standingsHomePoints: Int,
    val standingsAwayPos: Int,
    val standingsAwayPoints: Int,
    val homeHomeStats: HomeAwayStats,
    val awayAwayStats: HomeAwayStats,
    val predictions: List<PredictionBar>,
    val cornerHomeForecast: Double,
    val cornerAwayForecast: Double,
    val cornerTotalForecast: Double,
    val cornerTotalBet: String,
    val expertReview: String
)

@JsonClass(generateAdapter = true)
data class Highlight(
    val title: String,
    val subtitle: String,
    val description: String,
    val isPositive: Boolean
)

@JsonClass(generateAdapter = true)
data class RecentMatch(
    val opponent: String,
    val score: String,
    val halfTimeScore: String,
    val result: String // GAL, MAĞ, BRB
)

@JsonClass(generateAdapter = true)
data class H2HMatch(
    val date: String,
    val homeTeam: String,
    val awayTeam: String,
    val score: String,
    val halfTimeScore: String,
    val winner: String // H, A, D
)

@JsonClass(generateAdapter = true)
data class HomeAwayStats(
    val matchCount: Int,
    val points: Int,
    val averagePoints: Double,
    val averageGoalsScored: Double,
    val averageGoalsConceded: Double
)

@JsonClass(generateAdapter = true)
data class PredictionBar(
    val title: String,
    val subtitle: String,
    val probability: Int,
    val riskLevel: String // GÜVENLİ, RİSKLİ, SINIR
)
