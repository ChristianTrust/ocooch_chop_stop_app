package com.christian.ocoochchopstop.ui

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
import androidx.compose.ui.unit.dp
import com.christian.ocoochchopstop.viewmodel.CopStopViewModel
import com.christian.ocoochchopstop.ui.util.ocoochCard

@Composable
fun numpad(mainModifier: Modifier = Modifier, chop: CopStopViewModel) {
    val cornerRadius = 12.dp
    val fontSize = 24

    val buttonColors = listOf(
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.onSecondary
    )

    Row(
        modifier = mainModifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ocoochCard(
                text = "7",
                onClick = { chop.addToNumber("7") },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(cornerRadius)),
                fontSize = fontSize,
                colors = buttonColors
            )
            ocoochCard(
                text = "4",
                onClick = { chop.addToNumber("4") },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(cornerRadius)),
                fontSize = fontSize,
                colors = buttonColors
            )
            ocoochCard(
                text = "1",
                onClick = { chop.addToNumber("1") },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(cornerRadius)),
                fontSize = fontSize,
                colors = buttonColors
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            modifier = Modifier
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ocoochCard(
                text = "8",
                onClick = { chop.addToNumber("8") },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(cornerRadius)),
                fontSize = fontSize,
                colors = buttonColors
            )
            ocoochCard(
                text = "5",
                onClick = { chop.addToNumber("5") },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(cornerRadius)),
                fontSize = fontSize,
                colors = buttonColors
            )
            ocoochCard(
                text = "2",
                onClick = { chop.addToNumber("2") },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(cornerRadius)),
                fontSize = fontSize,
                colors = buttonColors
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            modifier = Modifier
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ocoochCard(
                text = "9",
                onClick = { chop.addToNumber("9") },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(cornerRadius)),
                fontSize = fontSize,
                colors = buttonColors
            )
            ocoochCard(
                text = "6",
                onClick = { chop.addToNumber("6") },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(cornerRadius)),
                fontSize = fontSize,
                colors = buttonColors
            )
            ocoochCard(
                text = "3",
                onClick = { chop.addToNumber("3") },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(cornerRadius)),
                fontSize = fontSize,
                colors = buttonColors
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            modifier = Modifier
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ocoochCard(
                text = "C",
                onClick = { chop.clearInput() },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(cornerRadius)),
                fontSize = fontSize,
                colors = buttonColors
            )
            ocoochCard(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                onClick = { chop.back() },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(cornerRadius)),
                fontSize = 32,
                colors = buttonColors
            )

            val periodCommand = if (chop.isDecimal) "" else "."
            ocoochCard(
                text = ".",
                onClick = { chop.addToNumber(periodCommand) },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(cornerRadius)),
                fontSize = fontSize,
                colors = buttonColors
            )
            ocoochCard(
                text = "0",
                onClick = { chop.addToNumber("0") },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(cornerRadius)),
                fontSize = fontSize,
                colors = buttonColors
            )
        }
    }
}