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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.christian.ocoochchopstop.data.ChopStopRepository
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.*
import kotlin.math.round

class ChopStopViewModel(
    application: Application,
    private val repository: ChopStopRepository
) : AndroidViewModel(application) {

    var speed by mutableStateOf(0)
    var accel by mutableStateOf(0)
    var maxDelay by mutableStateOf(0)
    var minDelay by mutableStateOf(0)

    var eightFtStopHead by mutableStateOf(0.0)
    var tenFtStopHead by mutableStateOf(0.0)
    var twelveFtStopHead by mutableStateOf(0.0)

    var stepsPerInch by mutableStateOf(0.0)
    var stepsPerMm by mutableStateOf(0.0)

    var stepPosition by mutableStateOf(0)
    var minStepPosition by mutableStateOf(0)
    var maxStepPosition by mutableStateOf(170666)

    var stopHead by mutableStateOf("8ft")

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
            repository.speedFlow.collect { speedValue ->
                speed = speedValue // Update speed
            }
        }
        viewModelScope.launch {
            repository.accelFlow.collect { accelValue ->
                accel = accelValue // Update accel
            }
        }
        viewModelScope.launch {
            repository.maxDelayFlow.collect { maxDelayValue ->
                maxDelay = maxDelayValue // Update maxDelay
            }
        }
        viewModelScope.launch {
            repository.minDelayFlow.collect { minDelayValue ->
                minDelay = minDelayValue // Update minDelay
            }
        }

        viewModelScope.launch {
            repository.eightFtStopHeadFlow.collect { eightFtStopHeadValue ->
                eightFtStopHead = eightFtStopHeadValue // Update eightFtStopHead
            }
        }
        viewModelScope.launch {
            repository.tenFtStopHeadFlow.collect { tenFtStopHeadValue ->
                tenFtStopHead = tenFtStopHeadValue // Update tenFtStopHead
            }
        }
        viewModelScope.launch {
            repository.twelveFtStopHeadFlow.collect { twelveFtStopHeadValue ->
                twelveFtStopHead = twelveFtStopHeadValue // Update twelveFtStopHead
            }
        }

        viewModelScope.launch {
            repository.stepsPerInchFlow.collect { stepsPerInchValue ->
                stepsPerInch = stepsPerInchValue // Update stepsPerInch
            }
        }
        viewModelScope.launch {
            repository.stepsPerMmFlow.collect { stepsPerMmValue ->
                stepsPerMm = stepsPerMmValue // Update stepsPerMm
            }
        }

        viewModelScope.launch {
            repository.stepPositionFlow.collect { stepPositionValue ->
                stepPosition = stepPositionValue // Update stepPosition
            }
        }
        viewModelScope.launch {
            repository.minStepPositionFlow.collect { minStepPositionValue ->
                minStepPosition = minStepPositionValue // Update minStepPosition
            }
        }
        viewModelScope.launch {
            repository.maxStepPositionFlow.collect { maxStepPositionValue ->
                maxStepPosition = maxStepPositionValue // Update maxStepPosition
            }
        }

        viewModelScope.launch {
            repository.stopHeadFlow.collect { stopHeadValue ->
                stopHead = stopHeadValue // Update stopHead
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

        viewModelScope.launch {
            var tries = 0
            while (!parametersSet && tries < 10) {
                sendData("CONFIRM")
                tries++
                delay(5000)
            }
        }
    }

    fun saveSettings(key: String) {
        viewModelScope.launch {
            when (key) {
                "Speed" -> repository.setSpeed(speed)
                "Accel" -> repository.setAccel(accel)
                "Max Delay" -> repository.setMaxDelay(maxDelay)
                "Min Delay" -> repository.setMinDelay(minDelay)

                "8ft Stop Head" -> repository.setEightFtStopHead(eightFtStopHead)
                "10ft Stop Head" -> repository.setTenFtStopHead(tenFtStopHead)
                "12ft Stop Head" -> repository.setTwelveFtStopHead(twelveFtStopHead)

                "Steps/Inch" -> repository.setStepsPerInch(stepsPerInch)
                "Steps/mm" -> repository.setStepsPerMm(stepsPerMm)

                "Step Position" -> repository.setStepPosition(stepPosition)
                "Min Step Position" -> repository.setMinStepPosition(minStepPosition)
                "Max Step Position" -> repository.setMaxStepPosition(maxStepPosition)

                "Stop Head" -> repository.setStopHead(stopHead)
            }
        }
    }

    fun moveSteps(steps: Int) {
        if (steps < 0) {
            if (stepPosition + steps < minStepPosition) {
                log("Can't move $steps steps below min step position", "[ERR]")
                return
            }
        } else {
            if (stepPosition + steps > maxStepPosition) {
                log("Can't move $steps steps above max step position", "[ERR]")
                return
            }
        }
        sendData("MOVE:$steps")
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

        moveSteps(stepsToGo)
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

    fun changeStopHead(head: String = "8ft") {
        stopHead = head
        saveSettings("Stop Head")
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
                saveSettings("Step Position")
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
        if (param == "all") {
            sendData("SPEED:$speed")
            sendData("ACCEL:$accel")
            sendData("MAX_DELAY:$maxDelay")
            sendData("MIN_DELAY:$minDelay")
            sendData("POS:$stepPosition")
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