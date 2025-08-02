package com.christian.ocoochchopstop

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
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.*

class CopStopViewModel(application: Application) : AndroidViewModel(application) {
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

    var speed by mutableStateOf(20000)
    var accel by mutableStateOf(8000)
    var maxDelay by mutableStateOf(320)
    var minDelay by mutableStateOf(6)
    var stepsPerInch by mutableStateOf(1777.77777778)
    var stepsPerMm by mutableStateOf(69.9912510935)

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

    fun goToPosition(unitType: String = this.unit, distance: Float = this.inputNumber.toFloat()) {
        var stepsFromZero = if (unitType == "INCH:") {
            (distance * stepsPerInch).toInt()
        } else {
            (distance * stepsPerMm).toInt()
        }
        var stepsToGo = stepsFromZero - stepPosition

        if (unitType == "INCH:") {
            if ((stepsFromZero / stepsPerInch).let {
                    kotlin.math.round(it * 1000) / 1000
                } < distance) {
                stepsToGo++
            }
        } else {
            if ((stepsFromZero / stepsPerMm).let {
                    kotlin.math.round(it * 1000) / 1000
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