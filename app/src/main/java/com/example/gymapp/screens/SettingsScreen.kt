package com.example.gymapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gymapp.ui.theme.ThemeManager

@Composable
fun SettingsScreen() {
    val context = LocalContext.current

    // Memorizza lo stato del tema: 0 = System, 1 = Light, 2 = Dark
    val themeMode = ThemeManager.themeMode

    // Determina dinamicamente l'icona in base allo stato
    val themeIcon = when (themeMode) {
        1 -> Icons.Default.LightMode
        2 -> Icons.Default.DarkMode
        else -> Icons.Default.Palette
    }

    // Determina dinamicamente il testo in base allo stato
    val themeText = when (themeMode) {
        1 -> "(Light)"
        2 -> "(Dark)"
        else -> "(System)"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 16.dp)
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SettingsButton(
                text = "Tema",
                icon = themeIcon,
                trailingText = themeText
            ) {
                // Avanza ciclicamente tra 0, 1 e 2
                val newMode = (ThemeManager.themeMode + 1) % 3
                ThemeManager.setThemeMode(context, newMode)
            }
            SettingsButton(
                text = "Notifiche",
                icon = Icons.Default.Notifications,
                onClick = { /* TODO: Implementare navigazione */ }
            )
            SettingsButton(
                text = "Lingue",
                icon = Icons.Default.Language,
                onClick = { /* TODO: Implementare navigazione */ }
            )
        }

        Button(
            onClick = { /* TODO: Implementare eliminazione */ },
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(bottom = 16.dp)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = "Elimina i tuoi dati",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun SettingsButton(
    text: String,
    icon: ImageVector,
    trailingText: String? = null, // Parametro opzionale per il testo a destra
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(vertical = 8.dp)
            .height(56.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween // Spinge gli elementi ai due margini opposti
        ) {
            // Raggruppa icona e testo principale a sinistra
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Mostra il testo aggiuntivo a destra se è stato fornito
            trailingText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}