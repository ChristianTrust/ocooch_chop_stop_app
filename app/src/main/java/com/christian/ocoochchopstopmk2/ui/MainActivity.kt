package com.christian.ocoochchopstopmk2.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.christian.ocoochchopstopmk2.ui.screens.menuScreen
import com.christian.ocoochchopstopmk2.ui.viewmodel.ChopStopViewModel
import com.christian.ocoochchopstopmk2.ui.theme.ocoochChopStopTheme

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