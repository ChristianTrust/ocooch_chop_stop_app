package com.christian.ocoochchopstop.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun dinoGame() {
    var dinoY by remember { mutableStateOf(0f) }
    var dinoVelocity by remember { mutableStateOf(0f) }
    var isOnGround by remember { mutableStateOf(true) }

    var obstacles by remember { mutableStateOf(listOf(Offset(1000f, 0f))) }
    var speed by remember { mutableStateOf(8f) }
    var score by remember { mutableStateOf(0) }
    var gameOver by remember { mutableStateOf(false) }
    var gameStarted by remember { mutableStateOf(false) }

    val gravity = 1.2f
    val jumpStrength = -20f

    LaunchedEffect(gameStarted) {
        if (gameStarted && !gameOver) {
            while (true) {
                withFrameMillis {
                    // Update dino position
                    dinoY += dinoVelocity
                    dinoVelocity += gravity

                    if (dinoY > 0f) {
                        dinoY = 0f
                        dinoVelocity = 0f
                        isOnGround = true
                    }

                    // Move obstacles
                    obstacles = obstacles.map { it.copy(x = it.x - speed) }

                    // Respawn obstacles
                    if (obstacles.first().x < -50f) {
                        obstacles = obstacles.drop(1) + Offset(1000f + (200..600).random(), 0f)
                        score += 1
                        speed += 0.2f // Increase speed over time
                    }

                    // Collision detection
                    val dinoRect = Rect(50f, 300f + dinoY, 100f, 350f + dinoY)
                    for (obs in obstacles) {
                        val cactusRect = Rect(obs.x, 300f, obs.x + 30f, 350f)
                        if (dinoRect.overlaps(cactusRect)) {
                            gameOver = true
                            gameStarted = false
                        }
                    }
                }
            }
        }
    }

    // Tap controls
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .pointerInput(Unit) {
                detectTapGestures {
                    when {
                        !gameStarted && !gameOver -> gameStarted = true
                        gameOver -> {
                            // Reset
                            dinoY = 0f
                            dinoVelocity = 0f
                            isOnGround = true
                            obstacles = listOf(Offset(1000f, 0f))
                            speed = 8f
                            score = 0
                            gameOver = false
                            gameStarted = true
                        }
                        isOnGround -> {
                            dinoVelocity = jumpStrength
                            isOnGround = false
                        }
                    }
                }
            }
    ) {
        // Score
        Text(
            text = "Score: $score",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(16.dp)
        )

        // Start/Game Over prompts
        if (gameOver) {
            Text(
                "Game Over! Tap to restart",
                fontSize = 28.sp,
                color = Color.Red,
                modifier = Modifier.align(Alignment.Center)
            )

            // Disclaimer
            Text(
                "Disclaimer: This game is made by ChatGPT. Not Christian",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        } else if (!gameStarted) {
            Text(
                "Tap to start",
                fontSize = 28.sp,
                color = Color.Black,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Game canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val groundY = size.height - 200f

            // Ground line
            drawLine(
                color = Color.Black,
                start = Offset(0f, groundY),
                end = Offset(size.width, groundY),
                strokeWidth = 4f
            )

            // Dino
            drawRect(
                color = Color.Black,
                topLeft = Offset(50f, groundY - 50f + dinoY),
                size = Size(50f, 50f)
            )

            // Obstacles
            for (obs in obstacles) {
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(obs.x, groundY - 50f),
                    size = Size(30f, 50f)
                )
            }
        }
    }
}

