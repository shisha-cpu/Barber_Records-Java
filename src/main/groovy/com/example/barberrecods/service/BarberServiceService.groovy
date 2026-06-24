package com.example.barberrecods.service

import com.example.barberrecods.config.AppProperties
import com.example.barberrecods.dto.AvailableSlotsDayDto
import com.example.barberrecods.dto.AvailableSlotsDto
import com.example.barberrecods.dto.BookingRequest
import com.example.barberrecods.dto.ServiceDto
import com.example.barberrecods.dto.ServiceForm
import com.example.barberrecods.entity.BarberService
import com.example.barberrecods.entity.SalonSettings
import com.example.barberrecods.entity.Booking
import com.example.barberrecods.repository.BarberServiceRepository
import com.example.barberrecods.repository.BookingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class BarberServiceService {

    private final BarberServiceRepository serviceRepository
    private final BookingRepository bookingRepository

    BarberServiceService(BarberServiceRepository serviceRepository,
                         BookingRepository bookingRepository) {
        this.serviceRepository = serviceRepository
        this.bookingRepository = bookingRepository
    }

    List<ServiceDto> getActiveServices() {
        serviceRepository.findByActiveTrueOrderByNameAsc().collect { toDto(it) }
    }

    List<BarberService> getAllServices() {
        serviceRepository.findAll().sort { a, b -> a.name <=> b.name }
    }

    Optional<BarberService> findById(Long id) {
        serviceRepository.findById(id)
    }

    BarberService create(ServiceForm form) {
        validateForm(form)
        serviceRepository.save(new BarberService(
                name: form.name.trim(),
                durationMinutes: form.durationMinutes,
                price: form.price,
                active: form.active != null ? form.active : true
        ))
    }

    BarberService update(Long id, ServiceForm form) {
        validateForm(form)
        BarberService service = serviceRepository.findById(id)
                .orElseThrow { new IllegalArgumentException('Услуга не найдена') }
        service.name = form.name.trim()
        service.durationMinutes = form.durationMinutes
        service.price = form.price
        service.active = form.active != null ? form.active : true
        serviceRepository.save(service)
    }

    void delete(Long id) {
        if (!serviceRepository.existsById(id)) {
            throw new IllegalArgumentException('Услуга не найдена')
        }
        if (bookingRepository.countByServices_Id(id) > 0) {
            throw new IllegalArgumentException(
                    'Нельзя удалить услугу: есть записи с этой услугой. Снимите галочку «Активна», чтобы скрыть её.')
        }
        serviceRepository.deleteById(id)
    }

    private static void validateForm(ServiceForm form) {
        if (!form.name?.trim()) {
            throw new IllegalArgumentException('Укажите название услуги')
        }
        if (form.durationMinutes == null || form.durationMinutes <= 0) {
            throw new IllegalArgumentException('Длительность должна быть больше 0')
        }
        if (form.price == null || form.price < 0) {
            throw new IllegalArgumentException('Укажите корректную цену')
        }
    }

    private static ServiceDto toDto(BarberService service) {
        new ServiceDto(
                id: service.id,
                name: service.name,
                durationMinutes: service.durationMinutes,
                price: service.price
        )
    }
}

@Service
class BookingService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern('HH:mm')
    private static final DateTimeFormatter CREATED_AT_FORMAT = DateTimeFormatter.ofPattern('dd.MM.yyyy HH:mm')

    private final BookingRepository bookingRepository
    private final BarberServiceRepository serviceRepository
    private final AppProperties appProperties
    private final SalonSettingsService salonSettingsService
    private final ClosedDaysService closedDaysService
    private final WorkHoursOverrideService workHoursOverrideService
    private final BookingProtectionService bookingProtectionService

    BookingService(BookingRepository bookingRepository,
                   BarberServiceRepository serviceRepository,
                   AppProperties appProperties,
                   SalonSettingsService salonSettingsService,
                   ClosedDaysService closedDaysService,
                   WorkHoursOverrideService workHoursOverrideService,
                   BookingProtectionService bookingProtectionService) {
        this.bookingRepository = bookingRepository
        this.serviceRepository = serviceRepository
        this.appProperties = appProperties
        this.salonSettingsService = salonSettingsService
        this.closedDaysService = closedDaysService
        this.workHoursOverrideService = workHoursOverrideService
        this.bookingProtectionService = bookingProtectionService
    }

    List<Booking> getAllBookings() {
        bookingRepository.findAllByOrderByBookingDateDescBookingTimeDesc()
    }

    List<Booking> getBookingsBetween(LocalDate from, LocalDate to) {
        bookingRepository.findByBookingDateBetweenOrderByBookingDateAscBookingTimeAsc(from, to)
    }

    AvailableSlotsDto getAvailableSlotsForRange(Long serviceId, LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new IllegalArgumentException('Некорректный период')
        }
        if (from.plusDays(62).isBefore(to)) {
            throw new IllegalArgumentException('Период не может быть больше 62 дней')
        }

        BarberService service = serviceRepository.findById(serviceId)
                .orElseThrow { new IllegalArgumentException('Услуга не найдена') }

        LocalDate start = from.isBefore(today()) ? today() : from
        List days = []
        LocalDate date = start
        while (!date.isAfter(to)) {
            List<String> times = getAvailableTimes(serviceId, date)
            if (!times.isEmpty()) {
                days << new AvailableSlotsDayDto(
                        date: date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        times: times
                )
            }
            date = date.plusDays(1)
        }

        new AvailableSlotsDto(
                serviceId: service.id,
                serviceName: service.name,
                durationMinutes: service.durationMinutes,
                price: service.price,
                from: start.format(DateTimeFormatter.ISO_LOCAL_DATE),
                to: to.format(DateTimeFormatter.ISO_LOCAL_DATE),
                days: days
        )
    }

    List<String> getAvailableTimes(List<Long> serviceIds, LocalDate date) {
        List<BarberService> services = loadActiveServices(serviceIds)
        int totalDuration = totalDurationMinutes(services)

        if (date.isBefore(today())) {
            return []
        }

        if (closedDaysService.isDateClosed(date)) {
            return []
        }

        WorkHoursOverrideService.ResolvedWorkHours workHours = workHoursOverrideService.resolveForDate(date)
        LocalTime workStart = workHours.start
        LocalTime workEnd = workHours.end
        int interval = appProperties.slotIntervalMinutes
        LocalDate today = today()
        LocalTime now = now()

        List<Booking> dayBookings = bookingRepository.findByBookingDateOrderByBookingTimeAsc(date)
        List<String> slots = []

        LocalTime slot = workStart
        while (!slot.plusMinutes(totalDuration).isAfter(workEnd)) {
            if (date.equals(today) && !slot.isAfter(now)) {
                slot = slot.plusMinutes(interval)
                continue
            }

            if (isSlotFree(slot, totalDuration, dayBookings)
                    && !overlapsLunchBreak(slot, totalDuration)) {
                slots << slot.format(TIME_FORMAT)
            }
            slot = slot.plusMinutes(interval)
        }
        slots
    }

    List<String> getAvailableTimes(Long serviceId, LocalDate date) {
        getAvailableTimes([serviceId], date)
    }

    @Transactional
    Booking createBooking(BookingRequest request, String clientIp) {
        if (!request.clientName?.trim()) {
            throw new IllegalArgumentException('Укажите ФИО')
        }
        if (!request.clientPhone?.trim()) {
            throw new IllegalArgumentException('Укажите телефон')
        }

        bookingProtectionService.validate(request, clientIp)
        String normalizedPhone = BookingProtectionService.normalizePhone(request.clientPhone)

        List<Long> serviceIds = resolveServiceIds(request)
        List<BarberService> services = loadActiveServices(serviceIds)
        int totalDuration = totalDurationMinutes(services)

        LocalDate date = LocalDate.parse(request.date)
        LocalTime time = LocalTime.parse(request.time, TIME_FORMAT)
        LocalDate today = today()

        if (date.isBefore(today)) {
            throw new IllegalArgumentException('Нельзя записаться на прошедшую дату')
        }

        if (date.equals(today) && !time.isAfter(now())) {
            throw new IllegalArgumentException('Нельзя записаться на прошедшее время')
        }

        if (closedDaysService.isDateClosed(date)) {
            throw new IllegalArgumentException('На эту дату запись недоступна')
        }

        WorkHoursOverrideService.ResolvedWorkHours workHours = workHoursOverrideService.resolveForDate(date)
        LocalTime workStart = workHours.start
        LocalTime workEnd = workHours.end

        if (time.isBefore(workStart) || time.plusMinutes(totalDuration).isAfter(workEnd)) {
            throw new IllegalArgumentException('Выбранное время вне рабочих часов')
        }

        if (overlapsLunchBreak(time, totalDuration)) {
            throw new IllegalArgumentException('Это время недоступно')
        }

        List<Booking> dayBookings = bookingRepository.findByBookingDateOrderByBookingTimeAsc(date)
        if (!isSlotFree(time, totalDuration, dayBookings)) {
            throw new IllegalArgumentException('Это время уже занято')
        }

        bookingRepository.save(new Booking(
                services: services,
                bookingDate: date,
                bookingTime: time,
                clientName: request.clientName.trim(),
                clientPhone: normalizedPhone,
                clientIp: clientIp,
                createdAt: LocalDateTime.now(salonZone())
        ))
    }

    private static List<Long> resolveServiceIds(BookingRequest request) {
        List<Long> ids = request.serviceIds?.findAll { it != null } ?: []
        if (ids.isEmpty() && request.serviceId != null) {
            ids = [request.serviceId]
        }
        ids = ids.unique()
        if (ids.isEmpty()) {
            throw new IllegalArgumentException('Выберите хотя бы одну услугу')
        }
        ids
    }

    private List<BarberService> loadActiveServices(List<Long> serviceIds) {
        List<BarberService> services = serviceRepository.findAllById(serviceIds)
        if (services.size() != serviceIds.size()) {
            throw new IllegalArgumentException('Услуга не найдена')
        }
        if (services.any { !it.active }) {
            throw new IllegalArgumentException('Услуга недоступна')
        }
        services.sort { a, b -> serviceIds.indexOf(a.id) <=> serviceIds.indexOf(b.id) }
    }

    private static int totalDurationMinutes(List<BarberService> services) {
        services.sum { it.durationMinutes } as int
    }

    private ZoneId salonZone() {
        ZoneId.of(appProperties.timezone ?: 'Europe/Moscow')
    }

    private LocalDate today() {
        LocalDate.now(salonZone())
    }

    private LocalTime now() {
        LocalTime.now(salonZone())
    }

    static String formatCreatedAt(LocalDateTime createdAt) {
        createdAt?.format(CREATED_AT_FORMAT) ?: ''
    }

    void deleteBooking(Long id) {
        if (!bookingRepository.existsById(id)) {
            throw new IllegalArgumentException('Запись не найдена')
        }
        bookingRepository.deleteById(id)
    }

    private boolean overlapsLunchBreak(LocalTime start, int durationMinutes) {
        SalonSettings settings = salonSettingsService.getSettings()
        LocalTime end = start.plusMinutes(durationMinutes)
        start < settings.lunchBreakEnd && end > settings.lunchBreakStart
    }

    private static boolean isSlotFree(LocalTime start, int durationMinutes, List<Booking> bookings) {
        LocalTime end = start.plusMinutes(durationMinutes)
        !bookings.any { booking ->
            LocalTime bookingStart = booking.bookingTime
            LocalTime bookingEnd = bookingStart.plusMinutes(booking.totalDurationMinutes)
            start < bookingEnd && end > bookingStart
        }
    }
}
