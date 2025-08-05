package com.christian.ocoochchopstop.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import com.christian.ocoochchopstop.ui.elements.distanceDisplay
import com.christian.ocoochchopstop.ui.elements.numpad
import com.christian.ocoochchopstop.ui.elements.terminalView
import com.christian.ocoochchopstop.ui.input.addToDefault
import com.christian.ocoochchopstop.ui.util.columnOrRow
import com.christian.ocoochchopstop.ui.util.dropDownIcons
import com.christian.ocoochchopstop.ui.util.ocoochCard
import com.christian.ocoochchopstop.ui.viewmodel.ChopStopViewModel

@Composable
fun settingsPage(chop: ChopStopViewModel) {
    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    val distanceDisplayWidth = if (isPortrait) 232.dp else 464.dp
    val terminalWeight = if (isPortrait) 2f else 1f
    val numpadWeight = if (isPortrait) 1f else 2f
    var anchorWidth by remember { mutableStateOf(0f) }
    val density = LocalDensity.current

    var inputMove by remember { mutableStateOf("") }
    var inputSpeed by remember { mutableStateOf("") }
    var inputAccel by remember { mutableStateOf("") }
    var inputMaxDelay by remember { mutableStateOf("") }
    var inputMinDelay by remember { mutableStateOf("") }
    var inputNumber by remember { mutableStateOf("") }
    var selectedOption by remember { mutableStateOf("MOVE:") }
    var defaultExpanded by remember { mutableStateOf(false) }
    var selectedMoveInput by remember { mutableStateOf(false) }
    var selectedDefault by remember { mutableStateOf("") }
    var orgDefaultVal by remember { mutableStateOf("") }
    var inputNumberDefault by remember { mutableStateOf("") }
    var isDefaultDouble by remember { mutableStateOf(false) }
    var terminalScrollToEnd by remember { mutableStateOf(0) }

    val defaultValues = listOf(
        Pair("Speed", chop.speed),
        Pair("Accel", chop.accel),
        Pair("Max Delay", chop.maxDelay),
        Pair("Min Delay", chop.minDelay),

        Pair("Step Position", chop.stepPosition),
        Pair("Min Step Position", chop.minStepPosition),
        Pair("Max Step Position", chop.maxStepPosition),

        Pair("8ft Stop Head", chop.eightFtStopHead),
        Pair("10ft Stop Head", chop.tenFtStopHead),
        Pair("12ft Stop Head", chop.twelveFtStopHead),

        Pair("Steps/Inch", chop.stepsPerInch),
        Pair("Steps/mm", chop.stepsPerMm)
    )
//    val commands = listOf("MOVE:", "SPEED:", "ACCEL:", "MAX_DELAY:", "MIN_DELAY:", "HOME", "LOG", "POS")
    val commands = listOf("MOVE:", "HOME", "LOG")
    val singleCommands = listOf("HOME", "LOG")
    val isSingleCommand = selectedOption in singleCommands

    fun applyAndCloseDefault(key: String) {
        if (inputNumberDefault.isEmpty()) inputNumberDefault = "0"
        
        if (isDefaultDouble && !inputNumberDefault.contains(".")) {
            inputNumberDefault = "$inputNumberDefault.0"
        } else if (!isDefaultDouble && inputNumberDefault.contains(".")) {
            inputNumberDefault = inputNumberDefault.substring(0, inputNumberDefault.indexOf("."))
        }

        when (key) {
            "Speed" -> chop.speed = inputNumberDefault.toInt()
            "Accel" -> chop.accel = inputNumberDefault.toInt()
            "Max Delay" -> chop.maxDelay = inputNumberDefault.toInt()
            "Min Delay" -> chop.minDelay = inputNumberDefault.toInt()

            "Step Position" -> chop.stepPosition = inputNumberDefault.toInt()
            "Min Step Position" -> chop.minStepPosition = inputNumberDefault.toInt()
            "Max Step Position" -> chop.maxStepPosition = inputNumberDefault.toInt()

            "8ft Stop Head" -> chop.eightFtStopHead = inputNumberDefault.toDouble()
            "10ft Stop Head" -> chop.tenFtStopHead = inputNumberDefault.toDouble()
            "12ft Stop Head" -> chop.twelveFtStopHead = inputNumberDefault.toDouble()

            "Steps/Inch" -> chop.stepsPerInch = inputNumberDefault.toDouble()
            "Steps/mm" -> chop.stepsPerMm = inputNumberDefault.toDouble()
        }

        defaultExpanded = false
        chop.setMegaParameters(key)
        chop.saveSettings(key)
        selectedDefault = ""
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                distanceDisplay(chop, distanceDisplayWidth)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Terminal Display
                Column(
                    modifier = Modifier
                        .weight(terminalWeight)
                        .fillMaxHeight()
                ) {
                    terminalView(chop, Modifier.weight(1f), terminalScrollToEnd)

                    if (selectedDefault != "" || selectedMoveInput == true) {

                        Column(
                            modifier = Modifier
                                .weight(numpadWeight)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {

                            if (selectedMoveInput) {
                                numpad(
                                    onClick = {
                                        inputNumber = addToDefault(it, inputNumber, false)
                                    },
                                    onConfirmClick = {
                                        selectedMoveInput = false
                                    },
                                    isDecimalEnabled = false,
                                    modifier = Modifier
                                        .padding(4.dp)
                                )
                            } else {
                                numpad(
                                    onClick = {
                                        inputNumberDefault = addToDefault(it, inputNumberDefault, isDefaultDouble)
                                    },
                                    onConfirmClick = { applyAndCloseDefault(selectedDefault) },
                                    isDecimalEnabled = isDefaultDouble,
                                    modifier = Modifier
                                        .padding(4.dp)
                                )
                            }
                        }
                    }
                }

                // Input Fields and Buttons
                columnOrRow(
                    useColumn = isPortrait,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    content = {
                        Column(
                            modifier = Modifier
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            if (defaultExpanded) {
                                Column(
                                    modifier = Modifier
                                        .imePadding()
                                        .zIndex(10f)
                                        .verticalScroll(rememberScrollState())
                                        .fillMaxHeight()
                                        .width(with(density) { anchorWidth.toDp() })
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {

                                    Spacer(modifier = Modifier)

                                    ocoochCard(
                                        onClick = {
                                            defaultExpanded = !defaultExpanded
                                            selectedDefault = ""
                                        },
                                        modifier = Modifier
                                            .padding(start = 4.dp, end = 4.dp)
                                            .height(48.dp),
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            MaterialTheme.colorScheme.onPrimary
                                        ),
                                        fontSize = 16
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "Defaults",
                                                modifier = Modifier
                                                    .align(Alignment.Center),
                                                textAlign = TextAlign.Center,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold
                                            )

                                            dropDownIcons(
                                                faceDown = !defaultExpanded,
                                                modifier = Modifier.fillMaxWidth().padding(4.dp)
                                            )
                                        }
                                    }

                                    defaultValues.forEach { (key, value) ->

                                        ocoochCard(
                                            onClick = {
                                                terminalScrollToEnd++
                                                if (selectedDefault != key) {
                                                    selectedDefault = key
                                                    orgDefaultVal = value.toString()
                                                    inputNumberDefault = value.toString()
                                                } else selectedDefault = ""
                                            },
                                            modifier = Modifier
                                                .padding(start = 4.dp, end = 4.dp)
                                                .height(48.dp),
                                            fontSize = 12,
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.onPrimary
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = key,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )

                                                if (selectedDefault == key) {
                                                    isDefaultDouble = when (key) {
                                                        "Speed" -> false
                                                        "Accel" -> false
                                                        "Max Delay" -> false
                                                        "Min Delay" -> false

                                                        "Step Position" -> false
                                                        "Min Step Position" -> false
                                                        "Max Step Position" -> false

                                                        "8ft Stop Head" -> true
                                                        "10ft Stop Head" -> true
                                                        "12ft Stop Head" -> true

                                                        "Steps/Inch" -> true
                                                        "Steps/mm" -> true
                                                        else -> false
                                                    }

                                                    Text(
                                                        text = inputNumberDefault,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                } else {
                                                    Text(
                                                        text = value.toString(),
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    if (selectedDefault != "") {
                                        ocoochCard(
                                            text = "Reset",
                                            onClick = {
                                                chop.resetDefault(selectedDefault)
                                                selectedDefault = ""
                                            },
                                            modifier = Modifier
                                                .padding(start = 4.dp, end = 4.dp)
                                                .height(48.dp),
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                MaterialTheme.colorScheme.onPrimary
                                            ),
                                            fontSize = 16
                                        )
                                    }

                                    Spacer(modifier = Modifier)
                                }
                            } else {

                                columnOrRow(useColumn = isPortrait, modifier = Modifier.weight(1f), content = {

                                    if (!isPortrait) {
                                        Spacer(modifier = Modifier.weight(0.55f))
                                    }

                                    // Dropdown Menu for default values
                                    ocoochCard(
                                        onClick = {
                                            defaultExpanded = !defaultExpanded
                                            selectedDefault = ""
                                            selectedMoveInput = false
                                        },
                                        modifier = Modifier.weight(1f).fillMaxSize(),
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            MaterialTheme.colorScheme.onPrimary
                                        ),
                                        fontSize = 16
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(with(density) { anchorWidth.toDp() })
                                        ) {
                                            Text(
                                                text = "Defaults",
                                                modifier = Modifier
                                                    .align(Alignment.Center),
                                                textAlign = TextAlign.Center,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold
                                            )

                                            dropDownIcons(
                                                faceDown = !defaultExpanded,
                                                modifier = Modifier.fillMaxWidth().padding(4.dp)
                                            )
                                        }
                                    }

                                    if (!isPortrait) {
                                        Spacer(modifier = Modifier.weight(0.55f))
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                })

                                columnOrRow(useColumn = isPortrait, modifier = Modifier.weight(1f), content = {
                                    ocoochCard(
                                        text = "Move +",
                                        onClick = { chop.moveSteps(1600) },
                                        modifier = Modifier.weight(1f).fillMaxSize(),
                                        fontSize = 16
                                    )

                                    Spacer(modifier = Modifier.padding(4.dp))

                                    ocoochCard(
                                        text = "Move -",
                                        onClick = { chop.moveSteps(-1600) },
                                        modifier = Modifier.weight(1f).fillMaxSize(),
                                        fontSize = 16
                                    )
                                })

                                columnOrRow(useColumn = isPortrait, modifier = Modifier.weight(1f), content = {
                                    ocoochCard(
                                        text = "Jog +",
                                        onClick = { chop.moveSteps(50) },
                                        modifier = Modifier.weight(1f).fillMaxSize(),
                                        fontSize = 16
                                    )

                                    Spacer(modifier = Modifier.padding(4.dp))

                                    ocoochCard(
                                        text = "Jog -",
                                        onClick = { chop.moveSteps(-50) },
                                        modifier = Modifier.weight(1f).fillMaxSize(),
                                        fontSize = 16
                                    )
                                })

                                // Input Field and Send Button
                                columnOrRow(useColumn = isPortrait, modifier = Modifier.weight(1f), content = {
                                    // Dropdown Menu for Send Command
                                    var commandExpanded by remember { mutableStateOf(false) }
                                    ocoochCard(
                                        onClick = { commandExpanded = true },
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxSize(),
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            MaterialTheme.colorScheme.onPrimary
                                        ),
                                        fontSize = 16
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .onGloballyPositioned { coordinates ->
                                                    // Capture the width of the TextField
                                                    anchorWidth = coordinates.size.toSize().width
                                                }
                                        ) {
                                            // Centered Text
                                            if (selectedOption.length > 6) {
                                                Text(
                                                    text = selectedOption,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .align(Alignment.Center),
                                                    textAlign = TextAlign.Center,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            } else {
                                                Text(
                                                    text = selectedOption,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .align(Alignment.Center),
                                                    textAlign = TextAlign.Center,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            dropDownIcons(
                                                faceDown = commandExpanded,
                                                modifier = Modifier.fillMaxWidth().padding(4.dp)
                                            )
                                        }
                                    }


                                    DropdownMenu(
                                        expanded = commandExpanded,
                                        onDismissRequest = { commandExpanded = false },
                                        modifier = Modifier
                                            .width(with(density) { anchorWidth.toDp() })
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                            .border(4.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        commands.forEach { option ->
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(start = 10.dp, end = 10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                ocoochCard(
                                                    text = option,
                                                    onClick = {
                                                        selectedOption = option
                                                        commandExpanded = false
                                                        inputNumber = when (selectedOption) {
                                                            "MOVE:" -> inputMove
                                                            "SPEED:" -> inputSpeed
                                                            "ACCEL:" -> inputAccel
                                                            "MAX_DELAY:" -> inputMaxDelay
                                                            "MIN_DELAY:" -> inputMinDelay
                                                            else -> inputNumber
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .height(32.dp),
                                                    fontSize = 16,
                                                    colors = if (option == selectedOption) {
                                                        listOf(
                                                            MaterialTheme.colorScheme.primary,
                                                            MaterialTheme.colorScheme.onPrimary
                                                        )
                                                    } else {
                                                        listOf(Color.Transparent, MaterialTheme.colorScheme.onPrimary)
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.padding(4.dp))

                                    // Number Input Field
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White)
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (selectedOption == "MOVE:") {
                                            var isNegative = inputNumber.startsWith("-")
                                            var symbol = if (isNegative) "-" else "+"

                                            ocoochCard(
                                                text = symbol,
                                                onClick = {
                                                    isNegative = !isNegative
                                                    if (isNegative && !inputNumber.startsWith("-")) {
                                                        inputNumber = "-$inputNumber"
                                                    } else if (!isNegative && inputNumber.startsWith("-")) {
                                                        inputNumber = inputNumber.substring(1)
                                                    }
                                                },
                                                modifier = Modifier
                                                    .height(24.dp)
                                                    .width(16.dp)
                                                    .clip(RoundedCornerShape(12.dp)),
                                                fontSize = 16
                                            )

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clickable {
                                                        selectedMoveInput = !selectedMoveInput
                                                        terminalScrollToEnd++
                                                    },
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Start
                                            ) {
                                                Text(
                                                    text = inputNumber,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp
                                                )
                                            }
                                        }
                                    }
                                })

                                columnOrRow(useColumn = isPortrait, modifier = Modifier.weight(1f), content = {
                                    val command = if (isSingleCommand) selectedOption else selectedOption + inputNumber

                                    ocoochCard(
                                        text = "Send",
                                        onClick = {
                                            if (selectedOption == "MOVE:") {
                                                chop.moveSteps(inputNumber.toInt())
                                            } else {
                                                chop.sendData(command)
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        fontSize = 24,
                                        enabled = isSingleCommand || (inputNumber.isNotBlank() && inputNumber != "-")
                                    )

                                    Spacer(modifier = Modifier.padding(4.dp))

                                    ocoochCard(
                                        text = "Clear",
                                        onClick = {
                                            chop.clearTerminal()
                                        },
                                        modifier = Modifier.weight(1f),
                                        fontSize = 24,
                                    )
                                })
                            }
                        }
                    }
                )
            }
        }
    }
}