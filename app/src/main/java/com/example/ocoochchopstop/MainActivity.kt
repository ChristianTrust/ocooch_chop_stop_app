package com.example.ocoochchopstop

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

class MainActivity : ComponentActivity() {
    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_USB_PERMISSION == intent.action) {
                synchronized(this) {
                    val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.apply {
                            // Permission granted, you can now communicate with the device
                        }
                    } else {
                        // Permission denied
                    }
                }
            }
        }
    }

    companion object {
        private const val ACTION_USB_PERMISSION = "com.example.USB_PERMISSION"
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register USB permission receiver
        val permissionIntent = PendingIntent.getBroadcast(
            this, 0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE
        )
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(usbReceiver, filter)
        }

        // Request permission for USB devices
        val usbManager = getSystemService(USB_SERVICE) as UsbManager
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        for (driver in availableDrivers) {
            if (!usbManager.hasPermission(driver.device)) {
                usbManager.requestPermission(driver.device, permissionIntent)
            }
        }

        setContent {
            chopStopApp()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbReceiver)
    }
}

@Composable
fun chopStopApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Function to send command to Arduino
    fun sendCommand(command: String) {
        scope.launch(Dispatchers.IO) {
            try {
                // Get USB manager
                val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
                // Find available drivers
                val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
                if (availableDrivers.isEmpty()) return@launch

                // Open a connection to the first available driver
                val driver = availableDrivers[0]
                val connection = usbManager.openDevice(driver.device)
                    ?: return@launch // Handle permission or connection failure

                val port = driver.ports[0] // Most devices have one port
                port.open(connection)
                port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)

                // Send command with timeout (e.g., 1000ms)
                port.write(command.toByteArray(), 1000)
                port.write("\n".toByteArray(), 1000) // Add newline as delimiter

                // Close port
                port.close()
            } catch (e: IOException) {
                e.printStackTrace() // Handle errors (e.g., show a toast)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = { sendCommand("START") }) {
            Text("Start Motor")
        }
        Button(onClick = { sendCommand("STOP") }, modifier = Modifier.padding(top = 16.dp)) {
            Text("Stop Motor")
        }
        Button(onClick = { sendCommand("HOME") }, modifier = Modifier.padding(top = 16.dp)) {
            Text("Home Position")
        }
    }
}
