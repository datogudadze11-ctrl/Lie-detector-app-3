package com.liedetector.test

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LieDetectorApp() }
    }
}

@Composable
fun LieDetectorApp() {
    var tab by remember { mutableStateOf(0) }
    var text by remember { mutableStateOf("") }
    var score by remember { mutableStateOf<Int?>(null) }
    var reasons by remember { mutableStateOf(listOf<String>()) }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF6C4DFF),
            background = Color(0xFFF7F7FA)
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF7F7FA)) {
            Column(Modifier.fillMaxSize()) {
                Header()
                when (tab) {
                    0 -> TextAnalyzer(
                        text = text,
                        onText = { text = it; score = null },
                        score = score,
                        reasons = reasons,
                        onAnalyze = {
                            val result = analyze(text)
                            score = result.first
                            reasons = result.second
                        }
                    )
                    1 -> ScreenshotTab()
                    else -> HistoryTab()
                }
                NavigationBar {
                    NavigationBarItem(selected = tab == 0, onClick = { tab = 0 },
                        icon = { Text("📝") }, label = { Text("Text") })
                    NavigationBarItem(selected = tab == 1, onClick = { tab = 1 },
                        icon = { Text("📷") }, label = { Text("Photo") })
                    NavigationBarItem(selected = tab == 2, onClick = { tab = 2 },
                        icon = { Text("📊") }, label = { Text("History") })
                }
            }
        }
    }
}

@Composable
fun Header() {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
        Text("Lie Detector AI", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("Credibility & deception analysis", color = Color.Gray, fontSize = 14.sp)
    }
}

@Composable
fun TextAnalyzer(
    text: String,
    onText: (String) -> Unit,
    score: Int?,
    reasons: List<String>,
    onAnalyze: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Paste a message", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = text,
            onValueChange = onText,
            modifier = Modifier.fillMaxWidth().height(180.dp),
            placeholder = { Text("Paste an SMS, chat message or statement here…") },
            shape = RoundedCornerShape(16.dp)
        )
        Button(
            onClick = onAnalyze,
            enabled = text.trim().isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp)
        ) { Text("ANALYZE", fontWeight = FontWeight.Bold) }

        if (score != null) {
            ResultCard(score, reasons)
        } else {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("How it works", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("This demo uses linguistic signals and contradictions to estimate how suspicious a statement sounds. It does not prove whether a person is actually lying.")
                }
            }
        }
    }
}

@Composable
fun ResultCard(score: Int, reasons: List<String>) {
    Card(shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Deception probability", color = Color.Gray)
            Text("$score%", fontSize = 48.sp, fontWeight = FontWeight.Bold)
            Text(
                when {
                    score >= 75 -> "Highly suspicious"
                    score >= 50 -> "Suspicious"
                    score >= 30 -> "Uncertain"
                    else -> "Mostly credible"
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider()
            Text("Why?", fontWeight = FontWeight.Bold)
            reasons.forEach { Text("• $it") }
            Spacer(Modifier.height(4.dp))
            Text(
                "⚠ This is an AI-style estimate, not proof of deception.",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun ScreenshotTab() {
    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Screenshot analysis", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Card(shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📷", fontSize = 44.sp)
                Spacer(Modifier.height(10.dp))
                Text("Screenshot / photo OCR", fontWeight = FontWeight.Bold)
                Text("The production version will extract chat text from screenshots and analyze each speaker separately.")
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = {}) { Text("CHOOSE IMAGE (demo)") }
            }
        }
    }
}

@Composable
fun HistoryTab() {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Analysis history", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Card(shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text("No analyses saved yet.")
                Text("Your future results will appear here.", color = Color.Gray, fontSize = 13.sp)
            }
        }
    }
}

fun analyze(input: String): Pair<Int, List<String>> {
    val t = input.lowercase()
    var score = 18
    val reasons = mutableListOf<String>()

    val defensive = listOf(
        "გეფიცები", "მართლა", "ნამდვილად", "trust me", "believe me",
        "i swear", "honestly", "to be honest", "სიმართლეს გეუბნები"
    )
    val vague = listOf(
        "ალბათ", "როგორც მახსოვს", "არ ვიცი", "maybe", "probably",
        "i think", "not sure", "somewhere", "later", "some time"
    )
    val absolute = listOf(
        "არასდროს", "ყოველთვის", "never", "always", "100%"
    )

    if (defensive.any { t.contains(it) }) {
        score += 18
        reasons += "Strong reassurance / defensive wording detected."
    }
    if (vague.any { t.contains(it) }) {
        score += 12
        reasons += "Vague or uncertain wording reduces credibility."
    }
    if (absolute.any { t.contains(it) }) {
        score += 10
        reasons += "Absolute claims can be a suspicious linguistic signal."
    }
    if (input.length < 35) {
        score += 8
        reasons += "Very short statement provides limited verifiable detail."
    } else if (input.length > 300) {
        score += 5
        reasons += "Long explanation contains more opportunities for inconsistency."
    }
    val exclamations = input.count { it == '!' }
    if (exclamations >= 2) {
        score += 7
        reasons += "Repeated exclamation marks suggest heightened emphasis."
    }

    score = min(score, 96)
    if (reasons.isEmpty()) reasons += "No strong deception signals were detected."
    return score to reasons.take(4)
}
