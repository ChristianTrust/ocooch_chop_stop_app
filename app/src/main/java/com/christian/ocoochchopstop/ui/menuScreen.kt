package com.christian.ocoochchopstop.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.christian.ocoochchopstop.viewmodel.CopStopViewModel
import com.christian.ocoochchopstop.ui.util.ocoochCard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun menuScreen(chop: CopStopViewModel) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            drawerContent(navController, drawerState, scope, chop)
        },
        content = {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .windowInsetsPadding(WindowInsets.safeContent)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = "home",
                    modifier = Modifier.padding(8.dp)
                ) {
                    composable("home") { homePage(chop) }
                    composable("settings") { settingsPage(chop) }
                }
                IconButton(
                    onClick = { scope.launch { drawerState.open() } },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 8.dp)
                ) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                }
            }
        }
    )
}

@Composable
fun drawerContent(
    navController: NavHostController,
    drawerState: DrawerState,
    scope: CoroutineScope,
    chop: CopStopViewModel
) {
    val width = 128.dp
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .wrapContentWidth()
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .windowInsetsPadding(WindowInsets.safeContent),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(24.dp).width(1.dp))
        NavigationDrawerItem(
            modifier = Modifier.width(width),
            label = { Text("Home") },
            selected = navController.currentDestination?.route == "home",
            onClick = {
                navController.navigate("home")
                scope.launch { drawerState.close() }
            },
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0f),
                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                unselectedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )

        Spacer(modifier = Modifier
            .height(2.dp)
            .width(width - 32.dp)
            .padding(end = 16.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
        )

        NavigationDrawerItem(
            modifier = Modifier.width(width),
            label = { Text("Settings") },
            selected = navController.currentDestination?.route == "settings",
            onClick = {
                navController.navigate("settings")
                scope.launch { drawerState.close() }
            },
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0f),
                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                unselectedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )

        // Toggle bar
        val isInches by remember(chop.isInch) { mutableStateOf(chop.isInch) }

        // Animation for the toggle bar position
        val toggleOffset by animateDpAsState(
            targetValue = if (isInches) 2.dp else 34.dp
        )
        val toggleWidth by animateDpAsState(
            targetValue = if (isInches) 32.dp else 36.dp
        )
        val toggleColor by animateColorAsState(
            targetValue = if (isInches) {
                MaterialTheme.colorScheme.primary
            } else MaterialTheme.colorScheme.tertiary
        )
        // Animation for text colors
        val inchesColor by animateColorAsState(
            targetValue = if (isInches) Color.White else Color.Gray
        )
        val mmColor by animateColorAsState(
            targetValue = if (isInches) Color.Gray else Color.White
        )


        Column(
            modifier = Modifier
                .width(width)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (!chop.parametersSet) {
                ocoochCard(
                    text = "Confirm Connection",
                    onClick = {
                        chop.sendData("CONFIRM")
                    },
                    modifier = Modifier
                        .width(width - 16.dp)
                        .height(48.dp),
                    fontSize = 12,
                    colors = listOf(
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.75f),
                        MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.75f)
                    )
                )
            }

            Spacer(modifier = Modifier.padding(8.dp))

            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .width(72.dp)
                    .height(32.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
            ) {
                // Background
                Box(
                    modifier = Modifier
                        .width(width)
                        .height(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f))
                        .clickable {
                            chop.toggleInch() // Update external state
                        }
                )

                // Bar/Circle
                Box(
                    modifier = Modifier
                        .offset(x = toggleOffset)
                        .width(toggleWidth)
                        .height(28.dp)
                        .align(Alignment.CenterStart)
                        .background(toggleColor, RoundedCornerShape(16.dp))
                )

                // Labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 6.dp, top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "in",
                        color = inchesColor,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "mm",
                        color = mmColor,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}