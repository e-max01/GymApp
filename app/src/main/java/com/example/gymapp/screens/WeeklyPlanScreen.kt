package com.example.gymapp.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// LIVELLO 1: Schermata Lista Schede Settimanali
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyPlansListScreen(
    weeklyPlans: List<WeeklyWorkoutPlan>,
    onBackClick: () -> Unit,
    onSelectWeeklyPlan: (WeeklyWorkoutPlan) -> Unit,
    onAddWeeklyPlan: (String) -> Unit,
    onDeleteWeeklyPlan: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Le mie schede settimanali") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Torna indietro"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Nuova scheda settimanale")
            }
        }
    ) { paddingValues ->
        if (weeklyPlans.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nessuna scheda settimanale presente.\nPremi + in basso per crearne una nuova.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                itemsIndexed(weeklyPlans, key = { _, plan -> plan.id }) { _, plan ->
                    WeeklyPlanCard(
                        plan = plan,
                        onClick = { onSelectWeeklyPlan(plan) },
                        onDelete = { onDeleteWeeklyPlan(plan.id) }
                    )
                }
            }
        }

        if (showAddDialog) {
            AddNameDialog(
                title = "Nuova Scheda Settimanale",
                label = "Nome scheda (es. Scheda Massa)",
                onDismiss = { showAddDialog = false },
                onConfirm = { name ->
                    onAddWeeklyPlan(name)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun WeeklyPlanCard(
    plan: WeeklyWorkoutPlan,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 16.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plan.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${plan.dailyPlans.size} schede giornaliere",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Elimina scheda",
                    tint = MaterialTheme.colorScheme.error
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// LIVELLO 2: Schermata Dettaglio Scheda Settimanale (Lista Schede Giornaliere)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyPlanDetailScreen(
    weeklyPlan: WeeklyWorkoutPlan,
    onBackClick: () -> Unit,
    onSelectDailyPlan: (DailyWorkoutPlan) -> Unit,
    onAddDailyPlan: (String) -> Unit,
    onDeleteDailyPlan: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(weeklyPlan.name) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Torna indietro"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Nuova scheda giornaliera")
            }
        }
    ) { paddingValues ->
        if (weeklyPlan.dailyPlans.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nessuna scheda giornaliera in questa settimana.\nPremi + in basso per aggiungerne una.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                itemsIndexed(weeklyPlan.dailyPlans, key = { _, daily -> daily.id }) { _, daily ->
                    DailyPlanCard(
                        dailyPlan = daily,
                        onClick = { onSelectDailyPlan(daily) },
                        onDelete = { onDeleteDailyPlan(daily.id) }
                    )
                }
            }
        }

        if (showAddDialog) {
            AddNameDialog(
                title = "Nuova Scheda Giornaliera",
                label = "Nome giorno (es. Lunedì - Petto)",
                onDismiss = { showAddDialog = false },
                onConfirm = { name ->
                    onAddDailyPlan(name)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun DailyPlanCard(
    dailyPlan: DailyWorkoutPlan,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(end = 16.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dailyPlan.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${dailyPlan.exercises.size} esercizi inseriti",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Elimina giorno",
                    tint = MaterialTheme.colorScheme.error
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
