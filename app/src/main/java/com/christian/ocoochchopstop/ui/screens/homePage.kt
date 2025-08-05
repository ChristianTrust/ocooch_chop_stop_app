package com.christian.ocoochchopstop.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.christian.ocoochchopstop.ui.util.columnOrRow
import com.christian.ocoochchopstop.ui.viewmodel.ChopStopViewModel
import com.christian.ocoochchopstop.ui.elements.distanceDisplay
import com.christian.ocoochchopstop.ui.elements.numpad
import com.christian.ocoochchopstop.ui.elements.ocoochPopupAlert
import com.christian.ocoochchopstop.ui.input.addToMain
import com.christian.ocoochchopstop.ui.util.ocoochCard

@Composable
fun homePage(chop: ChopStopViewModel) {
    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    val padding = 8.dp

    LaunchedEffect(Unit) {
        chop.closeLengthError()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(bottom = padding)
            .windowInsetsPadding(WindowInsets.safeContent),
        contentAlignment = if (isPortrait) Alignment.BottomCenter else Alignment.BottomEnd
    ) {
        val buttonBoxWidth = if (isPortrait) maxWidth else maxWidth / 2
        val buttonBoxHeight = if (isPortrait) maxHeight / 2 else maxHeight
        val goButtonWidth = if (isPortrait) maxWidth / 4 else buttonBoxWidth / 4 - 4.dp

        columnOrRow(useColumn = isPortrait, modifier = Modifier.fillMaxSize(), content = {

            ocoochPopupAlert(
                show = chop.showLengthError,
                title = chop.lengthErrorTitle,
                message = chop.lengthErrorMessage,
                onCancel = { chop.closeLengthError() }
            )

            Column(
                modifier = Modifier
                    .width(buttonBoxWidth)
                    .height(buttonBoxHeight),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val distance = if (chop.inputNumber.isEmpty()) {
                        chop.getDisplayPosition()
                    } else chop.inputNumber

                    distanceDisplay(chop, 160.dp, distance)

                    Column {
                        val showGoButton = (chop.inputNumber.isNotEmpty())
                        val slideStart: Int = (buttonBoxWidth * 0.8f).value.toInt()

                        AnimatedVisibility(
                            visible = showGoButton,
                            enter = slideInHorizontally(initialOffsetX = { -slideStart }) + fadeIn(),
                            exit = slideOutHorizontally(targetOffsetX = { -slideStart }) + fadeOut(),
                            modifier = Modifier.zIndex(0f) // Ensure button is below other elements
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .zIndex(0f),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ocoochCard(
                                    text = "Go",
                                    onClick = { chop.goToPosition() },
                                    modifier = Modifier
                                        .width(goButtonWidth)
                                        .height(48.dp)
                                        .padding(end = padding),
                                    fontSize = 24
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(padding))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    numpad(
                        onClick = {
                            chop.inputNumber = addToMain(it, chop.inputNumber, chop)
                        },
                        modifier = Modifier.padding(top = 8.dp),
                        isDecimalEnabled = true,
                        useConfirmButton = false
                    )
                }
            }

            Column(
                modifier = Modifier
                    .width(buttonBoxWidth)
                    .height(buttonBoxHeight),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                val numberOfRows = 4
                val presetRowHeight = (buttonBoxHeight - (padding * (numberOfRows - 1))) / numberOfRows
                val presetUnit = "INCH:"
                val presets = listOf(
                    listOf("5\"", "24.25\"", "32\""),
                    listOf("60\"", "72\"", "95\""),
                    listOf("36\"", "47\""),
                    listOf("12\"", "24\"")
                )

                presets.forEach { rowPresets ->
                    Row(
                        modifier = Modifier
                            .width(buttonBoxWidth)
                            .height(presetRowHeight)
                            .padding(start = padding, end = padding, top = padding),
                        horizontalArrangement = Arrangement.spacedBy(padding),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rowPresets.forEach { text ->
                            ocoochCard(
                                text = text,
                                onClick = { chop.goToPosition(presetUnit, text.replace("\"", "").toFloat()) },
                                modifier = Modifier.weight(1f).fillMaxSize(),
                            )
                        }
                    }
                }
            }
        })
    }
}