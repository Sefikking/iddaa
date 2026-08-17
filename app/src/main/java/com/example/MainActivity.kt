package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.database.MatchAnalysisEntity
import com.example.data.model.*
import com.example.ui.MatchUiState
import com.example.ui.MatchViewModel
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ProgeniusBg)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                ) {
                    ProgeniusApp()
                }
            }
        }
    }
}

@Composable
fun ProgeniusApp() {
    val context = LocalContext.current
    val viewModel: MatchViewModel = viewModel(
        factory = MatchViewModel.Factory(context.applicationContext as android.app.Application)
    )

    val uiState by viewModel.uiState.collectAsState()
    val selectedSport by viewModel.selectedSport.collectAsState()
    val rawInput by viewModel.rawInput.collectAsState()
    val activeAnalysis by viewModel.activeAnalysis.collectAsState()
    val history by viewModel.history.collectAsState()

    var showHistoryOnly by remember { mutableStateOf(false) }
    var selectedSubTab by remember { mutableStateOf(0) }
    var currentNavTab by remember { mutableStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            ProgeniusBottomBar(currentTab = currentNavTab, onTabSelected = { currentNavTab = it })
        },
        containerColor = ProgeniusBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ProgeniusBg)
        ) {
            when (currentNavTab) {
                0 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 32.dp, top = 16.dp)
                    ) {
        // App Header
        item {
            AppHeader()
        }

        // AI Synthesis Card from Sleek Interface Theme
        item {
            AiSynthesisCard(activeAnalysis = activeAnalysis)
        }

        // Only show Scanner/Input when NOT viewing detailed analysis results
        if (activeAnalysis == null) {
            // Sport selector tabs (Football / Volleyball)
            item {
                SportSelectorTabs(
                    selectedSport = selectedSport,
                    onSportSelected = { viewModel.selectSport(it) }
                )
            }

            // Redirect button to statistics websites
            item {
                DataSourceButton(sport = selectedSport)
            }

            // Paste Panel (Scanner box)
            item {
                PasteScannerBox(
                    sport = selectedSport,
                    uiState = uiState,
                    onPasteClicked = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clipData = clipboard.primaryClip
                        if (clipData != null && clipData.itemCount > 0) {
                            val pastedText = clipData.getItemAt(0).text?.toString() ?: ""
                            if (pastedText.isNotBlank()) {
                                viewModel.updateRawInput(pastedText)
                                viewModel.performAnalysis(pastedText)
                                Toast.makeText(context, "Veri yapıştırıldı ve analiz başlatıldı!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Pano boş veya metin içermiyor.", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "Panoda kopyalanmış veri bulunamadı.", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }

            // History header and Clear History button
            if (history.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Geçmiş Analizler (${history.size})",
                            color = ProgeniusTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = { viewModel.clearHistory() },
                            colors = ButtonDefaults.textButtonColors(contentColor = ProgeniusRed)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Temizle", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tümünü Sil", fontSize = 12.sp)
                        }
                    }
                }

                // Previous Analyses List
                items(history) { entity ->
                    HistoryItemCard(
                        entity = entity,
                        onClicked = { viewModel.loadAnalysisFromHistory(entity) },
                        onDelete = { viewModel.deleteAnalysis(entity.id) }
                    )
                }
            } else {
                item {
                    EmptyHistoryState()
                }
            }
        } else {
            // Detailed analysis is active! Show results
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.setActiveAnalysis(null) },
                        colors = ButtonDefaults.buttonColors(containerColor = ProgeniusCardBg),
                        border = BorderStroke(1.dp, ProgeniusBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri", tint = ProgeniusTextPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Yeni Analiz Yap", color = ProgeniusTextPrimary)
                    }

                    Badge(
                        containerColor = if (selectedSport == "futbol") ProgeniusPrimary else ProgeniusSecondary,
                        contentColor = Color.White,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            text = if (selectedSport == "futbol") "FUTBOL" else "VOLEYBOL",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Teams comparison header
            item {
                TeamsComparisonHeader(analysis = activeAnalysis!!)
            }

            // Double buttons tabs (Detailed Analiz / Premium Özet as shown in video)
            item {
                AnalysisSubTabs(selectedTab = selectedSubTab, onTabSelected = { selectedSubTab = it })
            }

            // Prediction Panel Header
            item {
                SectionTitle(title = "ÖNE ÇIKAN TAHMİN PANELİ", icon = Icons.Default.Bolt, color = ProgeniusAccent)
            }

            // Highlights (Positive & Negative Highlights with green/red circles)
            item {
                HighlightsCard(analysis = activeAnalysis!!)
            }

            // Bu Sezon Ligde Bullet Points
            if (selectedSubTab == 0) {
                item {
                    LeaguePerformanceCard(analysis = activeAnalysis!!)
                }
            }

            // Last 5 Matches Performance
            if (selectedSubTab == 0) {
                item {
                    RecentMatchesCard(analysis = activeAnalysis!!)
                }
            }

            // Head to Head (H2H) Matches Table
            if (selectedSubTab == 0) {
                item {
                    H2HMatchesCard(analysis = activeAnalysis!!)
                }
            }

            // Standings and Goal Stats
            if (selectedSubTab == 0) {
                item {
                    StandingsStatsCard(analysis = activeAnalysis!!)
                }
            }

            // Progress Bar Predictions List (Maç Sonucu, Çifte Şans, Alt/Üst)
            item {
                PredictionProgressBarsCard(analysis = activeAnalysis!!)
            }

            // Corners Forecast Panel
            if (selectedSport == "futbol") {
                item {
                    CornersForecastCard(analysis = activeAnalysis!!)
                }
            }

            // Expert Review Paragraphs
            item {
                ExpertReviewCard(analysis = activeAnalysis!!)
            }

            // Back to top button row with "Yeni Analiz" and "Raporu Kopyala" buttons
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.setActiveAnalysis(null) },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("back_to_new_analysis"),
                        colors = ButtonDefaults.buttonColors(containerColor = ProgeniusPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Yeni Analiz")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Yeni Analiz Yap", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            val analysisVal = activeAnalysis!!
                            val predictionReport = analysisVal.predictions.joinToString("\n") {
                                "- ${it.title} (${it.subtitle}): %${it.probability} (${it.riskLevel})"
                            }
                            val report = """
                                🏆 PROGENİUS ULTRA TAHMİN RAPORU 🏆
                                ----------------------------------
                                ⚽ Maç: ${analysisVal.teamHome} vs ${analysisVal.teamAway}
                                📈 Güç Dengesi: %${analysisVal.winProbabilityHome} - %${analysisVal.winProbabilityAway}
                                
                                📌 ÖNE ÇIKAN TAHMİNLER:
                                $predictionReport
                                ${if (selectedSport == "futbol") "- Köşe Vuruşları Beklentisi: Toplam ${analysisVal.cornerTotalForecast} (${analysisVal.cornerTotalBet})" else ""}
                                
                                💡 UZMAN YAPAY ZEKA YORUMU:
                                ${analysisVal.expertReview}
                            """.trimIndent()

                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Progenius Analiz Raporu", report)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Analiz raporu başarıyla kopyalandı!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("copy_report_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ProgeniusPrimary),
                        border = BorderStroke(1.dp, ProgeniusPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Raporu Kopyala")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Raporu Kopyala", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
            1 -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp, top = 16.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Geçmiş Analizler (${history.size})",
                                color = ProgeniusTextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (history.isNotEmpty()) {
                                TextButton(
                                    onClick = { viewModel.clearHistory() },
                                    colors = ButtonDefaults.textButtonColors(contentColor = ProgeniusRed)
                                ) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = "Temizle", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Tümünü Sil", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    if (history.isNotEmpty()) {
                        items(history) { entity ->
                            HistoryItemCard(
                                entity = entity,
                                onClicked = {
                                    viewModel.loadAnalysisFromHistory(entity)
                                    currentNavTab = 0
                                },
                                onDelete = { viewModel.deleteAnalysis(entity.id) }
                            )
                        }
                    } else {
                        item {
                            EmptyHistoryState()
                        }
                    }
                }
            }
            2 -> {
                AssistantScreen()
            }
            3 -> {
                SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
}

@Composable
fun AppHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "PROGENİUS ULTRA",
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 2.sp,
            style = TextStyle(
                brush = Brush.horizontalGradient(
                    colors = listOf(ProgeniusPrimary, ProgeniusSecondary, ProgeniusAccent)
                )
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "TREND & SİNYAL PANELİ",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = ProgeniusTextPrimary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Yapay zeka ile lig durumları, son karşılaşmalar ve anlık istatistikler taranarak otomatik kesin tahmin çıkarılır.",
            fontSize = 11.sp,
            color = ProgeniusTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
            lineHeight = 16.sp
        )
        Divider(
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth(0.3f),
            color = ProgeniusBorder,
            thickness = 1.dp
        )
    }
}

@Composable
fun SportSelectorTabs(
    selectedSport: String,
    onSportSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ProgeniusSurface, RoundedCornerShape(12.dp))
            .border(1.dp, ProgeniusBorder, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (selectedSport == "futbol") ProgeniusPrimary else Color.Transparent)
                .clickable { onSportSelected("futbol") }
                .testTag("futbol_tab"),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.SportsSoccer,
                contentDescription = "Futbol",
                tint = if (selectedSport == "futbol") Color.White else ProgeniusTextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Futbol",
                color = if (selectedSport == "futbol") Color.White else ProgeniusTextSecondary,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (selectedSport == "voleybol") ProgeniusSecondary else Color.Transparent)
                .clickable { onSportSelected("voleybol") }
                .testTag("voleybol_tab"),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.SportsVolleyball,
                contentDescription = "Voleybol",
                tint = if (selectedSport == "voleybol") Color.White else ProgeniusTextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Voleybol",
                color = if (selectedSport == "voleybol") Color.White else ProgeniusTextSecondary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun DataSourceButton(sport: String) {
    val context = LocalContext.current
    val url = if (sport == "futbol") {
        "https://live10.goaloo28.com/"
    } else {
        "https://www.volleyballstats247.com"
    }

    Button(
        onClick = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("data_source_button"),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, ProgeniusBorder),
        shape = RoundedCornerShape(10.dp)
    ) {
        Icon(Icons.Default.Launch, contentDescription = "Veri Kaynağı", tint = ProgeniusTextPrimary, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("VERİ KAYNAĞINA GİT", color = ProgeniusTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PasteScannerBox(
    sport: String,
    uiState: MatchUiState,
    onPasteClicked: () -> Unit
) {
    val context = LocalContext.current
    val browserUrl = if (sport == "futbol") {
        "https://live10.goaloo28.com/"
    } else {
        "https://www.volleyballstats247.com"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(ProgeniusSurface, RoundedCornerShape(16.dp))
            .border(BorderStroke(1.dp, Brush.linearGradient(listOf(ProgeniusPrimary, ProgeniusSecondary))), RoundedCornerShape(16.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Futuristic target corner brackets inside the box
        Box(modifier = Modifier.fillMaxSize()) {
            // Top Left corner bracket
            Box(modifier = Modifier.size(20.dp).align(Alignment.TopStart).border(BorderStroke(3.dp, ProgeniusPrimary), RoundedCornerShape(topStart = 4.dp, topEnd = 0.dp, bottomStart = 0.dp, bottomEnd = 0.dp)))
            // Top Right
            Box(modifier = Modifier.size(20.dp).align(Alignment.TopEnd).border(BorderStroke(3.dp, ProgeniusPrimary), RoundedCornerShape(topStart = 0.dp, topEnd = 4.dp, bottomStart = 0.dp, bottomEnd = 0.dp)))
            // Bottom Left
            Box(modifier = Modifier.size(20.dp).align(Alignment.BottomStart).border(BorderStroke(3.dp, ProgeniusPrimary), RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 4.dp, bottomEnd = 0.dp)))
            // Bottom Right
            Box(modifier = Modifier.size(20.dp).align(Alignment.BottomEnd).border(BorderStroke(3.dp, ProgeniusPrimary), RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 0.dp, bottomEnd = 4.dp)))
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            when (uiState) {
                is MatchUiState.Loading -> {
                    CircularProgressIndicator(color = ProgeniusSecondary, modifier = Modifier.size(44.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "YAPAY ZEKA VERİLERİ ANALİZ EDİYOR...",
                        color = ProgeniusSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Lütfen bekleyin, istatistikler ve oranlar hesaplanıyor.",
                        color = ProgeniusTextSecondary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
                else -> {
                    Icon(
                        Icons.Default.ContentPaste,
                        contentDescription = "Pano",
                        tint = ProgeniusPrimary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "VERİNİZİ KOPYALAYIP BURAYA YAPIŞTIRIN",
                        color = ProgeniusTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Standings, Previous Scores, Statistics and Live Odds Comparison verisi otomatik taranacaktır.",
                        color = ProgeniusTextSecondary,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        lineHeight = 14.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Status pill indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(ProgeniusBg, RoundedCornerShape(32.dp))
                            .border(1.dp, ProgeniusBorder, RoundedCornerShape(32.dp))
                            .clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(browserUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Tarayıcı açılamadı!", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (uiState is MatchUiState.Error) ProgeniusRed else ProgeniusPrimary, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState is MatchUiState.Error) "HATA OLUŞTU" else "TARAYICI HAZIR",
                            fontSize = 11.sp,
                            color = if (uiState is MatchUiState.Error) ProgeniusRed else ProgeniusPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Big Paste Button
                    Button(
                        onClick = onPasteClicked,
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(44.dp)
                            .testTag("paste_data_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = ProgeniusPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ContentPasteGo, contentDescription = "Paste", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Veri Yapıştır & Analiz Et", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    // Display Error Message if in Error state
    if (uiState is MatchUiState.Error) {
        Card(
            colors = CardDefaults.cardColors(containerColor = ProgeniusRed.copy(alpha = 0.15f)),
            border = BorderStroke(1.dp, ProgeniusRed.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Error, contentDescription = "Hata", tint = ProgeniusRed)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = uiState.message,
                    color = ProgeniusTextPrimary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun EmptyHistoryState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Analytics,
            contentDescription = "Analiz Bulunmamaktadır",
            tint = ProgeniusBorder,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Henüz analiz geçmişi bulunmamaktadır.",
            color = ProgeniusTextSecondary,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Yukarıdaki butonu kullanarak bir maç istatistiği analiz edin.",
            color = ProgeniusTextSecondary,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun HistoryItemCard(
    entity: MatchAnalysisEntity,
    onClicked: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClicked() }
            .testTag("history_item_${entity.id}"),
        colors = CardDefaults.cardColors(containerColor = ProgeniusSurface),
        border = BorderStroke(1.dp, ProgeniusBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(if (entity.sportType == "futbol") ProgeniusPrimary.copy(alpha = 0.15f) else ProgeniusSecondary.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (entity.sportType == "futbol") Icons.Default.SportsSoccer else Icons.Default.SportsVolleyball,
                        contentDescription = "Spor",
                        tint = if (entity.sportType == "futbol") ProgeniusPrimary else ProgeniusSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "${entity.teamHome} vs ${entity.teamAway}",
                        color = ProgeniusTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (entity.sportType == "futbol") "Futbol Analizi" else "Voleybol Analizi",
                        color = ProgeniusTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Sil", tint = ProgeniusRed.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun TeamsComparisonHeader(analysis: MatchAnalysis) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ProgeniusSurface),
        border = BorderStroke(1.dp, ProgeniusBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = analysis.teamHome,
                        color = ProgeniusTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = analysis.homeTier,
                        color = ProgeniusPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "VS",
                    color = ProgeniusAccent,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = analysis.teamAway,
                        color = ProgeniusTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = analysis.awayTier,
                        color = ProgeniusSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // TEAM POWER RATING GRAPHIC
            Text(
                text = "TAKIM GÜCÜ KARŞILAŞTIRMASI",
                color = ProgeniusTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ProgeniusBg)
            ) {
                val homeWeight = analysis.winProbabilityHome.toFloat() / 100f
                val awayWeight = analysis.winProbabilityAway.toFloat() / 100f

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(homeWeight.coerceAtLeast(0.05f))
                        .background(ProgeniusPrimary)
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(awayWeight.coerceAtLeast(0.05f))
                        .background(ProgeniusSecondary)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "%${analysis.winProbabilityHome} ${analysis.teamHome}",
                    color = ProgeniusPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "%${analysis.winProbabilityAway} ${analysis.teamAway}",
                    color = ProgeniusSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun AnalysisSubTabs(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ProgeniusSurface, RoundedCornerShape(10.dp))
            .border(1.dp, ProgeniusBorder, RoundedCornerShape(10.dp))
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (selectedTab == 0) ProgeniusBorder else Color.Transparent)
                .clickable { onTabSelected(0) },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Analytics, contentDescription = "Detay", tint = ProgeniusTextPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("DETAYLI ANALİZ", color = ProgeniusTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (selectedTab == 1) ProgeniusBorder else Color.Transparent)
                .clickable { onTabSelected(1) },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = "Premium", tint = ProgeniusAccent, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("PREMIUM ÖZET", color = ProgeniusAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SectionTitle(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun HighlightsCard(analysis: MatchAnalysis) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ProgeniusSurface),
        border = BorderStroke(1.dp, ProgeniusBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Positive highlights section
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(ProgeniusGreen, CircleShape))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "OLUMLU (${analysis.positiveHighlights.size})",
                    color = ProgeniusGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            analysis.positiveHighlights.forEach { highlight ->
                HighlightRow(highlight = highlight)
                Spacer(modifier = Modifier.height(12.dp))
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp), color = ProgeniusBorder)

            // Negative/Risk highlights section
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(ProgeniusRed, CircleShape))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "OLUMSUZ / RİSK (${analysis.negativeHighlights.size})",
                    color = ProgeniusRed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            analysis.negativeHighlights.forEach { highlight ->
                HighlightRow(highlight = highlight)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun HighlightRow(highlight: Highlight) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ProgeniusCardBg, RoundedCornerShape(10.dp))
            .border(1.dp, ProgeniusBorder, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(6.dp)
                .background(if (highlight.isPositive) ProgeniusGreen else ProgeniusRed, CircleShape)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = highlight.title,
                color = ProgeniusTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = highlight.subtitle,
                color = if (highlight.isPositive) ProgeniusPrimary else ProgeniusSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = highlight.description,
                color = ProgeniusTextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun LeaguePerformanceCard(analysis: MatchAnalysis) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ProgeniusSurface),
        border = BorderStroke(1.dp, ProgeniusBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "BU SEZON LİGDE",
                color = ProgeniusTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "${analysis.teamHome} - BU SEZON LİGDE",
                color = ProgeniusPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            analysis.homeLeaguePerformance.forEach { point ->
                Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                    Text("•", color = ProgeniusPrimary, fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
                    Text(text = point, color = ProgeniusTextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp), color = ProgeniusBorder)

            Text(
                text = "${analysis.teamAway} - BU SEZON LİGDE",
                color = ProgeniusSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            analysis.awayLeaguePerformance.forEach { point ->
                Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                    Text("•", color = ProgeniusSecondary, fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
                    Text(text = point, color = ProgeniusTextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
                }
            }
        }
    }
}

@Composable
fun RecentMatchesCard(analysis: MatchAnalysis) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ProgeniusSurface),
        border = BorderStroke(1.dp, ProgeniusBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "TAKIM PERFORMANSI - SON 5 MAÇ",
                color = ProgeniusTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Home Team Last Matches
            Text(
                text = "${analysis.teamHome} (Ev Sahibi)",
                color = ProgeniusPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            analysis.recentMatchesHome.forEach { match ->
                RecentMatchRow(match = match)
                Spacer(modifier = Modifier.height(6.dp))
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp), color = ProgeniusBorder)

            // Away Team Last Matches
            Text(
                text = "${analysis.teamAway} (Deplasman)",
                color = ProgeniusSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            analysis.recentMatchesAway.forEach { match ->
                RecentMatchRow(match = match)
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
fun RecentMatchRow(match: RecentMatch) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ProgeniusCardBg, RoundedCornerShape(8.dp))
            .border(1.dp, ProgeniusBorder, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = match.opponent,
                color = ProgeniusTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 140.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = match.halfTimeScore,
                color = ProgeniusTextSecondary,
                fontSize = 10.sp
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = match.score,
                color = ProgeniusAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(12.dp))

            // Result Badge
            val (badgeBg, badgeText) = when (match.result) {
                "GAL" -> Pair(ProgeniusGreen.copy(alpha = 0.15f), ProgeniusGreen)
                "MAĞ" -> Pair(ProgeniusRed.copy(alpha = 0.15f), ProgeniusRed)
                else -> Pair(ProgeniusOrange.copy(alpha = 0.15f), ProgeniusOrange)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(badgeBg)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = match.result,
                    color = badgeText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun H2HMatchesCard(analysis: MatchAnalysis) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ProgeniusSurface),
        border = BorderStroke(1.dp, ProgeniusBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Kendi Aralarında Oynadıkları Maçlar (H2H)",
                color = ProgeniusTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("TARİH / EV-DEPLASMAN", color = ProgeniusTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
                Text("MS", color = ProgeniusTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center)
                Text("İY", color = ProgeniusTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center)
                Text("SONUÇ", color = ProgeniusTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.6f), textAlign = TextAlign.End)
            }

            analysis.h2hMatches.forEach { match ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(ProgeniusCardBg, RoundedCornerShape(6.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1.5f)) {
                        Text(text = match.date, color = ProgeniusAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${match.homeTeam} - ${match.awayTeam}",
                            color = ProgeniusTextPrimary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(text = match.score, color = ProgeniusTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center)
                    Text(text = match.halfTimeScore, color = ProgeniusTextSecondary, fontSize = 11.sp, modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center)

                    val resultCircleColor = when (match.winner) {
                        "H" -> ProgeniusPrimary
                        "A" -> ProgeniusSecondary
                        else -> ProgeniusOrange
                    }

                    val resultCircleChar = when (match.winner) {
                        "H" -> "G"
                        "A" -> "M"
                        else -> "B"
                    }

                    Box(
                        modifier = Modifier
                            .weight(0.6f)
                            .wrapContentWidth(Alignment.End)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(resultCircleColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = resultCircleChar, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(ProgeniusPrimary, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ev sahibi galip", color = ProgeniusTextSecondary, fontSize = 10.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(ProgeniusOrange, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Berabere", color = ProgeniusTextSecondary, fontSize = 10.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(ProgeniusSecondary, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ev sahibi mağlup", color = ProgeniusTextSecondary, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun StandingsStatsCard(analysis: MatchAnalysis) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ProgeniusSurface),
        border = BorderStroke(1.dp, ProgeniusBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FormatListNumbered, contentDescription = "Puan Durumu", tint = ProgeniusAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PUAN DURUMU & SAHA İÇİ PERFORMANS",
                    color = ProgeniusTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Standings list
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ProgeniusBg, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("SIRA", color = ProgeniusTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.5f))
                Text("TAKIM", color = ProgeniusTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                Text("PUAN", color = ProgeniusTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.5f), textAlign = TextAlign.End)
            }

            // Home Team Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${analysis.standingsHomePos}.", color = ProgeniusTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.5f))
                Text(analysis.teamHome, color = ProgeniusPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                Text("${analysis.standingsHomePoints}", color = ProgeniusTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.5f), textAlign = TextAlign.End)
            }

            Divider(color = ProgeniusBorder, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 8.dp))

            // Away Team Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${analysis.standingsAwayPos}.", color = ProgeniusTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.5f))
                Text(analysis.teamAway, color = ProgeniusSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                Text("${analysis.standingsAwayPoints}", color = ProgeniusTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.5f), textAlign = TextAlign.End)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Inner performance stats comparison
            Text(
                text = "${analysis.teamHome} (İç Sahada)",
                color = ProgeniusPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            HomeAwayStatsLayout(stats = analysis.homeHomeStats)

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "${analysis.teamAway} (Dış Sahada)",
                color = ProgeniusSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            HomeAwayStatsLayout(stats = analysis.awayAwayStats)
        }
    }
}

@Composable
fun HomeAwayStatsLayout(stats: HomeAwayStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ProgeniusCardBg, RoundedCornerShape(8.dp))
            .border(1.dp, ProgeniusBorder, RoundedCornerShape(8.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Maç Sayısı", color = ProgeniusTextSecondary, fontSize = 10.sp)
            Text("${stats.matchCount}", color = ProgeniusTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Puan Ort.", color = ProgeniusTextSecondary, fontSize = 10.sp)
            Text(String.format("%.2f", stats.averagePoints), color = ProgeniusAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Atılan Gol", color = ProgeniusTextSecondary, fontSize = 10.sp)
            Text(String.format("%.2f", stats.averageGoalsScored), color = ProgeniusTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Yenilen Gol", color = ProgeniusTextSecondary, fontSize = 10.sp)
            Text(String.format("%.2f", stats.averageGoalsConceded), color = ProgeniusTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PredictionProgressBarsCard(analysis: MatchAnalysis) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ProgeniusSurface),
        border = BorderStroke(1.dp, ProgeniusBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "TAHMİN VE SİNYAL PANELİ",
                color = ProgeniusTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            analysis.predictions.forEach { prediction ->
                PredictionProgressItem(prediction = prediction)
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

@Composable
fun PredictionProgressItem(prediction: PredictionBar) {
    val barColor = when (prediction.riskLevel) {
        "GÜVENLİ" -> ProgeniusGreen
        "RİSKLİ" -> ProgeniusRed
        else -> ProgeniusOrange
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = prediction.title,
                    color = ProgeniusTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = prediction.subtitle,
                    color = ProgeniusTextSecondary,
                    fontSize = 11.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "%${prediction.probability}",
                    color = barColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(32.dp))
                        .background(barColor.copy(alpha = 0.15f))
                        .border(1.dp, barColor.copy(alpha = 0.4f), RoundedCornerShape(32.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = prediction.riskLevel,
                        color = barColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Visual progress bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(ProgeniusBg)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(prediction.probability.toFloat() / 100f)
                    .background(barColor)
            )
        }
    }
}

@Composable
fun CornersForecastCard(analysis: MatchAnalysis) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ProgeniusSurface),
        border = BorderStroke(1.dp, ProgeniusBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "KORNER TAHMİN PANELİ",
                color = ProgeniusTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(ProgeniusCardBg, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Ev Sahibi", color = ProgeniusTextSecondary, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${analysis.cornerHomeForecast}", color = ProgeniusPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Korner", color = ProgeniusTextSecondary, fontSize = 10.sp)
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(ProgeniusCardBg, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Deplasman", color = ProgeniusTextSecondary, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${analysis.cornerAwayForecast}", color = ProgeniusSecondary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Korner", color = ProgeniusTextSecondary, fontSize = 10.sp)
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(ProgeniusCardBg, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Toplam", color = ProgeniusTextSecondary, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${analysis.cornerTotalForecast}", color = ProgeniusAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Korner", color = ProgeniusTextSecondary, fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ProgeniusBg, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Toplam Tahmini Korner Tercihi:",
                    color = ProgeniusTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(ProgeniusAccent.copy(alpha = 0.15f))
                        .border(1.dp, ProgeniusAccent, RoundedCornerShape(4.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = analysis.cornerTotalBet,
                        color = ProgeniusAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ExpertReviewCard(analysis: MatchAnalysis) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ProgeniusSurface),
        border = BorderStroke(1.dp, ProgeniusBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Psychology,
                    contentDescription = "Uzman Yorumu",
                    tint = ProgeniusAccent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Uzman Yapay Zeka Yorumu",
                    color = ProgeniusAccent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = analysis.expertReview,
                color = ProgeniusTextPrimary,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Justify
            )
        }
    }
}

@Composable
fun AiSynthesisCard(activeAnalysis: MatchAnalysis?) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SleekSynthesisBg),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_synthesis_card"),
        border = BorderStroke(1.dp, ProgeniusBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Sentez Sinyali",
                        tint = ProgeniusPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "YAPAY ZEKA SENTEZİ",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekSynthesisText,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Günlük Spor Özet Raporu",
                        fontSize = 11.sp,
                        color = SleekSynthesisText.copy(alpha = 0.7f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val summaryText = if (activeAnalysis == null) {
                "'Sistem genelinde yapılan son taramalarda form grafiği yükselişte olan takımlar tespit edilmiştir. İstatistik kopyalayıp alt panele ekleyerek anında trend analizini başlatabilirsiniz.'"
            } else {
                "'${activeAnalysis.teamHome} - ${activeAnalysis.teamAway} karşılaşması için son lig durumları ve form grafikleri işlendi. Yapay zekaya göre en baskın ihtimal belirlendi.'"
            }
            
            Text(
                text = summaryText,
                fontSize = 13.sp,
                color = SleekSynthesisText,
                fontWeight = FontWeight.Medium,
                fontStyle = FontStyle.Italic,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun ProgeniusBottomBar(currentTab: Int, onTabSelected: (Int) -> Unit) {
    NavigationBar(
        containerColor = ProgeniusSurface,
        tonalElevation = 0.dp,
        modifier = Modifier.border(BorderStroke(1.dp, ProgeniusBorder))
    ) {
        NavigationBarItem(
            selected = currentTab == 0,
            onClick = { onTabSelected(0) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Ana Sayfa") },
            label = { Text("Ana Sayfa", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = ProgeniusPrimary,
                indicatorColor = ProgeniusPrimary,
                unselectedIconColor = ProgeniusTextSecondary,
                unselectedTextColor = ProgeniusTextSecondary
            )
        )
        NavigationBarItem(
            selected = currentTab == 1,
            onClick = { onTabSelected(1) },
            icon = { Icon(Icons.Default.History, contentDescription = "Geçmiş") },
            label = { Text("Geçmiş", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = ProgeniusPrimary,
                indicatorColor = ProgeniusPrimary,
                unselectedIconColor = ProgeniusTextSecondary,
                unselectedTextColor = ProgeniusTextSecondary
            )
        )
        NavigationBarItem(
            selected = currentTab == 2,
            onClick = { onTabSelected(2) },
            icon = { Icon(Icons.Default.QuestionAnswer, contentDescription = "Asistan") },
            label = { Text("Asistan", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = ProgeniusPrimary,
                indicatorColor = ProgeniusPrimary,
                unselectedIconColor = ProgeniusTextSecondary,
                unselectedTextColor = ProgeniusTextSecondary
            )
        )
        NavigationBarItem(
            selected = currentTab == 3,
            onClick = { onTabSelected(3) },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Ayarlar") },
            label = { Text("Ayarlar", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = ProgeniusPrimary,
                indicatorColor = ProgeniusPrimary,
                unselectedIconColor = ProgeniusTextSecondary,
                unselectedTextColor = ProgeniusTextSecondary
            )
        )
    }
}

@Composable
fun AssistantScreen() {
    var chatInput by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<Pair<String, Boolean>>(
        Pair("Progenius Sinyal Asistanı'na hoş geldiniz! ⚽🏐\nUygulama hakkında sorularınızı sorabilir, analiz formüllerimizi öğrenebilir veya yapıştırmak istediğiniz verilerle ilgili tüyo alabilirsiniz.", false)
    ) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ProgeniusBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "YAPAY ZEKA ASİSTANI",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ProgeniusTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Canlı Sinyal & Veri Entegrasyonu Rehberi",
                fontSize = 12.sp,
                color = ProgeniusTextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Quick Actions Scrollable Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "Nasıl Veri Yapıştırırım?",
                    "Hangi Siteler Destekleniyor?",
                    "Sinyal Güvenilirliği Nedir?"
                ).forEach { action ->
                    Box(
                        modifier = Modifier
                            .background(ProgeniusSurface, RoundedCornerShape(20.dp))
                            .border(1.dp, ProgeniusBorder, RoundedCornerShape(20.dp))
                            .clickable {
                                messages.add(Pair(action, true))
                                val response = when (action) {
                                    "Nasıl Veri Yapıştırırım?" -> "Goaloo veya Volleyballstats247 web sitesinden tüm sayfa istatistiklerini (Puan durumu, H2H, Maç Sonuçları) kopyalayın. Ardından 'Ana Sayfa' sekmesindeki büyük mavi 'Veri Yapıştır & Analiz Et' butonuna dokunun. Sistemimiz tüm verileri milisaniyeler içinde tarayıp analiz edecektir."
                                    "Hangi Siteler Destekleniyor?" -> "Futbol için 'http://www.goaloo28.com' ve Voleybol için 'https://www.volleyballstats247.com' tam uyumlu çalışmaktadır. 'VERİ KAYNAĞINA GİT' butonuna tıklayarak doğrudan desteklenen sitelere ulaşabilirsiniz."
                                    "Sinyal Güvenilirliği Nedir?" -> "Sinyallerimiz; takımların son 5 maçı, iç/dış saha gol-puan ortalamaları, lig sıralamaları ve kendi aralarındaki tarihi maç verilerini yapay zeka olasılık formülleriyle süzerek hesaplar. %85 üzeri olasılıklar GÜVENLİ, %60-85 arası SINIR, %60 altı ise RİSKLİ kabul edilir."
                                    else -> "Size nasıl yardımcı olabilirim?"
                                }
                                messages.add(Pair(response, false))
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(action, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ProgeniusPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages.toList()) { msg ->
                    val (text, isUser) = msg
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isUser) ProgeniusPrimary else ProgeniusSurface
                            ),
                            border = BorderStroke(1.dp, if (isUser) ProgeniusPrimary else ProgeniusBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Text(
                                text = text,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 13.sp,
                                color = if (isUser) Color.White else ProgeniusTextPrimary,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = chatInput,
                onValueChange = { chatInput = it },
                placeholder = { Text("Sorunuzu buraya yazın...", fontSize = 13.sp) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ProgeniusPrimary,
                    unfocusedBorderColor = ProgeniusBorder
                ),
                maxLines = 1
            )
            FloatingActionButton(
                onClick = {
                    if (chatInput.isNotBlank()) {
                        val query = chatInput
                        messages.add(Pair(query, true))
                        chatInput = ""
                        val response = "Sorunuz alındı: '$query'. Progenius Sinyal Motoru istatistik bazlı veri taraması için özel olarak optimize edilmiştir. Detaylı tahmin analizi için lütfen Ana Sayfa'daki 'Veri Yapıştır' özelliğini kullanın."
                        messages.add(Pair(response, false))
                    }
                },
                containerColor = ProgeniusPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Gönder", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: MatchViewModel) {
    val context = LocalContext.current
    var isNotificationEnabled by remember { mutableStateOf(true) }
    var defaultSportLeague by remember { mutableStateOf("Tümü (Uluslararası)") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ProgeniusBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "HIZLI AYARLAR",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = ProgeniusTextPrimary
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = ProgeniusSurface),
            border = BorderStroke(1.dp, ProgeniusBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("KULLANICI TERCİHLERİ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ProgeniusPrimary)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Akıllı Bildirim Sinyalleri", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ProgeniusTextPrimary)
                        Text("Yeni analizler tamamlandığında anlık bildirim al", fontSize = 11.sp, color = ProgeniusTextSecondary)
                    }
                    Switch(
                        checked = isNotificationEnabled,
                        onCheckedChange = { isNotificationEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ProgeniusPrimary)
                    )
                }

                Divider(color = ProgeniusBorder, thickness = 0.5.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Varsayılan Lig Tercihi", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ProgeniusTextPrimary)
                        Text("Sadece seçili lig verilerini filtrele", fontSize = 11.sp, color = ProgeniusTextSecondary)
                    }
                    Button(
                        onClick = {
                            defaultSportLeague = if (defaultSportLeague == "Tümü (Uluslararası)") "Avrupa Kupaları" else "Tümü (Uluslararası)"
                            Toast.makeText(context, "Tercih güncellendi: $defaultSportLeague", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ProgeniusCardBg),
                        border = BorderStroke(1.dp, ProgeniusBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(defaultSportLeague, color = ProgeniusTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = ProgeniusSurface),
            border = BorderStroke(1.dp, ProgeniusBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("VERİ MOTORU YÖNETİMİ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ProgeniusSecondary)

                Button(
                    onClick = {
                        viewModel.clearHistory()
                        Toast.makeText(context, "Analiz geçmişi ve önbelleği tamamen temizlendi!", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ProgeniusRed.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, ProgeniusRed.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = "Temizle", tint = ProgeniusRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Önbelleği & Geçmişi Sıfırla", color = ProgeniusRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = SleekSynthesisBg),
            border = BorderStroke(1.dp, ProgeniusBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("PROGENİUS ULTRA v5.0", fontSize = 14.sp, fontWeight = FontWeight.Black, color = SleekSynthesisText)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Yapay Zeka Spor Karşılaşmaları Analiz ve Sinyal Paneli.", fontSize = 11.sp, color = SleekSynthesisText.copy(alpha = 0.7f), textAlign = TextAlign.Center)
            }
        }
    }
}
