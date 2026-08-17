package com.editor.es.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.editor.es.R
import com.editor.es.ui.components.EditorEsButton
import com.editor.es.ui.components.EntranceItem
import com.editor.es.ui.theme.EditorEsPalette

@Composable
fun StorageGateScreen(onGrant: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        EntranceItem(visible = true, delayMillis = 0) {
            Icon(
                painter = painterResource(R.drawable.sd_card),
                contentDescription = null,
                tint = EditorEsPalette.mint,
                modifier = Modifier.size(72.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        EntranceItem(visible = true, delayMillis = 90) {
            Text(
                text = stringResource(R.string.storage_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = EditorEsPalette.textPrimary
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        EntranceItem(visible = true, delayMillis = 160) {
            Text(
                text = stringResource(R.string.storage_message),
                fontSize = 14.sp,
                color = EditorEsPalette.textSecondary,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(36.dp))
        EntranceItem(visible = true, delayMillis = 240) {
            EditorEsButton(
                primary = true,
                label = stringResource(R.string.grant_access),
                iconRes = R.drawable.lock,
                onClick = onGrant
            )
        }
    }
}
