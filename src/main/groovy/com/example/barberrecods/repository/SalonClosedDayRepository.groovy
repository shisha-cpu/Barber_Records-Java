package com.example.barberrecods.repository

import com.example.barberrecods.entity.SalonClosedDay
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

import java.time.LocalDate

interface SalonClosedDayRepository extends JpaRepository<SalonClosedDay, Long> {

    List<SalonClosedDay> findAllByOrderByFromDateAsc()

    @Query('''SELECT c FROM SalonClosedDay c
              WHERE c.toDate >= :from AND c.fromDate <= :to
              ORDER BY c.fromDate ASC''')
    List<SalonClosedDay> findOverlapping(@Param('from') LocalDate from, @Param('to') LocalDate to)
}
