package com.example.barberrecods.service

import com.example.barberrecods.dto.WorkHoursOverrideForm
import com.example.barberrecods.dto.WorkHoursOverrideViewDto
import com.example.barberrecods.entity.SalonWorkHoursOverride
import com.example.barberrecods.repository.SalonWorkHoursOverrideRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Service
class WorkHoursOverrideService {

    private static final DateTimeFormatter DATE_RU = DateTimeFormatter.ofPattern('dd.MM.yyyy')
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern('HH:mm')

    private final SalonWorkHoursOverrideRepository repository
    private final SalonSettingsService salonSettingsService

    WorkHoursOverrideService(SalonWorkHoursOverrideRepository repository,
                             SalonSettingsService salonSettingsService) {
        this.repository = repository
        this.salonSettingsService = salonSettingsService
    }

    List<WorkHoursOverrideViewDto> listAll() {
        repository.findAllByOrderByFromDateAsc().collect { toDto(it) }
    }

    ResolvedWorkHours resolveForDate(LocalDate date) {
        List<SalonWorkHoursOverride> overrides = repository.findOverlapping(date, date)
        if (!overrides.isEmpty()) {
            SalonWorkHoursOverride override = overrides.first()
            return new ResolvedWorkHours(start: override.workStart, end: override.workEnd)
        }
        def settings = salonSettingsService.getSettings()
        new ResolvedWorkHours(start: settings.workStart, end: settings.workEnd)
    }

    @Transactional
    SalonWorkHoursOverride add(WorkHoursOverrideForm form) {
        if (!form.fromDate?.trim() || !form.toDate?.trim()) {
            throw new IllegalArgumentException('Укажите даты')
        }
        if (!form.workStart?.trim() || !form.workEnd?.trim()) {
            throw new IllegalArgumentException('Укажите время работы')
        }

        LocalDate from = LocalDate.parse(form.fromDate.trim())
        LocalDate to = LocalDate.parse(form.toDate.trim())
        LocalTime start = LocalTime.parse(form.workStart.trim(), TIME_FORMAT)
        LocalTime end = LocalTime.parse(form.workEnd.trim(), TIME_FORMAT)

        if (to.isBefore(from)) {
            throw new IllegalArgumentException('Дата окончания раньше начала')
        }
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException('Время начала должно быть раньше окончания')
        }
        if (!repository.findOverlapping(from, to).isEmpty()) {
            throw new IllegalArgumentException('На этот период уже задано особое время работы')
        }

        String label = form.label?.trim()
        if (!label) {
            label = from == to ? 'Особый график' : 'Особый график'
        }

        repository.save(new SalonWorkHoursOverride(
                fromDate: from,
                toDate: to,
                workStart: start,
                workEnd: end,
                label: label
        ))
    }

    @Transactional
    void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException('Запись не найдена')
        }
        repository.deleteById(id)
    }

    private static WorkHoursOverrideViewDto toDto(SalonWorkHoursOverride period) {
        String periodText = period.fromDate == period.toDate
                ? period.fromDate.format(DATE_RU)
                : "${period.fromDate.format(DATE_RU)} — ${period.toDate.format(DATE_RU)}"
        String hoursText = "${period.workStart.format(TIME_FORMAT)}–${period.workEnd.format(TIME_FORMAT)}"

        new WorkHoursOverrideViewDto(
                id: period.id,
                fromDate: period.fromDate.toString(),
                toDate: period.toDate.toString(),
                workStart: period.workStart.format(TIME_FORMAT),
                workEnd: period.workEnd.format(TIME_FORMAT),
                label: period.label,
                periodText: periodText,
                hoursText: hoursText
        )
    }

    static class ResolvedWorkHours {
        LocalTime start
        LocalTime end
    }
}
