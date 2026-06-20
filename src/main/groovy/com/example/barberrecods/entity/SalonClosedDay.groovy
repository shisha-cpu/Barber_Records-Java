package com.example.barberrecods.entity

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = 'salon_closed_days')
class SalonClosedDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id

    @Column(name = 'from_date', nullable = false)
    LocalDate fromDate

    @Column(name = 'to_date', nullable = false)
    LocalDate toDate

    @Column(length = 128)
    String label
}
