package com.workbuddy.house.monitor.model

data class AvailabilityResult(
    val propertyId: String,
    val checkTime: Long,
    val unavailableDates: List<DateAvailability>,
    val totalDays: Int
)

data class DateAvailability(
    val date: String,
    val available: Boolean,
    val price: Double?
)