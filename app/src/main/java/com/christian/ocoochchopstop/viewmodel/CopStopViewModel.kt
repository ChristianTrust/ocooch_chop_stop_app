package com.christian.ocoochchopstop.viewmodel

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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.christian.ocoochchopstop.datastore.dataStore
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.map
import kotlin.math.round

class CopStopViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private val SPEED_KEY = intPreferencesKey("speed")
        private val ACCEL_KEY = intPreferencesKey("accel")
        private val MAX_DELAY_KEY = intPreferencesKey("max_delay")
        private val MIN_DELAY_KEY = intPreferencesKey("min_delay")
        private val STEPS_PER_INCH_KEY = doublePreferencesKey("steps_per_inch")
        private val STEPS_PER_MM_KEY = doublePreferencesKey("steps_per_mm")

        private val STEP_POSITION_KEY = intPreferencesKey("step_position")
    }

    val speedFlow = application.applicationContext.dataStore.data.map { preferences ->
        preferences[SPEED_KEY] ?: 20000
    }
    val accelFlow = application.applicationContext.dataStore.data.map { preferences ->
        preferences[ACCEL_KEY] ?: 8000
    }
    val maxDelayFlow = application.applicationContext.dataStore.data.map { preferences ->
        preferences[MAX_DELAY_KEY] ?: 320
    }
    val minDelayFlow = application.applicationContext.dataStore.data.map { preferences ->
        preferences[MIN_DELAY_KEY] ?: 6
    }
    val stepsPerInchFlow = application.applicationContext.dataStore.data.map { preferences ->
        preferences[STEPS_PER_INCH_KEY] ?: 1777.77777778
    }
    val stepsPerMmFlow = application.applicationContext.dataStore.data.map { preferences ->
        preferences[STEPS_PER_MM_KEY] ?: 69.9912510935
    }
    val stepPositionFlow = application.applicationContext.dataStore.data.map { preferences ->
        preferences[STEP_POSITION_KEY] ?: 0
    }

    var speed by mutableStateOf(0)
    var accel by mutableStateOf(0)
    var maxDelay by mutableStateOf(0)
    var minDelay by mutableStateOf(0)
    var stepsPerInch by mutableStateOf(0.0)
    var stepsPerMm by mutableStateOf(0.0)

    var stepPosition by mutableStateOf(0)
    var unitPosition by mutableStateOf("")
    var parametersSet by mutableStateOf(false)

    var inputNumber by mutableStateOf("")
    var validNumber by mutableStateOf("")
    var isInvalidInput by mutableStateOf(false)
    var isInch by mutableStateOf(true)
    var unit by mutableStateOf("")
    var unitMarker by mutableStateOf("")
    var maxLength by mutableStateOf(96.0)
    var isDecimal by mutableStateOf(false)

    val terminalText = mutableStateOf<List<String>>(listOf())
    val lastReadLine = mutableStateOf("")

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
                    log("USB Device Attached")
                    connectToDevice() // Attempt to reconnect
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    log("USB Device Detached")
                    disconnectDevice() // Handle disconnection cleanup
                }
            }
        }
    }

    init {
        // Observing dataStore values
        viewModelScope.launch {
            speedFlow.collect { speedValue ->
                speed = speedValue // Update speed
            }
        }
        viewModelScope.launch {
            accelFlow.collect { accelValue ->
                accel = accelValue // Update accel
            }
        }
        viewModelScope.launch {
            maxDelayFlow.collect { maxDelayValue ->
                maxDelay = maxDelayValue // Update maxDelay
            }
        }
        viewModelScope.launch {
            minDelayFlow.collect { minDelayValue ->
                minDelay = minDelayValue // Update minDelay
            }
        }
        viewModelScope.launch {
            stepsPerInchFlow.collect { stepsPerInchValue ->
                stepsPerInch = stepsPerInchValue // Update stepsPerInch
            }
        }
        viewModelScope.launch {
            stepsPerMmFlow.collect { stepsPerMmValue ->
                stepsPerMm = stepsPerMmValue // Update stepsPerMm
            }
        }
        viewModelScope.launch {
            stepPositionFlow.collect { stepPositionValue ->
                stepPosition = stepPositionValue // Update stepPosition
            }
        }

        // Register for USB events
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        getApplication<Application>().registerReceiver(usbBroadcastReceiver, filter)

        connectToDevice()

        unit = if (isInch) "INCH:" else "MM:"
        unitMarker = if (isInch) "\"" else "mm"
    }

    suspend fun setSpeed(speed: Int) {
        application.applicationContext.dataStore.edit { preferences ->
            preferences[SPEED_KEY] = speed
        }
    }
    suspend fun setAccel(accel: Int) {
        application.applicationContext.dataStore.edit { preferences ->
            preferences[ACCEL_KEY] = accel
        }
    }
    suspend fun setMaxDelay(maxDelay: Int) {
        application.applicationContext.dataStore.edit { preferences ->
            preferences[MAX_DELAY_KEY] = maxDelay
        }
    }
    suspend fun setMinDelay(minDelay: Int) {
        application.applicationContext.dataStore.edit { preferences ->
            preferences[MIN_DELAY_KEY] = minDelay
        }
    }
    suspend fun setStepsPerInch(stepsPerInch: Double) {
        application.applicationContext.dataStore.edit { preferences ->
            preferences[STEPS_PER_INCH_KEY] = stepsPerInch
        }
    }
    suspend fun setStepsPerMm(stepsPerMm: Double) {
        application.applicationContext.dataStore.edit { preferences ->
            preferences[STEPS_PER_MM_KEY] = stepsPerMm
        }
    }

    fun saveSettings(key: String) {
        viewModelScope.launch {
            when (key) {
                "Speed" -> setSpeed(speed)
                "Accel" -> setAccel(accel)
                "Max Delay" -> setMaxDelay(maxDelay)
                "Min Delay" -> setMinDelay(minDelay)
                "Steps/Inch" -> setStepsPerInch(stepsPerInch)
                "Steps/mm" -> setStepsPerMm(stepsPerMm)
            }
        }
    }

    fun saveStepPosition(newStepPosition: Int) {
        viewModelScope.launch {
            application.applicationContext.dataStore.edit { preferences ->
                preferences[STEP_POSITION_KEY] = newStepPosition
            }
        }
    }

    fun goToPosition(unitType: String = this.unit, distance: Float = this.inputNumber.toFloat()) {
        var stepsFromZero = if (unitType == "INCH:") {
            (distance * stepsPerInch).toInt()
        } else {
            (distance * stepsPerMm).toInt()
        }
        var stepsToGo = stepsFromZero - stepPosition

        if (unitType == "INCH:") {
            if ((stepsFromZero / stepsPerInch).let {
                    round(it * 1000) / 1000
                } < distance) {
                stepsToGo++
            }
        } else {
            if ((stepsFromZero / stepsPerMm).let {
                    round(it * 1000) / 1000
                } < distance) {
                stepsToGo++
            }
        }

        sendData("MOVE:$stepsToGo")
        clearInput()
    }

    fun addToNumber(number: String) {
        val maxDecimalPlaces = if (isInch) 4 else 2
        val maxDigits = if (isInch) 7 else 7

        inputNumber += number

        if (!isDecimal) {
            isDecimal = (number == ".")

            if (inputNumber == ".") {
                inputNumber = "0."
            }
        }

        if (inputNumber == "00") {
            inputNumber = "0"
        }

        // Limit the number of characters
        if (inputNumber.length > maxDigits) {
            inputNumber = inputNumber.substring(0, maxDigits)
            validNumber = inputNumber
            isInvalidInput = true
        }

        // Check and limit decimal places
        if (inputNumber.contains(".")) {
            val parts = inputNumber.split(".")
            var mainPart = parts[0]
            var decimalPart = parts[1]

            if (parts.size == 2) {
                if (decimalPart.length > maxDecimalPlaces) {
                    validNumber = "${mainPart}.${decimalPart.substring(0, maxDecimalPlaces)}"
                    isInvalidInput = true
                }
            }
        }

        inputNumber.toDoubleOrNull()?.let { num ->
            if (num > maxLength) {
                validNumber = inputNumber.substring(0, inputNumber.length - 1)
                isInvalidInput = true
            }
        }
    }

    fun toggleInch() {
        isInch = !isInch
        unit = if (isInch) "INCH:" else "MM:"
        unitMarker = if (isInch) "\"" else "mm"
        maxLength = if (isInch) 96.0 else 2440.0

        inputNumber.toDoubleOrNull()?.let { num ->
            var newNum = num

            while (newNum > maxLength) {
                newNum = newNum / 10
            }
            inputNumber = newNum.toString()
        }

        addToNumber("")
    }

    fun back() {
        if (inputNumber.isNotEmpty()) {
            if (inputNumber.last() == '.') {
                isDecimal = false
            }
            inputNumber = inputNumber.substring(0, inputNumber.length - 1)
        }
    }

    fun clearInput() {
        inputNumber = ""
        validNumber = ""
        isInvalidInput = false
        isDecimal = false
    }

    internal fun connectToDevice() {
        viewModelScope.launch {
            val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
            if (availableDrivers.isEmpty()) {
                log("No USB device found")
                return@launch
            }

            val driver: UsbSerialDriver = availableDrivers[0]
            val device = driver.device

            val connection = usbManager.openDevice(device) ?: run {
                log("Failed to open USB device", "[ERR]")
                return@launch
            }

            port = driver.ports[0].apply {
                open(connection)
                setParameters(baudRate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
                log("Connected to ${device.deviceName}")
                lastReadLine.value = "Loading"
            }

            // Wait for the port to be ready
            delay(1000)
            sendData("CONFIRM")
            delay(2000)
            sendData("CONFIRM")
            readData()
        }
    }

    private fun disconnectDevice() {
        viewModelScope.launch {
            try {
                port?.close()
                port = null
                log("USB device disconnected and port closed")
            } catch (e: Exception) {
                log("Error during port disconnection: ${e.message}", "[ERR]")
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
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        log("Read error: ${e.message}", "[ERR]")
                    }
                    break
                }
            }
            // If there's remaining data in the buffer when the port closes, process it
            if (lineBuffer.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    log(lineBuffer.toString())
                }
            }
        }
    }

    private fun handleIncomingLine(line: String) {
        log(line, "")
        lastReadLine.value = line

        if (line.startsWith("POS:")) {
            val parts = line.split(":")
            if (parts.size == 2) {
                stepPosition = parts[1].toInt()
                saveStepPosition(stepPosition)
            }
        }
        else if (line == "STARTED") {
            moveTimer(true)
        }
        else if (line == "STOPPED") {
            moveTimer(false)
        }
        else if (line == "MEGA READY") {
            setMegaParameters("all")
            parametersSet = true
            viewModelScope.launch {
                delay(1000)
                sendData("LOG")
            }
        }
    }

    fun setMegaParameters(param: String) {
        if (param == "Speed" || param == "all") {
            sendData("SPEED:$speed")
        }
        if (param == "Accel" || param == "all") {
            sendData("ACCEL:$accel")
        }
        if (param == "Max Delay" || param == "all") {
            sendData("MAX_DELAY:$maxDelay")
        }
        if (param == "Min Delay" || param == "all") {
            sendData("MIN_DELAY:$minDelay")
        }
        if (param == "Step Position" || param == "all") {
            sendData("POS:$stepPosition")
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
            log("%.2f sec".format(moveTime), "[TIME]")
            moveTime = 0.0
        }
    }


    fun sendData(text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val data = (text + "\n").toByteArray()
                port?.write(data, 1000)
                withContext(Dispatchers.Main) {
                    log(text, "[Sent]")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    log("Send error: ${e.message}", "[ERR]")
                }
            }
        }
    }

    fun getDisplayPosition(): String {
        if (lastReadLine.value.isEmpty()) {
            return "STATE: Disconnected"
        } else if (lastReadLine.value == "Loading") {
            return "STATE: Loading..."
        }

        unitPosition = (if (unit == "INCH:") {
            "%.3f".format(stepPosition / stepsPerInch)
        } else "%.1f".format(stepPosition / stepsPerMm))

        return unitPosition
    }

    fun log(text: String, type: String = "[LOG]") {
        terminalText.value = (terminalText.value + "$type $text\n").takeLast(1000)
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