package com.example.comptaperso.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.comptaperso.data.Transaction
import com.example.comptaperso.data.TransactionType

/**
 * Affiche une ligne pour une transaction, avec son nom, sa date, son montant et un statut (payé/non payé).
 *
 * @param transaction La transaction à afficher.
 * @param onStatusChange Callback exécuté lorsque l'état "payé" de la transaction est modifié.
 * @param onClick Callback exécuté lorsque l'utilisateur clique sur la ligne de la transaction.
 */
@Composable
fun TransactionRow(transaction: Transaction, onStatusChange: (Boolean) -> Unit, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Case à cocher pour marquer la transaction comme payée ou non.
        Checkbox(checked = transaction.isPaid, onCheckedChange = onStatusChange)
        Spacer(Modifier.width(16.dp))
        // Ligne cliquable contenant les détails de la transaction.
        Row(
            modifier = Modifier.weight(1f).clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(transaction.name, style = MaterialTheme.typography.bodyLarge)
                Text("Le ${transaction.dayOfMonth} de chaque mois", style = MaterialTheme.typography.bodySmall)
            }
            // Le montant est affiché avec une couleur différente pour les crédits.
            val color = if (transaction.type == TransactionType.CREDIT) MaterialTheme.colorScheme.tertiary else Color.Unspecified
            Text("%.2f€".format(transaction.amount), fontWeight = FontWeight.Bold, color = color)
        }
    }
}