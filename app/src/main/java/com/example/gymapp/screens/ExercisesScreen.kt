package com.example.gymapp.screens

import com.example.gymapp.components.AppAlertDialog
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.navigation.NavController
import com.example.gymapp.data.DEFAULT_EXERCISES
import com.example.gymapp.data.ExerciseItem
import org.json.JSONArray
import org.json.JSONObject

// Definizione delle categorie
val MUSCLE_GROUPS = listOf("Bicipiti", "Tricipiti", "Petto", "Schiena", "Gambe", "Spalle")
val EQUIPMENT_TYPES = listOf("Bilanciere", "Manubri", "Cavi", "Macchinari", "Corpo libero")

sealed class ExerciseUiItem {
    data class GroupHeader(val muscleGroup: String) : ExerciseUiItem()
    data class EquipmentHeader(val muscleGroup: String, val equipment: String) : ExerciseUiItem()
    data class ExerciseCard(val exercise: ExerciseItem) : ExerciseUiItem()
}

object ExerciseManager {
    private const val PREFS_NAME = "ExercisePrefs"
    private const val KEY_EXERCISES = "custom_exercises_list"

    fun loadCustomExercises(context: Context): List<ExerciseItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_EXERCISES, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<ExerciseItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    ExerciseItem(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        muscleGroup = obj.getString("muscleGroup"),
                        equipment = obj.getString("equipment"),
                        isCustom = true
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveCustomExercises(context: Context, items: List<ExerciseItem>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        items.filter { it.isCustom }.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("muscleGroup", item.muscleGroup)
                put("equipment", item.equipment)
            }
            jsonArray.put(obj)
        }
        prefs.edit { putString(KEY_EXERCISES, jsonArray.toString()) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisesScreen(
    navController: NavController,
    onSelectExercise: ((ExerciseItem, sets: Int, reps: Int) -> Unit)? = null,
    onBackClickOverride: (() -> Unit)? = null
) {
    val context = LocalContext.current

    // Stato per la modalità modifica (ispirato alle schede workout)
    var isEditing by remember { mutableStateOf(false) }

    // Carica gli esercizi personalizzati dell'utente
    var customExercises by remember { mutableStateOf(ExerciseManager.loadCustomExercises(context)) }

    // Unisce gli esercizi hardcoded di default con quelli aggiunti dall'utente
    val allExercises = remember(customExercises) {
        DEFAULT_EXERCISES + customExercises
    }

    // Stati per ricerca e filtri
    var searchQuery by remember { mutableStateOf("") }
    var selectedMuscleGroup by remember { mutableStateOf("Tutti") }
    var selectedEquipment by remember { mutableStateOf("Tutti") }

    // Stati per dialogs
    var showAddDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }

    val isFilterActive = selectedMuscleGroup != "Tutti" || selectedEquipment != "Tutti"

    // Filtra gli esercizi in base alla ricerca e ai filtri
    val filteredExercises = remember(searchQuery, selectedMuscleGroup, selectedEquipment, allExercises) {
        allExercises.filter { exercise ->
            val matchesQuery = searchQuery.isBlank() || exercise.name.contains(searchQuery, ignoreCase = true)
            val matchesGroup = selectedMuscleGroup == "Tutti" || exercise.muscleGroup == selectedMuscleGroup
            val matchesEquipment = selectedEquipment == "Tutti" || exercise.equipment == selectedEquipment
            matchesQuery && matchesGroup && matchesEquipment
        }
    }

    // Costruisce la lista di elementi UI raggruppati per Gruppo Muscolare e Strumento
    val uiItems = remember(filteredExercises) {
        val list = mutableListOf<ExerciseUiItem>()
        val groupedByMuscle = filteredExercises.groupBy { it.muscleGroup }

        groupedByMuscle.forEach { (group, groupExercises) ->
            list.add(ExerciseUiItem.GroupHeader(group))
            val groupedByEquip = groupExercises.groupBy { it.equipment }
            groupedByEquip.forEach { (equip, equipExercises) ->
                list.add(ExerciseUiItem.EquipmentHeader(group, equip))
                equipExercises.sortedBy { it.name }.forEach { exercise ->
                    list.add(ExerciseUiItem.ExerciseCard(exercise))
                }
            }
        }
        list
    }

    val listState = rememberLazyListState()

    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (onSelectExercise != null) "Seleziona Esercizio" else "Esercizi") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (onBackClickOverride != null) {
                                onBackClickOverride()
                            } else {
                                navController.popBackStack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Indietro"
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp)
            ) {
                if (isEditing) {
                    FloatingActionButton(
                        onClick = { showAddDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.align(Alignment.BottomStart)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Aggiungi esercizio"
                        )
                    }
                }
                FloatingActionButton(
                    onClick = { isEditing = !isEditing },
                    containerColor = if (isEditing) MaterialTheme.colorScheme.primary else Color(0xFF9E9E9E),
                    contentColor = if (isEditing) MaterialTheme.colorScheme.onPrimary else Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = if (isEditing) 6.dp else 2.dp,
                        pressedElevation = if (isEditing) 8.dp else 4.dp
                    ),
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Icon(
                        imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                        contentDescription = if (isEditing) "Conferma modifica" else "Attiva modifica"
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Barra di ricerca con icona filtro a destra
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth(),
                placeholder = { Text("Inserisci nome esercizio") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cerca") },
                trailingIcon = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filtri",
                            tint = if (isFilterActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            // Chip per filtri attivi
            if (isFilterActive) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedMuscleGroup != "Tutti") {
                        AssistChip(
                            onClick = { selectedMuscleGroup = "Tutti" },
                            label = { Text("Muscolo: $selectedMuscleGroup") },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Rimuovi filtro",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                    if (selectedEquipment != "Tutti") {
                        AssistChip(
                            onClick = { selectedEquipment = "Tutti" },
                            label = { Text("Strumento: $selectedEquipment") },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Rimuovi filtro",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                    TextButton(onClick = {
                        selectedMuscleGroup = "Tutti"
                        selectedEquipment = "Tutti"
                    }) {
                        Text("Resetta", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (uiItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nessun esercizio trovato.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        items = uiItems,
                        key = { item ->
                            when (item) {
                                is ExerciseUiItem.GroupHeader -> "group_${item.muscleGroup}"
                                is ExerciseUiItem.EquipmentHeader -> "equip_${item.muscleGroup}_${item.equipment}"
                                is ExerciseUiItem.ExerciseCard -> item.exercise.id
                            }
                        }
                    ) { item ->
                        when (item) {
                            is ExerciseUiItem.GroupHeader -> {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = item.muscleGroup,
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                )
                            }
                            is ExerciseUiItem.EquipmentHeader -> {
                                Text(
                                    text = item.equipment,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp, bottom = 6.dp)
                                )
                            }
                            is ExerciseUiItem.ExerciseCard -> {
                                ExerciseFrameCard(
                                    exercise = item.exercise,
                                    isEditing = isEditing,
                                    onClick = if (onSelectExercise != null) {
                                        { onSelectExercise.invoke(item.exercise, 3, 10) }
                                    } else null,
                                    onDelete = {
                                        val updated = customExercises.filterNot { it.id == item.exercise.id }
                                        customExercises = updated
                                        ExerciseManager.saveCustomExercises(context, updated)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Dialog Filtri
        if (showFilterDialog) {
            FilterDialog(
                currentGroup = selectedMuscleGroup,
                currentEquip = selectedEquipment,
                onDismiss = { showFilterDialog = false },
                onApply = { group, equip ->
                    selectedMuscleGroup = group
                    selectedEquipment = equip
                    showFilterDialog = false
                }
            )
        }

        // Dialog per aggiungere un nuovo esercizio
        if (showAddDialog) {
            AddExerciseDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { name, group, equip ->
                    val newExercise = ExerciseItem(
                        name = name,
                        muscleGroup = group,
                        equipment = equip,
                        isCustom = true
                    )
                    val updated = customExercises + newExercise
                    customExercises = updated
                    ExerciseManager.saveCustomExercises(context, updated)
                    showAddDialog = false
                }
            )
        }
    }
}

// Card Esercizio con cornice sottile, ombra e compatibilità tema chiaro/scuro
@Composable
fun ExerciseFrameCard(
    exercise: ExerciseItem,
    isEditing: Boolean = false,
    onClick: (() -> Unit)? = null,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            // Mostra il pulsante di eliminazione SOLO per gli esercizi aggiunti manualmente dall'utente quando in modalità modifica
            if (isEditing && exercise.isCustom) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Elimina esercizio",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                // Riserva uno spazio vuoto identico di 32.dp per garantire una dimensione verticale sempre uniforme
                Spacer(modifier = Modifier.size(32.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDialog(
    currentGroup: String,
    currentEquip: String,
    onDismiss: () -> Unit,
    onApply: (group: String, equip: String) -> Unit
) {
    var tempGroup by remember { mutableStateOf(currentGroup) }
    var tempEquip by remember { mutableStateOf(currentEquip) }

    var groupExpanded by remember { mutableStateOf(false) }
    var equipExpanded by remember { mutableStateOf(false) }

    val allGroups = listOf("Tutti") + MUSCLE_GROUPS
    val allEquip = listOf("Tutti") + EQUIPMENT_TYPES

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = "Filtra Esercizi",
        confirmButtonText = "Applica",
        onConfirm = {
            onApply(tempGroup, tempEquip)
        },
        dismissButtonText = "Indietro",
        onDismiss = onDismiss
    ) {
        // Dropdown Gruppo Muscolare
        ExposedDropdownMenuBox(
            expanded = groupExpanded,
            onExpandedChange = { groupExpanded = !groupExpanded }
        ) {
            OutlinedTextField(
                value = tempGroup,
                onValueChange = {},
                readOnly = true,
                label = { Text("Gruppo Muscolare") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
            )
            ExposedDropdownMenu(
                expanded = groupExpanded,
                onDismissRequest = { groupExpanded = false }
            ) {
                allGroups.forEach { group ->
                    DropdownMenuItem(
                        text = { Text(group) },
                        onClick = {
                            tempGroup = group
                            groupExpanded = false
                        }
                    )
                }
            }
        }

        // Dropdown Strumento
        ExposedDropdownMenuBox(
            expanded = equipExpanded,
            onExpandedChange = { equipExpanded = !equipExpanded }
        ) {
            OutlinedTextField(
                value = tempEquip,
                onValueChange = {},
                readOnly = true,
                label = { Text("Strumento") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = equipExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
            )
            ExposedDropdownMenu(
                expanded = equipExpanded,
                onDismissRequest = { equipExpanded = false }
            ) {
                allEquip.forEach { equip ->
                    DropdownMenuItem(
                        text = { Text(equip) },
                        onClick = {
                            tempEquip = equip
                            equipExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExerciseDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, muscleGroup: String, equipment: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf(MUSCLE_GROUPS[0]) }
    var selectedEquip by remember { mutableStateOf(EQUIPMENT_TYPES[0]) }

    var groupExpanded by remember { mutableStateOf(false) }
    var equipExpanded by remember { mutableStateOf(false) }

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = "Nuovo Esercizio",
        confirmButtonText = "Aggiungi",
        onConfirm = {
            if (name.isNotBlank()) {
                onAdd(name.trim(), selectedGroup, selectedEquip)
            }
        },
        dismissButtonText = "Annulla",
        onDismiss = onDismiss,
        confirmEnabled = name.isNotBlank()
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nome esercizio") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Dropdown Gruppo Muscolare
        ExposedDropdownMenuBox(
            expanded = groupExpanded,
            onExpandedChange = { groupExpanded = !groupExpanded }
        ) {
            OutlinedTextField(
                value = selectedGroup,
                onValueChange = {},
                readOnly = true,
                label = { Text("Gruppo Muscolare") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
            )
            ExposedDropdownMenu(
                expanded = groupExpanded,
                onDismissRequest = { groupExpanded = false }
            ) {
                MUSCLE_GROUPS.forEach { group ->
                    DropdownMenuItem(
                        text = { Text(group) },
                        onClick = {
                            selectedGroup = group
                            groupExpanded = false
                        }
                    )
                }
            }
        }

        // Dropdown Strumento
        ExposedDropdownMenuBox(
            expanded = equipExpanded,
            onExpandedChange = { equipExpanded = !equipExpanded }
        ) {
            OutlinedTextField(
                value = selectedEquip,
                onValueChange = {},
                readOnly = true,
                label = { Text("Strumento") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = equipExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
            )
            ExposedDropdownMenu(
                expanded = equipExpanded,
                onDismissRequest = { equipExpanded = false }
            ) {
                EQUIPMENT_TYPES.forEach { equip ->
                    DropdownMenuItem(
                        text = { Text(equip) },
                        onClick = {
                            selectedEquip = equip
                            equipExpanded = false
                        }
                    )
                }
            }
        }
    }
}
