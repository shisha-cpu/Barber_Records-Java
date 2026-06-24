package com.example.barberrecods.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
@Table(name = 'bookings')
class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = 'booking_services',
            joinColumns = @JoinColumn(name = 'booking_id'),
            inverseJoinColumns = @JoinColumn(name = 'service_id')
    )
    List<BarberService> services = []

    @Column(name = 'booking_date', nullable = false)
    LocalDate bookingDate

    @Column(name = 'booking_time', nullable = false)
    LocalTime bookingTime

    @Column(name = 'client_name', nullable = false)
    String clientName

    @Column(name = 'client_phone', nullable = false)
    String clientPhone

    @Column(name = 'created_at', nullable = false)
    LocalDateTime createdAt = LocalDateTime.now()

    @Column(name = 'client_ip')
    String clientIp

    int getTotalDurationMinutes() {
        services?.sum { it.durationMinutes } ?: 0
    }

    BigDecimal getTotalPrice() {
        services?.collect { it.price }?.inject(BigDecimal.ZERO) { acc, price -> acc + price } ?: BigDecimal.ZERO
    }

    String getServiceNames() {
        services?.collect { it.name }?.join(', ') ?: ''
    }
}
