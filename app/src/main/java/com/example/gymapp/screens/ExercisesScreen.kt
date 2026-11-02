package com.example.gymapp.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

// Definizione delle categorie
val MUSCLE_GROUPS = listOf("Bicipiti", "Tricipiti", "Petto", "Schiena", "Gambe", "Spalle")
val EQUIPMENT_TYPES = listOf("Bilanciere", "Manubri", "Cavi", "Macchinari")

// Modello dati per il singolo esercizio
data class ExerciseItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val muscleGroup: String,
    val equipment: String,
    val isCustom: Boolean = false,
)

// Lista di esercizi hardcoded di default
val DEFAULT_EXERCISES = listOf(
    // Bicipiti
    ExerciseItem(id = "def_1", name = "Arm curl alla macchina", muscleGroup = "Bicipiti", equipment = "Macchinari", isCustom = false),
    ExerciseItem(id = "def_2", name = "Curl ai cavi bassi", muscleGroup = "Bicipiti", equipment = "Cavi", isCustom = false),
    ExerciseItem(id = "def_3", name = "Curl alternato con manubri", muscleGroup = "Bicipiti", equipment = "Manubri", isCustom = false),
    ExerciseItem(id = "def_4", name = "Curl con bilanciere", muscleGroup = "Bicipiti", equipment = "Bilanciere", isCustom = false),

    // Tricipiti
    ExerciseItem(id = "def_5", name = "Estensioni manubrio sopra la testa", muscleGroup = "Tricipiti", equipment = "Manubri", isCustom = false),
    ExerciseItem(id = "def_6", name = "French press con bilanciere", muscleGroup = "Tricipiti", equipment = "Bilanciere", isCustom = false),
    ExerciseItem(id = "def_7", name = "Pushdown ai cavi", muscleGroup = "Tricipiti", equipment = "Cavi", isCustom = false),
    ExerciseItem(id = "def_8", name = "Tricipiti alla macchina", muscleGroup = "Tricipiti", equipment = "Macchinari", isCustom = false),

    // Petto
    ExerciseItem(id = "def_9", name = "Chest press", muscleGroup = "Petto", equipment = "Macchinari", isCustom = false),
    ExerciseItem(id = "def_10", name = "Croci ai cavi alti", muscleGroup = "Petto", equipment = "Cavi", isCustom = false),
    ExerciseItem(id = "def_11", name = "Panca piana con bilanciere", muscleGroup = "Petto", equipment = "Bilanciere", isCustom = false),
    ExerciseItem(id = "def_12", name = "Spinte su panca inclinata con manubri", muscleGroup = "Petto", equipment = "Manubri", isCustom = false),

    // Schiena
    ExerciseItem(id = "def_13", name = "Lat machine avanti", muscleGroup = "Schiena", equipment = "Cavi", isCustom = false),
    ExerciseItem(id = "def_14", name = "Pulley basso", muscleGroup = "Schiena", equipment = "Macchinari", isCustom = false),
    ExerciseItem(id = "def_15", name = "Rematore con bilanciere", muscleGroup = "Schiena", equipment = "Bilanciere", isCustom = false),
    ExerciseItem(id = "def_16", name = "Rematore mono-braccio con manubrio", muscleGroup = "Schiena", equipment = "Manubri", isCustom = false),

    // Gambe
    ExerciseItem(id = "def_17", name = "Affondi con manubri", muscleGroup = "Gambe", equipment = "Manubri", isCustom = false),
    ExerciseItem(id = "def_18", name = "Cable pull through", muscleGroup = "Gambe", equipment = "Cavi", isCustom = false),
    ExerciseItem(id = "def_19", name = "Leg press", muscleGroup = "Gambe", equipment = "Macchinari", isCustom = false),
    ExerciseItem(id = "def_20", name = "Squat con bilanciere", muscleGroup = "Gambe", equipment = "Bilanciere", isCustom = false),

    // Spalle
    ExerciseItem(id = "def_21", name = "Alzate laterali ai cavi", muscleGroup = "Spalle", equipment = "Cavi", isCustom = false),
    ExerciseItem(id = "def_22", name = "Alzate laterali con manubri", muscleGroup = "Spalle", equipment = "Manubri", isCustom = false),
    ExerciseItem(id = "def_23", name = "Lento avanti con bilanciere", muscleGroup = "Spalle", equipment = "Bilanciere", isCustom = false),
    ExerciseItem(id = "def_24", name = "Shoulder press", muscleGroup = "Spalle", equipment = "Macchinari", isCustom = false)
)

sealed class ExerciseUiItem {
    data class GroupHeader(val muscleGroup: String) : ExerciseUiItem()
    data class EquipmentHeader(val equipment: String) : ExerciseUiItem()
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
    val coroutineScope = rememberCoroutineScope()

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
                list.add(ExerciseUiItem.EquipmentHeader(equip))
                equipExercises.sortedBy { it.name }.forEach { exercise ->
                    list.add(ExerciseUiItem.ExerciseCard(exercise))
                }
            }
        }
        list
    }

    val listState = rememberLazyListState()

    // Determina la lettera attiva nell'alfabeto in base al primo elemento visibile
    val firstVisibleIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    val activeLetter by remember(firstVisibleIndex, uiItems) {
        derivedStateOf {
            var letter: Char? = null
            for (i in firstVisibleIndex until uiItems.size) {
                val item = uiItems.getOrNull(i)
                if (item is ExerciseUiItem.ExerciseCard) {
                    letter = item.exercise.name.firstOrNull()?.uppercaseChar()
                    break
                }
            }
            letter
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
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
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi esercizio")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
        ) {
            // Barra di ricerca con icona filtro a destra
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 12.dp),
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
                    modifier = Modifier.padding(end = 12.dp),
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
                Row(modifier = Modifier.fillMaxSize()) {
                    // Lista principale a sinistra
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentPadding = PaddingValues(end = 8.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(uiItems) { _, item ->
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

                    // Colonna alfabetica verticale scorrevole a destra (A...Z)
                    AlphabetIndexColumn(
                        activeLetter = activeLetter,
                        onLetterSelected = { letter ->
                            val targetIndex = uiItems.indexOfFirst { uiItem ->
                                when (uiItem) {
                                    is ExerciseUiItem.ExerciseCard -> uiItem.exercise.name.startsWith(letter, ignoreCase = true)
                                    else -> false
                                }
                            }
                            if (targetIndex >= 0) {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(targetIndex)
                                }
                            }
                        },
                        modifier = Modifier.width(22.dp)
                    )
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
    onClick: (() -> Unit)? = null,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            ),
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

            // Mostra il pulsante di eliminazione SOLO per gli esercizi aggiunti manualmente dall'utente
            if (exercise.isCustom) {
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

// Colonna alfabetica A...Z a destra con evidenziazione della lettera attiva
@Composable
fun AlphabetIndexColumn(
    activeLetter: Char?,
    onLetterSelected: (Char) -> Unit,
    modifier: Modifier = Modifier
) {
    val alphabet = ('A'..'Z').toList()

    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        alphabet.forEach { letter ->
            val isSelected = activeLetter == letter
            Text(
                text = letter.toString(),
                fontSize = if (isSelected) 12.sp else 9.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .clickable { onLetterSelected(letter) }
                    .padding(vertical = 0.5.dp)
            )
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filtra Esercizi") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
        },
        confirmButton = {
            Button(
                onClick = {
                    onApply(tempGroup, tempEquip)
                }
            ) {
                Text("Applica")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Indietro")
            }
        }
    )
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuovo Esercizio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onAdd(name.trim(), selectedGroup, selectedEquip)
                    }
                }
            ) {
                Text("Aggiungi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}