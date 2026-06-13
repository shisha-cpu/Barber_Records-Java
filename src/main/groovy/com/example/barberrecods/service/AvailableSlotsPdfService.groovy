package com.example.barberrecods.service

import com.example.barberrecods.dto.AvailableSlotsDto
import com.lowagie.text.Document
import com.lowagie.text.Element
import com.lowagie.text.Font
import com.lowagie.text.PageSize
import com.lowagie.text.Paragraph
import com.lowagie.text.Phrase
import com.lowagie.text.pdf.BaseFont
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import com.lowagie.text.pdf.PdfWriter
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service

import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Service
class AvailableSlotsPdfService {

    private static final DateTimeFormatter DATE_RU = DateTimeFormatter.ofPattern('dd.MM.yyyy (EEEE)', Locale.forLanguageTag('ru'))

    private final BookingService bookingService

    AvailableSlotsPdfService(BookingService bookingService) {
        this.bookingService = bookingService
    }

    byte[] generate(Long serviceId, LocalDate from, LocalDate to) {
        AvailableSlotsDto slots = bookingService.getAvailableSlotsForRange(serviceId, from, to)
        BaseFont baseFont = loadFont()
        Font titleFont = new Font(baseFont, 18, Font.BOLD)
        Font headerFont = new Font(baseFont, 12, Font.BOLD)
        Font bodyFont = new Font(baseFont, 11, Font.NORMAL)
        Font mutedFont = new Font(baseFont, 10, Font.NORMAL)

        ByteArrayOutputStream out = new ByteArrayOutputStream()
        Document document = new Document(PageSize.A4, 40, 40, 48, 48)
        PdfWriter.getInstance(document, out)
        document.open()

        Paragraph title = new Paragraph('Barber Records', titleFont)
        title.alignment = Element.ALIGN_CENTER
        document.add(title)

        document.add(new Paragraph('Доступные окна для записи', headerFont))
        document.add(new Paragraph("Услуга: ${slots.serviceName}", bodyFont))
        document.add(new Paragraph("Длительность: ${slots.durationMinutes} мин", bodyFont))
        document.add(new Paragraph("Цена: ${formatPrice(slots.price)}", bodyFont))
        document.add(new Paragraph("Период: ${formatDate(slots.from)} — ${formatDate(slots.to)}", bodyFont))
        document.add(new Paragraph(' ', bodyFont))

        if (slots.days.isEmpty()) {
            document.add(new Paragraph('На выбранный период свободных окон нет.', mutedFont))
        } else {
            slots.days.each { day ->
                document.add(new Paragraph(formatDate(day.date), headerFont))
                PdfPTable table = new PdfPTable(4)
                table.widthPercentage = 100
                table.spacingAfter = 10f
                day.times.each { time ->
                    PdfPCell cell = new PdfPCell(new Phrase(time, bodyFont))
                    cell.border = PdfPCell.NO_BORDER
                    cell.horizontalAlignment = Element.ALIGN_CENTER
                    cell.padding = 6f
                    table.addCell(cell)
                }
                int remainder = day.times.size() % 4
                if (remainder != 0) {
                    (4 - remainder).times { table.addCell(emptyCell()) }
                }
                document.add(table)
            }
        }

        document.add(new Paragraph(' ', bodyFont))
        Paragraph footer = new Paragraph('Запишитесь онлайн или ответьте удобное время мастеру.', mutedFont)
        footer.alignment = Element.ALIGN_CENTER
        document.add(footer)

        document.close()
        out.toByteArray()
    }

    private static BaseFont loadFont() {
        List<String> candidates = [
                'fonts/Arial.ttf',
                'fonts/DejaVuSans.ttf',
                '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',
                'C:/Windows/Fonts/arial.ttf'
        ]
        for (String path : candidates) {
            BaseFont font = tryLoadFont(path)
            if (font != null) {
                return font
            }
        }
        BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED)
    }

    private static BaseFont tryLoadFont(String path) {
        try {
            if (path.startsWith('fonts/')) {
                ClassPathResource resource = new ClassPathResource(path)
                if (!resource.exists()) {
                    return null
                }
                byte[] fontBytes = resource.inputStream.bytes
                return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, fontBytes, null)
            }
            File file = new File(path)
            if (!file.exists()) {
                return null
            }
            return BaseFont.createFont(file.absolutePath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED)
        } catch (Exception ignored) {
            return null
        }
    }

    private static PdfPCell emptyCell() {
        PdfPCell cell = new PdfPCell()
        cell.border = PdfPCell.NO_BORDER
        cell
    }

    private static String formatDate(String iso) {
        LocalDate.parse(iso).format(DATE_RU)
    }

    private static String formatPrice(BigDecimal price) {
        "${price.stripTrailingZeros().toPlainString()} ₽"
    }
}
