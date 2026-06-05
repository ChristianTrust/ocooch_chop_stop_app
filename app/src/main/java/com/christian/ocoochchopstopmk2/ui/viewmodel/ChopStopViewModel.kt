package com.christian.ocoochchopstopmk2.ui.viewmodel

import android.app.Application
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.christian.ocoochchopstopmk2.data.ChopStopRepository
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.round

@Suppress("PLATFORM_TYPE_INFERENCE")
class ChopStopViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ChopStopRepository(application.applicationContext)

    companion object {
        private const val INTENT_ACTION_GRANT_USB = "com.christian.GRANT_USB"
    }

    // Flow variables
    val speedFlow: Flow<Int> = repository.speedFlow
    val accelFlow: Flow<Int> = repository.accelFlow
    val maxDelayFlow: Flow<Int> = repository.maxDelayFlow
    val minDelayFlow: Flow<Int> = repository.minDelayFlow

    val directionFlow: Flow<String> = repository.directionFlow
    val stepPositionFlow: Flow<Int> = repository.stepPositionFlow
    val minStepPositionFlow: Flow<Int> = repository.minStepPositionFlow
    val maxStepPositionFlow: Flow<Int> = repository.maxStepPositionFlow

    val eightFtStopHeadFlow: Flow<Double> = repository.eightFtStopHeadFlow
    val tenFtStopHeadFlow: Flow<Double> = repository.tenFtStopHeadFlow
    val sixFtStopHeadFlow: Flow<Double> = repository.sixFtStopHeadFlow

    val stepsPerInchFlow: Flow<Double> = repository.stepsPerInchFlow

    val stopHeadFlow: Flow<String> = repository.stopHeadFlow
    val tableLengthFlow: Flow<String> = repository.tableLengthFlow

    val deviceIdFlow: Flow<String> = repository.deviceIdFlow
    val basicAuthUsernameFlow: Flow<String> = repository.basicAuthUsernameFlow
    val basicAuthPasswordFlow: Flow<String> = repository.basicAuthPasswordFlow

    var speed: Int by mutableIntStateOf(0)
    var accel: Int by mutableIntStateOf(0)
    var maxDelay: Int by mutableIntStateOf(0)
    var minDelay: Int by mutableIntStateOf(0)

    var direction: String by mutableStateOf("")
    var stepPosition: Int by mutableIntStateOf(0)
    var minStepPosition: Int by mutableIntStateOf(0)
    var maxStepPosition: Int by mutableIntStateOf(0)

    var eightFtStopHead: Double by mutableDoubleStateOf(0.0)
    var tenFtStopHead: Double by mutableDoubleStateOf(0.0)
    var sixFtStopHead: Double by mutableDoubleStateOf(0.0)

    var deviceId: String by mutableStateOf("")
    var basicAuthUsername: String by mutableStateOf("")
    var basicAuthPassword: String by mutableStateOf("")

    var stepsPerInch: Double by mutableDoubleStateOf(1775.36)

    var stopHead: String by mutableStateOf("8ft")
    var tableLength: String by mutableStateOf("8ft")

    var inchPosition: Double by mutableDoubleStateOf(0.0)
    var parametersSet: Boolean by mutableStateOf(false)

    var inputNumber: String by mutableStateOf("")
    var validNumber: String by mutableStateOf("")
    var isInvalidInput: Boolean by mutableStateOf(false)
    var isInch: Boolean by mutableStateOf(true)
    var unit: String by mutableStateOf("")
    var unitMarker: String by mutableStateOf("")

    var isMotorPowered: Boolean by mutableStateOf(false)
    var isMoving: Boolean by mutableStateOf(false)
    var isStopping: Boolean by mutableStateOf(false)
    var isHoming: Boolean by mutableStateOf(false)
    var isCalibrating: Boolean by mutableStateOf(false)

    var calibrationInput: String by mutableStateOf("")
    var newMovePosition: Int by mutableIntStateOf(0)

    var showError: Boolean by mutableStateOf(false)
    var errorTitle: String by mutableStateOf("")
    var errorMessage: String by mutableStateOf("")
//    var connectionState by mutableIntStateOf(0)

    var activeBlockState by mutableStateOf(BlockState.NONE)

    val terminalText: MutableState<List<String>> = mutableStateOf(listOf())
    val stepPositionText: MutableState<String> = mutableStateOf("")
    val lastSentLine: MutableState<String> = mutableStateOf("")

    //    private val dataQueue = ArrayDeque<ByteArray>()
    private val lineBuffer = StringBuilder()
    private val dataQueue = mutableListOf<ByteArray>()
    private val baudRate = 115200
    private val usbManager = application.getSystemService(Context.USB_SERVICE) as UsbManager
    private var port: UsbSerialPort? = null
    private var ioManager: SerialInputOutputManager? = null
    private var usbPermissionReceiver: BroadcastReceiver? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        ERROR
    }

    enum class BlockState(val value: Int) {
        NONE(0),
        TWELVE(12),
        TWENTY(20)
    }

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

        viewModelScope.launch { directionFlow.collect { directionValue -> direction = directionValue } }
        viewModelScope.launch { stepPositionFlow.collect { stepPositionValue -> stepPosition = stepPositionValue } }
        viewModelScope.launch {
            minStepPositionFlow.collect { minStepPositionValue ->
                minStepPosition = minStepPositionValue
            }
        }
        viewModelScope.launch {
            maxStepPositionFlow.collect { maxStepPositionValue ->
                maxStepPosition = maxStepPositionValue
            }
        }

        viewModelScope.launch {
            eightFtStopHeadFlow.collect { eightFtStopHeadValue ->
                eightFtStopHead = eightFtStopHeadValue
            }
        }
        viewModelScope.launch { tenFtStopHeadFlow.collect { tenFtStopHeadValue -> tenFtStopHead = tenFtStopHeadValue } }
        viewModelScope.launch {
            sixFtStopHeadFlow.collect { sixFtStopHeadValue ->
                sixFtStopHead = sixFtStopHeadValue
            }
        }

        viewModelScope.launch { stepsPerInchFlow.collect { stepsPerInchValue -> stepsPerInch = stepsPerInchValue } }

        viewModelScope.launch { stopHeadFlow.collect { stopHeadValue -> stopHead = stopHeadValue } }
        viewModelScope.launch { tableLengthFlow.collect { tableLengthValue -> tableLength = tableLengthValue } }

        viewModelScope.launch { deviceIdFlow.collect { deviceId = it } }
        viewModelScope.launch { basicAuthUsernameFlow.collect { basicAuthUsername = it } }
        viewModelScope.launch { basicAuthPasswordFlow.collect { basicAuthPassword = it } }

        setupUsbPermissionReceiver()
        // Register for USB events
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        getApplication<Application>().registerReceiver(usbBroadcastReceiver, filter)

        connectToDevice()

        unit = if (isInch) "INCH:" else "MM:"
        unitMarker = if (isInch) "\"" else "mm"
        inchPosition = calculateInchPosition()

        // Wait for parameters to be set
        confirmConnection(2)
    }

    fun confirmConnection(tries: Int) {
        viewModelScope.launch {
            repeat(tries) {
                if (parametersSet) return@launch
                sendData("CONFIRM")
                delay(5000)
            }
        }
    }

    fun resetDefault(key: String) {
        viewModelScope.launch {
            when (key) {
                "Speed" -> repository.resetSpeed()
                "Accel" -> repository.resetAccel()
                "Max Delay" -> repository.resetMaxDelay()
                "Min Delay" -> repository.resetMinDelay()
                "Direction" -> repository.resetDirection()
                "Step Position" -> repository.resetStepPosition()
                "Min Step Position" -> repository.resetMinStepPosition()
                "Max Step Position" -> repository.resetMaxStepPosition()
                "8ft Stop Head" -> repository.resetEightFtStopHead()
                "10ft Stop Head" -> repository.resetTenFtStopHead()
                "6ft Stop Head" -> repository.resetSixFtStopHead()
                "Steps/Inch" -> repository.resetStepsPerInch()
                "Table Length" -> repository.resetTableLength()
            }
        }
    }

    fun saveSettings(key: String) {
        viewModelScope.launch {
            when (key) {
                "Speed" -> repository.saveSpeed(speed)
                "Accel" -> repository.saveAccel(accel)
                "Max Delay" -> repository.saveMaxDelay(maxDelay)
                "Min Delay" -> repository.saveMinDelay(minDelay)
                "Direction" -> {
                    repository.saveDirection(direction)
                    setMegaParameters("Direction")
                }
                "Step Position" -> repository.saveStepPosition(stepPosition)
                "Min Step Position" -> repository.saveMinStepPosition(minStepPosition)
                "Max Step Position" -> repository.saveMaxStepPosition(maxStepPosition)
                "8ft Stop Head" -> repository.saveEightFtStopHead(eightFtStopHead)
                "10ft Stop Head" -> repository.saveTenFtStopHead(tenFtStopHead)
                "6ft Stop Head" -> repository.saveSixFtStopHead(sixFtStopHead)
                "Steps/Inch" -> repository.saveStepsPerInch(stepsPerInch)
                "Stop Head" -> repository.saveStopHead(stopHead)
                "Table Length" -> repository.saveTableLength(tableLength)
            }
        }
    }

    fun getStopHeadSteps(): Int {
        return when (stopHead) {
            "8ft" -> (eightFtStopHead * stepsPerInch).toInt()
            "10ft" -> (tenFtStopHead * stepsPerInch).toInt()
            "6ft" -> (sixFtStopHead * stepsPerInch).toInt()
            else -> 0
        }
    }
    
    fun calculateInchPosition(): Double {
        return ((stepPosition + getStopHeadSteps()) / stepsPerInch) - activeBlockState.value
    }

    fun lengthError(steps: Int, error: Int) {
        val stopHeadSteps = getStopHeadSteps()
        val currentPosition = (stepPosition + stopHeadSteps + steps) / stepsPerInch - activeBlockState.value
        val minPosition = (stopHeadSteps + minStepPosition) / stepsPerInch - activeBlockState.value
        val maxPosition = (stopHeadSteps + maxStepPosition) / stepsPerInch - activeBlockState.value

        val inches = BigDecimal(currentPosition)
            .setScale(3, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
        val minInches = BigDecimal(minPosition)
            .setScale(3, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
        val maxInches = BigDecimal(maxPosition)
            .setScale(3, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()

        when (error) {
            1 -> {
                logToTerminal("$inches\" is below the min position", "[ERR]")
                showError = true
                errorTitle = "Too Short"
                errorMessage = "$inches\" is below the minimum length of $minInches\", For this stop head"
            }

            2 -> {
                logToTerminal("$inches\" is above the max position", "[ERR]")
                showError = true
                errorTitle = "Too Long"
                errorMessage = "$inches\" is above the maximum length of $maxInches\", For this stop head"
            }
        }
    }

    fun moveSteps(steps: Int) {

        if (connectionState.value != ConnectionState.CONNECTED) {
            showError = true
            errorTitle = "Not Connected"
            errorMessage = "Please connect to the Chop Stop"
            return
        } else if (!isMotorPowered) {
            showError = true
            errorTitle = "No Power"
            errorMessage = "The motor is not powered, Please turn it on"
            return
        }

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
        val physicalDistance = if (unitType == "MM:") {
            distance + (activeBlockState.value * 25.4f)
        } else {
            distance + activeBlockState.value
        }

        val stepsFromZero = if (unitType == "MM:") {
            (physicalDistance * (stepsPerInch / 25.4)).toInt()
        } else {
            (physicalDistance * stepsPerInch).toInt()
        }
        var stepsToGo = stepsFromZero - stepPosition

        if (unitType == "MM:") {
            if ((stepsFromZero / (stepsPerInch / 25.4)).let {
                    round(it * 1000) / 1000
                } < physicalDistance) {
                stepsToGo++
            }
        } else {
            if ((stepsFromZero / stepsPerInch).let {
                    round(it * 1000) / 1000
                } < physicalDistance) {
                stepsToGo++
            }
        }

        // If moving, stop and move to a new position
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
            val stepsToGo = steps - stepPosition
            moveSteps(stepsToGo - getStopHeadSteps())
        }
    }

    fun home(ready: Boolean) {

        if (connectionState.value != ConnectionState.CONNECTED) {
            showError = true
            errorTitle = "Not Connected"
            errorMessage = "Please connect to the Chop Stop"
            return
        } else if (!isMotorPowered) {
            showError = true
            errorTitle = "No Power"
            errorMessage = "The motor is not powered, Please turn it on"
            return
        }

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

    fun closeError() {
        showError = false
        errorTitle = ""
        errorMessage = ""
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
        val inputInches = if (unit == "MM:") calibrationInput.toDouble() / 25.4 else calibrationInput.toDouble()
        return inputInches + activeBlockState.value - (stepPosition / stepsPerInch)
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

            "6ft" -> {
                logToTerminal("6ft Stop Head recalibrated", "[INFO]")
                logToTerminal("from $sixFtStopHead", "[INFO]")
                sixFtStopHead = getCalibrationInput()
                logToTerminal("to $sixFtStopHead", "[INFO]")
                saveSettings("6ft Stop Head")
            }
        }

        endCalibration()
    }

    // USB ////////////////////////////////////////////////////////////////////////////////////////////////////// USB //

    private fun setupUsbPermissionReceiver() {
        logToTerminal("USB permission receiver setup")

        // Unregister any existing receiver first
        usbPermissionReceiver?.let {
            try {
                application.unregisterReceiver(it)
            } catch (_: IllegalArgumentException) {
                // Receiver wasn't registered, ignore
            }
        }

        usbPermissionReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (INTENT_ACTION_GRANT_USB == intent.action) {
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, true)
                    if (granted) {
                        logToTerminal("USB permission granted")
                        connectToDevice()
                    } else {
                        logToTerminal("USB permission denied", "[ERR]")
                        _connectionState.value = ConnectionState.ERROR
                        connectToDevice()
                    }
                }
            }
        }

        val filter = IntentFilter(INTENT_ACTION_GRANT_USB)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            application.registerReceiver(usbPermissionReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            application.registerReceiver(usbPermissionReceiver, filter)
        }
    }

    internal fun connectToDevice() {
        viewModelScope.launch {
            val customProber = UsbSerialProber.getDefaultProber()
            var availableDrivers = customProber.findAllDrivers(usbManager)
            // If no drivers found with default prober, try CDC prober
            if (availableDrivers.isEmpty()) {
                // Try to manually find CDC devices
                for (device in usbManager.deviceList.values) {
                    val driver = CdcAcmSerialDriver(device)
                    if (driver.ports.isNotEmpty()) {
                        availableDrivers = listOf(driver)
                        break
                    }
                }
            }
            if (availableDrivers.isEmpty()) {
                logToTerminal("No USB device found")
                return@launch
            }

            val driver: UsbSerialDriver = availableDrivers[0]
            val device = driver.device

            // Check if we have permission
            if (!usbManager.hasPermission(device)) {
                logToTerminal("Requesting USB permission...")
                val intent = Intent(INTENT_ACTION_GRANT_USB)
                val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.getBroadcast(
                        application,
                        0,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                } else {
                    PendingIntent.getBroadcast(
                        application,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                }
                usbManager.requestPermission(device, pendingIntent)
                return@launch
            }

            // Try to open the device
            val connection = usbManager.openDevice(device)
            if (connection == null) {
                logToTerminal("Failed to open USB device - no permission", "[ERR]")
                _connectionState.value = ConnectionState.ERROR
                return@launch
            }

            try {
                // Get the first available port
                if (driver.ports.isEmpty()) {
                    logToTerminal("No ports available on device", "[ERR]")
                    connection.close()
                    _connectionState.value = ConnectionState.ERROR
                    return@launch
                }

                port = driver.ports[0].apply {
                    open(connection)

                    // Set parameters with error handling
                    try {
                        setParameters(
                            baudRate,
                            8,
                            UsbSerialPort.STOPBITS_1,
                            UsbSerialPort.PARITY_NONE
                        )
                    } catch (e: Exception) {
                        logToTerminal("Warning: Could not set parameters: ${e.message}")
                        // Some CDC devices ignore baud rate, continue anyway
                    }

                    // Optional: Set control lines for better compatibility
                    dtr = true
                    rts = true

                    logToTerminal("Connected to ${device.deviceName}")
                    logToTerminal("Driver: ${driver.javaClass.simpleName}")
                    _connectionState.value = ConnectionState.CONNECTING
                }

                startIoManager()

            } catch (e: Exception) {
                logToTerminal("Connection error: ${e.message}", "[ERR]")
                connection.close()
                port = null
                _connectionState.value = ConnectionState.ERROR
            }
        }
    }

    private fun startIoManager() {
        val currentPort = port ?: return

        ioManager = SerialInputOutputManager(currentPort, object : SerialInputOutputManager.Listener {
            override fun onNewData(data: ByteArray) {
                // This callback runs on IoManager's background thread
                synchronized(dataQueue) {
                    dataQueue.add(data)
                }

                // Convert to string and process line by line
                val text = String(data, Charsets.UTF_8)

                synchronized(lineBuffer) {
                    lineBuffer.append(text)

                    // Process complete lines
                    var lineEndIndex = lineBuffer.indexOf("\r\n")
                    while (lineEndIndex >= 0) {
                        // Extract complete line (without \r\n)
                        val line = lineBuffer.substring(0, lineEndIndex)
                        lineBuffer.delete(0, lineEndIndex + 2) // Remove line + \r\n

                        if (line.isNotEmpty()) {
                            // Switch to the main thread to handle the line
                            viewModelScope.launch(Dispatchers.Main) {
                                handleIncomingLine(line)
                            }
                        }

                        lineEndIndex = lineBuffer.indexOf("\r\n")
                    }
                }
            }

            override fun onRunError(e: Exception) {
                // Process any remaining partial line before disconnecting
                synchronized(lineBuffer) {
                    if (lineBuffer.isNotEmpty()) {
                        val remainingText = lineBuffer.toString()
                        lineBuffer.clear()
                        viewModelScope.launch(Dispatchers.Main) {
                            logToTerminal(remainingText)
                        }
                    }
                }

                viewModelScope.launch(Dispatchers.Main) {
                    logToTerminal("Connection lost: ${e.message}", "[ERR]")
                    disconnectDevice()
                }
            }
        }).apply {
            start()
        }
    }

    private fun disconnectDevice() {
        viewModelScope.launch {
            try {
                ioManager?.stop()
                ioManager = null

                port?.apply {
                    close()
                    logToTerminal("USB device disconnected and port closed")
                }
            } catch (e: Exception) {
                logToTerminal("Error during port disconnection: ${e.message}", "[ERR]")
            } finally {
                port = null
                _connectionState.value = ConnectionState.DISCONNECTED
            }
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

    // USB ////////////////////////////////////////////////////////////////////////////////////////////////////// USB //

    fun moveStart() {
        isMoving = true
    }

    fun moveStop() {
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

    fun chopStopReady() {
        _connectionState.update { ConnectionState.CONNECTED }
        setMegaParameters("all")
        parametersSet = true
        viewModelScope.launch {
            delay(1000)
            sendData("LOG")
        }
    }

    private fun handleIncomingLine(line: String) {
        if (line.startsWith("POS:") || line.startsWith("OS:") || line.startsWith("S:")) { // sometimes the P in POS gets lost
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

                "POWER:ON" -> {
                    isMotorPowered = true

                    if (errorTitle == "No Power") {
                        closeError()
                    }
                }

                "POWER:OFF" -> {
                    isMotorPowered = false

                    showError = true
                    errorTitle = "No Power"
                    errorMessage = "The motor is not powered, Please turn it on"
                }

                "MEGA_READY" -> {
                    chopStopReady()
                }

                "CHOP_STOP_MK1" -> {
                    chopStopReady()
                }

                "CHOP_STOP_MK2" -> {
                    chopStopReady()
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
                delay(100)
                val invert = if (direction == "LEFT") 1 else 0
                sendData("INVERT_DIR:$invert")
            }
        } else {
            when (param) {
                "Speed" -> sendData("SPEED:$speed")
                "Accel" -> sendData("ACCEL:$accel")
                "Max Delay" -> sendData("MAX_DELAY:$maxDelay")
                "Min Delay" -> sendData("MIN_DELAY:$minDelay")
                "Step Position" -> sendData("POS:$stepPosition")
                "Direction" -> {
                    val invert = if (direction == "LEFT") 1 else 0
                    sendData("INVERT_DIR:$invert")
                }
            }
        }
    }

    fun getDisplayPosition(): String {
        inchPosition = calculateInchPosition()
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

    fun backupSettings(
        deviceIdInput: String,
        usernameInput: String,
        passwordInput: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            repository.saveDeviceId(deviceIdInput)
            repository.saveBasicAuthUsername(usernameInput)
            repository.saveBasicAuthPassword(passwordInput)

            val settingsJson = JSONObject().apply {
                put("speed", speed)
                put("accel", accel)
                put("max_delay", maxDelay)
                put("min_delay", minDelay)
                put("direction", direction)
                put("step_position", stepPosition)
                put("min_step_position", minStepPosition)
                put("max_step_position", maxStepPosition)
                put("8ft_stop_head", eightFtStopHead)
                put("10ft_stop_head", tenFtStopHead)
                put("6ft_stop_head", sixFtStopHead)
                put("steps_per_inch", stepsPerInch)
                put("stop_head", stopHead)
                put("table_length", tableLength)
            }

            val requestBodyJson = JSONObject().apply {
                put("device_id", deviceIdInput)
                put("device_name", deviceIdInput)
                put("version", 1)
                put("settings", settingsJson)
            }

            withContext(Dispatchers.IO) {
                try {
                    val client = OkHttpClient()
                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val body = requestBodyJson.toString().toRequestBody(mediaType)

                    val credential = Credentials.basic(usernameInput, passwordInput)
                    val request = Request.Builder()
                        .url("https://admin.ocoochhardwoods.com/api/chopstop/backup")
                        .post(body)
                        .header("Authorization", credential)
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            withContext(Dispatchers.Main) {
                                logToTerminal("Settings backup successful", "[INFO]")
                                onSuccess()
                            }
                        } else {
                            val errMsg = "HTTP ${response.code}: ${response.message}"
                            withContext(Dispatchers.Main) {
                                logToTerminal("Backup failed: $errMsg", "[ERR]")
                                onFailure(errMsg)
                            }
                        }
                    }
                } catch (e: Exception) {
                    val errMsg = e.localizedMessage ?: "Unknown error"
                    withContext(Dispatchers.Main) {
                        logToTerminal("Backup error: $errMsg", "[ERR]")
                        onFailure(errMsg)
                    }
                }
            }
        }
    }

    fun restoreSettings(
        deviceIdInput: String,
        usernameInput: String,
        passwordInput: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            repository.saveDeviceId(deviceIdInput)
            repository.saveBasicAuthUsername(usernameInput)
            repository.saveBasicAuthPassword(passwordInput)

            withContext(Dispatchers.IO) {
                try {
                    val client = OkHttpClient()
                    val credential = Credentials.basic(usernameInput, passwordInput)
                    val url = "https://admin.ocoochhardwoods.com"
                        .toHttpUrl()
                        .newBuilder()
                        .addPathSegments("api/chopstop/backup")
                        .addPathSegment(deviceIdInput)
                        .build()
                    val request = Request.Builder()
                        .url(url)
                        .get()
                        .header("Authorization", credential)
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val responseBody = response.body?.string() ?: ""
                            val rootJson = JSONObject(responseBody)

                            val settingsJson = if (rootJson.has("settings")) {
                                val settingsValue = rootJson.get("settings")
                                settingsValue as? JSONObject
                                    ?: if (settingsValue is String) {
                                        try {
                                            JSONObject(settingsValue)
                                        } catch (_: Exception) {
                                            rootJson
                                        }
                                    } else {
                                        rootJson
                                    }
                            } else {
                                rootJson
                            }

                            withContext(Dispatchers.Main) {
                                logToTerminal("Restoring settings...", "[INFO]")
                                val newSpeed = if (settingsJson.has("speed")) settingsJson.getInt("speed") else speed
                                val newAccel = if (settingsJson.has("accel")) settingsJson.getInt("accel") else accel
                                val newMaxDelay = if (settingsJson.has("max_delay")) settingsJson.getInt("max_delay") else maxDelay
                                val newMinDelay = if (settingsJson.has("min_delay")) settingsJson.getInt("min_delay") else minDelay
                                val newDirection = if (settingsJson.has("direction")) settingsJson.getString("direction") else direction
                                val newStepPosition = if (settingsJson.has("step_position")) settingsJson.getInt("step_position") else stepPosition
                                val newMinStepPosition = if (settingsJson.has("min_step_position")) settingsJson.getInt("min_step_position") else minStepPosition
                                val newMaxStepPosition = if (settingsJson.has("max_step_position")) settingsJson.getInt("max_step_position") else maxStepPosition
                                val newEightFtStopHead = if (settingsJson.has("8ft_stop_head")) settingsJson.getDouble("8ft_stop_head") else eightFtStopHead
                                val newTenFtStopHead = if (settingsJson.has("10ft_stop_head")) settingsJson.getDouble("10ft_stop_head") else tenFtStopHead
                                val newSixFtStopHead = if (settingsJson.has("6ft_stop_head")) settingsJson.getDouble("6ft_stop_head") else sixFtStopHead
                                val newStepsPerInch = if (settingsJson.has("steps_per_inch")) settingsJson.getDouble("steps_per_inch") else stepsPerInch
                                val newStopHead = if (settingsJson.has("stop_head")) settingsJson.getString("stop_head") else stopHead
                                val newTableLength = if (settingsJson.has("table_length")) settingsJson.getString("table_length") else tableLength

                                speed = newSpeed
                                accel = newAccel
                                maxDelay = newMaxDelay
                                minDelay = newMinDelay
                                direction = newDirection
                                stepPosition = newStepPosition
                                minStepPosition = newMinStepPosition
                                maxStepPosition = newMaxStepPosition
                                eightFtStopHead = newEightFtStopHead
                                tenFtStopHead = newTenFtStopHead
                                sixFtStopHead = newSixFtStopHead
                                stepsPerInch = newStepsPerInch
                                stopHead = newStopHead
                                tableLength = newTableLength

                                repository.saveAllSettings(
                                    speed = newSpeed,
                                    accel = newAccel,
                                    maxDelay = newMaxDelay,
                                    minDelay = newMinDelay,
                                    direction = newDirection,
                                    stepPosition = newStepPosition,
                                    minStepPosition = newMinStepPosition,
                                    maxStepPosition = newMaxStepPosition,
                                    eightFtStopHead = newEightFtStopHead,
                                    tenFtStopHead = newTenFtStopHead,
                                    sixFtStopHead = newSixFtStopHead,
                                    stepsPerInch = newStepsPerInch,
                                    stopHead = newStopHead,
                                    tableLength = newTableLength
                                )

                                setMegaParameters("all")

                                logToTerminal("Settings restored successfully", "[INFO]")
                                onSuccess()
                            }
                        } else if (response.code == 404) {
                            withContext(Dispatchers.Main) {
                                logToTerminal("Restore failed: Device ID not found", "[ERR]")
                                onFailure("Device ID not found")
                            }
                        } else {
                            val errMsg = "HTTP ${response.code}: ${response.message}"
                            withContext(Dispatchers.Main) {
                                logToTerminal("Restore failed: $errMsg", "[ERR]")
                                onFailure(errMsg)
                            }
                        }
                    }
                } catch (e: Exception) {
                    val errMsg = e.localizedMessage ?: "Unknown error"
                    withContext(Dispatchers.Main) {
                        logToTerminal("Restore error: $errMsg", "[ERR]")
                        onFailure(errMsg)
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        usbPermissionReceiver?.let {
            try {
                application.unregisterReceiver(it)
            } catch (_: IllegalArgumentException) {
                // Already unregistered
            }
        }
        getApplication<Application>().unregisterReceiver(usbBroadcastReceiver)
        disconnectDevice()
    }
}
