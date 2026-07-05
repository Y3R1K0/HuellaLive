package com.huellalive.app.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.huellalive.app.ui.theme.Error
import com.huellalive.app.ui.theme.Surface
import com.huellalive.app.ui.theme.TextOnAccent
import com.huellalive.app.ui.theme.TextPrimary
import com.huellalive.app.ui.theme.TextSecondary

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    confirmLabel: String = "Eliminar",
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Error) },
        title = { Text(title, color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = { Text(message, color = TextSecondary) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Error)
            ) {
                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = TextOnAccent)
                Spacer(Modifier.width(6.dp))
                Text(confirmLabel, color = TextOnAccent)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(14.dp)) {
                Text("Cancelar")
            }
        },
        containerColor = Surface
    )
}
