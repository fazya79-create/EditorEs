package com.editor.es.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.editor.es.ui.theme.EditorEsPalette

private val FieldShape = RoundedCornerShape(16.dp)

@Composable
fun EditorEsDropdown(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(FieldShape)
                .background(EditorEsPalette.buttonSecondaryBackground)
                .border(1.dp, EditorEsPalette.buttonSecondaryBorder, FieldShape)
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = options.getOrElse(selectedIndex) { "" },
                color = EditorEsPalette.textPrimary,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = EditorEsPalette.mint,
                modifier = Modifier.size(22.dp)
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(animationSpec = tween(160)) + expandVertically(animationSpec = spring()),
            exit = fadeOut(animationSpec = tween(120)) + shrinkVertically(animationSpec = spring())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(FieldShape)
                    .background(EditorEsPalette.abyss)
                    .border(1.dp, EditorEsPalette.buttonSecondaryBorder, FieldShape)
            ) {
                options.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(index)
                                expanded = false
                            }
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option,
                            color = if (index == selectedIndex) EditorEsPalette.amber else EditorEsPalette.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        if (index == selectedIndex) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = EditorEsPalette.amber,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
