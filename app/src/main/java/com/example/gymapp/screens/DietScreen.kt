package com.example.gymapp.screens

import com.example.gymapp.components.AppAlertDialog
import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.navigation.NavController
import java.util.Calendar
import java.util.Locale

enum class MacroType {
    PROTEINS, FATS, CARBS
}

data class DietData(
    val protCurrent: Float,
    val protMax: Float,
    val fatCurrent: Float,
    val fatMax: Float,
    val carbCurrent: Float,
    val carbMax: Float
)

object DietManager {
    private const val PREFS_NAME = "DietPrefs"
    private const val KEY_MAX_PROT = "max_prot"
    private const val KEY_MAX_FAT = "max_fat"
    private const val KEY_MAX_CARB = "max_carb"
    private const val KEY_CUR_PROT = "cur_prot"
    private const val KEY_CUR_FAT = "cur_fat"
    private const val KEY_CUR_CARB = "cur_carb"
    private const val KEY_DIET_DAY = "diet_day_key"

    fun getDietDayKey(currentTimeMillis: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
        if (calendar.get(Calendar.HOUR_OF_DAY) < 5) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        val year = calendar.get(Calendar.YEAR)
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        return "${year}_$dayOfYear"
    }

    fun loadData(context: Context): DietData {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentDayKey = getDietDayKey(System.currentTimeMillis())
        val savedDayKey = prefs.getString(KEY_DIET_DAY, "")

        val maxProt = prefs.getFloat(KEY_MAX_PROT, 100f)
        val maxFat = prefs.getFloat(KEY_MAX_FAT, 80f)
        val maxCarb = prefs.getFloat(KEY_MAX_CARB, 500f)

        val curProt: Float
        val curFat: Float
        val curCarb: Float

        if (savedDayKey != currentDayKey) {
            curProt = 0f
            curFat = 0f
            curCarb = 0f
            prefs.edit {
                putString(KEY_DIET_DAY, currentDayKey)
                putFloat(KEY_CUR_PROT, 0f)
                putFloat(KEY_CUR_FAT, 0f)
                putFloat(KEY_CUR_CARB, 0f)
            }
        } else {
            curProt = prefs.getFloat(KEY_CUR_PROT, 0f)
            curFat = prefs.getFloat(KEY_CUR_FAT, 0f)
            curCarb = prefs.getFloat(KEY_CUR_CARB, 0f)
        }

        return DietData(curProt, maxProt, curFat, maxFat, curCarb, maxCarb)
    }

    fun saveData(context: Context, data: DietData) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentDayKey = getDietDayKey(System.currentTimeMillis())
        prefs.edit {
            putString(KEY_DIET_DAY, currentDayKey)
            putFloat(KEY_MAX_PROT, data.protMax)
            putFloat(KEY_MAX_FAT, data.fatMax)
            putFloat(KEY_MAX_CARB, data.carbMax)
            putFloat(KEY_CUR_PROT, data.protCurrent)
            putFloat(KEY_CUR_FAT, data.fatCurrent)
            putFloat(KEY_CUR_CARB, data.carbCurrent)
        }
    }
}

fun formatFloat(value: Float): String {
    return if (value % 1f == 0f) {
        value.toInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", value)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietScreen(navController: NavController) {
    val context = LocalContext.current
    var dietData by remember { mutableStateOf(DietManager.loadData(context)) }
    var editingMacro by remember { mutableStateOf<MacroType?>(null) }

    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    // Calcolo Kcal attuali e massime
    val currentKcal = (dietData.protCurrent * 4) + (dietData.fatCurrent * 9) + (dietData.carbCurrent * 4)
    val maxKcal = (dietData.protMax * 4) + (dietData.fatMax * 9) + (dietData.carbMax * 4)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("La mia dieta") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
                onClick = { navController.navigate("food_list") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Lista cibi")
            }
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

            // Grafico a ciambella basato sui massimi calorici
            DonutChart(
                protKcal = dietData.protMax * 4,
                fatKcal = dietData.fatMax * 9,
                carbKcal = dietData.carbMax * 4,
                currentKcal = currentKcal,
                maxKcal = maxKcal
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Lista dei macro-nutrienti cliccabili
            MacroRow(
                name = "Proteine",
                value = "${formatFloat(dietData.protCurrent)}/${formatFloat(dietData.protMax)} g",
                color = Color(0xFF4CAF50),
                onClick = { editingMacro = MacroType.PROTEINS }
            )
            MacroRow(
                name = "Grassi",
                value = "${formatFloat(dietData.fatCurrent)}/${formatFloat(dietData.fatMax)} g",
                color = Color(0xFFFF9800),
                onClick = { editingMacro = MacroType.FATS }
            )
            MacroRow(
                name = "Carboidrati",
                value = "${formatFloat(dietData.carbCurrent)}/${formatFloat(dietData.carbMax)} g",
                color = Color(0xFF2196F3),
                onClick = { editingMacro = MacroType.CARBS }
            )
        }
    }

    // Dialog per la modifica del valore attuale e del limite massimo
    editingMacro?.let { macro ->
        val (title, currentVal, maxVal) = when (macro) {
            MacroType.PROTEINS -> Triple("Proteine", dietData.protCurrent, dietData.protMax)
            MacroType.FATS -> Triple("Grassi", dietData.fatCurrent, dietData.fatMax)
            MacroType.CARBS -> Triple("Carboidrati", dietData.carbCurrent, dietData.carbMax)
        }

        EditMacroDialog(
            title = title,
            initialCurrent = currentVal,
            initialMax = maxVal,
            onDismiss = { editingMacro = null },
            onConfirm = { newCurrent, newMax ->
                val updatedData = when (macro) {
                    MacroType.PROTEINS -> dietData.copy(protCurrent = newCurrent, protMax = newMax)
                    MacroType.FATS -> dietData.copy(fatCurrent = newCurrent, fatMax = newMax)
                    MacroType.CARBS -> dietData.copy(carbCurrent = newCurrent, carbMax = newMax)
                }
                dietData = updatedData
                DietManager.saveData(context, updatedData)
                editingMacro = null
            }
        )
    }
}

@Composable
fun DonutChart(
    protKcal: Float,
    fatKcal: Float,
    carbKcal: Float,
    currentKcal: Float,
    maxKcal: Float
) {
    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(220.dp)) {
            val strokeWidth = 40f
            val totalKcal = protKcal + fatKcal + carbKcal

            val protAngle = if (totalKcal > 0) (protKcal / totalKcal) * 360f else 120f
            val fatAngle = if (totalKcal > 0) (fatKcal / totalKcal) * 360f else 120f
            val carbAngle = if (totalKcal > 0) (carbKcal / totalKcal) * 360f else 120f

            var startAngle = -90f

            // Arco delle Proteine (Verde)
            drawArc(
                color = Color(0xFF4CAF50),
                startAngle = startAngle,
                sweepAngle = protAngle.coerceAtMost(360f),
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            startAngle += protAngle

            // Arco dei Grassi (Arancione)
            drawArc(
                color = Color(0xFFFF9800),
                startAngle = startAngle,
                sweepAngle = fatAngle.coerceAtMost(360f - protAngle),
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            startAngle += fatAngle

            // Arco dei Carboidrati (Blu)
            drawArc(
                color = Color(0xFF2196F3),
                startAngle = startAngle,
                sweepAngle = carbAngle.coerceAtMost(360f - protAngle - fatAngle),
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        // Testo al centro della ciambella
        Text(
            text = "kcal\n${formatFloat(currentKcal)} / ${formatFloat(maxKcal)}",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun MacroRow(name: String, value: String, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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

@Composable
fun EditMacroDialog(
    title: String,
    initialCurrent: Float,
    initialMax: Float,
    onDismiss: () -> Unit,
    onConfirm: (Float, Float) -> Unit
) {
    var currentStr by remember { mutableStateOf(if (initialCurrent % 1f == 0f) initialCurrent.toInt().toString() else initialCurrent.toString()) }
    var maxStr by remember { mutableStateOf(if (initialMax % 1f == 0f) initialMax.toInt().toString() else initialMax.toString()) }

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = "Modifica $title",
        confirmButtonText = "Salva",
        onConfirm = {
            val cur = currentStr.toFloatOrNull() ?: initialCurrent
            val mx = maxStr.toFloatOrNull() ?: initialMax
            onConfirm(cur, mx)
        },
        dismissButtonText = "Annulla",
        onDismiss = onDismiss
    ) {
        OutlinedTextField(
            value = currentStr,
            onValueChange = { currentStr = it },
            label = { Text("Valore attuale (g)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = maxStr,
            onValueChange = { maxStr = it },
            label = { Text("Limite massimo (g)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
