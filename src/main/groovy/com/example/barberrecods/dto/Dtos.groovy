package com.example.barberrecods.dto

import java.math.BigDecimal

class ServiceDto {
    Long id
    String name
    Integer durationMinutes
    BigDecimal price
}

class BookingRequest {
    Long serviceId
    String date
    String time
    String clientName
    String clientPhone
    String website
}

class LimitsForm {
    Integer maxActiveBookingsPerPhone
    Integer maxBookingsPerPhonePerDay
    Integer maxBookingsPerIpPerHour
    Integer maxBookingsPerIpPerDay
}

class ServiceForm {
    String name
    Integer durationMinutes
    BigDecimal price
    Boolean active = true
}

class LunchBreakForm {
    String lunchBreakStart
    String lunchBreakEnd
}

class ClosedDayForm {
    String fromDate
    String toDate
    String label
}

class WeekendsForm {
    Boolean weekendsClosed = true
}

class ClosedDayViewDto {
    Long id
    String fromDate
    String toDate
    String label
    String periodText
}

class BookingViewDto {
    Long id
    String date
    String time
    String serviceName
    Integer durationMinutes
    BigDecimal price
    String clientName
    String clientPhone
    String createdAt
}

class AvailableSlotsDayDto {
    String date
    List<String> times
}

class AvailableSlotsDto {
    Long serviceId
    String serviceName
    Integer durationMinutes
    BigDecimal price
    String from
    String to
    List<AvailableSlotsDayDto> days
}
