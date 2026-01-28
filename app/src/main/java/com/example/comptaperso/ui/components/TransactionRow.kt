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
 * `TransactionRow` est un composant qui affiche une seule ligne de transaction.
 * Il montre le nom, la date, le montant et un statut (payé/non payé) via une checkbox.
 *
 * @param transaction L'objet `Transaction` à afficher.
 * @param onStatusChange Callback déclenché lorsque l'utilisateur coche ou décoche la case.
 * @param onClick Callback déclenché lorsque l'utilisateur clique sur la ligne (pour modification).
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
        // Le reste de la ligne est cliquable pour ouvrir le dialogue de modification.
        Row(
            modifier = Modifier.weight(1f).clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(transaction.name, style = MaterialTheme.typography.bodyLarge)
                Text("Le ${transaction.dayOfMonth} de chaque mois", style = MaterialTheme.typography.bodySmall)
            }
            // La couleur du montant change s'il s'agit d'un crédit.
            val color = if (transaction.type == TransactionType.CREDIT) MaterialTheme.colorScheme.tertiary else Color.Unspecified
            Text("%.2f€".format(transaction.amount), fontWeight = FontWeight.Bold, color = color)
        }
    }
}
