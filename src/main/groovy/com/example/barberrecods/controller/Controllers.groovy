package com.example.barberrecods.controller

import com.example.barberrecods.dto.BookingRequest
import com.example.barberrecods.dto.ClosedDayForm
import com.example.barberrecods.dto.AvailableSlotsDto
import com.example.barberrecods.dto.BookingViewDto
import com.example.barberrecods.dto.LimitsForm
import com.example.barberrecods.dto.LunchBreakForm
import com.example.barberrecods.dto.WeekendsForm
import com.example.barberrecods.dto.ServiceForm
import com.example.barberrecods.entity.Booking
import com.example.barberrecods.entity.SalonSettings
import com.example.barberrecods.service.AvailableSlotsPdfService
import com.example.barberrecods.service.BackupService
import com.example.barberrecods.service.BarberServiceService
import com.example.barberrecods.service.BookingService
import com.example.barberrecods.service.ClosedDaysService
import com.example.barberrecods.service.SalonSettingsService
import com.example.barberrecods.util.ClientIpUtils
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.io.UrlResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*

import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Controller
class PublicController {

    private final BarberServiceService barberServiceService

    PublicController(BarberServiceService barberServiceService) {
        this.barberServiceService = barberServiceService
    }

    @GetMapping('/')
    String index(Model model) {
        model.addAttribute('services', barberServiceService.getActiveServices())
        'index'
    }
}

@RestController
@RequestMapping('/api')
class PublicApiController {

    private final BarberServiceService barberServiceService
    private final BookingService bookingService
    private final ClosedDaysService closedDaysService

    PublicApiController(BarberServiceService barberServiceService,
                        BookingService bookingService,
                        ClosedDaysService closedDaysService) {
        this.barberServiceService = barberServiceService
        this.bookingService = bookingService
        this.closedDaysService = closedDaysService
    }

    @GetMapping('/services')
    List getServices() {
        barberServiceService.getActiveServices()
    }

    @GetMapping('/times')
    List<String> getAvailableTimes(@RequestParam('serviceId') Long serviceId,
                                   @RequestParam('date') String date) {
        bookingService.getAvailableTimes(serviceId, LocalDate.parse(date))
    }

    @GetMapping('/closed-days')
    List<String> getClosedDays(@RequestParam('from') String from,
                               @RequestParam('to') String to) {
        closedDaysService.getClosedIsoDatesBetween(LocalDate.parse(from), LocalDate.parse(to))
    }

    @PostMapping('/bookings')
    ResponseEntity<?> createBooking(@RequestBody BookingRequest request, HttpServletRequest httpRequest) {
        try {
            String clientIp = ClientIpUtils.resolve(httpRequest)
            Booking booking = bookingService.createBooking(request, clientIp)
            ResponseEntity.ok([
                    id: booking.id,
                    message: 'Запись успешно создана'
            ])
        } catch (IllegalArgumentException e) {
            ResponseEntity.badRequest().body([error: e.message])
        }
    }
}

@Controller
@RequestMapping('/admin')
class AdminController {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern('HH:mm')

    private final BarberServiceService barberServiceService
    private final BookingService bookingService
    private final SalonSettingsService salonSettingsService
    private final ClosedDaysService closedDaysService
    private final BackupService backupService

    AdminController(BarberServiceService barberServiceService,
                    BookingService bookingService,
                    SalonSettingsService salonSettingsService,
                    ClosedDaysService closedDaysService,
                    BackupService backupService) {
        this.barberServiceService = barberServiceService
        this.bookingService = bookingService
        this.salonSettingsService = salonSettingsService
        this.closedDaysService = closedDaysService
        this.backupService = backupService
    }

    @GetMapping('/login')
    String login() {
        'admin/login'
    }

    @GetMapping('')
    String dashboard(Model model, @RequestParam(value = 'tab', defaultValue = 'calendar') String tab) {
        SalonSettings settings = salonSettingsService.getSettings()
        model.addAttribute('activeTab', tab)
        model.addAttribute('services', barberServiceService.getAllServices())
        model.addAttribute('activeServices', barberServiceService.getActiveServices())
        model.addAttribute('serviceForm', new ServiceForm())
        model.addAttribute('lunchBreakForm', new LunchBreakForm(
                lunchBreakStart: settings.lunchBreakStart.format(TIME_FORMAT),
                lunchBreakEnd: settings.lunchBreakEnd.format(TIME_FORMAT)
        ))
        model.addAttribute('limitsForm', new LimitsForm(
                maxActiveBookingsPerPhone: settings.maxActiveBookingsPerPhone,
                maxBookingsPerPhonePerDay: settings.maxBookingsPerPhonePerDay,
                maxBookingsPerIpPerHour: settings.maxBookingsPerIpPerHour,
                maxBookingsPerIpPerDay: settings.maxBookingsPerIpPerDay
        ))
        model.addAttribute('backups', backupService.listBackups())
        model.addAttribute('closedDayForm', new ClosedDayForm())
        model.addAttribute('weekendsForm', new WeekendsForm(weekendsClosed: closedDaysService.isWeekendsClosed()))
        model.addAttribute('closedDays', closedDaysService.listAll())
        'admin/dashboard'
    }

    @PostMapping('/services')
    String createService(@ModelAttribute ServiceForm serviceForm) {
        try {
            barberServiceService.create(serviceForm)
        } catch (IllegalArgumentException ignored) {
        }
        'redirect:/admin?tab=services'
    }

    @PostMapping('/services/{id}')
    String updateService(@PathVariable('id') Long id, @ModelAttribute ServiceForm serviceForm) {
        try {
            barberServiceService.update(id, serviceForm)
        } catch (IllegalArgumentException ignored) {
        }
        'redirect:/admin?tab=services'
    }

    @PostMapping('/services/{id}/delete')
    String deleteService(@PathVariable('id') Long id) {
        try {
            barberServiceService.delete(id)
        } catch (IllegalArgumentException ignored) {
        }
        'redirect:/admin?tab=services'
    }

    @PostMapping('/settings/limits')
    String updateLimits(@ModelAttribute LimitsForm limitsForm) {
        try {
            salonSettingsService.updateLimits(limitsForm)
        } catch (IllegalArgumentException ignored) {
        }
        'redirect:/admin?tab=settings'
    }

    @PostMapping('/backups/create')
    String createBackup() {
        try {
            backupService.createBackup()
        } catch (Exception ignored) {
        }
        'redirect:/admin?tab=settings'
    }

    @GetMapping('/backups/download')
    ResponseEntity<?> downloadBackup(@RequestParam('file') String file) {
        try {
            def resource = new UrlResource(backupService.getBackupFile(file).toUri())
            ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${file}\"")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(resource)
        } catch (IllegalArgumentException e) {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping('/settings/lunch-break')
    String updateLunchBreak(@ModelAttribute LunchBreakForm lunchBreakForm) {
        try {
            salonSettingsService.updateLunchBreak(lunchBreakForm)
        } catch (IllegalArgumentException ignored) {
        }
        'redirect:/admin?tab=settings'
    }

    @PostMapping('/settings/closed-days')
    String addClosedDay(@ModelAttribute ClosedDayForm closedDayForm) {
        try {
            closedDaysService.add(closedDayForm)
        } catch (IllegalArgumentException ignored) {
        }
        'redirect:/admin?tab=settings'
    }

    @PostMapping('/settings/weekends')
    String updateWeekends(@ModelAttribute WeekendsForm weekendsForm) {
        closedDaysService.updateWeekendsClosed(weekendsForm.weekendsClosed == true)
        'redirect:/admin?tab=settings'
    }

    @PostMapping('/settings/closed-days/{id}/delete')
    String deleteClosedDay(@PathVariable('id') Long id) {
        try {
            closedDaysService.delete(id)
        } catch (IllegalArgumentException ignored) {
        }
        'redirect:/admin?tab=settings'
    }

    @PostMapping('/bookings/{id}/delete')
    String deleteBooking(@PathVariable('id') Long id) {
        try {
            bookingService.deleteBooking(id)
        } catch (IllegalArgumentException ignored) {
        }
        'redirect:/admin?tab=calendar'
    }
}

@RestController
@RequestMapping('/admin/api')
class AdminApiController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern('yyyy-MM-dd')
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern('HH:mm')

    private final BookingService bookingService
    private final AvailableSlotsPdfService pdfService

    AdminApiController(BookingService bookingService, AvailableSlotsPdfService pdfService) {
        this.bookingService = bookingService
        this.pdfService = pdfService
    }

    @GetMapping('/bookings')
    List<BookingViewDto> getBookings(@RequestParam('from') String from,
                                     @RequestParam('to') String to) {
        bookingService.getBookingsBetween(LocalDate.parse(from), LocalDate.parse(to))
                .collect { toDto(it) }
    }

    @GetMapping('/available-slots')
    AvailableSlotsDto getAvailableSlots(@RequestParam('serviceId') Long serviceId,
                                        @RequestParam('from') String from,
                                        @RequestParam('to') String to) {
        bookingService.getAvailableSlotsForRange(serviceId, LocalDate.parse(from), LocalDate.parse(to))
    }

    @GetMapping('/available-slots/pdf')
    ResponseEntity<byte[]> getAvailableSlotsPdf(@RequestParam('serviceId') Long serviceId,
                                                @RequestParam('from') String from,
                                                @RequestParam('to') String to) {
        byte[] pdf = pdfService.generate(serviceId, LocalDate.parse(from), LocalDate.parse(to))
        ResponseEntity.ok()
                .header('Content-Disposition', 'inline; filename="available-slots.pdf"')
                .header('Content-Type', 'application/pdf')
                .body(pdf)
    }

    private static BookingViewDto toDto(Booking booking) {
        new BookingViewDto(
                id: booking.id,
                date: booking.bookingDate.format(DATE_FORMAT),
                time: booking.bookingTime.format(TIME_FORMAT),
                serviceName: booking.service.name,
                durationMinutes: booking.service.durationMinutes,
                price: booking.service.price,
                clientName: booking.clientName,
                clientPhone: booking.clientPhone,
                createdAt: BookingService.formatCreatedAt(booking.createdAt)
        )
    }
}
