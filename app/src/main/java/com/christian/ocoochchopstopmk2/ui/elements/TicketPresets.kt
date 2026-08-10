package com.christian.ocoochchopstopmk2.ui.elements

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.christian.ocoochchopstopmk2.ui.util.ocoochCard
import com.christian.ocoochchopstopmk2.ui.viewmodel.ChopStopViewModel

@SuppressLint("ComposableNaming")
@Composable
fun TicketPresets(
    chop: ChopStopViewModel,
    modifier: Modifier = Modifier,
    padding: Dp = 8.dp,
    cornerRadius: Dp = 12.dp,
    fontSize: Int = 40,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    buttonColors: List<Color> = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.onPrimary
    )
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(padding)
    ) {
        val presetUnit = "INCH:"
        val rowModifier = Modifier.weight(1f)

        Row(
            modifier = rowModifier,
            horizontalArrangement = Arrangement.spacedBy(padding)
        ) {
            ocoochCard(
                text = "36\"",
                onClick = { chop.goToPosition(presetUnit, 36f) },
                modifier = Modifier.weight(1f).fillMaxSize(),
                fontSize = fontSize,
                colors = buttonColors
            )
            ocoochCard(
                text = "47\"",
                onClick = { chop.goToPosition(presetUnit, 47f) },
                modifier = Modifier.weight(1f).fillMaxSize(),
                fontSize = fontSize,
                colors = buttonColors
            )
        }
        Row(
            modifier = rowModifier,
            horizontalArrangement = Arrangement.spacedBy(padding)
        ) {
            ocoochCard(
                text = "12\"",
                onClick = { chop.goToPosition(presetUnit, 12f) },
                modifier = Modifier.weight(1f).fillMaxSize(),
                fontSize = fontSize,
                colors = buttonColors
            )
            ocoochCard(
                text = "24\"",
                onClick = { chop.goToPosition(presetUnit, 24f) },
                modifier = Modifier.weight(1f).fillMaxSize(),
                fontSize = fontSize,
                colors = buttonColors
            )
        }
    }
}
