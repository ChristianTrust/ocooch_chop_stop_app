package com.christian.ocoochchopstopmk2.ui.elements

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.christian.ocoochchopstopmk2.data.TicketItem
import com.christian.ocoochchopstopmk2.ui.viewmodel.ChopStopViewModel
import java.util.Locale

@Composable
fun TicketPanel(
    chop: ChopStopViewModel,
    modifier: Modifier = Modifier,
    panelHeight: androidx.compose.ui.unit.Dp
) {
    val expanded = chop.ticketPanelExpanded
    val targetHeight = if (expanded) panelHeight else 48.dp
    val animatedHeight by animateDpAsState(targetValue = targetHeight, label = "panelHeight")

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(animatedHeight)
            .clip(RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 8.dp
    ) {
        Column {
            // Header / Handle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clickable { chop.ticketPanelExpanded = !chop.ticketPanelExpanded }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ticket Items",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                IconButton(onClick = { chop.showScanner = true }) {
                    Icon(Icons.Default.QrCode, contentDescription = "Scan Ticket")
                }
            }

            if (expanded) {
                if (chop.isTicketLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (chop.ticketItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No items. Scan a ticket to begin.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(chop.ticketItems) { item ->
                            TicketItemRow(
                                item = item,
                                isSelected = chop.selectedTicketItemId == item.id,
                                hasSelection = chop.selectedTicketItemId.isNotEmpty(),
                                onClick = {
                                    chop.selectedTicketItemId = item.id
                                    val lengthVal = item.length.toFloatOrNull() ?: 0f
                                    chop.goToPosition("INCH:", lengthVal, fromTicket = true)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TicketItemRow(
    item: TicketItem,
    isSelected: Boolean,
    hasSelection: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (item.isCustom) Color.Black else Color.Transparent
    val borderStroke = if (item.isCustom) 4.dp else 0.dp

    val scale by animateFloatAsState(
        targetValue = if (hasSelection && !isSelected) 0.92f else 1.0f,
        label = "itemScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isSelected) {
                    val density = LocalDensity.current
                    val stripeWidth = with(density) { 48.dp.toPx() }
                    Modifier
                        .background(
                            Brush.linearGradient(
                                0.0f to Color.White,
                                0.5f to Color.White,
                                0.5f to Color(0xffe5e5e5),
                                1.0f to Color(0xffe5e5e5),
                                start = Offset.Zero,
                                end = Offset(stripeWidth, stripeWidth),
                                tileMode = TileMode.Repeated
                            )
                        )
                        .border(borderStroke, borderColor, RoundedCornerShape(12.dp))
                } else {
                    Modifier
                        .background(Color.White)
                        .then(
                            if (item.isCustom) Modifier.border(
                                borderStroke,
                                borderColor,
                                RoundedCornerShape(12.dp)
                            )
                            else Modifier
                        )
                }
            )
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left column: Total need and unit
            Column(
                modifier = Modifier.padding(start = 4.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = formatFloat(item.totalNeed),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = Color.Black
                        ),
                        lineHeight = 24.sp
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = if ((item.unit == null || item.unit == "ea" || item.unit == "") && !item.isSplit) "Qty" else item.unit ?: "",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp,
                            color = Color.Black
                        ),
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                if (item.isSplit) {
                    Text(
                        text = "of ${formatFloat(item.splitItemNeed)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp,
                            color = Color.Black
                        )
                    )
                }
            }

            // Middle column: Description and order number
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            lineHeight = 18.sp,
                            color = Color.Black
                        ),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (item.isCustom) {
                        Text(
                            text = "#${lastThreeDigits(item.orderNumber)}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = Color.Black
                            ),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }

            // Right column: Total ordered - inventory
            if (item.totalNeed != (item.totalOrdered - item.inventory)) {
                Text(
                    text = formatFloat(item.totalOrdered - item.inventory),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color.Black
                    ),
                    modifier = Modifier.width(IntrinsicSize.Min),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

fun formatFloat(value: Float): String {
    return if (value % 1.0 == 0.0) {
        String.format(Locale.US, "%.0f", value)
    } else {
        String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
    }
}

fun lastThreeDigits(orderNumber: String): String {
    return if (orderNumber.length >= 3) {
        orderNumber.substring(orderNumber.length - 3)
    } else {
        orderNumber
    }
}

