package com.example.gymapp.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.navigation.NavController
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

// Modello dati per il singolo desiderio
data class WishlistItem(
    val id: String = UUID.randomUUID().toString(), // Genera un ID unico per riconoscere l'elemento da eliminare
    val name: String,
    val category: String,
)

object WishlistManager {
    private const val PREFS_NAME = "WishlistPrefs"
    private const val KEY_ITEMS = "wishlist_items"

    fun loadWishlist(context: Context): List<WishlistItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_ITEMS, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<WishlistItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    WishlistItem(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        category = obj.getString("category")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveWishlist(context: Context, items: List<WishlistItem>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        items.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("category", item.category)
            }
            jsonArray.put(obj)
        }
        prefs.edit { putString(KEY_ITEMS, jsonArray.toString()) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(navController: NavController) {
    val context = LocalContext.current

    // Carica la lista memorizzata perennemente all'avvio
    var wishlist by remember { mutableStateOf(WishlistManager.loadWishlist(context)) }

    // Stato per mostrare o nascondere la finestra di aggiunta
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wishlist") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Torna indietro"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            // Bottone fluttuante in stile Google per aggiungere elementi
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi desiderio")
            }
        }
    ) { paddingValues ->
        if (wishlist.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("La tua wishlist è vuota.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Genera una riga per ogni elemento nella lista
                items(wishlist, key = { it.id }) { item ->
                    WishlistItemRow(
                        item = item,
                        onDelete = {
                            // Aggiorna la lista escludendo l'elemento eliminato e salva perennemente
                            val updatedList = wishlist.filterNot { it.id == item.id }
                            wishlist = updatedList
                            WishlistManager.saveWishlist(context, updatedList)
                        }
                    )
                }
            }
        }

        // Mostra il popup di aggiunta se showDialog è true
        if (showDialog) {
            AddWishlistItemDialog(
                onDismiss = { showDialog = false },
                onAdd = { nome, categoria ->
                    val updatedList = wishlist + WishlistItem(name = nome, category = categoria)
                    wishlist = updatedList
                    WishlistManager.saveWishlist(context, updatedList)
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun WishlistItemRow(item: WishlistItem, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                // Categoria colorata per risaltare
                Text(
                    text = item.category,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            // Bottone con cestino rosso
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun AddWishlistItemDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var itemName by remember { mutableStateOf("") }
    val categories = listOf("Essenziale", "Importante", "Utile")
    var selectedCategory by remember { mutableStateOf(categories[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuovo acquisto") },
        text = {
            Column {
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Cosa vorresti acquistare?") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Priorità:", style = MaterialTheme.typography.labelMedium)

                // Genera i pallini di selezione per le categorie
                categories.forEach { category ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category }
                        )
                        Text(text = category)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (itemName.isNotBlank()) onAdd(itemName, selectedCategory)
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