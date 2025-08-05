package com.christian.ocoochchopstop.ui.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Check
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
    onClick: (String) -> Unit,
    isDecimalEnabled: Boolean,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    fontSize: Int = 24,
    useConfirmButton: Boolean = true,
    onConfirmClick: () -> Unit = {},
    buttonColors: List<androidx.compose.ui.graphics.Color> = listOf(
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.onSecondary
    )
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {

        if (useConfirmButton) {
            ocoochCard(
                icon = Icons.Filled.Check,
                onClick = { onConfirmClick() },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.25f),
                fontSize = fontSize,
                colors = buttonColors
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
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
                    onClick = { onClick("7") },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(cornerRadius)),
                    fontSize = fontSize,
                    colors = buttonColors
                )
                ocoochCard(
                    text = "4",
                    onClick = { onClick("4") },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(cornerRadius)),
                    fontSize = fontSize,
                    colors = buttonColors
                )
                ocoochCard(
                    text = "1",
                    onClick = { onClick("1") },
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
                    onClick = { onClick("8") },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(cornerRadius)),
                    fontSize = fontSize,
                    colors = buttonColors
                )
                ocoochCard(
                    text = "5",
                    onClick = { onClick("5") },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(cornerRadius)),
                    fontSize = fontSize,
                    colors = buttonColors
                )
                ocoochCard(
                    text = "2",
                    onClick = { onClick("2") },
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
                    onClick = { onClick("9") },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(cornerRadius)),
                    fontSize = fontSize,
                    colors = buttonColors
                )
                ocoochCard(
                    text = "6",
                    onClick = { onClick("6") },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(cornerRadius)),
                    fontSize = fontSize,
                    colors = buttonColors
                )
                ocoochCard(
                    text = "3",
                    onClick = { onClick("3") },
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
                    onClick = { onClick("clear") },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(cornerRadius)),
                    fontSize = fontSize,
                    colors = buttonColors
                )
                ocoochCard(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = { onClick("backspace") },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(cornerRadius)),
                    fontSize = 32,
                    colors = buttonColors
                )
                if (isDecimalEnabled) {
                    ocoochCard(
                        text = ".",
                        onClick = { onClick(".") },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(cornerRadius)),
                        fontSize = fontSize,
                        colors = buttonColors
                    )
                }
                ocoochCard(
                    text = "0",
                    onClick = { onClick("0") },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(cornerRadius)),
                    fontSize = fontSize,
                    colors = buttonColors
                )
            }
        }
    }
}