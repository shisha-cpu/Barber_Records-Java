package com.example.barberrecods.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "app")
class AppProperties {

    Admin admin = new Admin()
    WorkingHours workingHours = new WorkingHours()
    int slotIntervalMinutes = 30

    static class Admin {
        String username = 'admin'
        String password = 'admin123'
    }

    static class WorkingHours {
        String start = '09:00'
        String end = '20:00'
    }
}
