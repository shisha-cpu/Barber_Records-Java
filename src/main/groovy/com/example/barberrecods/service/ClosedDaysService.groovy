package com.example.barberrecods.service

import com.example.barberrecods.dto.ClosedDayForm
import com.example.barberrecods.dto.ClosedDayViewDto
import com.example.barberrecods.entity.SalonClosedDay
import com.example.barberrecods.repository.SalonClosedDayRepository
import com.example.barberrecods.repository.SalonSettingsRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class ClosedDaysService {

    private static final DateTimeFormatter DATE_RU = DateTimeFormatter.ofPattern('dd.MM.yyyy')

    private final SalonClosedDayRepository repository
    private final SalonSettingsRepository settingsRepository

    ClosedDaysService(SalonClosedDayRepository repository,
                      SalonSettingsRepository settingsRepository) {
        this.repository = repository
        this.settingsRepository = settingsRepository
    }

    boolean isWeekendsClosed() {
        settingsRepository.findById(1L).map { it.weekendsClosed }.orElse(true)
    }

    List<ClosedDayViewDto> listAll() {
        repository.findAllByOrderByFromDateAsc().collect { toDto(it) }
    }

    boolean isDateClosed(LocalDate date) {
        if (isWeekendClosed(date)) {
            return true
        }
        repository.findOverlapping(date, date).any { period ->
            !date.isBefore(period.fromDate) && !date.isAfter(period.toDate)
        }
    }

    List<String> getClosedIsoDatesBetween(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            return []
        }
        Set<String> dates = [] as Set
        if (isWeekendsClosed()) {
            LocalDate day = from
            while (!day.isAfter(to)) {
                if (isWeekend(day)) {
                    dates << day.toString()
                }
                day = day.plusDays(1)
            }
        }
        repository.findOverlapping(from, to).each { period ->
            LocalDate start = period.fromDate.isBefore(from) ? from : period.fromDate
            LocalDate end = period.toDate.isAfter(to) ? to : period.toDate
            LocalDate day = start
            while (!day.isAfter(end)) {
                dates << day.toString()
                day = day.plusDays(1)
            }
        }
        dates.sort()
    }

    @Transactional
    SalonClosedDay add(ClosedDayForm form) {
        if (!form.fromDate?.trim() || !form.toDate?.trim()) {
            throw new IllegalArgumentException('Укажите даты')
        }

        LocalDate from = LocalDate.parse(form.fromDate.trim())
        LocalDate to = LocalDate.parse(form.toDate.trim())

        if (to.isBefore(from)) {
            throw new IllegalArgumentException('Дата окончания раньше начала')
        }

        String label = form.label?.trim()
        if (!label) {
            label = from == to ? 'Выходной' : 'Отпуск'
        }

        repository.save(new SalonClosedDay(
                fromDate: from,
                toDate: to,
                label: label
        ))
    }

    @Transactional
    void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException('Период не найден')
        }
        repository.deleteById(id)
    }

    @Transactional
    void updateWeekendsClosed(boolean weekendsClosed) {
        def settings = settingsRepository.findById(1L).orElseGet {
            settingsRepository.save(new com.example.barberrecods.entity.SalonSettings())
        }
        settings.weekendsClosed = weekendsClosed
        settingsRepository.save(settings)
    }

    private static boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.dayOfWeek
        day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY
    }

    private boolean isWeekendClosed(LocalDate date) {
        isWeekendsClosed() && isWeekend(date)
    }

    private static ClosedDayViewDto toDto(SalonClosedDay period) {
        String periodText = period.fromDate == period.toDate
                ? period.fromDate.format(DATE_RU)
                : "${period.fromDate.format(DATE_RU)} — ${period.toDate.format(DATE_RU)}"

        new ClosedDayViewDto(
                id: period.id,
                fromDate: period.fromDate.toString(),
                toDate: period.toDate.toString(),
                label: period.label,
                periodText: periodText
        )
    }
}
