package com.example.barberrecods.service

import com.example.barberrecods.dto.BookingRequest
import com.example.barberrecods.entity.SalonSettings
import com.example.barberrecods.repository.BookingRepository
import com.example.barberrecods.util.PhoneUtils
import org.springframework.stereotype.Service

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Service
class BookingProtectionService {

    private final BookingRepository bookingRepository
    private final SalonSettingsService salonSettingsService

    BookingProtectionService(BookingRepository bookingRepository,
                             SalonSettingsService salonSettingsService) {
        this.bookingRepository = bookingRepository
        this.salonSettingsService = salonSettingsService
    }

    void validate(BookingRequest request, String clientIp) {
        if (request.website?.trim()) {
            throw new IllegalArgumentException('Не удалось создать запись')
        }

        String phone = PhoneUtils.normalize(request.clientPhone)
        if (phone.length() < 11) {
            throw new IllegalArgumentException('Укажите корректный телефон')
        }

        LocalDate date = LocalDate.parse(request.date)
        LocalTime time = LocalTime.parse(request.time)

        SalonSettings settings = salonSettingsService.getSettings()
        LocalDate today = LocalDate.now()
        LocalTime now = LocalTime.now()

        if (bookingRepository.existsByClientPhoneAndBookingDateAndBookingTime(phone, date, time)) {
            throw new IllegalArgumentException('На этот номер уже есть запись на это время')
        }

        long activeCount = bookingRepository.countActiveBookingsByPhone(phone, today, now)
        if (activeCount >= settings.maxActiveBookingsPerPhone) {
            throw new IllegalArgumentException('Превышен лимит активных записей на этот номер')
        }

        long dayCount = bookingRepository.countByClientPhoneAndBookingDate(phone, date)
        if (dayCount >= settings.maxBookingsPerPhonePerDay) {
            throw new IllegalArgumentException('На этот номер уже есть запись на выбранный день')
        }

        if (clientIp) {
            LocalDateTime hourAgo = LocalDateTime.now().minusHours(1)
            LocalDateTime dayAgo = LocalDateTime.now().minusDays(1)

            if (bookingRepository.countByClientIpSince(clientIp, hourAgo) >= settings.maxBookingsPerIpPerHour) {
                throw new IllegalArgumentException('Слишком много попыток записи. Попробуйте позже')
            }
            if (bookingRepository.countByClientIpSince(clientIp, dayAgo) >= settings.maxBookingsPerIpPerDay) {
                throw new IllegalArgumentException('Превышен дневной лимит записей. Попробуйте завтра')
            }
        }
    }

    static String normalizePhone(String phone) {
        PhoneUtils.normalize(phone)
    }
}
