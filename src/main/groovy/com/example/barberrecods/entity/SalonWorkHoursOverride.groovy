package com.example.barberrecods.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalTime

@Entity
@Table(name = 'salon_work_hours_overrides')
class SalonWorkHoursOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id

    @Column(name = 'from_date', nullable = false)
    LocalDate fromDate

    @Column(name = 'to_date', nullable = false)
    LocalDate toDate

    @Column(name = 'work_start', nullable = false)
    LocalTime workStart

    @Column(name = 'work_end', nullable = false)
    LocalTime workEnd

    @Column(length = 128)
    String label
}
