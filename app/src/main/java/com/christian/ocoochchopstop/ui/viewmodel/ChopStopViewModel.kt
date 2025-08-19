package com.christian.ocoochchopstop.ui.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.christian.ocoochchopstop.data.dataStore
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.round

class ChopStopViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private val SPEED_KEY = intPreferencesKey("speed")
        private val ACCEL_KEY = intPreferencesKey("accel")
        private val MAX_DELAY_KEY = intPreferencesKey("max_delay")
        private val MIN_DELAY_KEY = intPreferencesKey("min_delay")

        private val STEP_POSITION_KEY = intPreferencesKey("step_position")
        private val MIN_STEP_POSITION_KEY = intPreferencesKey("min_step_position")
        private val MAX_STEP_POSITION_KEY = intPreferencesKey("max_step_position")

        private val EIGHT_FT_STOP_HEAD_KEY = doublePreferencesKey("8ft_stop_head")
        private val TEN_FT_STOP_HEAD_KEY = doublePreferencesKey("10ft_stop_head")
        private val TWELVE_FT_STOP_HEAD_KEY = doublePreferencesKey("12ft_stop_head")

        private val STEPS_PER_INCH_KEY = doublePreferencesKey("steps_per_inch")

        private val STOP_HEAD_KEY = stringPreferencesKey("stop_head")
    }

    // Flow variables
    val speedFlow = application.applicationContext.dataStore.data
        .map { preferences -> preferences[SPEED_KEY] ?: 20000 }
    val accelFlow = application.applicationContext.dataStore.data
        .map { preferences -> preferences[ACCEL_KEY] ?: 8000 }
    val maxDelayFlow = application.applicationContext.dataStore.data
        .map { preferences -> preferences[MAX_DELAY_KEY] ?: 320 }
    val minDelayFlow = application.applicationContext.dataStore.data
        .map { preferences -> preferences[MIN_DELAY_KEY] ?: 6 }

    val stepPositionFlow = application.applicationContext.dataStore.data
        .map { preferences -> preferences[STEP_POSITION_KEY] ?: 0 }
    val minStepPositionFlow = application.applicationContext.dataStore.data
        .map { preferences -> preferences[MIN_STEP_POSITION_KEY] ?: 0 }
    val maxStepPositionFlow = application.applicationContext.dataStore.data
        .map { preferences -> preferences[MAX_STEP_POSITION_KEY] ?: 166044 }

    val eightFtStopHeadFlow = application.applicationContext.dataStore.data
        .map { preferences -> preferences[EIGHT_FT_STOP_HEAD_KEY] ?: 2.6 }
    val tenFtStopHeadFlow = application.applicationContext.dataStore.data
        .map { preferences -> preferences[TEN_FT_STOP_HEAD_KEY] ?: 26.6 }
    val twelveFtStopHeadFlow = application.applicationContext.dataStore.data
        .map { preferences -> preferences[TWELVE_FT_STOP_HEAD_KEY] ?: 50.6 }

    val stepsPerInchFlow = application.applicationContext.dataStore.data
        .map { preferences -> preferences[STEPS_PER_INCH_KEY] ?: 1775.36 }

    val stopHeadFlow = application.applicationContext.dataStore.data
        .map { preferences -> preferences[STOP_HEAD_KEY] ?: "8ft" }

    var speed by mutableStateOf(0)
    var accel by mutableStateOf(0)
    var maxDelay by mutableStateOf(0)
    var minDelay by mutableStateOf(0)

    var stepPosition by mutableStateOf(0)
    var minStepPosition by mutableStateOf(0)
    var maxStepPosition by mutableStateOf(0)

    var eightFtStopHead by mutableStateOf(0.0)
    var tenFtStopHead by mutableStateOf(0.0)
    var twelveFtStopHead by mutableStateOf(0.0)

    var stepsPerInch by mutableStateOf(1775.36)

    var stopHead by mutableStateOf("8ft")

    var inchPosition by mutableStateOf(0.0)
    var parametersSet by mutableStateOf(false)

    var inputNumber by mutableStateOf("")
    var validNumber by mutableStateOf("")
    var isInvalidInput by mutableStateOf(false)
    var isInch by mutableStateOf(true)
    var unit by mutableStateOf("")
    var unitMarker by mutableStateOf("")
    var isMoving by mutableStateOf(false)
    var isStopping by mutableStateOf(false)
    var isHoming by mutableStateOf(false)
    var firstHoming by mutableStateOf(false)
    var isCalibrating by mutableStateOf(false)
    var calibrationInput by mutableStateOf("")
    var newMovePosition by mutableStateOf(0)

    var showLengthError by mutableStateOf(false)
    var lengthErrorTitle by mutableStateOf("")
    var lengthErrorMessage by mutableStateOf("")
    var connectionState by mutableStateOf(0)

    val terminalText = mutableStateOf<List<String>>(listOf())
    val stepPositionText = mutableStateOf("")
    val lastSentLine = mutableStateOf("")

    private var moveTime = 0.0
    private var timerJob: Job? = null
    private val dataQueue = ArrayDeque<ByteArray>()
    private val baudRate = 115200
    private val usbManager = application.getSystemService(Context.USB_SERVICE) as UsbManager
    private var port: UsbSerialPort? = null

    private val usbBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    logToTerminal("USB Device Attached")
                    connectToDevice() // Attempt to reconnect
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    logToTerminal("USB Device Detached")
                    disconnectDevice() // Handle disconnection cleanup
                }
            }
        }
    }

    init {
        // Observing dataStore values
        viewModelScope.launch { speedFlow.collect { speedValue -> speed = speedValue } }
        viewModelScope.launch { accelFlow.collect { accelValue -> accel = accelValue } }
        viewModelScope.launch { maxDelayFlow.collect { maxDelayValue -> maxDelay = maxDelayValue } }
        viewModelScope.launch { minDelayFlow.collect { minDelayValue -> minDelay = minDelayValue } }

        viewModelScope.launch { stepPositionFlow.collect { stepPositionValue -> stepPosition = stepPositionValue } }
        viewModelScope.launch { minStepPositionFlow.collect { minStepPositionValue -> minStepPosition = minStepPositionValue } }
        viewModelScope.launch { maxStepPositionFlow.collect { maxStepPositionValue -> maxStepPosition = maxStepPositionValue } }

        viewModelScope.launch { eightFtStopHeadFlow.collect { eightFtStopHeadValue -> eightFtStopHead = eightFtStopHeadValue } }
        viewModelScope.launch { tenFtStopHeadFlow.collect { tenFtStopHeadValue -> tenFtStopHead = tenFtStopHeadValue } }
        viewModelScope.launch { twelveFtStopHeadFlow.collect { twelveFtStopHeadValue -> twelveFtStopHead = twelveFtStopHeadValue } }

        viewModelScope.launch { stepsPerInchFlow.collect { stepsPerInchValue -> stepsPerInch = stepsPerInchValue } }

        viewModelScope.launch { stopHeadFlow.collect { stopHeadValue -> stopHead = stopHeadValue } }

        // Register for USB events
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        getApplication<Application>().registerReceiver(usbBroadcastReceiver, filter)

        connectToDevice()

        unit = if (isInch) "INCH:" else "MM:"
        unitMarker = if (isInch) "\"" else "mm"
        inchPosition = (stepPosition + getStopHeadSteps()) / stepsPerInch

        // Wait for parameters to be set
        viewModelScope.launch {
            var tries = 0
            while (!parametersSet && tries < 10) {
                sendData("CONFIRM")
                tries++
                delay(5000)
            }
        }
    }

    fun resetDefault(key: String) {
        viewModelScope.launch {
            when (key) {
                "Speed" -> {
                    application.applicationContext.dataStore.edit { preferences ->
                        preferences.remove(SPEED_KEY)
                    }
                }
                "Accel" -> {
                    application.applicationContext.dataStore.edit { preferences ->
                        preferences.remove(ACCEL_KEY)
                    }
                }
                "Max Delay" -> {
                    application.applicationContext.dataStore.edit { preferences ->
                        preferences.remove(MAX_DELAY_KEY)
                    }
                }
                "Min Delay" -> {
                    application.applicationContext.dataStore.edit { preferences ->
                        preferences.remove(MIN_DELAY_KEY)
                    }
                }

                "Step Position" -> {
                    application.applicationContext.dataStore.edit { preferences ->
                        preferences.remove(STEP_POSITION_KEY)
                    }
                }
                "Min Step Position" -> {
                    application.applicationContext.dataStore.edit { preferences ->
                        preferences.remove(MIN_STEP_POSITION_KEY)
                    }
                }
                "Max Step Position" -> {
                    application.applicationContext.dataStore.edit { preferences ->
                        preferences.remove(MAX_STEP_POSITION_KEY)
                    }
                }

                "8ft Stop Head" -> {
                    application.applicationContext.dataStore.edit { preferences ->
                        preferences.remove(EIGHT_FT_STOP_HEAD_KEY)
                    }
                }
                "10ft Stop Head" -> {
                    application.applicationContext.dataStore.edit { preferences ->
                        preferences.remove(TEN_FT_STOP_HEAD_KEY)
                    }
                }
                "12ft Stop Head" -> {
                    application.applicationContext.dataStore.edit { preferences ->
                        preferences.remove(TWELVE_FT_STOP_HEAD_KEY)
                    }
                }

                "Steps/Inch" -> {
                    application.applicationContext.dataStore.edit { preferences ->
                        preferences.remove(STEPS_PER_INCH_KEY)
                    }
                }
            }
        }
    }

    fun saveSettings(key: String) {
        viewModelScope.launch {
            when (key) {
                "Speed" -> {
                    application.applicationContext.dataStore.edit { preferences ->
                        preferences[SPEED_KEY] = speed
                    }
                }
                "Accel" -> {
                    application.applicationContext.dataStore.edit { preferences ->
                        preferences[ACCEL_KEY] = accel
                    }
                }
                "Max Delay" -> {
                    application.applicationContext.dataStore.edit { preferences ->
                        preferences[MAX_DELAY_KEY] = maxDelay
                    }
                }
                "Min Delay" -> {
                    application.applicationContext.dataStore.edit { preferences ->
                        preferences[MIN_DELAY_KEY] = minDelay
                    }
                }

                "Step Position" -> {
                    application.applicationContext.dataStore.edit { preferences ->
                        preferences[STEP_POSITION_KEY] = stepPosition
                    }
                }
                "Min Step Position" -> {
                    application.applicationContext.dataStore.edit { preferences ->
                        preferences[MIN_STEP_POSITION_KEY] = minStepPosition
                    }
                }
                "Max Step Position" -> {
                    application.applicationContext.dataStore.edit { preferences ->
                        preferences[MAX_STEP_POSITION_KEY] = maxStepPosition
                    }
                }

                "8ft Stop Head" -> {
                    application.applicationContext.dataStore.edit { preferences ->
                        preferences[EIGHT_FT_STOP_HEAD_KEY] = eightFtStopHead
                    }
                }
                "10ft Stop Head" -> {
                    application.applicationContext.dataStore.edit { preferences ->
                        preferences[TEN_FT_STOP_HEAD_KEY] = tenFtStopHead
                    }
                }
                "12ft Stop Head" -> {
                    application.applicationContext.dataStore.edit { preferences ->
                        preferences[TWELVE_FT_STOP_HEAD_KEY] = twelveFtStopHead
                    }
                }

                "Steps/Inch" -> {
                    application.applicationContext.dataStore.edit { preferences ->
                        preferences[STEPS_PER_INCH_KEY] = stepsPerInch
                    }
                }

                "Stop Head" -> {
                    application.applicationContext.dataStore.edit { preferences ->
                        preferences[STOP_HEAD_KEY] = stopHead
                    }
                }
            }
        }
    }

    fun getStopHeadSteps(): Int {
        return when (stopHead) {
            "8ft" -> (eightFtStopHead * stepsPerInch).toInt()
            "10ft" -> (tenFtStopHead * stepsPerInch).toInt()
            "12ft" -> (twelveFtStopHead * stepsPerInch).toInt()
            else -> 0
        }
    }

    fun lengthError(steps: Int, error: Int) {
        val stopHeadSteps = getStopHeadSteps()
        val inches = BigDecimal((stepPosition + stopHeadSteps + steps) / stepsPerInch)
            .setScale(3, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
        val minInches = BigDecimal((stopHeadSteps + minStepPosition) / stepsPerInch)
            .setScale(3, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
        val maxInches = BigDecimal((stopHeadSteps + maxStepPosition) / stepsPerInch)
            .setScale(3, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()

        when (error) {
            1 -> {
                logToTerminal("$inches\" is below the min position", "[ERR]")
                showLengthError = true
                lengthErrorTitle = "Too Short"
                lengthErrorMessage = "$inches\" is below the minimum length of $minInches\", For this stop head"
            }
            2 -> {
                logToTerminal("$inches\" is above the max position", "[ERR]")
                showLengthError = true
                lengthErrorTitle = "Too Long"
                lengthErrorMessage = "$inches\" is above the maximum length of $maxInches\", For this stop head"
            }
        }
    }

    fun moveSteps(steps: Int) {

        if (stepPosition + steps < minStepPosition) {
            lengthError(steps, 1)
            return
        } else if (stepPosition + steps > maxStepPosition) {
            lengthError(steps, 2)
            return
        }

        sendData("MOVE:$steps")
    }

    fun goToPosition(unitType: String = this.unit, distance: Float = this.inputNumber.toFloat()) {
        var stepsFromZero = if (unitType == "MM:") {
            (distance * (stepsPerInch / 25.4)).toInt()
        } else {
            (distance * stepsPerInch).toInt()
        }
        var stepsToGo = stepsFromZero - stepPosition

        if (unitType == "MM:") {
            if ((stepsFromZero / (stepsPerInch / 25.4)).let {
                    round(it * 1000) / 1000
                } < distance) {
                stepsToGo++
            }
        } else {
            if ((stepsFromZero / stepsPerInch).let {
                    round(it * 1000) / 1000
                } < distance) {
                stepsToGo++
            }
        }

        // If moving, stop and move to new position
        if (isMoving) {
            newMovePosition = stepsFromZero

            if (!isStopping) {
                sendData("X") // Stop Command
                isStopping = true
            } else {
                sendData("CHECK")
            }
        } else {
            moveSteps(stepsToGo - getStopHeadSteps())
            clearInput()
        }
    }

    fun goToStepPosition(steps: Int) {
        viewModelScope.launch {
            delay(100)
            var stepsToGo = steps - stepPosition
            moveSteps(stepsToGo - getStopHeadSteps())
        }
    }

    fun home(ready: Boolean) {
        if (ready) {
            sendData("HOME", true)
        } else {
            if (isHoming) {
                isHoming = false
                logToTerminal("Homing canceled", "[INFO]")
                return
            }
            isHoming = true
            sendData("MOVE:200", true)
        }
    }

    fun closeLengthError() {
        showLengthError = false
        lengthErrorTitle = ""
        lengthErrorMessage = ""
    }

    fun clearInput() {
        inputNumber = ""
        validNumber = ""
        isInvalidInput = false
    }

    fun toggleInch() {
        isInch = !isInch
        unit = if (isInch) "INCH:" else "MM:"
        unitMarker = if (isInch) "\"" else "mm"
    }

    fun changeStopHead(head: String = "8ft") {
        stopHead = head
        saveSettings("Stop Head")
    }

    fun endCalibration() {
        isCalibrating = false
        calibrationInput = ""
    }

    fun getCalibrationInput(): Double {
        return calibrationInput.toDouble() - (stepPosition / stepsPerInch)
    }

    fun recalibrate() {
        if (calibrationInput.isEmpty()) {
            endCalibration()
            return
        }

        when (stopHead) {
            "8ft" -> {
                logToTerminal("8ft Stop Head recalibrated", "[INFO]")
                logToTerminal("from $eightFtStopHead", "[INFO]")
                eightFtStopHead = getCalibrationInput()
                logToTerminal("to $eightFtStopHead", "[INFO]")
                saveSettings("8ft Stop Head")
            }
            "10ft" -> {
                logToTerminal("10ft Stop Head recalibrated", "[INFO]")
                logToTerminal("from $tenFtStopHead", "[INFO]")
                tenFtStopHead = getCalibrationInput()
                logToTerminal("to $tenFtStopHead", "[INFO]")
                saveSettings("10ft Stop Head")
            }
            "12ft" -> {
                logToTerminal("12ft Stop Head recalibrated", "[INFO]")
                logToTerminal("from $twelveFtStopHead", "[INFO]")
                twelveFtStopHead = getCalibrationInput()
                logToTerminal("to $twelveFtStopHead", "[INFO]")
                saveSettings("12ft Stop Head")
            }
        }

        endCalibration()
    }

    internal fun connectToDevice() {
        viewModelScope.launch {
            val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
            if (availableDrivers.isEmpty()) {
                logToTerminal("No USB device found")
                return@launch
            }

            val driver: UsbSerialDriver = availableDrivers[0]
            val device = driver.device

            val connection = usbManager.openDevice(device) ?: run {
                logToTerminal("Failed to open USB device", "[ERR]")
                return@launch
            }

            port = driver.ports[0].apply {
                open(connection)
                setParameters(baudRate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
                logToTerminal("Connected to ${device.deviceName}")
                connectionState = 1
            }

            readData()
        }
    }

    private fun disconnectDevice() {
        viewModelScope.launch {
            try {
                port?.close()
                port = null
                logToTerminal("USB device disconnected and port closed")
                connectionState = 0
            } catch (e: Exception) {
                logToTerminal("Error during port disconnection: ${e.message}", "[ERR]")
            }
        }
    }

    private fun readData() {
        viewModelScope.launch(Dispatchers.IO) {
            val buffer = ByteArray(1024)
            val lineBuffer = StringBuilder() // Buffer to accumulate partial lines
            while (port != null) {
                try {
                    val len = port!!.read(buffer, 1000)
                    if (len > 0) {
                        val data = buffer.copyOf(len)
                        synchronized(dataQueue) { dataQueue.add(data) }
                        val text = String(data, Charsets.UTF_8) // Explicitly use UTF-8
                        lineBuffer.append(text) // Append new data to the buffer

                        // Process complete lines
                        var lineEndIndex: Int
                        while (lineBuffer.indexOf("\r\n").also { lineEndIndex = it } >= 0) {
                            // Extract complete line (without \r\n)
                            val line = lineBuffer.substring(0, lineEndIndex)
                            lineBuffer.delete(0, lineEndIndex + 2) // Remove line + \r\n
                            if (line.isNotEmpty()) { // Ignore empty lines
                                withContext(Dispatchers.Main) {
                                    handleIncomingLine(line)
                                }
                            }
                        }
                    } else if (len < 0) {
                        // Handle end of stream (e.g., port closed)
                        withContext(Dispatchers.Main) {
                            logToTerminal("Port closed or end of stream reached", "[INFO]")
                        }
                        break // Exit loop if port is closed
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        logToTerminal("Read error: ${e.message}", "[ERR]")
                    }
                    // Delay before retrying to avoid tight loop on persistent errors
                    delay(100) // Wait 1 second before retrying
                    continue // Continue the loop instead of breaking
                }
            }
            // If there's remaining data in the buffer when the port closes, process it
            if (lineBuffer.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    logToTerminal(lineBuffer.toString())
                }
            }
        }
    }

    fun moveStart() {
        moveTimer(true)
        isMoving = true
    }

    fun moveStop() {
        moveTimer(false)
        isMoving = false

        if (isStopping) {
            goToStepPosition(newMovePosition)
            newMovePosition = 0
            isStopping = false
        }

        if (isHoming) {
            home(true)
        }
    }

    private fun handleIncomingLine(line: String) {
        if (line.startsWith("POS:") || line.startsWith("OS:")) { // sometimes the P in POS gets lost
            stepPositionText.value = line

            val parts = line.split(":")
            if (parts.size == 2) {
                stepPosition = parts[1].toInt()
                saveSettings("Step Position")
            }
        } else {
            logToTerminal(line, "")

            when (line) {
                "STARTED" -> {
                    moveStart()
                }
                "STOPPED" -> {
                    moveStop()
                }
                "TOPPED" -> { // sometimes the S in STOPPED gets lost
                    moveStop()
                }
                "MEGA READY" -> {
                    firstHoming = true
                    connectionState = 100
                    setMegaParameters("all")
                    parametersSet = true
                    viewModelScope.launch {
                        delay(1000)
                        sendData("LOG")
                    }
                }
                "HOME" -> {
                    isHoming = false
                }
                "ERR:HOMING_ERROR" -> {
                    home(false)
                }
                else -> {
                }
            }
        }
    }

    fun setMegaParameters(param: String) {
        if (param == "all") {
            viewModelScope.launch {
                sendData("SPEED:$speed")
                delay(100)
                sendData("ACCEL:$accel")
                delay(100)
                sendData("MAX_DELAY:$maxDelay")
                delay(100)
                sendData("MIN_DELAY:$minDelay")
                delay(100)
                sendData("POS:$stepPosition")
            }
        } else {
            when (param) {
                "Speed" -> sendData("SPEED:$speed")
                "Accel" -> sendData("ACCEL:$accel")
                "Max Delay" -> sendData("MAX_DELAY:$maxDelay")
                "Min Delay" -> sendData("MIN_DELAY:$minDelay")
                "Step Position" -> sendData("POS:$stepPosition")
            }
        }
    }

    fun moveTimer(run: Boolean) {
        if (run) {
            // Start a coroutine to track time
            timerJob = CoroutineScope(Dispatchers.Default).launch {
                while (isActive) { // `isActive` ensures that the coroutine stops when it's cancelled
                    delay(1)
                    moveTime += 0.001
                }
            }
        } else {
            // Stop the coroutine
            timerJob?.cancel()
            logToTerminal("%.2f sec".format(moveTime), "[TIME]")
            moveTime = 0.0
        }
    }


    fun sendData(text: String, force: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (isHoming && !force) {
                    logToTerminal("Homing in progress", "[ERR]")
                    return@launch
                }
                val data = (text + "\n").toByteArray()
                port?.write(data, 1000)
                withContext(Dispatchers.Main) {
                    logToTerminal(text, "[Sent]")
                    lastSentLine.value = text
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    logToTerminal("Send error: ${e.message}", "[ERR]")
                }
            }
        }
    }

    fun getDisplayPosition(): String {
        if (connectionState == 0) return "STATE: Disconnected"
        if (connectionState == 1) return "STATE: Loading..."
        if (isHoming) return "STATE: Homing..."

        inchPosition = (stepPosition + getStopHeadSteps()) / stepsPerInch
        return if (unit == "MM:") {
            "%.1f".format(inchPosition * 25.4)
        } else {
            "%.3f".format(inchPosition)
        }
    }

    fun logToTerminal(text: String, type: String = "[LOG]") {
        terminalText.value = (terminalText.value + "$type $text\n").takeLast(500)
    }

    fun clearTerminal() {
        if (terminalText.value.isNotEmpty()) {
            terminalText.value = emptyList()
        }
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().unregisterReceiver(usbBroadcastReceiver)
        disconnectDevice()

    }
}