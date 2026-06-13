package com.example.barberrecods.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
@Table(name = 'bookings')
class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = 'service_id', nullable = false)
    BarberService service

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
}
