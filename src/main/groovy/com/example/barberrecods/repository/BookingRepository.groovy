package com.example.barberrecods.repository

import com.example.barberrecods.entity.Booking
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findAllByOrderByBookingDateDescBookingTimeDesc()

    List<Booking> findByBookingDateOrderByBookingTimeAsc(LocalDate date)

    List<Booking> findByBookingDateBetweenOrderByBookingDateAscBookingTimeAsc(LocalDate from, LocalDate to)

    @Query('''SELECT COUNT(b) FROM Booking b
              WHERE b.clientPhone = :phone
              AND (b.bookingDate > :today OR (b.bookingDate = :today AND b.bookingTime >= :nowTime))''')
    long countActiveBookingsByPhone(@Param('phone') String phone,
                                    @Param('today') LocalDate today,
                                    @Param('nowTime') LocalTime nowTime)

    long countByClientPhoneAndBookingDate(String clientPhone, LocalDate bookingDate)

    boolean existsByClientPhoneAndBookingDateAndBookingTime(String clientPhone,
                                                            LocalDate bookingDate,
                                                            LocalTime bookingTime)

    @Query('''SELECT COUNT(b) FROM Booking b
              WHERE b.clientIp = :ip AND b.createdAt >= :since''')
    long countByClientIpSince(@Param('ip') String ip, @Param('since') LocalDateTime since)

    long countByServices_Id(Long serviceId)
}
