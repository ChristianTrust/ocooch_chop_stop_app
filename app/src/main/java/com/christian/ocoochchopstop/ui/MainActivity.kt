package com.christian.ocoochchopstop.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.christian.ocoochchopstop.ui.screens.menuScreen
import com.christian.ocoochchopstop.ui.viewmodel.ChopStopViewModel
import com.christian.ocoochchopstop.ui.theme.ocoochChopStopTheme

class MainActivity : ComponentActivity() {
    private val chop: ChopStopViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ocoochChopStopTheme {
                menuScreen(chop)
            }
        }
    }
}