package com.example.barberrecods.config

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
@Order(0)
class DatabaseMigrationRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate

    DatabaseMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate
    }

    @Override
    void run(ApplicationArguments args) {
        migrateSalonSettings()
        migrateBookings()
        migrateClosedDays()
    }

    private void migrateSalonSettings() {
        jdbcTemplate.execute('''
            CREATE TABLE IF NOT EXISTS salon_settings (
                id BIGINT PRIMARY KEY,
                lunch_break_start TIME NOT NULL DEFAULT '14:00',
                lunch_break_end TIME NOT NULL DEFAULT '15:00'
            )
        ''')

        jdbcTemplate.execute('''
            ALTER TABLE salon_settings
            ADD COLUMN IF NOT EXISTS max_active_bookings_per_phone INTEGER NOT NULL DEFAULT 2
        ''')
        jdbcTemplate.execute('''
            ALTER TABLE salon_settings
            ADD COLUMN IF NOT EXISTS max_bookings_per_phone_per_day INTEGER NOT NULL DEFAULT 1
        ''')
        jdbcTemplate.execute('''
            ALTER TABLE salon_settings
            ADD COLUMN IF NOT EXISTS max_bookings_per_ip_per_hour INTEGER NOT NULL DEFAULT 10
        ''')
        jdbcTemplate.execute('''
            ALTER TABLE salon_settings
            ADD COLUMN IF NOT EXISTS max_bookings_per_ip_per_day INTEGER NOT NULL DEFAULT 20
        ''')
        jdbcTemplate.execute('''
            ALTER TABLE salon_settings
            ADD COLUMN IF NOT EXISTS weekends_closed BOOLEAN NOT NULL DEFAULT true
        ''')

        jdbcTemplate.update('''
            INSERT INTO salon_settings (id, lunch_break_start, lunch_break_end,
                max_active_bookings_per_phone, max_bookings_per_phone_per_day,
                max_bookings_per_ip_per_hour, max_bookings_per_ip_per_day)
            VALUES (1, '14:00', '15:00', 2, 1, 10, 20)
            ON CONFLICT (id) DO NOTHING
        ''')
    }

    private void migrateBookings() {
        jdbcTemplate.execute('''
            ALTER TABLE bookings
            ADD COLUMN IF NOT EXISTS client_ip VARCHAR(64)
        ''')
        jdbcTemplate.execute('''
            ALTER TABLE bookings
            ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW()
        ''')
    }

    private void migrateClosedDays() {
        jdbcTemplate.execute('''
            CREATE TABLE IF NOT EXISTS salon_closed_days (
                id BIGSERIAL PRIMARY KEY,
                from_date DATE NOT NULL,
                to_date DATE NOT NULL,
                label VARCHAR(128)
            )
        ''')
    }
}
