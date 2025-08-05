package com.christian.ocoochchopstop.ui.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.christian.ocoochchopstop.ui.util.ocoochCard

@Composable
fun ocoochPopupAlert(
    show: Boolean,
    optional: Boolean = false,
    title: String,
    message: String,
    onConfirm: (() -> Unit)? = null,
    onCancel: () -> Unit,
    properties: PopupProperties = PopupProperties(focusable = true, dismissOnBackPress = true)
) {
    if (show) {
        Popup(
            onDismissRequest = { onCancel() },
            properties = properties,
            alignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onCancel() },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .width(320.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.tertiaryContainer),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ocoochCard(
                                icon = if (optional) Icons.Filled.Close else Icons.Filled.Check,
                                onClick = { onCancel() },
                                modifier = Modifier
                                    .padding(8.dp)
                                    .height(48.dp)
                                    .weight(1f),
                                fontSize = 32,
                                colors = listOf(
                                    MaterialTheme.colorScheme.tertiary,
                                    MaterialTheme.colorScheme.onSurface
                                )
                            )

                            if (optional) {
                                Spacer(modifier = Modifier.height(8.dp))

                                ocoochCard(
                                    icon = Icons.Filled.Check,
                                    onClick = { onConfirm?.invoke() },
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .height(48.dp)
                                        .weight(1f),
                                    fontSize = 32,
                                    colors = listOf(
                                        MaterialTheme.colorScheme.tertiary,
                                        MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}