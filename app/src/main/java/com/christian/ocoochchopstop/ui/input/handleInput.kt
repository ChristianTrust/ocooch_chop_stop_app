package com.christian.ocoochchopstop.ui.input

import com.christian.ocoochchopstop.ui.viewmodel.ChopStopViewModel

fun addToDefault(number: String, inputNumber: String, isDouble: Boolean): String {
    return when (number) {
        "backspace" -> if (inputNumber.isNotEmpty()) {
            inputNumber.dropLast(1)
        } else ""
        "clear" -> ""
        "." -> {
            if (isDouble) {
                if (inputNumber.contains(".")) inputNumber else "$inputNumber."
            } else inputNumber
        }
        else -> if (inputNumber == "0") number else inputNumber + number
    }
}

fun addToMain(number: String, inputNumber: String, chop: ChopStopViewModel): String {
    val maxDecimalPlaces = if (chop.isInch) 4 else 2
    val maxDigits = 7
    val newInputNumber = inputNumber + number

    when (number) {
        "backspace" -> return if (inputNumber.isNotEmpty()) {
            inputNumber.dropLast(1)
        } else ""
        "clear" -> {
            chop.clearInput()
            return ""
        }
    }

    if (newInputNumber.length > maxDigits) {
        chop.validNumber = newInputNumber.substring(0, maxDigits)
        chop.isInvalidInput = true
        return inputNumber
    }

    // Check and limit decimal places
    if (inputNumber.contains(".")) {
        if (number == ".") {
            return inputNumber
        }

        val parts = inputNumber.split(".")
        var mainPart = parts[0]
        var decimalPart = parts[1]

        if (parts.size == 2) {
            if (decimalPart.length > maxDecimalPlaces) {
                chop.validNumber = "${mainPart}.${decimalPart.substring(0, maxDecimalPlaces)}"
                chop.isInvalidInput = true
            }
        }
    }

    return if (inputNumber == "0") number else newInputNumber
}
