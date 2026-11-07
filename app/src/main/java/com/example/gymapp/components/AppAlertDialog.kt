package com.example.gymapp.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gymapp.ui.theme.ThemeManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    confirmButtonText: String? = "Conferma",
    onConfirm: (() -> Unit)? = null,
    dismissButtonText: String? = "Annulla",
    onDismiss: (() -> Unit)? = null,
    confirmEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = when (ThemeManager.themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val border = if (isDark) BorderStroke(1.dp, Color.Gray) else null

    BasicAlertDialog(
        onDismissRequest = onDismissRequest
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = AlertDialogDefaults.containerColor,
            border = border,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title centered
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Content centered
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    content = content
                )

                if (dismissButtonText != null || confirmButtonText != null) {
                    Spacer(modifier = Modifier.height(24.dp))

                    // Buttons: Cancel on left, Confirm on right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (dismissButtonText != null && onDismiss != null) {
                            TextButton(onClick = onDismiss) {
                                Text(dismissButtonText)
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }
                        if (confirmButtonText != null && onConfirm != null) {
                            Button(
                                onClick = onConfirm,
                                enabled = confirmEnabled,
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text(confirmButtonText)
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }
                    }
                }
            }
        }
    }
}
