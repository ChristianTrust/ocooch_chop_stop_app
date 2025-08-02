package com.christian.ocoochchopstop.ui.util

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun dropDownIcons(
    faceDown: Boolean,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    icon: ImageVector = Icons.Default.ArrowDropDown,
    contentDescription: String? = null,
    rotationDegrees: Float = 180f
) {
    Box(modifier = modifier) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier
                .size(iconSize)
                .align(Alignment.CenterStart)
                .then(if (faceDown) Modifier else Modifier.rotate(rotationDegrees))
        )
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier
                .size(iconSize)
                .align(Alignment.CenterEnd)
                .then(if (faceDown) Modifier else Modifier.rotate(rotationDegrees))
        )
    }
}