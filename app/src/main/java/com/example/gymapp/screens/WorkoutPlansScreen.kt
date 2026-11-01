package com.example.gymapp.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import androidx.core.content.edit

// Modello per la singola riga di serie (es. 8 reps 0 kg)
data class SetDetail(
    val id: String = UUID.randomUUID().toString(),
    val reps: Int = 8,
    val weightKg: Float = 0f,
)

// Modello per l'esercizio con la lista delle serie
data class ExerciseSetInfo(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val setsList: List<SetDetail> = emptyList(),
)

// Modello dati per la scheda giornaliera
data class DailyWorkoutPlan(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val exercises: List<ExerciseSetInfo> = emptyList()
)

// Modello dati per la scheda settimanale
data class WeeklyWorkoutPlan(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val dailyPlans: List<DailyWorkoutPlan> = emptyList()
)

object WorkoutPlanManager {
    private const val PREFS_NAME = "WorkoutPlanPrefs"
    private const val KEY_PLANS = "weekly_workout_plans"

    fun loadPlans(context: Context): List<WeeklyWorkoutPlan> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_PLANS, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<WeeklyWorkoutPlan>()
            for (i in 0 until jsonArray.length()) {
                val weeklyObj = jsonArray.getJSONObject(i)
                val dailyArray = weeklyObj.optJSONArray("dailyPlans") ?: JSONArray()
                val dailyList = mutableListOf<DailyWorkoutPlan>()
                for (j in 0 until dailyArray.length()) {
                    val dailyObj = dailyArray.getJSONObject(j)
                    val exArray = dailyObj.optJSONArray("exercises") ?: JSONArray()
                    val exList = mutableListOf<ExerciseSetInfo>()
                    for (k in 0 until exArray.length()) {
                        val exObj = exArray.getJSONObject(k)
                        val setsArray = exObj.optJSONArray("setsList") ?: JSONArray()
                        val setsList = mutableListOf<SetDetail>()
                        for (s in 0 until setsArray.length()) {
                            val setObj = setsArray.getJSONObject(s)
                            setsList.add(
                                SetDetail(
                                    id = setObj.getString("id"),
                                    reps = setObj.optInt("reps", 8),
                                    weightKg = setObj.optDouble("weightKg", 0.0).toFloat()
                                )
                            )
                        }
                        exList.add(
                            ExerciseSetInfo(
                                id = exObj.getString("id"),
                                name = exObj.getString("name"),
                                setsList = setsList
                            )
                        )
                    }
                    dailyList.add(
                        DailyWorkoutPlan(
                            id = dailyObj.getString("id"),
                            name = dailyObj.getString("name"),
                            exercises = exList
                        )
                    )
                }
                list.add(
                    WeeklyWorkoutPlan(
                        id = weeklyObj.getString("id"),
                        name = weeklyObj.getString("name"),
                        dailyPlans = dailyList
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun savePlans(context: Context, plans: List<WeeklyWorkoutPlan>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        plans.forEach { weekly ->
            val weeklyObj = JSONObject().apply {
                put("id", weekly.id)
                put("name", weekly.name)
                val dailyArray = JSONArray()
                weekly.dailyPlans.forEach { daily ->
                    val dailyObj = JSONObject().apply {
                        put("id", daily.id)
                        put("name", daily.name)
                        val exArray = JSONArray()
                        daily.exercises.forEach { ex ->
                            val exObj = JSONObject().apply {
                                put("id", ex.id)
                                put("name", ex.name)
                                val setsArray = JSONArray()
                                ex.setsList.forEach { setItem ->
                                    val setObj = JSONObject().apply {
                                        put("id", setItem.id)
                                        put("reps", setItem.reps)
                                        put("weightKg", setItem.weightKg)
                                    }
                                    setsArray.put(setObj)
                                }
                                put("setsList", setsArray)
                            }
                            exArray.put(exObj)
                        }
                        put("exercises", exArray)
                    }
                    dailyArray.put(dailyObj)
                }
                put("dailyPlans", dailyArray)
            }
            jsonArray.put(weeklyObj)
        }
        prefs.edit { putString(KEY_PLANS, jsonArray.toString()) }
    }
}

@Composable
fun WorkoutPlansScreen(navController: NavController) {
    val context = LocalContext.current

    // Carica la lista delle schede settimanali memorizzate
    var weeklyPlans by remember { mutableStateOf(WorkoutPlanManager.loadPlans(context)) }

    // Navigation internal states
    var selectedWeeklyPlan by remember { mutableStateOf<WeeklyWorkoutPlan?>(null) }
    var selectedDailyPlan by remember { mutableStateOf<DailyWorkoutPlan?>(null) }

    // Intercetta il tasto indietro di sistema
    BackHandler(enabled = selectedDailyPlan != null) {
        selectedDailyPlan = null
    }
    BackHandler(enabled = selectedWeeklyPlan != null && selectedDailyPlan == null) {
        selectedWeeklyPlan = null
    }

    // Router tra i 3 livelli di interfaccia
    val currentWeekly = selectedWeeklyPlan
    val currentDaily = selectedDailyPlan

    if (currentWeekly == null) {
        // LIVELLO 1: Lista delle Schede Settimanali
        WeeklyPlansListScreen(
            weeklyPlans = weeklyPlans,
            onBackClick = { navController.popBackStack() },
            onSelectWeeklyPlan = { selectedWeeklyPlan = it },
            onAddWeeklyPlan = { name ->
                val newWeekly = WeeklyWorkoutPlan(name = name)
                val updatedPlans = weeklyPlans + newWeekly
                weeklyPlans = updatedPlans
                WorkoutPlanManager.savePlans(context, updatedPlans)
            },
            onDeleteWeeklyPlan = { weeklyId ->
                val updatedPlans = weeklyPlans.filterNot { it.id == weeklyId }
                weeklyPlans = updatedPlans
                WorkoutPlanManager.savePlans(context, updatedPlans)
            }
        )
    } else if (currentDaily == null) {
        // LIVELLO 2: Dettaglio Scheda Settimanale (Lista Schede Giornaliere)
        WeeklyPlanDetailScreen(
            weeklyPlan = currentWeekly,
            onBackClick = { selectedWeeklyPlan = null },
            onSelectDailyPlan = { selectedDailyPlan = it },
            onAddDailyPlan = { dailyName ->
                val newDaily = DailyWorkoutPlan(name = dailyName)
                val updatedWeekly = currentWeekly.copy(
                    dailyPlans = currentWeekly.dailyPlans + newDaily
                )
                selectedWeeklyPlan = updatedWeekly
                val updatedPlans = weeklyPlans.map {
                    if (it.id == updatedWeekly.id) updatedWeekly else it
                }
                weeklyPlans = updatedPlans
                WorkoutPlanManager.savePlans(context, updatedPlans)
            },
            onDeleteDailyPlan = { dailyId ->
                val updatedWeekly = currentWeekly.copy(
                    dailyPlans = currentWeekly.dailyPlans.filterNot { it.id == dailyId }
                )
                selectedWeeklyPlan = updatedWeekly
                val updatedPlans = weeklyPlans.map {
                    if (it.id == updatedWeekly.id) updatedWeekly else it
                }
                weeklyPlans = updatedPlans
                WorkoutPlanManager.savePlans(context, updatedPlans)
            }
        )
    } else {
        // LIVELLO 3: Dettaglio Scheda Giornaliera
        DailyPlanDetailScreen(
            dailyPlan = currentDaily,
            onBackClick = { selectedDailyPlan = null },
            onUpdateExercises = { newExercises ->
                val updatedDaily = currentDaily.copy(exercises = newExercises)
                selectedDailyPlan = updatedDaily
                val updatedWeekly = currentWeekly.copy(
                    dailyPlans = currentWeekly.dailyPlans.map {
                        if (it.id == updatedDaily.id) updatedDaily else it
                    }
                )
                selectedWeeklyPlan = updatedWeekly
                val updatedPlans = weeklyPlans.map {
                    if (it.id == updatedWeekly.id) updatedWeekly else it
                }
                weeklyPlans = updatedPlans
                WorkoutPlanManager.savePlans(context, updatedPlans)
            }
        )
    }
}

// Dialog generico per inserimento nomi schede
@Composable
fun AddNameDialog(
    title: String,
    label: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var textValue by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = textValue,
                onValueChange = { textValue = it },
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (textValue.isNotBlank()) {
                        onConfirm(textValue.trim())
                    }
                }
            ) {
                Text("Crea")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}
