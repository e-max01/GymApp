package com.example.gymapp.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("La mia dieta") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Torna indietro"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Richiama il componente personalizzato che disegna il grafico
            DonutChart()

            Spacer(modifier = Modifier.height(48.dp))

            // Lista dei macronutrienti (valori simili al tuo prototipo)
            MacroRow(name = "Proteine", value = "14/100 g", color = Color(0xFF4CAF50))
            MacroRow(name = "Grassi", value = "11/80 g", color = Color(0xFFFF9800))
            MacroRow(name = "Carboidrati", value = "212/400 g", color = Color(0xFF2196F3))
        }
    }
}

// Funzione che disegna fisicamente il grafico a ciambella
@Composable
fun DonutChart() {
    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(220.dp)) {
            val strokeWidth = 40f // Spessore dell'anello

            // Arco delle Proteine (Verde)
            drawArc(
                color = Color(0xFF4CAF50),
                startAngle = -90f,
                sweepAngle = 120f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            // Arco dei Grassi (Arancione)
            drawArc(
                color = Color(0xFFFF9800),
                startAngle = 30f,
                sweepAngle = 80f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            // Arco dei Carboidrati (Blu)
            drawArc(
                color = Color(0xFF2196F3),
                startAngle = 110f,
                sweepAngle = 160f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        // Testo al centro della ciambella
        Text(
            text = "Kcal\n1800",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

// Funzione per creare le righe di testo in basso con il quadratino colorato
@Composable
fun MacroRow(name: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.8f) // Limita la larghezza per centrarlo meglio
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Quadratino colorato
            Surface(
                modifier = Modifier.size(16.dp),
                shape = MaterialTheme.shapes.small,
                color = color
            ) {}
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = name, style = MaterialTheme.typography.bodyLarge)
        }
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}