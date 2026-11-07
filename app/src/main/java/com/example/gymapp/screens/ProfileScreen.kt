package com.example.gymapp.screens

import com.example.gymapp.components.AppAlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.graphics.createBitmap
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

@Composable
fun ProfileScreen() {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("GymAppPrefs", Context.MODE_PRIVATE) }

    var name by remember { mutableStateOf(sharedPreferences.getString("nome", "") ?: "") }
    var lastname by remember { mutableStateOf(sharedPreferences.getString("cognome", "") ?: "") }
    var height by remember { mutableStateOf(sharedPreferences.getString("altezza", "") ?: "") }
    var weight by remember { mutableStateOf(sharedPreferences.getString("peso", "") ?: "") }

    var imagePath by remember { mutableStateOf(sharedPreferences.getString("profile_image_path", null)) }
    var profileImageBitmap by remember(imagePath) {
        mutableStateOf(loadSavedProfileBitmap(imagePath))
    }

    // Stato per mostrare le opzioni della foto (Mostra / Cambia / Rimuovi)
    var showOptionsDialog by remember { mutableStateOf(false) }

    // Stato per visualizzare la foto a schermo intero/ingrandita
    var showFullImageDialog by remember { mutableStateOf(false) }

    // Bitmap selezionata dalla galleria in attesa di ritaglio
    var selectedBitmapForCrop by remember { mutableStateOf<Bitmap?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = loadBitmapFromUri(context, it)
            if (bitmap != null) {
                selectedBitmapForCrop = bitmap
            } else {
                Toast.makeText(context, "Impossibile caricare l'immagine", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Dialog opzioni per foto già impostata
    if (showOptionsDialog) {
        AppAlertDialog(
            onDismissRequest = { showOptionsDialog = false },
            title = "Foto profilo",
            confirmButtonText = null,
            onConfirm = null,
            dismissButtonText = "Annulla",
            onDismiss = { showOptionsDialog = false }
        ) {
            // Opzione 1: Mostra immagine
            TextButton(
                onClick = {
                    showOptionsDialog = false
                    showFullImageDialog = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Text(
                        "Mostra immagine",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Opzione 2: Scegli dalla galleria
            TextButton(
                onClick = {
                    showOptionsDialog = false
                    imagePickerLauncher.launch("image/*")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Text(
                        "Scegli dalla galleria",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Opzione 3: Rimuovi foto
            TextButton(
                onClick = {
                    showOptionsDialog = false
                    deleteProfilePicture(context, sharedPreferences)
                    imagePath = null
                    profileImageBitmap = null
                    Toast.makeText(context, "Foto profilo rimossa", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        "Rimuovi foto",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    // Dialog per mostrare la foto profilo ingrandita
    if (showFullImageDialog && profileImageBitmap != null) {
        AppAlertDialog(
            onDismissRequest = { showFullImageDialog = false },
            title = "Foto profilo",
            confirmButtonText = null,
            onConfirm = null,
            dismissButtonText = "Chiudi",
            onDismiss = { showFullImageDialog = false }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = profileImageBitmap!!,
                    contentDescription = "Foto profilo ingrandita",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }

    // Finestra di dialogo per ritaglio e ridimensionamento 1:1
    selectedBitmapForCrop?.let { srcBitmap ->
        ImageCropDialog(
            bitmap = srcBitmap,
            onDismiss = { selectedBitmapForCrop = null },
            onCropConfirmed = { croppedBitmap ->
                selectedBitmapForCrop = null
                val savedPath = saveCroppedBitmap(context, croppedBitmap)
                if (savedPath != null) {
                    imagePath = savedPath
                    sharedPreferences.edit { putString("profile_image_path", savedPath) }
                    profileImageBitmap = croppedBitmap.asImageBitmap()
                    Toast.makeText(context, "Foto profilo aggiornata", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Errore durante il salvataggio della foto", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Profile",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 16.dp)
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Foto Profilo Circolare 1:1
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .clickable {
                    if (profileImageBitmap != null) {
                        showOptionsDialog = true
                    } else {
                        imagePickerLauncher.launch("image/*")
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (profileImageBitmap != null) {
                Image(
                    bitmap = profileImageBitmap!!,
                    contentDescription = "Foto profilo",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Aggiungi foto profilo",
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(1.22f),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nome") },
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(vertical = 8.dp),
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        OutlinedTextField(
            value = lastname,
            onValueChange = { lastname = it },
            label = { Text("Cognome") },
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(vertical = 8.dp),
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = height,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || (newValue.length <= 3 && newValue.all { it.isDigit() })) {
                        height = newValue
                    }
                },
                label = { Text("Altezza") },
                trailingIcon = { Text("cm", modifier = Modifier.padding(end = 16.dp)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            OutlinedTextField(
                value = weight,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.matches("^\\d{0,3}([.,]\\d*)?$".toRegex())) {
                        weight = newValue
                    }
                },
                label = { Text("Peso") },
                trailingIcon = { Text("kg", modifier = Modifier.padding(end = 16.dp)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                sharedPreferences.edit {
                    putString("nome", name)
                    putString("cognome", lastname)
                    putString("altezza", height)
                    putString("peso", weight)
                }
                Toast.makeText(context, "Dati salvati correttamente", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(50.dp)
        ) {
            Text("Salva modifiche", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun ImageCropDialog(
    bitmap: Bitmap,
    onDismiss: () -> Unit,
    onCropConfirmed: (Bitmap) -> Unit
) {
    var userScale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val density = LocalDensity.current
    val containerDp = 260.dp
    val containerPx = with(density) { containerDp.toPx() }
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = "Ridimensiona foto (1:1)",
        confirmButtonText = "Salva",
        onConfirm = {
            val cropped = createCroppedBitmap(
                src = bitmap,
                containerSizePx = containerPx,
                userScale = userScale,
                offsetX = offsetX,
                offsetY = offsetY
            )
            onCropConfirmed(cropped)
        },
        dismissButtonText = "Annulla",
        onDismiss = onDismiss
    ) {
        Text(
            text = "Sposta o ingrandisci l'immagine per adattarla al cerchio.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Container per la preview circolare
        Box(
            modifier = Modifier
                .size(containerDp)
                .clip(CircleShape)
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        userScale = (userScale * zoom).coerceIn(1f, 3.5f)
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val baseScale = maxOf(size.width / imageBitmap.width, size.height / imageBitmap.height)
                val totalScale = baseScale * userScale
                val drawW = imageBitmap.width * totalScale
                val drawH = imageBitmap.height * totalScale
                val drawX = ((size.width - drawW) / 2f) + offsetX
                val drawY = ((size.height - drawH) / 2f) + offsetY

                drawImage(
                    image = imageBitmap,
                    dstOffset = IntOffset(drawX.roundToInt(), drawY.roundToInt()),
                    dstSize = IntSize(drawW.roundToInt(), drawH.roundToInt())
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Slider per zoom
        Text("Zoom", style = MaterialTheme.typography.labelMedium)
        Slider(
            value = userScale,
            onValueChange = { userScale = it },
            valueRange = 1f..3.5f,
            modifier = Modifier.fillMaxWidth(0.9f)
        )
    }
}

private fun createCroppedBitmap(
    src: Bitmap,
    containerSizePx: Float,
    userScale: Float,
    offsetX: Float,
    offsetY: Float,
    targetSize: Int = 512
): Bitmap {
    val output = createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(output)

    val baseScale = maxOf(containerSizePx / src.width.toFloat(), containerSizePx / src.height.toFloat())
    val totalScale = baseScale * userScale
    val drawW = src.width * totalScale
    val drawH = src.height * totalScale
    val drawX = ((containerSizePx - drawW) / 2f) + offsetX
    val drawY = ((containerSizePx - drawH) / 2f) + offsetY

    val factor = targetSize / containerSizePx

    val dstRect = android.graphics.RectF(
        drawX * factor,
        drawY * factor,
        (drawX + drawW) * factor,
        (drawY + drawH) * factor
    )

    val paint = android.graphics.Paint(
        android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG
    )
    canvas.drawBitmap(src, null, dstRect, paint)
    return output
}

private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
        }
    } catch (_: Exception) {
        null
    }
}

private fun loadSavedProfileBitmap(path: String?): ImageBitmap? {
    if (path.isNullOrEmpty()) return null
    val file = File(path)
    if (!file.exists()) return null
    return try {
        BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}

private fun saveCroppedBitmap(context: Context, bitmap: Bitmap): String? {
    val file = File(context.filesDir, "profile_picture.png")
    return try {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        file.absolutePath
    } catch (_: Exception) {
        null
    }
}

private fun deleteProfilePicture(context: Context, sharedPreferences: SharedPreferences) {
    val file = File(context.filesDir, "profile_picture.png")
    if (file.exists()) {
        file.delete()
    }
    sharedPreferences.edit { remove("profile_image_path") }
}