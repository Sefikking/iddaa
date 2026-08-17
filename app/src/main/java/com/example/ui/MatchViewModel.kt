package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.MatchAnalysisEntity
import com.example.data.model.MatchAnalysis
import com.example.data.repository.MatchRepository
import com.example.data.api.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface MatchUiState {
    object Idle : MatchUiState
    object Loading : MatchUiState
    data class Success(val analysis: MatchAnalysis) : MatchUiState
    data class Error(val message: String) : MatchUiState
}

class MatchViewModel(
    application: Application,
    private val repository: MatchRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<MatchUiState>(MatchUiState.Idle)
    val uiState: StateFlow<MatchUiState> = _uiState.asStateFlow()

    private val _selectedSport = MutableStateFlow("futbol") // "futbol" or "voleybol"
    val selectedSport: StateFlow<String> = _selectedSport.asStateFlow()

    private val _rawInput = MutableStateFlow("")
    val rawInput: StateFlow<String> = _rawInput.asStateFlow()

    private val _activeAnalysis = MutableStateFlow<MatchAnalysis?>(null)
    val activeAnalysis: StateFlow<MatchAnalysis?> = _activeAnalysis.asStateFlow()

    val history: StateFlow<List<MatchAnalysisEntity>> = repository.allAnalyses
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun selectSport(sport: String) {
        _selectedSport.value = sport
    }

    fun updateRawInput(input: String) {
        _rawInput.value = input
    }

    fun setActiveAnalysis(analysis: MatchAnalysis?) {
        _activeAnalysis.value = analysis
        if (analysis == null) {
            _uiState.value = MatchUiState.Idle
        } else {
            _uiState.value = MatchUiState.Success(analysis)
        }
    }

    fun loadAnalysisFromHistory(entity: MatchAnalysisEntity) {
        try {
            val adapter = RetrofitClient.moshiInstance.adapter(MatchAnalysis::class.java)
            val analysis = adapter.fromJson(entity.analysisJson)
            if (analysis != null) {
                _selectedSport.value = entity.sportType
                _rawInput.value = entity.rawInput
                setActiveAnalysis(analysis)
            } else {
                _uiState.value = MatchUiState.Error("Analiz verisi yüklenemedi.")
            }
        } catch (e: Exception) {
            _uiState.value = MatchUiState.Error("Hata: ${e.localizedMessage}")
        }
    }

    fun deleteAnalysis(id: Int) {
        viewModelScope.launch {
            repository.deleteAnalysis(id)
            if (_activeAnalysis.value != null) {
                // If the currently viewed analysis is deleted, clear active view
                setActiveAnalysis(null)
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            setActiveAnalysis(null)
        }
    }

    fun performAnalysis(rawText: String) {
        if (rawText.isBlank()) {
            _uiState.value = MatchUiState.Error("Lütfen analiz edilecek veriyi yapıştırın.")
            return
        }

        viewModelScope.launch {
            _uiState.value = MatchUiState.Loading
            try {
                val sport = _selectedSport.value
                val result = repository.analyzeData(sport, rawText)
                
                // Convert result back to JSON to store in Room
                val adapter = RetrofitClient.moshiInstance.adapter(MatchAnalysis::class.java)
                val jsonString = adapter.toJson(result)
                
                // Save to DB
                repository.saveAnalysis(
                    sportType = sport,
                    teamHome = result.teamHome,
                    teamAway = result.teamAway,
                    rawInput = rawText,
                    analysisJson = jsonString
                )

                _activeAnalysis.value = result
                _uiState.value = MatchUiState.Success(result)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = MatchUiState.Error(
                    e.localizedMessage ?: "Beklenmeyen bir hata oluştu. Lütfen API anahtarınızı ve internet bağlantınızı kontrol edin."
                )
            }
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = AppDatabase.getDatabase(application)
            val repository = MatchRepository(db.matchAnalysisDao())
            return MatchViewModel(application, repository) as T
        }
    }
}
