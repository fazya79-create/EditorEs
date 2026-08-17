package com.editor.es.ui.build

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.editor.es.build.GradleRequirement

private val CardSurface = Color(0xFF0C242D)
private val CardBorder = Color(0x2602F5A1)
private val TextPrimary = Color(0xFFDDF5EA)
private val TextSecondary = Color(0xFF6E9184)
private val Accent = Color(0xFF02F5A1)
private val CardShape = RoundedCornerShape(18.dp)

@Composable
fun GradleInstallDialog(
    requirement: GradleRequirement,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .clip(CardShape)
                .background(CardSurface)
                .border(1.dp, CardBorder, CardShape)
                .padding(20.dp)
        ) {
            Text(
                text = "Android build tools required",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Accent
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "This project needs the following before it can build:",
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))
            for (piece in requirement.missing) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(
                        text = piece.label,
                        fontSize = 12.sp,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = piece.sizeHint,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = onDismiss) {
                    Text(text = "Cancel", fontSize = 13.sp, color = TextSecondary)
                }
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent,
                        contentColor = Color(0xFF07191E)
                    )
                ) {
                    Text(text = "OK", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
