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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
    var isEditing by remember(weeklyPlans.isEmpty()) { mutableStateOf(weeklyPlans.isEmpty()) }

    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Schede settimanali") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Torna indietro"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                modifier = Modifier.drawBehind {
                    val strokeWidth = 1.dp.toPx()
                    drawLine(
                        color = outlineColor,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = strokeWidth
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isEditing = !isEditing },
                containerColor = if (isEditing) MaterialTheme.colorScheme.primary else Color(0xFF9E9E9E),
                contentColor = if (isEditing) MaterialTheme.colorScheme.onPrimary else Color.White,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = if (isEditing) 6.dp else 12.dp,
                    pressedElevation = if (isEditing) 8.dp else 16.dp
                )
            ) {
                Icon(
                    imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                    contentDescription = if (isEditing) "Conferma modifica" else "Attiva modifica"
                )
            }
        }
    ) { paddingValues ->
        if (weeklyPlans.isEmpty() && !isEditing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nessuna scheda settimanale presente.\nPremi il tasto Modifica in basso a destra per crearne una.",
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
                        isEditing = isEditing,
                        onClick = { onSelectWeeklyPlan(plan) },
                        onDelete = { onDeleteWeeklyPlan(plan.id) }
                    )
                }

                if (isEditing) {
                    item(key = "add_weekly_plan_button") {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showAddDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = BorderStroke(
                                width = 1.5.dp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = "Aggiungi scheda settimanale",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
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
    isEditing: Boolean,
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

            if (isEditing) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Elimina scheda",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
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
    var isEditing by remember(weeklyPlan.dailyPlans.isEmpty()) { mutableStateOf(weeklyPlan.dailyPlans.isEmpty()) }

    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Schede giornaliere") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Torna indietro"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                modifier = Modifier.drawBehind {
                    val strokeWidth = 1.dp.toPx()
                    drawLine(
                        color = outlineColor,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = strokeWidth
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isEditing = !isEditing },
                containerColor = if (isEditing) MaterialTheme.colorScheme.primary else Color(0xFF9E9E9E),
                contentColor = if (isEditing) MaterialTheme.colorScheme.onPrimary else Color.White,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = if (isEditing) 6.dp else 12.dp,
                    pressedElevation = if (isEditing) 8.dp else 16.dp
                )
            ) {
                Icon(
                    imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                    contentDescription = if (isEditing) "Conferma modifica" else "Attiva modifica"
                )
            }
        }
    ) { paddingValues ->
        if (weeklyPlan.dailyPlans.isEmpty() && !isEditing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nessuna scheda giornaliera in questa settimana.\nPremi il tasto Modifica in basso a destra per aggiungerne una.",
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
                        isEditing = isEditing,
                        onClick = { onSelectDailyPlan(daily) },
                        onDelete = { onDeleteDailyPlan(daily.id) }
                    )
                }

                if (isEditing) {
                    item(key = "add_daily_plan_button") {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showAddDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = BorderStroke(
                                width = 1.5.dp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = "Aggiungi scheda giornaliera",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
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
    isEditing: Boolean,
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
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 16.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dailyPlan.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${dailyPlan.exercises.size} esercizi inseriti",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isEditing) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Elimina giorno",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
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
