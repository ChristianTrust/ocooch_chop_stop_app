package com.christian.ocoochchopstopmk2.data

data class TicketItem(
    val length: String,
    val description: String = "",
    val isCustom: Boolean = false,
    val totalNeed: Float = 0f,
    val unit: String? = "",
    val isSplit: Boolean = false,
    val splitItemNeed: Float = 0f,
    val orderNumber: String = "",
    val totalOrdered: Float = 0f,
    val inventory: Float = 0f
)
