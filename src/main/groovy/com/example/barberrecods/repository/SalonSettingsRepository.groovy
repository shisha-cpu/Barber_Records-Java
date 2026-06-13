package com.example.barberrecods.repository

import com.example.barberrecods.entity.SalonSettings
import org.springframework.data.jpa.repository.JpaRepository

interface SalonSettingsRepository extends JpaRepository<SalonSettings, Long> {
}
