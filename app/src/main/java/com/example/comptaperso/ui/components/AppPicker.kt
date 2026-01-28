package com.example.comptaperso.ui.components

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap

/**
 * `AppInfo` est une classe de données qui stocke les informations essentielles sur une application installée.
 * @param name Le nom de l'application.
 * @param packageName Le nom du package de l'application.
 * @param icon L'icône de l'application.
 */
data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable
)

/**
 * `AppPicker` est un composant qui affiche une boîte de dialogue permettant à l'utilisateur
 * de sélectionner une application parmi celles installées sur l'appareil.
 * Il inclut une barre de recherche pour filtrer la liste.
 *
 * @param onAppSelected Callback qui renvoie le nom du package de l'application sélectionnée.
 * @param onDismissRequest Callback pour fermer la boîte de dialogue.
 */
@Composable
fun AppPicker(
    onAppSelected: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    var searchQuery by remember { mutableStateOf("") }
    var installedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }

    // `LaunchedEffect` pour charger la liste des applications installées une seule fois.
    LaunchedEffect(Unit) {
        val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { packageManager.getLaunchIntentForPackage(it.packageName) != null } // Ne garde que les applications lançables.
            .map {
                AppInfo(
                    name = it.loadLabel(packageManager).toString(),
                    packageName = it.packageName,
                    icon = it.loadIcon(packageManager)
                )
            }
            .sortedBy { it.name.lowercase() } // Trie les applications par ordre alphabétique.
        installedApps = apps
    }

    // Filtre la liste des applications en fonction de la recherche de l'utilisateur.
    val filteredApps = if (searchQuery.isBlank()) {
        installedApps
    } else {
        installedApps.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Sélectionner une application") },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Rechercher...") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                // Affiche la liste filtrée des applications.
                LazyColumn {
                    items(filteredApps) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAppSelected(app.packageName) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val bitmap = remember(app.icon) { app.icon.toBitmap() }
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Icône de ${app.name}",
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = app.name,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Annuler")
            }
        }
    )
}
