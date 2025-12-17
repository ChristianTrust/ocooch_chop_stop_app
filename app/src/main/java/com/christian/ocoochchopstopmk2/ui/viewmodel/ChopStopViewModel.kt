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
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.christian.ocoochchopstopmk2.data.dataStore
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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


        private const val INTENT_ACTION_GRANT_USB = "com.christian.GRANT_USB"
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

    var speed by mutableIntStateOf(0)
    var accel by mutableIntStateOf(0)
    var maxDelay by mutableIntStateOf(0)
    var minDelay by mutableIntStateOf(0)

    var stepPosition by mutableIntStateOf(0)
    var minStepPosition by mutableIntStateOf(0)
    var maxStepPosition by mutableIntStateOf(0)

    var eightFtStopHead by mutableDoubleStateOf(0.0)
    var tenFtStopHead by mutableDoubleStateOf(0.0)
    var twelveFtStopHead by mutableDoubleStateOf(0.0)

    var stepsPerInch by mutableDoubleStateOf(1775.36)

    var stopHead by mutableStateOf("8ft")

    var inchPosition by mutableDoubleStateOf(0.0)
    var parametersSet by mutableStateOf(false)

    var inputNumber by mutableStateOf("")
    var validNumber by mutableStateOf("")
    var isInvalidInput by mutableStateOf(false)
    var isInch by mutableStateOf(true)
    var unit by mutableStateOf("")
    var unitMarker by mutableStateOf("")

    var isMotorPowered by mutableStateOf(false)
    var isMoving by mutableStateOf(false)
    var isStopping by mutableStateOf(false)
    var isHoming by mutableStateOf(false)
    var isCalibrating by mutableStateOf(false)

    var calibrationInput by mutableStateOf("")
    var newMovePosition by mutableIntStateOf(0)

    var showError by mutableStateOf(false)
    var errorTitle by mutableStateOf("")
    var errorMessage by mutableStateOf("")
//    var connectionState by mutableIntStateOf(0)

    val terminalText = mutableStateOf<List<String>>(listOf())
    val stepPositionText = mutableStateOf("")
    val lastSentLine = mutableStateOf("")
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
        inchPosition = (stepPosition + getStopHeadSteps()) / stepsPerInch

        // Wait for parameters to be set
        confirmConnection(10)
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
        val stepsFromZero = if (unitType == "MM:") {
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
            application.registerReceiver(usbPermissionReceiver, filter, Context.RECEIVER_EXPORTED )
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
                    _connectionState.update { ConnectionState.CONNECTED }
                    setMegaParameters("all")
                    parametersSet = true
                    viewModelScope.launch {
                        delay(1000)
                        sendData("LOG")
                    }
                }
                "CHOP_STOP_MK2" -> {
                    _connectionState.update { ConnectionState.CONNECTED }
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

    fun getDisplayPosition(): String {
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