package com.example.barberrecods.entity

import jakarta.persistence.*
import java.time.LocalTime

@Entity
@Table(name = 'salon_settings')
class SalonSettings {

    @Id
    Long id = 1L

    @Column(name = 'lunch_break_start', nullable = false)
    LocalTime lunchBreakStart = LocalTime.of(14, 0)

    @Column(name = 'lunch_break_end', nullable = false)
    LocalTime lunchBreakEnd = LocalTime.of(15, 0)

    @Column(name = 'max_active_bookings_per_phone', nullable = false)
    Integer maxActiveBookingsPerPhone = 2

    @Column(name = 'max_bookings_per_phone_per_day', nullable = false)
    Integer maxBookingsPerPhonePerDay = 1

    @Column(name = 'max_bookings_per_ip_per_hour', nullable = false)
    Integer maxBookingsPerIpPerHour = 10

    @Column(name = 'max_bookings_per_ip_per_day', nullable = false)
    Integer maxBookingsPerIpPerDay = 20
}
