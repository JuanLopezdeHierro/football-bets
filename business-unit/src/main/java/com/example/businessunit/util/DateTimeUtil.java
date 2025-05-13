package com.example.businessunit.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateTimeUtil {

    private static final Logger logger = LoggerFactory.getLogger(DateTimeUtil.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DAY_MONTH_TIME_FORMATTER_ES = DateTimeFormatter.ofPattern("d MMM HH:mm", new Locale("es", "ES"));
    private static final Pattern DAY_MONTH_PATTERN = Pattern.compile("(\\d{1,2})\\s+([a-zA-Z]+)");

    public static ZonedDateTime parseCustomDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
            return null;
        }
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        LocalDate datePart = today;
        LocalTime timePart;

        try {
            if (dateTimeString.matches("\\d{2}:\\d{2}")) { // Formato "19:00"
                timePart = LocalTime.parse(dateTimeString, TIME_FORMATTER);
            } else if (dateTimeString.toLowerCase().startsWith("mañana")) { // Formato "Mañana 19:00"
                datePart = today.plusDays(1);
                String timeStr = dateTimeString.substring(dateTimeString.indexOf(" ") + 1);
                timePart = LocalTime.parse(timeStr, TIME_FORMATTER);
            } else if (dateTimeString.matches("\\d{1,2}\\s+[a-zA-Z]+\\s+\\d{2}:\\d{2}")) { // Formato "15 may 19:00"
                // Necesitamos parsear el mes en español
                Matcher matcher = DAY_MONTH_PATTERN.matcher(dateTimeString);
                if (matcher.find()) {
                    int day = Integer.parseInt(matcher.group(1));
                    String monthStrEs = matcher.group(2).toLowerCase();
                    Month month = mapSpanishMonth(monthStrEs);
                    if (month == null) {
                        logger.warn("Mes no reconocido en español: {}", monthStrEs);
                        return null;
                    }
                    // Asumimos el año actual. Esto podría necesitar ajuste si los eventos cruzan años.
                    datePart = LocalDate.of(today.getYear(), month, day);
                    String timeStr = dateTimeString.substring(dateTimeString.lastIndexOf(" ") + 1);
                    timePart = LocalTime.parse(timeStr, TIME_FORMATTER);
                } else {
                    logger.warn("No se pudo parsear la fecha con formato 'dd MMM HH:mm': {}", dateTimeString);
                    return null;
                }
            } else {
                logger.warn("Formato de dateTimeString no reconocido: {}", dateTimeString);
                return null; // Formato no reconocido
            }
            return LocalDateTime.of(datePart, timePart).atZone(ZoneId.systemDefault());
        } catch (DateTimeParseException e) {
            logger.error("Error parseando dateTimeString: '{}'. Error: {}", dateTimeString, e.getMessage());
            return null;
        }
    }

    private static Month mapSpanishMonth(String monthStrEs) {
        return switch (monthStrEs) {
            case "ene" -> Month.JANUARY;
            case "feb" -> Month.FEBRUARY;
            case "mar" -> Month.MARCH;
            case "abr" -> Month.APRIL;
            case "may" -> Month.MAY;
            case "jun" -> Month.JUNE;
            case "jul" -> Month.JULY;
            case "ago" -> Month.AUGUST;
            case "sep", "set" -> Month.SEPTEMBER; // "set" es común también
            case "oct" -> Month.OCTOBER;
            case "nov" -> Month.NOVEMBER;
            case "dic" -> Month.DECEMBER;
            default -> null;
        };
    }
}
