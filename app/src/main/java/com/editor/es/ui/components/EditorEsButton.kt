package com.editor.es.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.editor.es.ui.theme.EditorEsPalette

@Composable
fun EditorEsButton(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressScale"
    )
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            },
        interactionSource = interactionSource,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary) EditorEsPalette.buttonPrimaryBackground else EditorEsPalette.buttonSecondaryBackground,
            contentColor = if (primary) EditorEsPalette.buttonPrimaryContent else EditorEsPalette.buttonSecondaryContent,
            disabledContainerColor = (if (primary) EditorEsPalette.buttonPrimaryBackground else EditorEsPalette.buttonSecondaryBackground).copy(alpha = 0.45f),
            disabledContentColor = EditorEsPalette.textSecondary
        ),
        border = if (primary) null else BorderStroke(1.dp, EditorEsPalette.buttonSecondaryBorder),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(text = label, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}
