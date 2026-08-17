package com.example.data.repository

import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GeminiRequest
import com.example.data.api.Part
import com.example.data.api.GenerationConfig
import com.example.data.api.RetrofitClient
import com.example.data.database.MatchAnalysisDao
import com.example.data.database.MatchAnalysisEntity
import com.example.data.model.MatchAnalysis
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class MatchRepository(private val dao: MatchAnalysisDao) {

    val allAnalyses: Flow<List<MatchAnalysisEntity>> = dao.getAllAnalyses()

    suspend fun saveAnalysis(sportType: String, teamHome: String, teamAway: String, rawInput: String, analysisJson: String): Long {
        val entity = MatchAnalysisEntity(
            sportType = sportType,
            teamHome = teamHome,
            teamAway = teamAway,
            rawInput = rawInput,
            analysisJson = analysisJson
        )
        return dao.insertAnalysis(entity)
    }

    suspend fun deleteAnalysis(id: Int) {
        dao.deleteAnalysisById(id)
    }

    suspend fun clearHistory() {
        dao.clearAllAnalyses()
    }

    suspend fun analyzeData(sportType: String, rawText: String): MatchAnalysis = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty()) {
            throw IllegalStateException("Gemini API key is not configured. Please add it to the Secrets Panel in AI Studio.")
        }

        val systemPrompt = """
            You are Progenius Ultra, an elite, expert sports statistician and professional betting analyst.
            Your task is to analyze raw sport statistics text (Football or Volleyball) and return a highly detailed, accurate match prediction and analysis report.
            The user will provide raw text copied from stats websites (containing standings, previous matches, head-to-head info, scores, live odds comparison, etc.).
            
            You MUST parse and analyze this text and return a single, valid JSON object that exactly matches the structure defined below. Do NOT write any markdown, explanatory text, or wrap the JSON in ```json blocks. Just output raw JSON.
            
            Return the JSON in this strict structure:
            {
              "teamHome": "Home Team Name",
              "teamAway": "Away Team Name",
              "homeTier": "Short description of home team's current level/tier/form, e.g. 'Son 7 (#16)' or 'Orta Sıralar'",
              "awayTier": "Short description of away team's current level/tier/form, e.g. 'İlk 5 (#2)' or 'Şampiyonluk Adayı'",
              "winProbabilityHome": Int, // (0 to 100 percentage)
              "winProbabilityAway": Int, // (0 to 100 percentage)
              "positiveHighlights": [
                {
                  "title": "Catchy title (in Turkish, e.g. 'Deplasman Canavarı', 'Sağlam Savunma', 'Ev Sahibi Kalesi')",
                  "subtitle": "Team name related to this highlight",
                  "description": "Short explanation in Turkish describing the stats back-up (e.g., 'Viking deplasmanda oynadığı son 8 maçın 5 tanesini kazandı (%63).')",
                  "isPositive": true
                }
              ], // generate exactly 3-4 positive highlights
              "negativeHighlights": [
                {
                  "title": "Catchy risk/negative title (in Turkish, e.g. 'Savunma Zaafı', 'Deplasman Fobisi', 'Deplasman Karnesi Zayıf')",
                  "subtitle": "Team name",
                  "description": "Short explanation in Turkish describing the risk/stats (e.g., 'Start Kristiansand evinde maç başı 1.67 gol yiyor (6 maçta 10 gol) - savunma ciddi risk taşıyor.')",
                  "isPositive": false
                }
              ], // generate exactly 3-4 negative highlights
              "homeLeaguePerformance": [
                "Detailed Turkish sentence 1 analyzing home team's league standing/record.",
                "Detailed Turkish sentence 2 analyzing home team's goals or defense.",
                "Detailed Turkish sentence 3 analyzing home team's recent home matches."
              ],
              "awayLeaguePerformance": [
                "Detailed Turkish sentence 1 analyzing away team's league standing/record.",
                "Detailed Turkish sentence 2 analyzing away team's goals or defense.",
                "Detailed Turkish sentence 3 analyzing away team's recent away matches."
              ],
              "recentMatchesHome": [
                {
                  "opponent": "vs Opponent Name (e.g. 'vs Rosenborg')",
                  "score": "Score (e.g. '0 - 3')",
                  "halfTimeScore": "Half-time score (e.g. 'İY 0-1')",
                  "result": "GAL' (for win), 'MAĞ' (for loss), or 'BRB' (for draw)"
                }
              ], // generate exactly 4-5 recent matches based on raw text or realistically inferred from context
              "recentMatchesAway": [
                {
                  "opponent": "vs Opponent Name (e.g. 'vs Tromso')",
                  "score": "Score (e.g. '1 - 1')",
                  "halfTimeScore": "Half-time score (e.g. 'İY 0-1')",
                  "result": "GAL', 'MAĞ', or 'BRB'"
                }
              ], // generate exactly 4-5 recent matches
              "h2hMatches": [
                {
                  "date": "Date of match (e.g., '16-06')",
                  "homeTeam": "Home Team Name",
                  "awayTeam": "Away Team Name",
                  "score": "Score (e.g. '1-3')",
                  "halfTimeScore": "Half-time score (e.g. '0-2')",
                  "winner": "'H' (Home won), 'A' (Away won), or 'D' (Draw)"
                }
              ], // generate 3-5 H2H matches
              "standingsHomePos": Int, // League position of home team (e.g. 16)
              "standingsHomePoints": Int, // Points of home team
              "standingsAwayPos": Int, // League position of away team (e.g. 2)
              "standingsAwayPoints": Int, // Points of away team
              "homeHomeStats": {
                "matchCount": Int, // count of home matches
                "points": Int, // points in home matches
                "averagePoints": Double, // average points (e.g. 1.0)
                "averageGoalsScored": Double, // average goals scored (e.g. 1.0)
                "averageGoalsConceded": Double // average goals conceded (e.g. 2.0)
              },
              "awayAwayStats": {
                "matchCount": Int, // count of away matches
                "points": Int, // points in away matches
                "averagePoints": Double,
                "averageGoalsScored": Double,
                "averageGoalsConceded": Double
              },
              "predictions": [
                {
                  "title": "Maç Sonucu (MS)",
                  "subtitle": "Home won, Draw, or Away won, e.g. 'MS 2' or 'MS 1' or 'Beraberlik'",
                  "probability": Int, // 0 to 100 percentage
                  "riskLevel": "Either 'GÜVENLİ', 'RİSKLİ', or 'SINIR'"
                },
                {
                  "title": "Yarı Kazanan (İlk Yarı)",
                  "subtitle": "e.g. 'Viking' or 'Start Kristiansand' or 'Beraberlik'",
                  "probability": Int,
                  "riskLevel": "Either 'GÜVENLİ', 'RİSKLİ', or 'SINIR'"
                },
                {
                  "title": "Çifte Şans 1X",
                  "subtitle": "e.g., 'Start Kristiansand veya Beraberlik'",
                  "probability": Int,
                  "riskLevel": "Either 'GÜVENLİ', 'RİSKLİ', or 'SINIR'"
                },
                {
                  "title": "Çifte Şans X2",
                  "subtitle": "e.g., 'Beraberlik veya Viking'",
                  "probability": Int,
                  "riskLevel": "Either 'GÜVENLİ', 'RİSKLİ', or 'SINIR'"
                },
                {
                  "title": "0,5 Alt/Üst",
                  "subtitle": "e.g. 'ÜST 0,5'",
                  "probability": Int,
                  "riskLevel": "Either 'GÜVENLİ', 'RİSKLİ', or 'SINIR'"
                },
                {
                  "title": "IY 1,5 Alt/Üst",
                  "subtitle": "e.g. 'ALT 1,5'",
                  "probability": Int,
                  "riskLevel": "Either 'GÜVENLİ', 'RİSKLİ', or 'SINIR'"
                },
                {
                  "title": "2,5 Alt/Üst",
                  "subtitle": "e.g. 'ÜST 2,5'",
                  "probability": Int,
                  "riskLevel": "Either 'GÜVENLİ', 'RİSKLİ', or 'SINIR'"
                },
                {
                  "title": "Karşılıklı Gol",
                  "subtitle": "e.g. 'VAR' or 'YOK'",
                  "probability": Int,
                  "riskLevel": "Either 'GÜVENLİ', 'RİSKLİ', or 'SINIR'"
                },
                {
                  "title": "İlk Golü Atacak",
                  "subtitle": "e.g. 'Viking ilk golü'",
                  "probability": Int,
                  "riskLevel": "Either 'GÜVENLİ', 'RİSKLİ', or 'SINIR'"
                },
                {
                  "title": "En Olası Skor",
                  "subtitle": "e.g. '1 - 2'",
                  "probability": Int,
                  "riskLevel": "Either 'GÜVENLİ', 'RİSKLİ', or 'SINIR'"
                },
                {
                  "title": "2. İhtimal Skor",
                  "subtitle": "e.g. '1 - 1'",
                  "probability": Int,
                  "riskLevel": "Either 'GÜVENLİ', 'RİSKLİ', or 'SINIR'"
                }
              ], // generate EXACTLY these prediction types
              "cornerHomeForecast": Double, // expected home corners (e.g. 3.4)
              "cornerAwayForecast": Double, // expected away corners (e.g. 5.4)
              "cornerTotalForecast": Double, // expected total corners (e.g. 8.8)
              "cornerTotalBet": "ALT 9.5" or "UST 9.5", // betting tip for corners (e.g. 'ALT 9.5')
              "expertReview": "A professional 2-3 paragraph sports summary in Turkish covering team momentum, defensive/offensive analysis, expected match tempo, key statistical indicators, and a final logical conclusion and recommendation for betting options."
            }
            
            IMPORTANT:
            - If sport type is 'voleybol', customize values (e.g. Set predictions instead of Goal predictions, corner forecasts can be empty/0, predictions should make sense for Volleyball set scores like 3-0, 3-1, 3-2, etc.).
            - All texts, expert reviews, and descriptions MUST be in Turkish.
            - Ensure all JSON is completely valid, clean, with correct types. Do not include trailing commas or non-standard characters.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = "Sport Type: $sportType\n\nRaw Stats Data:\n$rawText")))
            ),
            generationConfig = GenerationConfig(responseMimeType = "application/json"),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )

        val response = RetrofitClient.service.generateContent(apiKey, request)
        val textResponse = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw IllegalStateException("Gemini returned an empty response")

        // Clean the response if it contains markdown formatting
        val cleanedJson = textResponse.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val adapter = RetrofitClient.moshiInstance.adapter(MatchAnalysis::class.java)
        adapter.fromJson(cleanedJson) ?: throw IllegalStateException("Failed to parse Gemini response JSON")
    }
}
