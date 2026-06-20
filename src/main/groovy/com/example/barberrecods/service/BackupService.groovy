package com.example.barberrecods.service

import com.example.barberrecods.entity.BarberService
import com.example.barberrecods.entity.Booking
import com.example.barberrecods.entity.SalonSettings
import com.example.barberrecods.repository.BarberServiceRepository
import com.example.barberrecods.repository.BookingRepository
import com.example.barberrecods.repository.SalonClosedDayRepository
import com.example.barberrecods.repository.SalonSettingsRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class BackupService {

    private static final DateTimeFormatter FILE_FORMAT = DateTimeFormatter.ofPattern('yyyy-MM-dd_HHmmss')

    private final BarberServiceRepository serviceRepository
    private final BookingRepository bookingRepository
    private final SalonSettingsRepository settingsRepository
    private final SalonClosedDayRepository closedDayRepository
    private final ObjectMapper objectMapper
    private final Path backupDir

    BackupService(BarberServiceRepository serviceRepository,
                  BookingRepository bookingRepository,
                  SalonSettingsRepository settingsRepository,
                  SalonClosedDayRepository closedDayRepository,
                  @Value('${app.backups.path:backups}') String backupPath) {
        this.serviceRepository = serviceRepository
        this.bookingRepository = bookingRepository
        this.settingsRepository = settingsRepository
        this.closedDayRepository = closedDayRepository
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        this.backupDir = Paths.get(backupPath)
    }

    String createBackup() {
        Files.createDirectories(backupDir)
        String filename = "backup-${LocalDateTime.now().format(FILE_FORMAT)}.json"
        Path file = backupDir.resolve(filename)

        Map payload = [
                createdAt: LocalDateTime.now().toString(),
                services : serviceRepository.findAll(),
                bookings : bookingRepository.findAll(),
                settings : settingsRepository.findAll(),
                closedDays: closedDayRepository.findAll()
        ]

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), payload)
        filename
    }

    List<String> listBackups() {
        if (!Files.exists(backupDir)) {
            return []
        }
        Files.list(backupDir)
                .filter { Files.isRegularFile(it) && it.fileName.toString().endsWith('.json') }
                .map { it.fileName.toString() }
                .sorted(Comparator.reverseOrder())
                .toList()
    }

    Path getBackupFile(String filename) {
        if (!filename || !filename.matches(/backup-\d{4}-\d{2}-\d{2}_\d{6}\.json/)) {
            throw new IllegalArgumentException('Некорректное имя файла')
        }
        Path file = backupDir.resolve(filename).normalize()
        if (!file.startsWith(backupDir.normalize()) || !Files.exists(file)) {
            throw new IllegalArgumentException('Файл не найден')
        }
        file
    }

    @Scheduled(cron = '0 0 3 * * *')
    void scheduledBackup() {
        createBackup()
    }
}
