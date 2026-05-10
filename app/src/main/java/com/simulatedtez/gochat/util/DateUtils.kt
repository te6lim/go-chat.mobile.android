package com.simulatedtez.gochat.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter


fun formatTimestamp(isoString: String): String {
    return try {
        val instant = LocalDateTime.parse(isoString, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        DateTimeFormatter.ofPattern("hh:mm a")
            .format(instant)
    } catch (e: Exception) {
        ""
    }
}

fun formatDateLabel(isoString: String): String {
    return try {
        val messageDate = LocalDateTime.parse(isoString, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDate()
        val today = LocalDate.now()
        when {
            messageDate == today -> "Today"
            messageDate == today.minusDays(1) -> "Yesterday"
            else -> DateTimeFormatter.ofPattern("EEE, MMM d").format(messageDate)
        }
    } catch (e: Exception) {
        ""
    }
}

fun LocalDateTime.toISOString(): String {
    val zoneDateTime = ZonedDateTime.of(this, ZoneOffset.systemDefault())
    return DateTimeFormatter.ofPattern(
        "yyyy-MM-dd'T'HH:mm:ssXXX"
    ).format(zoneDateTime)
}