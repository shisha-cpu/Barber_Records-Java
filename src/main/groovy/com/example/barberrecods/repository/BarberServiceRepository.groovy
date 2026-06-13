package com.example.barberrecods.repository

import com.example.barberrecods.entity.BarberService
import org.springframework.data.jpa.repository.JpaRepository

interface BarberServiceRepository extends JpaRepository<BarberService, Long> {
    List<BarberService> findByActiveTrueOrderByNameAsc()
}
