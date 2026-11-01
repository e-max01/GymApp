package com.example.gymapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun HomeScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Home",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 16.dp)
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // Spazio vuoto flessibile sopra l'orologio
        Spacer(modifier = Modifier.weight(1f))

        ClockText()

        // Spazio vuoto flessibile tra l'orologio e le cards
        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HomeCard(
                    modifier = Modifier.weight(1f),
                    title = "Schede",
                    icon = Icons.Default.DateRange,
                    onClick = { navController.navigate("schede") }
                )
                HomeCard(
                    modifier = Modifier.weight(1f),
                    title = "Dieta",
                    icon = Icons.Default.PieChart,
                    onClick = { navController.navigate("dieta") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HomeCard(
                    modifier = Modifier.weight(1f),
                    title = "Esercizi",
                    icon = Icons.Default.FitnessCenter,
                    onClick = { navController.navigate("esercizi") }
                )
                HomeCard(
                    modifier = Modifier.weight(1f),
                    title = "Wishlist",
                    icon = Icons.Default.Star,
                    onClick = { navController.navigate("wishlist") }
                )
            }
        }

        // Spazio vuoto flessibile sotto le cards per bilanciare l'altezza totale
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun ClockText() {
    var time by remember { mutableStateOf(getCurrentTime()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000.milliseconds)
            time = getCurrentTime()
        }
    }

    Text(
        text = time,
        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary
    )
}

fun getCurrentTime(): String {
    return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
}

@Composable
fun HomeCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}