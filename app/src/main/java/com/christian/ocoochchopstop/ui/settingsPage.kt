package com.christian.ocoochchopstop.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.christian.ocoochchopstop.ui.util.distanceDisplay
import com.christian.ocoochchopstop.ui.util.dropDownIcons
import com.christian.ocoochchopstop.ui.util.ocoochCard
import com.christian.ocoochchopstop.viewmodel.CopStopViewModel

@Composable
fun settingsPage(chop: CopStopViewModel) {
    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    val distanceDisplayWidth = if (isPortrait) 232.dp else 464.dp

    var inputMove by remember { mutableStateOf("") }
    var inputSpeed by remember { mutableStateOf("") }
    var inputAccel by remember { mutableStateOf("") }
    var inputMaxDelay by remember { mutableStateOf("") }
    var inputMinDelay by remember { mutableStateOf("") }
    var inputNumber by remember { mutableStateOf("") }
    var selectedOption by remember { mutableStateOf("MOVE:") }
    var defaultExpanded by remember { mutableStateOf(false) }
    var selectedDefault by remember { mutableStateOf("") }
    var orgDefaultVal by remember { mutableStateOf("") }

    val defaultValues = listOf(
        Pair("Speed", chop.speed),
        Pair("Accel", chop.accel),
        Pair("Max Delay", chop.maxDelay),
        Pair("Min Delay", chop.minDelay),
        Pair("Steps/Inch", chop.stepsPerInch),
        Pair("Steps/mm", chop.stepsPerMm),
        Pair("Step Position", chop.stepPosition),
        Pair("Min Step Position", chop.minStepPosition),
        Pair("Max Step Position", chop.maxStepPosition)
    )
//    val commands = listOf("MOVE:", "SPEED:", "ACCEL:", "MAX_DELAY:", "MIN_DELAY:", "HOME", "LOG", "POS")
    val commands = listOf("MOVE:", "HOME", "LOG")
    val singleCommands = listOf("HOME", "LOG")
    val isSingleCommand = selectedOption in singleCommands

    fun applyAndCloseDefault(key: String) {
        defaultExpanded = false
        chop.setMegaParameters(key)
        chop.saveSettings(key)
        selectedDefault = ""
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
    ) {
        val columnOrRowWidth = if (isPortrait) maxWidth / 3 else maxWidth / 4 - 8.dp
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
                val terminalWeight = if (isPortrait) 2f else 1f
                Column(
                    modifier = Modifier
                        .weight(terminalWeight)
                        .fillMaxHeight()
                ) {
                    terminalView(chop)
                }

                // Input Fields and Buttons
                columnOrRow(
                    column = isPortrait,
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

                            columnOrRow(column = isPortrait, modifier = Modifier.weight(1f), content = {

                                if (!isPortrait) {
                                    Spacer(modifier = Modifier.weight(0.55f))
                                }

                                // Dropdown Menu for default values

                                ocoochCard(
                                    onClick = {
                                        defaultExpanded = !defaultExpanded
                                        selectedDefault = ""
                                    },
                                    modifier = Modifier.weight(1f).fillMaxSize(),
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
                                                .width(columnOrRowWidth)
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

                                if (defaultExpanded) {
                                    Popup(
                                        onDismissRequest = {
                                            when (selectedDefault) {
                                                "Speed" -> chop.speed = orgDefaultVal.toInt()
                                                "Accel" -> chop.accel = orgDefaultVal.toInt()
                                                "Max Delay" -> chop.maxDelay = orgDefaultVal.toInt()
                                                "Min Delay" -> chop.minDelay = orgDefaultVal.toInt()
                                                "Steps/Inch" -> chop.stepsPerInch = orgDefaultVal.toDouble()
                                                "Steps/mm" -> chop.stepsPerMm = orgDefaultVal.toDouble()
                                                "Step Position" -> chop.stepPosition = orgDefaultVal.toInt()
                                                "Min Step Position" -> chop.minStepPosition = orgDefaultVal.toInt()
                                                "Max Step Position" -> chop.maxStepPosition = orgDefaultVal.toInt()
                                            }
                                            if (selectedDefault != "") {
                                                selectedDefault = ""
                                            } else {
                                                defaultExpanded = false
                                            }
                                        },
                                        properties = PopupProperties(focusable = true),
                                        alignment = Alignment.TopCenter,
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .imePadding()
                                                .verticalScroll(rememberScrollState())
                                                .width(columnOrRowWidth)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {

                                            Spacer(modifier = Modifier)

                                            ocoochCard(
                                                onClick = {
                                                    defaultExpanded = !defaultExpanded
                                                },
                                                modifier = Modifier
                                                    .width(columnOrRowWidth)
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
                                                            .width(columnOrRowWidth)
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
                                                        if (selectedDefault != key) {
                                                            selectedDefault = key
                                                            orgDefaultVal = value.toString()
                                                        } else selectedDefault = ""
                                                    },
                                                    modifier = Modifier
                                                        .width(columnOrRowWidth)
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
                                                            val keyboardType = when (key) {
                                                                "Speed" -> KeyboardType.Number
                                                                "Accel" -> KeyboardType.Number
                                                                "Max Delay" -> KeyboardType.Number
                                                                "Min Delay" -> KeyboardType.Number
                                                                "Steps/Inch" -> KeyboardType.Decimal
                                                                "Steps/mm" -> KeyboardType.Decimal
                                                                "Step Position" -> KeyboardType.Number
                                                                "Min Step Position" -> KeyboardType.Number
                                                                "Max Step Position" -> KeyboardType.Number
                                                                else -> KeyboardType.Number
                                                            }
                                                            val doubleRegex = Regex("^\\d*\\.?\\d*$") // Matches numbers like "123", "123.", "123.45"
                                                            val intRegex = Regex("^\\d*$") // Matches only whole numbers (e.g., 123, 0, etc.)


                                                            BasicTextField(
                                                                value = value.toString(),
                                                                onValueChange = { newValue ->
                                                                    if (key in listOf("Steps/Inch", "Steps/mm")) {
                                                                        // Allow decimal input for Double fields
                                                                        if (newValue.matches(doubleRegex)) {
                                                                            val doubleValue = newValue.toDoubleOrNull() ?: 0.0
                                                                            when (key) {
                                                                                "Steps/Inch" -> chop.stepsPerInch = doubleValue
                                                                                "Steps/mm" -> chop.stepsPerMm = doubleValue
                                                                            }
                                                                        }
                                                                    } else {
                                                                        // Allow only integers for Int fields
                                                                        if (newValue.matches(intRegex)) {
                                                                            val intValue = newValue.toIntOrNull() ?: 0
                                                                            when (key) {
                                                                                "Speed" -> chop.speed = intValue
                                                                                "Accel" -> chop.accel = intValue
                                                                                "Max Delay" -> chop.maxDelay = intValue
                                                                                "Min Delay" -> chop.minDelay = intValue
                                                                                "Step Position" -> chop.stepPosition = intValue
                                                                                "Min Step Position" -> chop.minStepPosition = intValue
                                                                                "Max Step Position" -> chop.maxStepPosition = intValue
                                                                            }
                                                                        }
                                                                    }

                                                                },
                                                                enabled = true,
                                                                singleLine = true,
                                                                keyboardOptions = KeyboardOptions(
                                                                    keyboardType = keyboardType,
                                                                    imeAction = ImeAction.Done
                                                                ),
                                                                keyboardActions = KeyboardActions(
                                                                    onDone = {
                                                                        applyAndCloseDefault(key)
                                                                    }
                                                                ),
                                                                textStyle = TextStyle(
                                                                    textAlign = TextAlign.Center
                                                                )
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

                                            Spacer(modifier = Modifier)
                                        }
                                    }
                                }
                            })

                            columnOrRow(column = isPortrait, modifier = Modifier.weight(1f), content = {
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

                            columnOrRow(column = isPortrait, modifier = Modifier.weight(1f), content = {
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
                            columnOrRow(column = isPortrait, modifier = Modifier.weight(1f), content = {
                                // Dropdown Menu for Send Command
                                var commandExpanded by remember { mutableStateOf(false) }
                                ocoochCard(
                                    onClick = { commandExpanded = true },
                                    modifier = Modifier.weight(1f).fillMaxSize(),
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.onPrimary
                                    ),
                                    fontSize = 16
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth()
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
                                        .width(columnOrRowWidth)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .border(4.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    commands.forEach { option ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(),
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
                                                    .width(columnOrRowWidth - 16.dp)
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
                                    }

                                    BasicTextField(
                                        modifier = Modifier
                                            .height(48.dp)
                                            .width(columnOrRowWidth - 16.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White),
                                        value = if (inputNumber == "-") "" else inputNumber,
                                        onValueChange = { newValue ->
                                            if (newValue.isEmpty() || newValue.toDoubleOrNull() != null) {
                                                inputNumber = newValue
                                                when (selectedOption) {
                                                    "MOVE:" -> inputMove = newValue
                                                    "SPEED:" -> inputSpeed = newValue
                                                    "ACCEL:" -> inputAccel = newValue
                                                    "MAX_DELAY:" -> inputMaxDelay = newValue
                                                    "MIN_DELAY:" -> inputMinDelay = newValue
                                                }
                                            }
                                        },
                                        enabled = !isSingleCommand,
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        textStyle = TextStyle(
                                            textAlign = TextAlign.Center,
                                            color = if (isSingleCommand) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface
                                        ),
                                        decorationBox = { innerTextField ->
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if ((inputNumber.isEmpty() || inputNumber == "-") && !isSingleCommand) {
                                                    Text(
                                                        text = "Amount",
                                                        color = Color.Gray,
                                                        textAlign = TextAlign.Center,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                }
                                                innerTextField()
                                            }
                                        }
                                    )
                                }
                            })

                            columnOrRow(column = isPortrait, modifier = Modifier.weight(1f), content = {
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
                                        inputNumber = ""
                                    },
                                    modifier = Modifier.weight(1f),
                                    fontSize = 24,
                                )
                            })
                        }
                    }
                )
            }
        }
    }
}
