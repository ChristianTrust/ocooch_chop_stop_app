package com.christian.ocoochchopstopmk2.ui.state

data class ChopStopUiState(
    // Hardware settings (from DataStore)
    val speed: Int = 20000,
    val accel: Int = 8000,
    val maxDelay: Int = 320,
    val minDelay: Int = 6,
    val stepPosition: Int = 0,
    val minStepPosition: Int = 0,
    val maxStepPosition: Int = 166044,
    val eightFtStopHead: Double = 2.6,
    val tenFtStopHead: Double = 26.6,
    val twelveFtStopHead: Double = 50.6,
    val stepsPerInch: Double = 1775.36,
    val stopHead: String = "8ft",
    // UI state
    val inchPosition: Double = 0.0,
    val isParametersSet: Boolean = false,
    val inputNumber: String = "",
    val isInvalidInput: Boolean = false,
    val isInch: Boolean = true,
    val unit: String = "INCH:",
    val unitMarker: String = "\"",
    val isMoving: Boolean = false,
    val isStopping: Boolean = false,
    val isHoming: Boolean = false,
    val firstHoming: Boolean = false,
    val isCalibrating: Boolean = false,
    val calibrationInput: String = "",
    val newMovePosition: Int = 0,
    val showLengthError: Boolean = false,
    val lengthError: LengthError? = null,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val terminalText: List<String> = emptyList(),
    val stepPositionText: String = "",
    val lastSentLine: String = ""
) {
    data class LengthError(val title: String, val message: String)
    enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, READY }
}