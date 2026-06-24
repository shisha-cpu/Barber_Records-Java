package com.example.barberrecods.service

import com.example.barberrecods.dto.LimitsForm
import com.example.barberrecods.dto.LunchBreakForm
import com.example.barberrecods.dto.WorkingHoursForm
import com.example.barberrecods.entity.SalonSettings
import com.example.barberrecods.repository.SalonSettingsRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Service
class SalonSettingsService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern('HH:mm')

    private final SalonSettingsRepository repository

    SalonSettingsService(SalonSettingsRepository repository) {
        this.repository = repository
    }

    SalonSettings getSettings() {
        repository.findById(1L).orElseGet {
            repository.save(new SalonSettings())
        }
    }

    @Transactional
    SalonSettings updateLunchBreak(LunchBreakForm form) {
        if (!form.lunchBreakStart?.trim() || !form.lunchBreakEnd?.trim()) {
            throw new IllegalArgumentException('Укажите время перерыва')
        }

        LocalTime start = LocalTime.parse(form.lunchBreakStart.trim(), TIME_FORMAT)
        LocalTime end = LocalTime.parse(form.lunchBreakEnd.trim(), TIME_FORMAT)

        if (!start.isBefore(end)) {
            throw new IllegalArgumentException('Время начала должно быть раньше окончания')
        }

        SalonSettings settings = getSettings()
        settings.lunchBreakStart = start
        settings.lunchBreakEnd = end
        repository.save(settings)
    }

    @Transactional
    SalonSettings updateWorkingHours(WorkingHoursForm form) {
        if (!form.workStart?.trim() || !form.workEnd?.trim()) {
            throw new IllegalArgumentException('Укажите время работы')
        }

        LocalTime start = LocalTime.parse(form.workStart.trim(), TIME_FORMAT)
        LocalTime end = LocalTime.parse(form.workEnd.trim(), TIME_FORMAT)

        if (!start.isBefore(end)) {
            throw new IllegalArgumentException('Время начала должно быть раньше окончания')
        }

        SalonSettings settings = getSettings()
        settings.workStart = start
        settings.workEnd = end
        repository.save(settings)
    }

    @Transactional
    SalonSettings updateLimits(LimitsForm form) {
        validateLimit(form.maxActiveBookingsPerPhone, 'активных записей на телефон', 1, 10)
        validateLimit(form.maxBookingsPerPhonePerDay, 'записей в день на телефон', 1, 5)
        validateLimit(form.maxBookingsPerIpPerHour, 'записей в час с IP', 1, 100)
        validateLimit(form.maxBookingsPerIpPerDay, 'записей в сутки с IP', 1, 500)

        SalonSettings settings = getSettings()
        settings.maxActiveBookingsPerPhone = form.maxActiveBookingsPerPhone
        settings.maxBookingsPerPhonePerDay = form.maxBookingsPerPhonePerDay
        settings.maxBookingsPerIpPerHour = form.maxBookingsPerIpPerHour
        settings.maxBookingsPerIpPerDay = form.maxBookingsPerIpPerDay
        repository.save(settings)
    }

    private static void validateLimit(Integer value, String label, int min, int max) {
        if (value == null || value < min || value > max) {
            throw new IllegalArgumentException("Некорректный лимит: ${label} (${min}–${max})")
        }
    }
}
