package com.christian.ocoochchopstop.ui.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.christian.ocoochchopstop.ui.theme.*
import com.christian.ocoochchopstop.ui.viewmodel.ChopStopViewModel

@Composable
fun terminalView(
    chop: ChopStopViewModel,
    modifier: Modifier = Modifier,
    scrollToEnd: Int = 0
) {
    val lazyListState = rememberLazyListState()
    var terminalTextColor = ocoochOrange80
    var terminalTextWeight = MaterialTheme.typography.bodySmall.fontWeight

    LaunchedEffect(chop.terminalText.value, scrollToEnd) {
        if (chop.terminalText.value.isNotEmpty()) {
            lazyListState.animateScrollToItem(chop.terminalText.value.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = chop.stepPositionText.value,
                style = MaterialTheme.typography.bodySmall.copy(
                    lineHeight = 10.sp,
                    fontWeight = MaterialTheme.typography.bodyLarge.fontWeight
                ),
                color = ocoochOrange40
            )
        }
        if (chop.terminalText.value.isNotEmpty()) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(chop.terminalText.value) { line ->
                    if (line.startsWith("[ERR]")) {
                        terminalTextColor = errorText
                        terminalTextWeight = MaterialTheme.typography.bodyLarge.fontWeight
                    } else if (line.startsWith(" ERR")) {
                        terminalTextColor = ocoochBlue80
                        terminalTextWeight = MaterialTheme.typography.bodySmall.fontWeight
                    } else if (line.startsWith("[Sent]")) {
                        terminalTextColor = ocoochBlue80
                        terminalTextWeight = MaterialTheme.typography.bodySmall.fontWeight
                    } else if (line.startsWith("[INFO]")) {
                        terminalTextColor = ocoochGray20
                        terminalTextWeight = MaterialTheme.typography.bodySmall.fontWeight
                    } else {
                        terminalTextColor = ocoochOrange80
                        terminalTextWeight = MaterialTheme.typography.bodySmall.fontWeight
                    }

                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall.copy(
                            lineHeight = 6.sp,
                            fontWeight = terminalTextWeight
                        ),
                        color = terminalTextColor
                    )
                }
            }
        } else {
            Text(
                text = "Empty",
                style = MaterialTheme.typography.bodySmall,
                color = ocoochGray10,
                modifier = Modifier
                    .fillMaxSize()
            )
        }
    }
}
