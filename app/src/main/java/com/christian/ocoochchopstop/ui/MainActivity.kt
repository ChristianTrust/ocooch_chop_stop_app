package com.christian.ocoochchopstop.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.christian.ocoochchopstop.viewmodel.CopStopViewModel
import com.christian.ocoochchopstop.ui.theme.ocoochChopStopTheme

class MainActivity : ComponentActivity() {
    private val chop: CopStopViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ocoochChopStopTheme {
                menuScreen(chop)
            }
        }
    }
}