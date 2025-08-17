package com.christian.ocoochchopstop.ui.elements

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.christian.ocoochchopstop.ui.input.addToMain
import com.christian.ocoochchopstop.ui.viewmodel.ChopStopViewModel

@Composable
fun calibrationPopup(
    chop: ChopStopViewModel,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        ocoochPopupAlert(
            show = chop.isCalibrating,
            title = if (chop.calibrationInput.isEmpty()) "Recalibrate" else "${chop.calibrationInput}\"",
            message = "Enter the correct length",
            onCancel = { chop.endCalibration() },
            onConfirm = { chop.recalibrate() },
            optional = true
        ) {
            Box(
                modifier = Modifier
                    .width(320.dp)
                    .height(320.dp),
                contentAlignment = Alignment.Center
            ) {
                numpad(
                    onClick = {
                        chop.calibrationInput = addToMain(it, chop.calibrationInput, chop)
                    },
                    modifier = Modifier.padding(top = 8.dp),
                    isDecimalEnabled = true,
                    useConfirmButton = false,
                    backgroundColor = MaterialTheme.colorScheme.surfaceContainer,
                )
            }
        }
    }
}