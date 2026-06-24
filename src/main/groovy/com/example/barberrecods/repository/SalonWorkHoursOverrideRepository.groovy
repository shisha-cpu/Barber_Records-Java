package com.example.barberrecods.repository

import com.example.barberrecods.entity.SalonWorkHoursOverride
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

import java.time.LocalDate

interface SalonWorkHoursOverrideRepository extends JpaRepository<SalonWorkHoursOverride, Long> {

    List<SalonWorkHoursOverride> findAllByOrderByFromDateAsc()

    @Query('''SELECT o FROM SalonWorkHoursOverride o
              WHERE o.toDate >= :from AND o.fromDate <= :to
              ORDER BY o.fromDate ASC''')
    List<SalonWorkHoursOverride> findOverlapping(@Param('from') LocalDate from, @Param('to') LocalDate to)
}
