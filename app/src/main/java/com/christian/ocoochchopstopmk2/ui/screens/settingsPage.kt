package com.christian.ocoochchopstopmk2.ui.screens

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.christian.ocoochchopstopmk2.R.drawable.power_16
import com.christian.ocoochchopstopmk2.ui.elements.distanceDisplay
import com.christian.ocoochchopstopmk2.ui.elements.numpad
import com.christian.ocoochchopstopmk2.ui.elements.terminalView
import com.christian.ocoochchopstopmk2.ui.input.addToDefaultInputNumber
import com.christian.ocoochchopstopmk2.ui.util.columnOrRow
import com.christian.ocoochchopstopmk2.ui.util.dropDownIcons
import com.christian.ocoochchopstopmk2.ui.util.ocoochCard
import com.christian.ocoochchopstopmk2.ui.viewmodel.ChopStopViewModel

@SuppressLint("ComposableNaming")
@Composable
fun settingsPage(
    chop: ChopStopViewModel,
//    navController: NavHostController
) {
    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    val distanceDisplayWidth = if (isPortrait) 232.dp else 464.dp
    val numpadWeight = if (isPortrait) 1f else 2f
    val density = LocalDensity.current

    var terminalWeight = if (isPortrait) 2f else 1f
    var anchorWidth by remember { mutableFloatStateOf(0f) }

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
    var inputNumberDefault by remember { mutableStateOf("") }
    var isDefaultDouble by remember { mutableStateOf(false) }
    var isFirstDefaultKeypress by remember { mutableStateOf(false) }
    var isFirstMoveKeypress by remember { mutableStateOf(false) }
    var terminalScrollToEnd by remember { mutableIntStateOf(0) }
    var showBackupPopup by remember { mutableStateOf(false) }

    val defaultValues = listOf(
        Pair("Speed", chop.speed),
        Pair("Accel", chop.accel),
        Pair("Max Delay", chop.maxDelay),
        Pair("Min Delay", chop.minDelay),

        Pair("Table Length", chop.tableLength),
        Pair("Direction", chop.direction),
        Pair("Step Position", chop.stepPosition),
        Pair("Min Step Position", chop.minStepPosition),
        Pair("Max Step Position", chop.maxStepPosition),

        Pair("8ft Stop Head", chop.eightFtStopHead),
        Pair("10ft Stop Head", chop.tenFtStopHead),
        Pair("6ft Stop Head", chop.sixFtStopHead),

        Pair("Steps/Inch", chop.stepsPerInch)
    )
//    val commands = listOf("MOVE:", "SPEED:", "ACCEL:", "MAX_DELAY:", "MIN_DELAY:", "HOME", "LOG", "POS")
    val commands = listOf("MOVE:", "X", "HOME", "LOG", "CONFIRM")
    val singleCommands = listOf("X", "HOME", "LOG", "CONFIRM")
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

            "Table Length" -> chop.tableLength = inputNumberDefault
            "Direction" -> chop.direction = inputNumberDefault
            "Step Position" -> chop.stepPosition = inputNumberDefault.toInt()
            "Min Step Position" -> chop.minStepPosition = inputNumberDefault.toInt()
            "Max Step Position" -> chop.maxStepPosition = inputNumberDefault.toInt()

            "8ft Stop Head" -> chop.eightFtStopHead = inputNumberDefault.toDouble()
            "10ft Stop Head" -> chop.tenFtStopHead = inputNumberDefault.toDouble()
            "6ft Stop Head" -> chop.sixFtStopHead = inputNumberDefault.toDouble()

            "Steps/Inch" -> chop.stepsPerInch = inputNumberDefault.toDouble()
        }

        chop.logToTerminal("$key set to $inputNumberDefault", "[INFO]")

        defaultExpanded = false
        chop.setMegaParameters(key)
        chop.saveSettings(key)
        selectedDefault = ""
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
    ) {
        cloudSyncPopup(
            show = showBackupPopup,
            chop = chop,
            onDismiss = { showBackupPopup = false }
        )

        if (maxWidth < 420.dp && isPortrait) terminalWeight = 1.75f

        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {

                Row(
                    modifier = Modifier
                        .width(distanceDisplayWidth + 64.dp)
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

                distanceDisplay(modifier = Modifier, chop, distanceDisplayWidth)
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

                    if (selectedDefault != "" || selectedMoveInput) {

                        val isTableLength = selectedDefault == "Table Length"

                        Column(
                            modifier = Modifier
                                .weight(numpadWeight)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {

                            if (selectedMoveInput) {
                                numpad(
                                    onClick = {
                                        if (isFirstMoveKeypress && it != "backspace" && it != "clear") {
                                            isFirstMoveKeypress = false
                                            inputNumber = if (it == ".") "0." else it
                                        } else {
                                            if (isFirstMoveKeypress) {
                                                isFirstMoveKeypress = false
                                            }
                                            inputNumber = addToDefaultInputNumber(it, inputNumber, false)
                                        }
                                    },
                                    onConfirmClick = {
                                        selectedMoveInput = false
                                    },
                                    isDecimalEnabled = false,
                                    modifier = Modifier
                                        .padding(4.dp)
                                )
                            } else if (isTableLength) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ocoochCard(
                                        text = "8ft",
                                        onClick = {
                                            inputNumberDefault = "8ft"
                                            applyAndCloseDefault("Table Length")
                                        },
                                        modifier = Modifier.weight(1f).fillMaxWidth(),
                                        colors = if (inputNumberDefault == "8ft") listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary) else listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface)
                                    )
                                    ocoochCard(
                                        text = "6ft",
                                        onClick = {
                                            inputNumberDefault = "6ft"
                                            applyAndCloseDefault("Table Length")
                                        },
                                        modifier = Modifier.weight(1f).fillMaxWidth(),
                                        colors = if (inputNumberDefault == "6ft") listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary) else listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface)
                                    )
                                }
                            } else {
                                numpad(
                                    onClick = {
                                        if (isFirstDefaultKeypress && it != "backspace" && it != "clear") {
                                            isFirstDefaultKeypress = false
                                            inputNumberDefault = if (it == ".") {
                                                if (isDefaultDouble) "0." else ""
                                            } else {
                                                it
                                            }
                                        } else {
                                            if (isFirstDefaultKeypress) {
                                                isFirstDefaultKeypress = false
                                            }
                                            inputNumberDefault = addToDefaultInputNumber(it, inputNumberDefault, isDefaultDouble)
                                        }
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
                                            isFirstDefaultKeypress = false
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
                                                if (key == "Direction") {
                                                    val newDir = if (chop.direction == "RIGHT") "LEFT" else "RIGHT"
                                                    chop.direction = newDir
                                                    chop.logToTerminal("Direction set to $newDir", "[INFO]")
                                                    chop.saveSettings("Direction")
                                                } else if (selectedDefault != key) {
                                                    selectedDefault = key
                                                    inputNumberDefault = value.toString()
                                                    isFirstDefaultKeypress = true
                                                } else {
                                                    selectedDefault = ""
                                                    isFirstDefaultKeypress = false
                                                }
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
                                                        "8ft Stop Head",
                                                        "10ft Stop Head",
                                                        "6ft Stop Head",

                                                        "Steps/Inch" -> true
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
                                                isFirstDefaultKeypress = false
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

                                    // Dropdown Menu for default values
                                    ocoochCard(
                                        onClick = {
                                            defaultExpanded = !defaultExpanded
                                            selectedDefault = ""
                                            selectedMoveInput = false
                                            isFirstDefaultKeypress = false
                                            isFirstMoveKeypress = false
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

                                    Spacer(modifier = Modifier.padding(4.dp))

                                    ocoochCard(
                                        text = "Cloud Backup",
                                        onClick = {
                                            showBackupPopup = true
                                        },
                                        modifier = Modifier.weight(1f).fillMaxSize(),
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            MaterialTheme.colorScheme.onPrimary
                                        ),
                                        fontSize = 16
                                    )
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
                                            val symbol = if (isNegative) "-" else "+"

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
                                                        if (selectedMoveInput) {
                                                            isFirstMoveKeypress = true
                                                        }
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
                                            when (selectedOption) {
                                                "MOVE:" -> chop.moveSteps(inputNumber.toInt())
                                                "HOME" -> chop.home(false)
                                                else -> chop.sendData(command)
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        fontSize = 24,
                                        enabled = (isSingleCommand || (inputNumber.isNotBlank() && inputNumber != "-"))
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

@SuppressLint("ComposableNaming")
@Composable
fun cloudSyncPopup(
    show: Boolean,
    chop: ChopStopViewModel,
    onDismiss: () -> Unit
) {
    if (show) {
        var deviceIdInput by remember { mutableStateOf(chop.deviceId) }
        var usernameInput by remember { mutableStateOf(chop.basicAuthUsername) }
        var passwordInput by remember { mutableStateOf(chop.basicAuthPassword) }
        var isLoading by remember { mutableStateOf(false) }
        var statusMessage by remember { mutableStateOf("") }
        var isSuccess by remember { mutableStateOf(false) }

        LaunchedEffect(chop.deviceId, chop.basicAuthUsername, chop.basicAuthPassword) {
            if (deviceIdInput.isEmpty()) deviceIdInput = chop.deviceId
            if (usernameInput.isEmpty()) usernameInput = chop.basicAuthUsername
            if (passwordInput.isEmpty()) passwordInput = chop.basicAuthPassword
        }

        Dialog(
            onDismissRequest = { if (!isLoading) onDismiss() },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .imePadding()
                    .clickable(enabled = !isLoading) { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .width(340.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .clickable(enabled = false) {}
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Cloud Sync Settings",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = deviceIdInput,
                        onValueChange = { deviceIdInput = it },
                        label = { Text("Device ID") },
                        singleLine = true,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("Basic Auth Username") },
                        singleLine = true,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Basic Auth Password") },
                        singleLine = true,
                        enabled = !isLoading,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    if (statusMessage.isNotEmpty()) {
                        Text(
                            text = statusMessage,
                            color = if (isSuccess) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    if (isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ocoochCard(
                                    text = "Backup",
                                    onClick = {
                                        if (deviceIdInput.isBlank()) {
                                            statusMessage = "Device ID is required"
                                            isSuccess = false
                                            return@ocoochCard
                                        }
                                        isLoading = true
                                        statusMessage = "Backing up..."
                                        chop.backupSettings(
                                            deviceIdInput = deviceIdInput.trim(),
                                            usernameInput = usernameInput.trim(),
                                            passwordInput = passwordInput.trim(),
                                            onSuccess = {
                                                isLoading = false
                                                isSuccess = true
                                                statusMessage = "Backup Successful!"
                                            },
                                            onFailure = { err ->
                                                isLoading = false
                                                isSuccess = false
                                                statusMessage = "Failed: $err"
                                            }
                                        )
                                    },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    fontSize = 16,
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.onPrimary
                                    )
                                )

                                ocoochCard(
                                    text = "Restore",
                                    onClick = {
                                        if (deviceIdInput.isBlank()) {
                                            statusMessage = "Device ID is required"
                                            isSuccess = false
                                            return@ocoochCard
                                        }
                                        isLoading = true
                                        statusMessage = "Restoring..."
                                        chop.restoreSettings(
                                            deviceIdInput = deviceIdInput.trim(),
                                            usernameInput = usernameInput.trim(),
                                            passwordInput = passwordInput.trim(),
                                            onSuccess = {
                                                isLoading = false
                                                isSuccess = true
                                                statusMessage = "Restore Successful!"
                                            },
                                            onFailure = { err ->
                                                isLoading = false
                                                isSuccess = false
                                                statusMessage = "Failed: $err"
                                            }
                                        )
                                    },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    fontSize = 16,
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }

                            ocoochCard(
                                text = "Close",
                                onClick = { onDismiss() },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                fontSize = 16,
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

