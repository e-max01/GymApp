package com.example.gymapp.screens

import com.example.gymapp.components.AppAlertDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.navigation.NavController
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class FoodItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val proteins: Float,
    val fats: Float,
    val carbs: Float
)

object FoodManager {
    private const val PREFS_NAME = "FoodPrefs"
    private const val KEY_FOODS = "food_items_list"

    fun loadFoods(context: Context): List<FoodItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_FOODS, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<FoodItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    FoodItem(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        proteins = obj.optDouble("proteins", 0.0).toFloat(),
                        fats = obj.optDouble("fats", 0.0).toFloat(),
                        carbs = obj.optDouble("carbs", 0.0).toFloat()
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveFoods(context: Context, foods: List<FoodItem>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        foods.forEach { food ->
            val obj = JSONObject().apply {
                put("id", food.id)
                put("name", food.name)
                put("proteins", food.proteins.toDouble())
                put("fats", food.fats.toDouble())
                put("carbs", food.carbs.toDouble())
            }
            jsonArray.put(obj)
        }
        prefs.edit { putString(KEY_FOODS, jsonArray.toString()) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodListScreen(navController: NavController) {
    val context = LocalContext.current
    var foodList by remember { mutableStateOf(FoodManager.loadFoods(context)) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedFoodForQuantity by remember { mutableStateOf<FoodItem?>(null) }
    var isEditing by remember(foodList.isEmpty()) { mutableStateOf(foodList.isEmpty()) }

    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("I miei cibi") },
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
                onClick = { isEditing = !isEditing },
                containerColor = if (isEditing) MaterialTheme.colorScheme.primary else Color(0xFF9E9E9E),
                contentColor = if (isEditing) MaterialTheme.colorScheme.onPrimary else Color.White,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = if (isEditing) 6.dp else 2.dp,
                    pressedElevation = if (isEditing) 8.dp else 4.dp
                )
            ) {
                Icon(
                    imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                    contentDescription = if (isEditing) "Conferma modifica" else "Attiva modifica"
                )
            }
        }
    ) { paddingValues ->
        if (foodList.isEmpty() && !isEditing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nessun cibo presente.\nPremi il tasto Modifica in basso a destra per aggiungerne uno.",
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
                itemsIndexed(foodList, key = { _, food -> food.id }) { _, food ->
                    FoodCard(
                        food = food,
                        isEditing = isEditing,
                        onClick = {
                            if (!isEditing) {
                                selectedFoodForQuantity = food
                            }
                        },
                        onDelete = {
                            val updated = foodList.filterNot { it.id == food.id }
                            foodList = updated
                            FoodManager.saveFoods(context, updated)
                        }
                    )
                }

                if (isEditing) {
                    item(key = "add_food_button") {
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
                                text = "Aggiungi cibo",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddFoodDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name, prot, fat, carb ->
                    val newFood = FoodItem(name = name, proteins = prot, fats = fat, carbs = carb)
                    val updated = foodList + newFood
                    foodList = updated
                    FoodManager.saveFoods(context, updated)
                    showAddDialog = false
                }
            )
        }

        selectedFoodForQuantity?.let { food ->
            FoodQuantityDialog(
                food = food,
                onDismiss = { selectedFoodForQuantity = null },
                onConfirm = { count ->
                    val addedProt = food.proteins * count
                    val addedFat = food.fats * count
                    val addedCarb = food.carbs * count

                    val currentDiet = DietManager.loadData(context)
                    val updatedDiet = currentDiet.copy(
                        protCurrent = currentDiet.protCurrent + addedProt,
                        fatCurrent = currentDiet.fatCurrent + addedFat,
                        carbCurrent = currentDiet.carbCurrent + addedCarb
                    )
                    DietManager.saveData(context, updatedDiet)

                    Toast.makeText(
                        context,
                        "Aggiunti ${formatFloat(addedProt)}g P, ${formatFloat(addedFat)}g G, ${formatFloat(addedCarb)}g C alla dieta",
                        Toast.LENGTH_SHORT
                    ).show()

                    selectedFoodForQuantity = null
                }
            )
        }
    }
}

@Composable
fun FoodCard(
    food: FoodItem,
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
                imageVector = Icons.Default.Fastfood,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 16.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = food.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "P: ${formatFloat(food.proteins)}g | G: ${formatFloat(food.fats)}g | C: ${formatFloat(food.carbs)}g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isEditing) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Elimina cibo",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
        }
    }
}

@Composable
fun FoodQuantityDialog(
    food: FoodItem,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    var quantityTextFieldValue by remember {
        mutableStateOf(TextFieldValue(text = "1"))
    }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed) {
        if (isPressed) {
            quantityTextFieldValue = quantityTextFieldValue.copy(
                selection = TextRange(0, quantityTextFieldValue.text.length)
            )
        }
    }

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = food.name,
        confirmButtonText = "Aggiungi",
        onConfirm = {
            val count = quantityTextFieldValue.text.replace(',', '.').toFloatOrNull() ?: 1f
            onConfirm(count)
        },
        dismissButtonText = "Annulla",
        onDismiss = onDismiss,
        confirmEnabled = quantityTextFieldValue.text.isNotBlank() && (quantityTextFieldValue.text.replace(',', '.').toFloatOrNull() ?: 0f) > 0f
    ) {
        Text(
            text = "Valori nutrizionali unitari:\nP: ${formatFloat(food.proteins)}g | G: ${formatFloat(food.fats)}g | C: ${formatFloat(food.carbs)}g",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = quantityTextFieldValue,
            onValueChange = { quantityTextFieldValue = it },
            label = { Text("Quantità (volte)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            interactionSource = interactionSource,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun AddFoodDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Float, Float, Float) -> Unit
) {
    var nameStr by remember { mutableStateOf("") }
    var protStr by remember { mutableStateOf("") }
    var fatStr by remember { mutableStateOf("") }
    var carbStr by remember { mutableStateOf("") }

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = "Nuovo Cibo",
        confirmButtonText = "Crea",
        onConfirm = {
            if (nameStr.isNotBlank()) {
                val p = protStr.toFloatOrNull() ?: 0f
                val f = fatStr.toFloatOrNull() ?: 0f
                val c = carbStr.toFloatOrNull() ?: 0f
                onConfirm(nameStr.trim(), p, f, c)
            }
        },
        dismissButtonText = "Annulla",
        onDismiss = onDismiss,
        confirmEnabled = nameStr.isNotBlank()
    ) {
        OutlinedTextField(
            value = nameStr,
            onValueChange = { nameStr = it },
            label = { Text("Nome cibo (es. Petto di pollo)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = protStr,
            onValueChange = { protStr = it },
            label = { Text("Proteine (g)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = fatStr,
            onValueChange = { fatStr = it },
            label = { Text("Grassi (g)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = carbStr,
            onValueChange = { carbStr = it },
            label = { Text("Carboidrati (g)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
