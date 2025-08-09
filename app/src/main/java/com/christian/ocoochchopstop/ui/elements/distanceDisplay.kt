package com.christian.ocoochchopstop.ui.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import com.christian.ocoochchopstop.ui.viewmodel.ChopStopViewModel
import kotlinx.coroutines.delay

@Composable
fun distanceDisplay(
    chop: ChopStopViewModel,
    navController: NavHostController,
    width: Dp,
    distance: String = chop.getDisplayPosition(),
    modifier: Modifier = Modifier
) {
    val displayDistance: String
    val fontSize: TextUnit
    val unitMarkerColor: Color

    val backgroundColor = if (chop.isInvalidInput) {
        MaterialTheme.colorScheme.error
    } else MaterialTheme.colorScheme.surfaceBright

    val borderColor = if (chop.isInch) {
        MaterialTheme.colorScheme.primaryContainer
    } else MaterialTheme.colorScheme.tertiary

    if (distance.startsWith("STATE:")) {
        displayDistance = distance.substringAfter(":")
        fontSize = 22.sp
        unitMarkerColor = Color.Transparent
    } else {
        displayDistance = distance
        fontSize = 32.sp
        unitMarkerColor = MaterialTheme.colorScheme.onSurface
    }

    if (chop.isInvalidInput) {
        LaunchedEffect(chop.isInvalidInput) {
            delay(100)
            chop.inputNumber = chop.validNumber
            chop.isInvalidInput = false
        }
    }

    Row(
        modifier = modifier
            .width(width)
            .height(48.dp)
            .clip(CircleShape)
            .border(3.dp, borderColor, CircleShape)
            .background(backgroundColor)
            .zIndex(100f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = displayDistance,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable {
                if (distance == "STATE: Disconnected") {
                    navController.navigate("dino_game")
                }
            }
        )

        val unitHeight = if (chop.isInch) 48.dp else 28.dp
        val unitAlign = if (chop.isInch) Alignment.CenterVertically else Alignment.Bottom
        Row(
            modifier = Modifier
                .height(unitHeight),
            verticalAlignment = unitAlign,
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = chop.unitMarker,
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.bodyLarge,
                color = unitMarkerColor,
                fontSize = if (chop.isInch) 32.sp else 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}