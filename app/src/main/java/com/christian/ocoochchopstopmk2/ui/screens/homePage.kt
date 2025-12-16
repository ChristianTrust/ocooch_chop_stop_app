package com.christian.ocoochchopstopmk2.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import com.christian.ocoochchopstopmk2.R.drawable.power_16
import com.christian.ocoochchopstopmk2.ui.elements.distanceDisplay
import com.christian.ocoochchopstopmk2.ui.elements.numpad
import com.christian.ocoochchopstopmk2.ui.elements.ocoochPopupAlert
import com.christian.ocoochchopstopmk2.ui.input.addToMain
import com.christian.ocoochchopstopmk2.ui.util.columnOrRow
import com.christian.ocoochchopstopmk2.ui.util.ocoochCard
import com.christian.ocoochchopstopmk2.ui.viewmodel.ChopStopViewModel

@Composable
fun homePage(chop: ChopStopViewModel, navController: NavHostController) {
    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    val padding = 8.dp

    LaunchedEffect(Unit) {
        chop.closeError()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(bottom = padding)
            .windowInsetsPadding(WindowInsets.safeContent),
        contentAlignment = if (isPortrait) Alignment.BottomCenter else Alignment.BottomEnd,
    ) {
        val buttonBoxWidth = if (isPortrait) maxWidth else maxWidth / 2
        val buttonBoxHeight = if (isPortrait) maxHeight / 2 else maxHeight
        val goButtonWidth = if (isPortrait) maxWidth / 4 else buttonBoxWidth / 4 - 4.dp

        columnOrRow(useColumn = isPortrait, modifier = Modifier.fillMaxSize(), content = {

            ocoochPopupAlert(
                show = chop.showError,
                title = chop.errorTitle,
                message = chop.errorMessage,
                onCancel = { chop.closeError() }
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
                    Row(
                        modifier = Modifier
                            .width(232.dp)
                            .height(40.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val powerColor = if (chop.isMotorPowered) Color.Green else Color.Red

                        Icon(
                            painter = painterResource(id = power_16),
                            contentDescription = "Power",
                            tint = powerColor,
                            modifier = Modifier
                                .size(24.dp)
                        )
                    }

                    val distance = chop.inputNumber.ifEmpty {
                        chop.getDisplayPosition()
                    }

                    distanceDisplay(modifier = Modifier, chop, navController, 160.dp, distance)

                    Column {
                        val showGoButton = (chop.inputNumber.isNotEmpty())
                        val slideStart: Int = (buttonBoxWidth * 0.8f).value.toInt()

                        AnimatedVisibility(
                            visible = showGoButton,
                            enter = slideInHorizontally(initialOffsetX = { -slideStart }) + fadeIn(),
                            exit = slideOutHorizontally(targetOffsetX = { -slideStart }) + fadeOut(),
                            modifier = Modifier.zIndex(0f) // Ensure the button is below other elements
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