package com.christian.ocoochchopstop.ui.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.christian.ocoochchopstop.ui.util.ocoochCard

@Composable
fun numpad(
    onNumberClick: (String) -> Unit,
    onClearClick: () -> Unit,
    onBackspaceClick: () -> Unit,
    isDecimalEnabled: Boolean,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    fontSize: Int = 24,
    buttonColors: List<androidx.compose.ui.graphics.Color> = listOf(
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.onSecondary
    )
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ocoochCard(
                text = "7",
                onClick = { onNumberClick("7") },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(cornerRadius)),
                fontSize = fontSize,
                colors = buttonColors
            )
            ocoochCard(
                text = "4",
                onClick = { onNumberClick("4") },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(cornerRadius)),
                fontSize = fontSize,
                colors = buttonColors
            )
            ocoochCard(
                text = "1",
                onClick = { onNumberClick("1") },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(cornerRadius)),
                fontSize = fontSize,
                colors = buttonColors
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ocoochCard(
                text = "8",
                onClick = { onNumberClick("8") },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(cornerRadius)),
                fontSize = fontSize,
                colors = buttonColors
            )
            ocoochCard(
                text = "5",
                onClick = { onNumberClick("5") },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(cornerRadius)),
                fontSize = fontSize,
                colors = buttonColors
            )
            ocoochCard(
                text = "2",
                onClick = { onNumberClick("2") },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(cornerRadius)),
                fontSize = fontSize,
                colors = buttonColors
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ocoochCard(
                text = "9",
                onClick = { onNumberClick("9") },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(cornerRadius)),
                fontSize = fontSize,
                colors = buttonColors
            )
            ocoochCard(
                text = "6",
                onClick = { onNumberClick("6") },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(cornerRadius)),
                fontSize = fontSize,
                colors = buttonColors
            )
            ocoochCard(
                text = "3",
                onClick = { onNumberClick("3") },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(cornerRadius)),
                fontSize = fontSize,
                colors = buttonColors
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ocoochCard(
                text = "C",
                onClick = onClearClick,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(cornerRadius)),
                fontSize = fontSize,
                colors = buttonColors
            )
            ocoochCard(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                onClick = onBackspaceClick,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(cornerRadius)),
                fontSize = 32,
                colors = buttonColors
            )
            ocoochCard(
                text = ".",
                onClick = { onNumberClick(if (isDecimalEnabled) "" else ".") },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(cornerRadius)),
                fontSize = fontSize,
                colors = buttonColors
            )
            ocoochCard(
                text = "0",
                onClick = { onNumberClick("0") },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(cornerRadius)),
                fontSize = fontSize,
                colors = buttonColors
            )
        }
    }
}