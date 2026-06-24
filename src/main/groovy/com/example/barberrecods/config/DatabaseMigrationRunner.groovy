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
        migrateBookingServices()
        migrateWorkHoursOverrides()
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
        jdbcTemplate.execute('''
            ALTER TABLE salon_settings
            ALTER COLUMN weekends_closed SET DEFAULT true
        ''')
        jdbcTemplate.update('''
            UPDATE salon_settings SET weekends_closed = true WHERE weekends_closed IS NULL
        ''')
        jdbcTemplate.execute('''
            ALTER TABLE salon_settings
            ADD COLUMN IF NOT EXISTS work_start TIME NOT NULL DEFAULT '09:00'
        ''')
        jdbcTemplate.execute('''
            ALTER TABLE salon_settings
            ADD COLUMN IF NOT EXISTS work_end TIME NOT NULL DEFAULT '20:00'
        ''')
        jdbcTemplate.execute('''
            ALTER TABLE salon_settings
            ALTER COLUMN work_start SET DEFAULT '09:00'
        ''')
        jdbcTemplate.execute('''
            ALTER TABLE salon_settings
            ALTER COLUMN work_end SET DEFAULT '20:00'
        ''')
        jdbcTemplate.update('''
            UPDATE salon_settings SET work_start = '09:00' WHERE work_start IS NULL
        ''')
        jdbcTemplate.update('''
            UPDATE salon_settings SET work_end = '20:00' WHERE work_end IS NULL
        ''')

        jdbcTemplate.update('''
            INSERT INTO salon_settings (id, lunch_break_start, lunch_break_end,
                max_active_bookings_per_phone, max_bookings_per_phone_per_day,
                max_bookings_per_ip_per_hour, max_bookings_per_ip_per_day, weekends_closed,
                work_start, work_end)
            VALUES (1, '14:00', '15:00', 2, 1, 10, 20, true, '09:00', '20:00')
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

    private void migrateBookingServices() {
        jdbcTemplate.execute('''
            CREATE TABLE IF NOT EXISTS booking_services (
                booking_id BIGINT NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
                service_id BIGINT NOT NULL REFERENCES services(id) ON DELETE RESTRICT,
                PRIMARY KEY (booking_id, service_id)
            )
        ''')

        Integer legacyColumnCount = jdbcTemplate.queryForObject('''
            SELECT COUNT(*) FROM information_schema.columns
            WHERE table_schema = current_schema()
              AND table_name = 'bookings'
              AND column_name = 'service_id'
        ''', Integer)

        if (legacyColumnCount > 0) {
            jdbcTemplate.execute('''
                INSERT INTO booking_services (booking_id, service_id)
                SELECT b.id, b.service_id
                FROM bookings b
                WHERE b.service_id IS NOT NULL
                ON CONFLICT DO NOTHING
            ''')
            jdbcTemplate.execute('ALTER TABLE bookings DROP COLUMN IF EXISTS service_id')
        }
    }

    private void migrateWorkHoursOverrides() {
        jdbcTemplate.execute('''
            CREATE TABLE IF NOT EXISTS salon_work_hours_overrides (
                id BIGSERIAL PRIMARY KEY,
                from_date DATE NOT NULL,
                to_date DATE NOT NULL,
                work_start TIME NOT NULL,
                work_end TIME NOT NULL,
                label VARCHAR(128)
            )
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
