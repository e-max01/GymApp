package com.example.gymapp.screens

import com.example.gymapp.components.AppAlertDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import kotlinx.coroutines.launch

// LIVELLO 3: Schermata Dettaglio Scheda Giornaliera

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyPlanDetailScreen(
    dailyPlan: DailyWorkoutPlan,
    onBackClick: () -> Unit,
    onUpdateExercises: (List<ExerciseSetInfo>) -> Unit
) {
    val density = LocalDensity.current

    // Stato per attivare la schermata di allenamento
    var isActiveWorkoutRunning by remember { mutableStateOf(false) }

    // Se la scheda è vuota, la modalità modifica si attiva automaticamente
    var isEditing by remember(dailyPlan.id) {
        mutableStateOf(dailyPlan.exercises.isEmpty())
    }
    var isPickingExercise by remember { mutableStateOf(false) }

    // Serie da modificare: exerciseId, numero serie, SetDetail
    var setToEditInfo by remember { mutableStateOf<Triple<String, Int, SetDetail>?>(null) }
    // Esercizio a cui aggiungere serie: ExerciseSetInfo
    var exerciseToAddSetsFor by remember { mutableStateOf<ExerciseSetInfo?>(null) }

    val listState = rememberLazyListState()

    val scope = rememberCoroutineScope()

    // ---------------------------------------------------------------
    // STATO DEL TRASCINAMENTO
    // Tutti gli stati sono creati con remember{} SENZA chiavi: così i lambda
    // dei gesti (che vivono a lungo) puntano sempre allo stesso oggetto State.
    // ---------------------------------------------------------------
    // Esercizi
    var draggedExerciseId by remember { mutableStateOf<String?>(null) }
    var draggedExerciseOffset by remember { mutableFloatStateOf(0f) }

    // Serie
    var draggedSetExerciseId by remember { mutableStateOf<String?>(null) }
    var draggedSetId by remember { mutableStateOf<String?>(null) }
    var draggedSetOffset by remember { mutableFloatStateOf(0f) }        // offset visivo rispetto allo slot corrente
    var draggedSetTotalDrag by remember { mutableFloatStateOf(0f) }     // spostamento totale del dito (non viene mai "compensato")
    var draggedSetStep by remember { mutableFloatStateOf(0f) }          // altezza reale della riga trascinata
    var draggedSetStartCenterY by remember { mutableFloatStateOf(0f) }  // centro della riga (coordinate root) a inizio drag
    var trashBounds by remember { mutableStateOf<Rect?>(null) }         // area del pulsante rosso (coordinate root)
    var lastSetDragEventMs by remember { mutableLongStateOf(0L) }       // per ignorare il click dopo un long-press

    // Copia locale della lista: durante il drag si modifica solo questa (fluido, niente
    // salvataggi su SharedPreferences a ogni frame), poi si salva UNA volta a fine gesto.
    val latestOnUpdate by rememberUpdatedState(onUpdateExercises)
    val latestPlanExercises by rememberUpdatedState(dailyPlan.exercises)
    var exercises by remember { mutableStateOf(dailyPlan.exercises) }
    LaunchedEffect(dailyPlan.exercises) {
        if (draggedExerciseId == null && draggedSetId == null) {
            exercises = dailyPlan.exercises
        }
    }

    val edgePx = with(density) { 72.dp.toPx() }              // fascia di autoscroll ai bordi
    val autoScrollStepPx = with(density) { 10.dp.toPx() }    // velocità autoscroll per frame
    val fingerFromTopPx = with(density) { 36.dp.toPx() }     // distanza approssimata dito-bordo alto card (handle)

    fun commit(newList: List<ExerciseSetInfo>) {
        exercises = newList
        if (newList != latestPlanExercises) latestOnUpdate(newList)
    }

    fun isSetOverTrash(): Boolean {
        val bounds = trashBounds ?: return false
        if (draggedSetId == null) return false
        val centerY = draggedSetStartCenterY + draggedSetTotalDrag
        return centerY >= bounds.top
    }

    // ---------------------------------------------------------------
    // RIORDINO ESERCIZI (card di altezza variabile, misurate dal LazyListState)
    // ---------------------------------------------------------------
    fun maybeReorderExercise() {
        val id = draggedExerciseId ?: return
        val list = exercises
        val fromIdx = list.indexOfFirst { it.id == id }
        if (fromIdx == -1) return

        val visible = listState.layoutInfo.visibleItemsInfo
        val dragged = visible.firstOrNull { it.key == id } ?: return
        // Layout non ancora aggiornato dopo l'ultimo scambio: aspetta il frame successivo
        if (dragged.index != fromIdx) return

        val draggedTop = dragged.offset + draggedExerciseOffset
        val draggedBottom = draggedTop + dragged.size

        val below = list.getOrNull(fromIdx + 1)?.let { n ->
            visible.firstOrNull { it.key == n.id && it.index == fromIdx + 1 }
        }
        val above = list.getOrNull(fromIdx - 1)?.let { n ->
            visible.firstOrNull { it.key == n.id && it.index == fromIdx - 1 }
        }

        // 1. Salviamo l'ancora di scorrimento ATTUALE prima di mutare la lista
        val currentFirstIndex = listState.firstVisibleItemIndex
        val currentFirstOffset = listState.firstVisibleItemScrollOffset
        var swapped = false

        if (below != null && draggedBottom > below.offset + below.size / 2f) {
            val gap = below.offset - (dragged.offset + dragged.size)
            exercises = list.toMutableList().apply { add(fromIdx + 1, removeAt(fromIdx)) }
            draggedExerciseOffset -= (below.size + gap)
            swapped = true
        } else if (above != null && draggedTop < above.offset + above.size / 2f) {
            val gap = dragged.offset - (above.offset + above.size)
            exercises = list.toMutableList().apply { add(fromIdx - 1, removeAt(fromIdx)) }
            draggedExerciseOffset += (above.size + gap)
            swapped = true
        }

        // 2. FIX: Se c'è stato uno scambio che ha coinvolto il primo elemento visibile
        // (sia trascinando il primo verso il basso, sia trascinando il secondo verso l'alto)
        if (swapped && (fromIdx == currentFirstIndex || (above != null && above.index == currentFirstIndex))) {
            scope.launch {
                // Blocchiamo il salto di layout dicendo a Compose di rimanere fermo sul nuovo primo elemento
                listState.scrollToItem(currentFirstIndex, currentFirstOffset)
            }
        }
    }

    fun finishExerciseDrag(cancelled: Boolean) {
        if (draggedExerciseId != null) {
            if (cancelled) exercises = latestPlanExercises else commit(exercises)
        }
        draggedExerciseId = null
        draggedExerciseOffset = 0f
    }

    // Autoscroll quando si trascina un esercizio vicino ai bordi della lista
    LaunchedEffect(draggedExerciseId) {
        if (draggedExerciseId == null) return@LaunchedEffect
        while (true) {
            withFrameNanos { }
            val id = draggedExerciseId ?: break
            val info = listState.layoutInfo
            val item = info.visibleItemsInfo.firstOrNull { it.key == id } ?: continue
            val fingerY = item.offset + draggedExerciseOffset + fingerFromTopPx
            val delta = when {
                fingerY < info.viewportStartOffset + edgePx -> -autoScrollStepPx
                fingerY > info.viewportEndOffset - edgePx -> autoScrollStepPx
                else -> 0f
            }
            if (delta != 0f) {
                val consumed = listState.scrollBy(delta)
                draggedExerciseOffset += consumed   // la card resta sotto il dito
                maybeReorderExercise()
            }
        }
    }

    // ---------------------------------------------------------------
    // RIORDINO / ELIMINAZIONE SERIE
    // ---------------------------------------------------------------
    fun maybeReorderSet() {
        val exId = draggedSetExerciseId ?: return
        val sId = draggedSetId ?: return
        val step = draggedSetStep
        if (step <= 0f) return

        val ex = exercises.firstOrNull { it.id == exId } ?: return
        val idx = ex.setsList.indexOfFirst { it.id == sId }
        if (idx == -1) return

        val newIdx = when {
            draggedSetOffset > step / 2f && idx < ex.setsList.lastIndex -> idx + 1
            draggedSetOffset < -step / 2f && idx > 0 -> idx - 1
            else -> return
        }
        val newSets = ex.setsList.toMutableList().apply { add(newIdx, removeAt(idx)) }
        exercises = exercises.map { if (it.id == exId) it.copy(setsList = newSets) else it }
        draggedSetOffset -= (newIdx - idx) * step
    }

    fun finishSetDrag(cancelled: Boolean) {
        val exId = draggedSetExerciseId
        val sId = draggedSetId
        if (exId != null && sId != null) {
            if (cancelled) {
                exercises = latestPlanExercises
            } else {
                val result = if (isSetOverTrash()) {
                    exercises.map { ex ->
                        if (ex.id == exId) ex.copy(setsList = ex.setsList.filterNot { it.id == sId }) else ex
                    }
                } else exercises
                commit(result)
            }
        }
        draggedSetExerciseId = null
        draggedSetId = null
        draggedSetOffset = 0f
        draggedSetTotalDrag = 0f
        draggedSetStep = 0f
        trashBounds = null
        lastSetDragEventMs = System.currentTimeMillis()
    }
    if (isActiveWorkoutRunning) {
        ActiveWorkoutScreen(
            dailyPlanName = dailyPlan.name,
            exercises = exercises,
            onUpdateExercises = { commit(it) },
            onClose = { isActiveWorkoutRunning = false }
        )
    } else if (isPickingExercise) {
        BackHandler {
            isPickingExercise = false
        }
        ExercisesScreen(
            navController = NavController(LocalContext.current),
            onBackClickOverride = {
                isPickingExercise = false
            },
            onSelectExercise = { selectedItem, _, _ ->
                val newExerciseInfo = ExerciseSetInfo(
                    name = selectedItem.name,
                    setsList = emptyList() // Parte vuoto senza serie predefinite
                )
                commit(exercises + newExerciseInfo)
                isPickingExercise = false
            }
        )
    } else {
    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(dailyPlan.name) },
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 32.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (exercises.isEmpty()) {
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
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(48.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FloatingActionButton(
                                onClick = { isActiveWorkoutRunning = true }, // APRE LA NUOVA SCHERMATA
                                containerColor = Color(0xFF4CAF50),
                                contentColor = Color.White,
                                elevation = FloatingActionButtonDefaults.elevation(
                                    defaultElevation = 12.dp,
                                    pressedElevation = 16.dp
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Avvia scheda"
                                )
                            }

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
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    if (exercises.isEmpty() && !isEditing) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nessun esercizio inserito in questa giornata.\nPremi il tasto Modifica in basso a destra per aggiungerne uno.",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
                        ) {
                            itemsIndexed(
                                items = exercises,
                                key = { _, item -> item.id }
                            ) { _, exercise ->
                                val isCardDragged = draggedExerciseId == exercise.id

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .zIndex(if (isCardDragged) 10f else 0f)
                                        .graphicsLayer {
                                            translationY = if (draggedExerciseId == exercise.id) draggedExerciseOffset else 0f
                                        },
                                    shape = MaterialTheme.shapes.medium,
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = if (isCardDragged) 8.dp else 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        // Intestazione: titolo centrato + maniglia / elimina a destra/sinistra
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = exercise.name,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.Center
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (isEditing) {
                                                    // Il drag dell'esercizio parte SOLO da qui: niente più conflitto
                                                    // con il long-press delle serie né con lo scroll della lista.
                                                    Box(
                                                        modifier = Modifier
                                                            .size(40.dp)
                                                            .pointerInput(exercise.id) {
                                                                detectDragGestures(
                                                                    onDragStart = {
                                                                        draggedExerciseId = exercise.id
                                                                        draggedExerciseOffset = 0f
                                                                    },
                                                                    onDrag = { change, dragAmount ->
                                                                        if (draggedExerciseId != exercise.id) return@detectDragGestures
                                                                        change.consume()
                                                                        draggedExerciseOffset += dragAmount.y
                                                                        maybeReorderExercise()
                                                                    },
                                                                    onDragEnd = { finishExerciseDrag(cancelled = false) },
                                                                    onDragCancel = { finishExerciseDrag(cancelled = true) }
                                                                )
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.DragHandle,
                                                            contentDescription = "Trascina per riordinare",
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                } else {
                                                    Spacer(modifier = Modifier.size(40.dp))
                                                }

                                                if (isEditing) {
                                                    IconButton(
                                                        onClick = {
                                                            commit(exercises.filterNot { it.id == exercise.id })
                                                        },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Rimuovi esercizio",
                                                            tint = MaterialTheme.colorScheme.error
                                                        )
                                                    }
                                                } else {
                                                    Spacer(modifier = Modifier.size(32.dp))
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Elenco delle serie (senza enumerazione, con reps e kg allineati in colonna)
                                        exercise.setsList.forEachIndexed { setIndex, setItem ->
                                            // key(): lo stato (e il gesto in corso) segue la serie, non lo slot.
                                            // Senza, dopo il primo scambio il gesto si interrompeva.
                                            key(setItem.id) {
                                                val isThisSetDragged = draggedSetId == setItem.id
                                                var rowCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

                                                var rowModifier = Modifier
                                                    .fillMaxWidth()
                                                    .onGloballyPositioned { rowCoords = it }
                                                    .zIndex(if (isThisSetDragged) 10f else 0f)
                                                    .graphicsLayer {
                                                        translationY = if (draggedSetId == setItem.id) draggedSetOffset else 0f
                                                    }

                                                if (isEditing) {
                                                    rowModifier = rowModifier
                                                        .pointerInput(setItem.id) {
                                                            detectDragGesturesAfterLongPress(
                                                                onDragStart = {
                                                                    val coords = rowCoords
                                                                    if (coords != null && coords.isAttached) {
                                                                        draggedSetStep = coords.size.height.toFloat()
                                                                        draggedSetStartCenterY =
                                                                            coords.positionInRoot().y + coords.size.height / 2f
                                                                        draggedSetTotalDrag = 0f
                                                                        draggedSetOffset = 0f
                                                                        draggedSetExerciseId = exercise.id
                                                                        draggedSetId = setItem.id
                                                                        lastSetDragEventMs = System.currentTimeMillis()
                                                                    }
                                                                },
                                                                onDrag = { change, dragAmount ->
                                                                    if (draggedSetId != setItem.id) return@detectDragGesturesAfterLongPress
                                                                    change.consume()
                                                                    draggedSetOffset += dragAmount.y
                                                                    draggedSetTotalDrag += dragAmount.y
                                                                    maybeReorderSet()
                                                                },
                                                                onDragEnd = { finishSetDrag(cancelled = false) },
                                                                onDragCancel = { finishSetDrag(cancelled = true) }
                                                            )
                                                        }
                                                        .clickable {
                                                            // Ignora il click "fantasma" al rilascio di un long-press/drag
                                                            val recentlyDragged =
                                                                draggedSetId != null ||
                                                                        System.currentTimeMillis() - lastSetDragEventMs < 300L
                                                            if (!recentlyDragged) {
                                                                setToEditInfo = Triple(exercise.id, setIndex + 1, setItem)
                                                            }
                                                        }
                                                }

                                                Box(modifier = rowModifier) {
                                                    Surface(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 4.dp),
                                                        shape = MaterialTheme.shapes.small,
                                                        color = MaterialTheme.colorScheme.surface,
                                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                                                    ) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(horizontal = 16.dp, vertical = 10.dp),
                                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            val weightFormatted = if (setItem.weightKg % 1f == 0f) {
                                                                setItem.weightKg.toInt().toString()
                                                            } else {
                                                                setItem.weightKg.toString()
                                                            }

                                                            Box(
                                                                modifier = Modifier.weight(1f),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(
                                                                    text = buildAnnotatedString {
                                                                        withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                                                                            append(setItem.reps.toString())
                                                                        }
                                                                        append(" reps")
                                                                    },
                                                                    style = MaterialTheme.typography.bodyLarge,
                                                                    color = MaterialTheme.colorScheme.onSurface,
                                                                    textAlign = TextAlign.Center
                                                                )
                                                            }

                                                            Box(
                                                                modifier = Modifier.weight(1f),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(
                                                                    text = buildAnnotatedString {
                                                                        withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                                                                            append(weightFormatted)
                                                                        }
                                                                        append(" kg")
                                                                    },
                                                                    style = MaterialTheme.typography.bodyLarge,
                                                                    color = MaterialTheme.colorScheme.onSurface,
                                                                    textAlign = TextAlign.Center
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(40.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isEditing) {
                                                val isDraggingSetInThisExercise = draggedSetId != null && draggedSetExerciseId == exercise.id

                                                if (isDraggingSetInThisExercise) {
                                                    val overTrash = isSetOverTrash()
                                                    Button(
                                                        onClick = {},
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .onGloballyPositioned { c ->
                                                                val p = c.positionInRoot()
                                                                trashBounds = Rect(
                                                                    p.x, p.y,
                                                                    p.x + c.size.width, p.y + c.size.height
                                                                )
                                                            },
                                                        shape = MaterialTheme.shapes.small,
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = if (overTrash) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.errorContainer,
                                                            contentColor = if (overTrash) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onErrorContainer
                                                        ),
                                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Elimina serie",
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = if (overTrash) "Rilascia per eliminare serie" else "Trascina qui per eliminare serie",
                                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                                        )
                                                    }
                                                } else {
                                                    OutlinedButton(
                                                        onClick = {
                                                            exerciseToAddSetsFor = exercise
                                                        },
                                                        modifier = Modifier.fillMaxSize(),
                                                        shape = MaterialTheme.shapes.small,
                                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Add,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("Aggiungi serie", style = MaterialTheme.typography.bodyMedium)
                                                    }
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.fillMaxSize())
                                            }
                                        }
                                    }
                                }
                            }

                            // Pulsante largo "+ Aggiungi esercizio" in fondo quando in modalità modifica
                            if (isEditing) {
                                item(key = "add_exercise_button") {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = { isPickingExercise = true },
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
                                            text = "Aggiungi esercizio",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dialog per modificare reps e kg di una riga di serie
        setToEditInfo?.let { (exId, _, setItem) ->
            EditSetValuesDialog(
                setDetail = setItem,
                onDismiss = { setToEditInfo = null },
                onConfirm = { newReps, newWeight ->
                    commit(
                        exercises.map { ex ->
                            if (ex.id == exId) {
                                ex.copy(
                                    setsList = ex.setsList.map { s ->
                                        if (s.id == setItem.id) s.copy(reps = newReps, weightKg = newWeight) else s
                                    }
                                )
                            } else ex
                        }
                    )
                    setToEditInfo = null
                }
            )
        }

        // Dialog per aggiungere una o più serie ad un esercizio
        exerciseToAddSetsFor?.let { exercise ->
            AddSetsDialog(
                exerciseName = exercise.name,
                defaultReps = exercise.setsList.lastOrNull()?.reps ?: 8,
                defaultWeightKg = exercise.setsList.lastOrNull()?.weightKg ?: 0f,
                onDismiss = { exerciseToAddSetsFor = null },
                onConfirm = { countToAdd, reps, weight ->
                    val newSets = List(countToAdd) { SetDetail(reps = reps, weightKg = weight) }
                    commit(
                        exercises.map { ex ->
                            if (ex.id == exercise.id) {
                                ex.copy(setsList = ex.setsList + newSets)
                            } else ex
                        }
                    )
                    exerciseToAddSetsFor = null
                }
            )
        }
    }
}


@Composable
fun EditSetValuesDialog(
    setDetail: SetDetail,
    onDismiss: () -> Unit,
    onConfirm: (Int, Float) -> Unit
) {
    val initialRepsText = remember(setDetail) { setDetail.reps.toString() }
    val initialWeightText = remember(setDetail) {
        if (setDetail.weightKg % 1f == 0f) {
            setDetail.weightKg.toInt().toString()
        } else {
            setDetail.weightKg.toString()
        }
    }

    var repsTextFieldValue by remember(setDetail) {
        mutableStateOf(
            TextFieldValue(
                text = initialRepsText,
                selection = TextRange(initialRepsText.length)
            )
        )
    }

    var weightTextFieldValue by remember(setDetail) {
        mutableStateOf(
            TextFieldValue(
                text = initialWeightText,
                selection = TextRange(initialWeightText.length)
            )
        )
    }

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = "Modifica Serie",
        confirmButtonText = "Salva",
        onConfirm = {
            val reps = repsTextFieldValue.text.toIntOrNull() ?: setDetail.reps
            val weightCleaned = weightTextFieldValue.text.replace(',', '.')
            val weight = weightCleaned.toFloatOrNull() ?: setDetail.weightKg
            onConfirm(reps, weight)
        },
        dismissButtonText = "Annulla",
        onDismiss = onDismiss
    ) {
        OutlinedTextField(
            value = repsTextFieldValue,
            onValueChange = { repsTextFieldValue = it },
            label = { Text("Ripetizioni (reps)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        repsTextFieldValue = repsTextFieldValue.copy(
                            selection = TextRange(0, repsTextFieldValue.text.length)
                        )
                    }
                }
        )
        OutlinedTextField(
            value = weightTextFieldValue,
            onValueChange = { weightTextFieldValue = it },
            label = { Text("Peso (kg)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        weightTextFieldValue = weightTextFieldValue.copy(
                            selection = TextRange(0, weightTextFieldValue.text.length)
                        )
                    }
                }
        )
    }
}


@Composable
fun AddSetsDialog(
    exerciseName: String,
    defaultReps: Int,
    defaultWeightKg: Float,
    onDismiss: () -> Unit,
    onConfirm: (count: Int, reps: Int, weight: Float) -> Unit
) {
    val initialCountText = "1"
    val initialRepsText = remember(defaultReps) { defaultReps.toString() }
    val initialWeightText = remember(defaultWeightKg) {
        if (defaultWeightKg % 1f == 0f) {
            defaultWeightKg.toInt().toString()
        } else {
            defaultWeightKg.toString()
        }
    }

    var countTextFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialCountText,
                selection = TextRange(initialCountText.length)
            )
        )
    }

    var repsTextFieldValue by remember(defaultReps) {
        mutableStateOf(
            TextFieldValue(
                text = initialRepsText,
                selection = TextRange(initialRepsText.length)
            )
        )
    }

    var weightTextFieldValue by remember(defaultWeightKg) {
        mutableStateOf(
            TextFieldValue(
                text = initialWeightText,
                selection = TextRange(initialWeightText.length)
            )
        )
    }

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = "Aggiungi Serie",
        confirmButtonText = "Aggiungi",
        onConfirm = {
            val count = countTextFieldValue.text.toIntOrNull()?.coerceAtLeast(1) ?: 1
            val reps = repsTextFieldValue.text.toIntOrNull() ?: defaultReps
            val weightCleaned = weightTextFieldValue.text.replace(',', '.')
            val weight = weightCleaned.toFloatOrNull() ?: defaultWeightKg
            onConfirm(count, reps, weight)
        },
        dismissButtonText = "Annulla",
        onDismiss = onDismiss
    ) {
        Text(
            text = exerciseName,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        OutlinedTextField(
            value = countTextFieldValue,
            onValueChange = { countTextFieldValue = it },
            label = { Text("Numero di serie da aggiungere") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        countTextFieldValue = countTextFieldValue.copy(
                            selection = TextRange(0, countTextFieldValue.text.length)
                        )
                    }
                }
        )
        OutlinedTextField(
            value = repsTextFieldValue,
            onValueChange = { repsTextFieldValue = it },
            label = { Text("Ripetizioni (reps)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        repsTextFieldValue = repsTextFieldValue.copy(
                            selection = TextRange(0, repsTextFieldValue.text.length)
                        )
                    }
                }
        )
        OutlinedTextField(
            value = weightTextFieldValue,
            onValueChange = { weightTextFieldValue = it },
            label = { Text("Peso (kg)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        weightTextFieldValue = weightTextFieldValue.copy(
                            selection = TextRange(0, weightTextFieldValue.text.length)
                        )
                    }
                }
        )
    }
}
