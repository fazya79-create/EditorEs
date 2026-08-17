package com.editor.es.ui.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.editor.es.R
import com.editor.es.agent.AgentCatalog
import com.editor.es.agent.AgentInstaller
import com.editor.es.agent.AgentStatus
import java.io.File

private val CardSurface = Color(0xFF0C242D)
private val CardBorder = Color(0x2602F5A1)
private val TextPrimary = Color(0xFFDDF5EA)
private val TextSecondary = Color(0xFF6E9184)
private val Accent = Color(0xFF02F5A1)
private val RowShape = RoundedCornerShape(12.dp)
private val SheetShape = RoundedCornerShape(18.dp)

@Composable
fun AgentPickerSheet(
    projectDir: File,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    val context = LocalContext.current
    val statuses = remember { mutableStateMapOf<String, AgentStatus>() }

    LaunchedEffect(Unit) {
        for (spec in AgentCatalog.agents) {
            statuses[spec.id] = AgentInstaller.detect(context, spec)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .clip(SheetShape)
                .background(CardSurface)
                .border(1.dp, CardBorder, SheetShape)
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            Text(
                text = "Run agent in sandbox",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Accent
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Only ${projectDir.name} is mounted. Device storage stays hidden.",
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(14.dp))

            val installed = AgentCatalog.agents.filter {
                statuses[it.id] == AgentStatus.Installed
            }
            if (installed.isEmpty()) {
                Text(
                    text = "No agent installed yet. Install one from Settings → Agent.",
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = Color(0xFFFFB74D)
                )
            } else {
                for (spec in installed) {
                    AgentRow(
                        name = spec.name,
                        command = spec.runCommand,
                        onClick = { onPick(spec.id) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Cancel",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    modifier = Modifier
                        .clip(RowShape)
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun AgentRow(name: String, command: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RowShape)
            .background(Color(0xFF0F2B34))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.bolt),
            contentDescription = null,
            tint = Accent,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = command,
                fontSize = 10.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
