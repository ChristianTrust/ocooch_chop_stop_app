package com.christian.ocoochchopstopmk2.ui.screens

import android.annotation.SuppressLint
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.christian.ocoochchopstopmk2.R.drawable.block12
import com.christian.ocoochchopstopmk2.R.drawable.block20
import com.christian.ocoochchopstopmk2.R.drawable.power_16
import com.christian.ocoochchopstopmk2.ui.elements.distanceDisplay
import com.christian.ocoochchopstopmk2.ui.elements.numpad
import com.christian.ocoochchopstopmk2.ui.elements.ocoochPopupAlert
import com.christian.ocoochchopstopmk2.ui.input.addToMainInputNumber
import com.christian.ocoochchopstopmk2.ui.util.columnOrRow
import com.christian.ocoochchopstopmk2.ui.util.ocoochCard
import com.christian.ocoochchopstopmk2.ui.viewmodel.ChopStopViewModel

@SuppressLint("ComposableNaming")
@Composable
fun homePage(
    chop: ChopStopViewModel,
//    navController: NavHostController
) {
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
        val blocksBarHeight = if (!chop.useBlock) 0.dp else if (isPortrait) maxHeight / 10 else maxHeight / 6
        val buttonBoxWidth = if (isPortrait) maxWidth else maxWidth / 2
        val numpadHeight = if (isPortrait) (maxHeight / 2f) + (blocksBarHeight / 3) else maxHeight
        val buttonBoxHeight = if (isPortrait) (maxHeight / 2f) - (blocksBarHeight / 3) else maxHeight
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
                    .height(numpadHeight),
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

                    distanceDisplay(modifier = Modifier, chop, 160.dp, distance)

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

                // blocks bar
                if (chop.useBlock) {
                    Row(
                        modifier = Modifier
                            .width(buttonBoxWidth)
                            .height(blocksBarHeight)
                            .padding(start = padding, end = padding, top = padding * 2, bottom = padding / 2),
                        horizontalArrangement = Arrangement.spacedBy(padding),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val activeColors = listOf(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiary)
                        val inactiveColors = listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.onSecondary)
                        val is12 = chop.activeBlockState == ChopStopViewModel.BlockState.TWELVE
                        val is20 = chop.activeBlockState == ChopStopViewModel.BlockState.TWENTY

                        ocoochCard(
                            icon = ImageVector.vectorResource(id = block12),
                            onClick = {
                                if (is12) {
                                    chop.activeBlockState = ChopStopViewModel.BlockState.NONE
                                } else {
                                    chop.activeBlockState = ChopStopViewModel.BlockState.TWELVE
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = if (is12) activeColors else inactiveColors,
                            fontSize = 90
                        )

                        ocoochCard(
                            icon = ImageVector.vectorResource(id = block20),
                            onClick = {
                                if (is20) {
                                    chop.activeBlockState = ChopStopViewModel.BlockState.NONE
                                } else {
                                    chop.activeBlockState = ChopStopViewModel.BlockState.TWENTY
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = if (is20) activeColors else inactiveColors,
                            fontSize = 90
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    numpad(
                        onClick = {
                            chop.inputNumber = addToMainInputNumber(it, chop.inputNumber, chop)
                        },
                        modifier = Modifier,
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