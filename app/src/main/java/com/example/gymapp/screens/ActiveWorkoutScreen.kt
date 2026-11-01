package com.example.gymapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    dailyPlanName: String,
    exercises: List<ExerciseSetInfo>,
    onUpdateExercises: (List<ExerciseSetInfo>) -> Unit,
    onClose: () -> Unit
) {
    // Appiattiamo la lista per navigare facilmente avanti e indietro tra le serie
    data class FlatSet(val exerciseIndex: Int, val setIndex: Int, val exercise: ExerciseSetInfo, val setDetail: SetDetail)

    val flatSets = remember(exercises) {
        exercises.flatMapIndexed { eIdx, ex ->
            ex.setsList.mapIndexed { sIdx, set -> FlatSet(eIdx, sIdx, ex, set) }
        }
    }

    var countdown by remember { mutableIntStateOf(3) }
    var currentGlobalSetIndex by remember { mutableIntStateOf(0) }

    var showOverview by remember { mutableStateOf(false) }
    var setToEdit by remember { mutableStateOf<FlatSet?>(null) }

    // Gestione del Timer Iniziale
    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000L)
            countdown--
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (countdown > 0) "Preparazione" else dailyPlanName) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Chiudi allenamento")
                    }
                },
                actions = {
                    if (countdown == 0) {
                        IconButton(onClick = { showOverview = true }) {
                            Icon(imageVector = Icons.Default.List, contentDescription = "Panoramica scheda")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (countdown == 0 && flatSets.isNotEmpty()) {
                BottomAppBar(
                    containerColor = Color.Transparent,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        OutlinedButton(
                            onClick = { if (currentGlobalSetIndex > 0) currentGlobalSetIndex-- },
                            enabled = currentGlobalSetIndex > 0
                        ) {
                            Text("Indietro")
                        }
                        Button(
                            onClick = {
                                if (currentGlobalSetIndex < flatSets.lastIndex) {
                                    currentGlobalSetIndex++
                                } else {
                                    onClose() // Fine allenamento
                                }
                            }
                        ) {
                            Text(if (currentGlobalSetIndex == flatSets.lastIndex) "Termina" else "Avanti")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (flatSets.isEmpty()) {
                Text("Nessuna serie disponibile in questa scheda.")
            } else if (countdown > 0) {
                // Layout del Countdown Circolare
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { countdown / 3f },
                        modifier = Modifier.size(150.dp),
                        strokeWidth = 8.dp,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Text(
                        text = "00:0$countdown",
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                // Layout della Serie Attiva
                val currentSet = flatSets[currentGlobalSetIndex]

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = currentSet.exercise.name,
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Serie ${currentSet.setIndex + 1} di ${currentSet.exercise.setsList.size}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Reps Cliccabile
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            onClick = { setToEdit = currentSet }
                        ) {
                            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = currentSet.setDetail.reps.toString(),
                                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black)
                                )
                                Text("Reps", style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        // Peso Cliccabile
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            onClick = { setToEdit = currentSet }
                        ) {
                            val weightFormatted = if (currentSet.setDetail.weightKg % 1f == 0f) {
                                currentSet.setDetail.weightKg.toInt().toString()
                            } else {
                                currentSet.setDetail.weightKg.toString()
                            }
                            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = weightFormatted,
                                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black)
                                )
                                Text("Kg", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog Modifica Volante
    setToEdit?.let { targetSet ->
        EditSetValuesDialog(
            setNumber = targetSet.setIndex + 1,
            setDetail = targetSet.setDetail,
            onDismiss = { setToEdit = null },
            onConfirm = { newReps, newWeight ->
                val updatedExercises = exercises.toMutableList()
                val ex = updatedExercises[targetSet.exerciseIndex]
                val updatedSets = ex.setsList.toMutableList()
                updatedSets[targetSet.setIndex] = targetSet.setDetail.copy(reps = newReps, weightKg = newWeight)
                updatedExercises[targetSet.exerciseIndex] = ex.copy(setsList = updatedSets)

                onUpdateExercises(updatedExercises)
                setToEdit = null
            }
        )
    }

    // Dialog Panoramica Scheda
    if (showOverview) {
        AlertDialog(
            onDismissRequest = { showOverview = false },
            title = { Text("Panoramica Allenamento") },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    itemsIndexed(flatSets) { index, flatSet ->
                        val isCurrent = index == currentGlobalSetIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(if (isCurrent) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = flatSet.exercise.name,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = "Serie ${flatSet.setIndex + 1}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text("${flatSet.setDetail.reps} x ${flatSet.setDetail.weightKg} kg")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOverview = false }) { Text("Chiudi") }
            }
        )
    }
}