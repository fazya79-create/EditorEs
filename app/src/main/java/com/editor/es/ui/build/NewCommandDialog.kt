package com.editor.es.ui.build

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.editor.es.build.ConfigurePreset

private val DialogBackground = Color(0xFF0A222B)
private val Accent = Color(0xFF02F5A1)
private val TextPrimary = Color(0xFFDDF5EA)
private val TextSecondary = Color(0xFF6E9184)
private val FieldBackground = Color(0xFF0E2B35)
private val DividerColor = Color(0xFF11333F)

data class NewCommandResult(
    val name: String,
    val configurePreset: String,
    val targets: List<String>,
    val jobs: Int,
    val verbose: Boolean,
    val cleanFirst: Boolean,
    val rawCommand: String?
)

@Composable
fun NewCommandDialog(
    configurePresets: List<ConfigurePreset>,
    defaultPreset: String?,
    onDismiss: () -> Unit,
    onSave: (NewCommandResult) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var targets by remember { mutableStateOf("") }
    var jobs by remember { mutableStateOf("") }
    var verbose by remember { mutableStateOf(false) }
    var cleanFirst by remember { mutableStateOf(false) }
    var raw by remember { mutableStateOf("") }
    var preset by remember {
        mutableStateOf(defaultPreset ?: configurePresets.firstOrNull()?.name ?: "")
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(DialogBackground)
                .padding(18.dp)
        ) {
            Text(
                text = "New command",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Saved into CMakeUserPresets.json",
                fontSize = 10.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(14.dp))

            Field(label = "Name", value = name, placeholder = "strip release") { name = it }
            Spacer(modifier = Modifier.height(10.dp))

            if (configurePresets.isNotEmpty()) {
                Label("Configuration")
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    for (option in configurePresets) {
                        val selected = option.name == preset
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) Accent else DividerColor)
                                .clickable { preset = option.name }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = option.label,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) Color(0xFF07191E) else TextPrimary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Field(label = "Targets", value = targets, placeholder = "mylib all") { targets = it }
            Spacer(modifier = Modifier.height(10.dp))
            Field(label = "Parallel jobs", value = jobs, placeholder = "4") { jobs = it }
            Spacer(modifier = Modifier.height(10.dp))

            ToggleRow("Verbose output", verbose) { verbose = it }
            ToggleRow("Clean first", cleanFirst) { cleanFirst = it }

            Spacer(modifier = Modifier.height(6.dp))
            Field(
                label = "Raw shell command",
                value = raw,
                placeholder = "llvm-strip build/*/lib*.so",
                monospace = true
            ) { raw = it }
            Text(
                text = "Fill this to run a shell command instead of cmake",
                fontSize = 9.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Cancel", fontSize = 12.sp, color = TextSecondary)
                }
                Button(
                    onClick = {
                        val trimmedName = name.trim()
                        if (trimmedName.isEmpty()) return@Button
                        val trimmedRaw = raw.trim().takeIf { it.isNotEmpty() }
                        if (trimmedRaw == null && preset.isEmpty()) return@Button
                        onSave(
                            NewCommandResult(
                                name = trimmedName,
                                configurePreset = preset,
                                targets = targets.trim().split(Regex("\\s+"))
                                    .filter { it.isNotEmpty() },
                                jobs = jobs.trim().toIntOrNull() ?: 0,
                                verbose = verbose,
                                cleanFirst = cleanFirst,
                                rawCommand = trimmedRaw
                            )
                        )
                    },
                    enabled = name.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent,
                        contentColor = Color(0xFF07191E)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = TextSecondary
    )
}

@Composable
private fun Field(
    label: String,
    value: String,
    placeholder: String,
    monospace: Boolean = false,
    onValueChange: (String) -> Unit
) {
    Column {
        Label(label)
        Spacer(modifier = Modifier.height(5.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = {
                Text(
                    text = placeholder,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default
                )
            },
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = FieldBackground,
                unfocusedContainerColor = FieldBackground,
                focusedBorderColor = Accent,
                unfocusedBorderColor = DividerColor,
                cursorColor = Accent,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF07191E),
                checkedTrackColor = Accent,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = DividerColor,
                uncheckedBorderColor = DividerColor
            )
        )
    }
}
