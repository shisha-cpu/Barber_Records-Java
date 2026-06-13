package com.example.barberrecods.entity

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = 'services')
class BarberService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id

    @Column(nullable = false)
    String name

    @Column(name = 'duration_minutes', nullable = false)
    Integer durationMinutes

    @Column(nullable = false, precision = 10, scale = 2)
    BigDecimal price

    @Column(nullable = false)
    Boolean active = true
}
