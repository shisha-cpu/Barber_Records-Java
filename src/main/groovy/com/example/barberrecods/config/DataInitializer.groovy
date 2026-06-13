package com.example.barberrecods.config

import com.example.barberrecods.entity.BarberService
import com.example.barberrecods.entity.SalonSettings
import com.example.barberrecods.repository.BarberServiceRepository
import com.example.barberrecods.repository.SalonSettingsRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order

@Configuration
class DataInitializer {

    @Bean
    @Order(1)
    CommandLineRunner seedData(BarberServiceRepository serviceRepository,
                               SalonSettingsRepository settingsRepository) {
        { args ->
            if (settingsRepository.count() == 0) {
                settingsRepository.save(new SalonSettings())
            }

            if (serviceRepository.count() == 0) {
                serviceRepository.saveAll([
                        new BarberService(name: 'Мужская стрижка', durationMinutes: 30, price: 800.0G, active: true),
                        new BarberService(name: 'Стрижка + борода', durationMinutes: 45, price: 1200.0G, active: true),
                        new BarberService(name: 'Моделирование бороды', durationMinutes: 20, price: 500.0G, active: true),
                        new BarberService(name: 'Детская стрижка', durationMinutes: 25, price: 600.0G, active: true),
                        new BarberService(name: 'Стрижка машинкой', durationMinutes: 20, price: 500.0G, active: true),
                        new BarberService(name: 'Королевское бритьё', durationMinutes: 40, price: 1000.0G, active: true),
                        new BarberService(name: 'Окантовка', durationMinutes: 15, price: 350.0G, active: true),
                        new BarberService(name: 'Укладка', durationMinutes: 15, price: 400.0G, active: true),
                        new BarberService(name: 'Камуфляж седины', durationMinutes: 30, price: 700.0G, active: true),
                        new BarberService(name: 'Тонирование бороды', durationMinutes: 25, price: 650.0G, active: true),
                        new BarberService(name: 'Бритьё головы', durationMinutes: 25, price: 550.0G, active: true),
                        new BarberService(name: 'Father & Son', durationMinutes: 60, price: 1400.0G, active: true),
                        new BarberService(name: 'Удаление воском', durationMinutes: 15, price: 300.0G, active: true),
                        new BarberService(name: 'Wet-укладка', durationMinutes: 20, price: 450.0G, active: true),
                        new BarberService(name: 'SPA-уход за бородой', durationMinutes: 30, price: 750.0G, active: true)
                ])
            }
        }
    }
}
