package com.example.gymapp.screens

import com.example.gymapp.components.AppAlertDialog
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

private var activeMediaPlayer: MediaPlayer? = null
private var activeVibrator: Vibrator? = null
private var backgroundTimerHandler: Handler = Handler(Looper.getMainLooper())
private var backgroundTimerRunnable: Runnable? = null
private var wakeLock: PowerManager.WakeLock? = null
@Volatile private var isAppInForeground = true

private fun stopRinging() {
    try {
        activeVibrator?.cancel()
        activeVibrator = null
    } catch (_: Exception) {}

    try {
        activeMediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        activeMediaPlayer = null
    } catch (_: Exception) {}
}

fun stopContinuousFeedback() {
    cancelBackgroundTimer()
    stopRinging()
}
private fun scheduleBackgroundFinish(appContext: Context, endTimeMs: Long, vibration: Boolean, soundUriString: String) {
    cancelBackgroundTimer()
    val delay = endTimeMs - System.currentTimeMillis()
    if (delay <= 0) return

    val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
    try {
        @Suppress("DEPRECATION")
        wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GymApp:RestTimer")?.apply {
            acquire(delay + 5000L)
        }
    } catch (_: Exception) {}

    backgroundTimerRunnable = Runnable {
        triggerTimerFinishedFeedback(appContext, vibration, soundUriString)
        releaseWakeLock()
    }
    backgroundTimerHandler.postDelayed(backgroundTimerRunnable!!, delay)
}

private fun cancelBackgroundTimer() {
    backgroundTimerRunnable?.let {
        backgroundTimerHandler.removeCallbacks(it)
        backgroundTimerRunnable = null
    }
    releaseWakeLock()
}

private fun releaseWakeLock() {
    try {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        wakeLock = null
    } catch (_: Exception) {}
}

private fun getRingtoneTitle(appContext: Context, soundUriString: String): String {
    if (soundUriString == "none" || soundUriString.isBlank()) {
        return "Nessuno"
    }
    return try {
        val ringtone = RingtoneManager.getRingtone(appContext, soundUriString.toUri())
        ringtone?.getTitle(appContext) ?: "Nessuno"
    } catch (_: Exception) {
        "Nessuno"
    }
}

private fun triggerTimerFinishedFeedback(appContext: Context, vibration: Boolean, soundUriString: String) {
    try {
        activeVibrator?.cancel()
        activeVibrator = null
    } catch (_: Exception) {}

    try {
        activeMediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        activeMediaPlayer = null
    } catch (_: Exception) {}

    val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
    val isInteractive = powerManager?.isInteractive ?: true

    val audioAttributes = AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .setUsage(AudioAttributes.USAGE_ALARM)
        .build()

    if (isInteractive && isAppInForeground) {
        // Schermo acceso e app in primo piano: feedback di durata standard
        if (vibration) {
            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vibratorManager.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    appContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }
                if (vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(VibrationEffect.createOneShot(500L, VibrationEffect.DEFAULT_AMPLITUDE), audioAttributes)
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(500L)
                    }
                }
            } catch (_: Exception) {}
        }

        if (soundUriString != "none" && soundUriString.isNotBlank()) {
            try {
                val ringtone = RingtoneManager.getRingtone(appContext, soundUriString.toUri())
                ringtone?.play()
            } catch (_: Exception) {}
        }
    } else {
        // Schermo spento / telefono bloccato: feedback continuo a oltranza
        if (vibration) {
            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vibratorManager.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    appContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }
                if (vibrator.hasVibrator()) {
                    activeVibrator = vibrator
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(
                            VibrationEffect.createWaveform(longArrayOf(0, 800, 800), 0),
                            audioAttributes
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(longArrayOf(0, 800, 800), 0)
                    }
                }
            } catch (_: Exception) {}
        }

        if (soundUriString != "none" && soundUriString.isNotBlank()) {
            try {
                val mediaPlayer = MediaPlayer().apply {
                    setDataSource(appContext, soundUriString.toUri())
                    setAudioAttributes(audioAttributes)
                    isLooping = true
                    prepare()
                    start()
                }
                activeMediaPlayer = mediaPlayer
            } catch (_: Exception) {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    dailyPlanName: String,
    exercises: List<ExerciseSetInfo>,
    onUpdateExercises: (List<ExerciseSetInfo>) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val sharedPreferences = remember { context.getSharedPreferences("GymAppPrefs", Context.MODE_PRIVATE) }
    var savedRestDurationMs by remember { mutableLongStateOf(sharedPreferences.getLong("rest_timer_ms", 60_000L)) }
    var isVibrationEnabled by remember { mutableStateOf(sharedPreferences.getBoolean("vibration_enabled", true)) }
    var soundUriString by remember { mutableStateOf(sharedPreferences.getString("sound_uri", "none") ?: "none") }

    // Ascolta la riaccensione dello schermo per fermare la vibrazione/suono continui
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_ON -> stopContinuousFeedback()
                    Intent.ACTION_SCREEN_OFF -> stopRinging() // NON cancella il timer
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        context.registerReceiver(receiver, filter)

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
            stopContinuousFeedback()
        }
    }

    // Ascolta il ritorno dell'app in primo piano per fermare il suono e la vibrazione
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    isAppInForeground = true
                    stopContinuousFeedback()
                }
                Lifecycle.Event.ON_RESUME -> stopContinuousFeedback()
                Lifecycle.Event.ON_STOP -> isAppInForeground = false
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val soundPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            val uriStr = uri?.toString() ?: "none"
            sharedPreferences.edit { putString("sound_uri", uriStr) }
            soundUriString = uriStr
        }
    }

    // Appiattiamo la lista per navigare facilmente avanti e indietro tra le serie
    data class FlatSet(val exerciseIndex: Int, val setIndex: Int, val exercise: ExerciseSetInfo, val setDetail: SetDetail)

    val flatSets = remember(exercises) {
        exercises.flatMapIndexed { eIdx, ex ->
            ex.setsList.mapIndexed { sIdx, set -> FlatSet(eIdx, sIdx, ex, set) }
        }
    }

    val startNow = remember { System.currentTimeMillis() }
    var initialDurationMs by remember { mutableLongStateOf(3_000L) }
    var endTimeMs by remember { mutableLongStateOf(startNow + 3_000L) }
    var currentTimeMs by remember { mutableLongStateOf(startNow) }
    var isTimerRunning by remember { mutableStateOf(true) }

    var currentGlobalSetIndex by remember { mutableIntStateOf(0) }
    var showOverview by remember { mutableStateOf(false) }
    var showTimerSelectionDialog by remember { mutableStateOf(false) }
    var setToEdit by remember { mutableStateOf<FlatSet?>(null) }
    var showExitConfirmation by remember { mutableStateOf(false) }

    BackHandler {
        showExitConfirmation = true
    }

    val timeLeftMs = if (isTimerRunning) maxOf(0L, endTimeMs - currentTimeMs) else 0L

    val adjustTime: (Long) -> Unit = { deltaMs ->
        val nowTime = System.currentTimeMillis()
        val currentRemaining = maxOf(0L, endTimeMs - nowTime)
        val newRemaining = currentRemaining + deltaMs
        if (newRemaining <= 0L) {
            endTimeMs = nowTime
            currentTimeMs = nowTime
            isTimerRunning = false
        } else {
            endTimeMs = nowTime + newRemaining
            currentTimeMs = nowTime
            if (deltaMs > 0) {
                initialDurationMs = maxOf(initialDurationMs, newRemaining)
            }
            isTimerRunning = true
        }
    }

    // Gestione del Timer basata su timestamp reale (aggiornamento a 16ms per fluidità a 60 FPS e scheduler in background)
    LaunchedEffect(isTimerRunning, endTimeMs) {
        if (isTimerRunning) {
            scheduleBackgroundFinish(context, endTimeMs, isVibrationEnabled, soundUriString)
            while (isTimerRunning) {
                val nowTime = System.currentTimeMillis()
                currentTimeMs = nowTime
                if (nowTime >= endTimeMs) {
                    isTimerRunning = false
                    triggerTimerFinishedFeedback(context, isVibrationEnabled, soundUriString)
                    break
                }
                delay(16.milliseconds)
            }
        } else {
            cancelBackgroundTimer()
        }
    }

    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (timeLeftMs > 0L) "Preparazione" else dailyPlanName) },
                navigationIcon = {
                    IconButton(onClick = { showExitConfirmation = true }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Chiudi allenamento")
                    }
                },
                actions = {
                    if (timeLeftMs == 0L) {
                        IconButton(onClick = { showOverview = true }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.List, contentDescription = "Panoramica scheda")
                        }
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
        bottomBar = {
            if (timeLeftMs == 0L && flatSets.isNotEmpty()) {
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
                                    val currentSet = flatSets[currentGlobalSetIndex]
                                    val nextSet = flatSets[currentGlobalSetIndex + 1]
                                    val isNextExercise = currentSet.exerciseIndex != nextSet.exerciseIndex

                                    val timerDuration = if (isNextExercise) 3_000L else savedRestDurationMs

                                    currentGlobalSetIndex++
                                    val nowTime = System.currentTimeMillis()
                                    initialDurationMs = timerDuration
                                    endTimeMs = nowTime + timerDuration
                                    currentTimeMs = nowTime
                                    isTimerRunning = true
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
            } else if (timeLeftMs > 0L) {
                // Layout del Countdown Circolare
                val isEditableTimer = initialDurationMs > 3_000L

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.extraLarge)
                            .clickable(enabled = isEditableTimer) { showTimerSelectionDialog = true }
                    ) {
                        CircularProgressIndicator(
                            progress = {
                                if (initialDurationMs > 0L) {
                                    (timeLeftMs.toFloat() / initialDurationMs.toFloat()).coerceIn(0f, 1f)
                                } else 0f
                            },
                            modifier = Modifier.size(220.dp),
                            strokeWidth = 14.dp,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val totalSeconds = (timeLeftMs / 1000).coerceAtLeast(0L)
                            val minutes = totalSeconds / 60
                            val secs = totalSeconds % 60
                            Text(
                                text = String.format(Locale.getDefault(), "%02d:%02d", minutes, secs),
                                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { adjustTime(-10_000L) },
                            enabled = isEditableTimer
                        ) {
                            Text("-10s")
                        }
                        Button(onClick = { adjustTime(-timeLeftMs) }) {
                            Text("Salta")
                        }
                        OutlinedButton(
                            onClick = { adjustTime(10_000L) },
                            enabled = isEditableTimer
                        ) {
                            Text("+10s")
                        }
                    }
                }
            } else {
                // Layout della Serie Attiva
                val currentSet = flatSets[currentGlobalSetIndex]
                val setDetail = currentSet.setDetail
                val weightFormatted = if (setDetail.weightKg % 1f == 0f) setDetail.weightKg.toInt().toString() else setDetail.weightKg.toString()

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
                    modifier = Modifier.padding(16.dp)
                ) {
                    // 1. Nome dell'esercizio
                    Text(
                        text = currentSet.exercise.name,
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )

                    // 2. Reps e Kg (scambiato con il numero della serie)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { setToEdit = currentSet }
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                                    append(setDetail.reps.toString())
                                }
                                append(" reps")
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = buildAnnotatedString {
                                withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                                    append(weightFormatted)
                                }
                                append(" kg")
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }

                    // 3. Numero della serie
                    Text(
                        text = "Serie ${currentSet.setIndex + 1} di ${currentSet.exercise.setsList.size}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // Dialog Selezione Tempo Recupero Predefinito
    if (showTimerSelectionDialog) {
        val options = listOf(
            "00:30" to 30_000L,
            "1:00" to 60_000L,
            "1:30" to 90_000L,
            "2:00" to 120_000L
        )
        AppAlertDialog(
            onDismissRequest = { showTimerSelectionDialog = false },
            title = "Imposta tempo di recupero",
            confirmButtonText = "OK",
            onConfirm = { showTimerSelectionDialog = false },
            dismissButtonText = null,
            onDismiss = null
        ) {
            // Contenitore per i Pulsanti di selezione del tempo
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    options.forEach { (label, durationMs) ->
                        val isSelected = (savedRestDurationMs == durationMs)
                        if (isSelected) {
                            Button(
                                onClick = {
                                    sharedPreferences.edit { putLong("rest_timer_ms", durationMs) }
                                    savedRestDurationMs = durationMs
                                    val nowTime = System.currentTimeMillis()
                                    initialDurationMs = durationMs
                                    endTimeMs = nowTime + durationMs
                                    currentTimeMs = nowTime
                                    isTimerRunning = true
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(label, style = MaterialTheme.typography.labelMedium)
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    sharedPreferences.edit { putLong("rest_timer_ms", durationMs) }
                                    savedRestDurationMs = durationMs
                                    val nowTime = System.currentTimeMillis()
                                    initialDurationMs = durationMs
                                    endTimeMs = nowTime + durationMs
                                    currentTimeMs = nowTime
                                    isTimerRunning = true
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(label, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            // Riga 2: Riquadro Vibrazione + Riquadro Suono (stessa altezza e colore di sfondo)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Contenitore Riquadro Vibrazione
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Switch(
                            checked = isVibrationEnabled,
                            onCheckedChange = { newValue ->
                                sharedPreferences.edit { putBoolean("vibration_enabled", newValue) }
                                isVibrationEnabled = newValue
                            }
                        )
                        Text("Vibrazione", style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Contenitore Riquadro Suono (stesso colore, stile e altezza)
                val soundTitle = remember(soundUriString) { getRingtoneTitle(context, soundUriString) }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable {
                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Seleziona suono notifica")
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                                val currentUri = if (soundUriString != "none" && soundUriString.isNotBlank()) {
                                    soundUriString.toUri()
                                } else null
                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentUri)
                            }
                            soundPickerLauncher.launch(intent)
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Suono notifica",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = soundTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }

    // Dialog Modifica Volante
    setToEdit?.let { targetSet ->
        EditSetValuesDialog(
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
        AppAlertDialog(
            onDismissRequest = { showOverview = false },
            title = "Panoramica Allenamento",
            confirmButtonText = "Chiudi",
            onConfirm = { showOverview = false },
            dismissButtonText = null,
            onDismiss = null
        ) {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                itemsIndexed(flatSets) { index, flatSet ->
                    val isCurrent = index == currentGlobalSetIndex
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(if (isCurrent) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable {
                                currentGlobalSetIndex = index
                                showOverview = false
                            }
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
                        val overviewWeightFormatted = if (flatSet.setDetail.weightKg % 1f == 0f) flatSet.setDetail.weightKg.toInt().toString() else flatSet.setDetail.weightKg.toString()
                        Text("${flatSet.setDetail.reps} x $overviewWeightFormatted kg")
                    }
                }
            }
        }
    }

    // Dialog Conferma Uscita
    if (showExitConfirmation) {
        AppAlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            title = "Torna indietro",
            confirmButtonText = "Sì",
            onConfirm = {
                showExitConfirmation = false
                onClose()
            },
            dismissButtonText = "Annulla",
            onDismiss = { showExitConfirmation = false }
        ) {
            Text(
                text = "Sei sicuro di voler tornare indietro?\nI progressi dell'allenamento andranno persi.",
                textAlign = TextAlign.Center
            )
        }
    }
}