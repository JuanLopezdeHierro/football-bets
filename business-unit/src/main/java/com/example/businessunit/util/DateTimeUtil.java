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
    private static final DateTimeFormatter TIME_ONLY_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    // Considera hacer este formatter más robusto o añadir más patrones si los formatos varían mucho.
    // Por ejemplo, para "d MMM HH:mm", el mes debe estar en el locale correcto.
    // private static final DateTimeFormatter DAY_MONTH_TIME_FORMATTER_ES = DateTimeFormatter.ofPattern("d MMM HH:mm", new Locale("es", "ES"));
    private static final Pattern DAY_MONTH_TIME_PATTERN = Pattern.compile("(\\d{1,2})\\s+([a-zA-Z]+)\\s+(\\d{2}:\\d{2})"); // ej. "15 may 19:00"
    private static final Pattern TOMORROW_TIME_PATTERN = Pattern.compile("Mañana\\s+(\\d{2}:\\d{2})", Pattern.CASE_INSENSITIVE);


    public static ZonedDateTime parseCustomDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
            logger.debug("Input dateTimeString is null or empty.");
            return null;
        }

        ZoneId systemZone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(systemZone);
        LocalDate datePart = today; // Por defecto, asumimos hoy si no se especifica otra fecha.
        LocalTime timePart;

        try {
            // 1. Intenta formato "HH:mm" (asume hoy)
            if (dateTimeString.matches("\\d{2}:\\d{2}")) {
                timePart = LocalTime.parse(dateTimeString, TIME_ONLY_FORMATTER);
                return LocalDateTime.of(datePart, timePart).atZone(systemZone);
            }

            // 2. Intenta formato "Mañana HH:mm"
            Matcher tomorrowMatcher = TOMORROW_TIME_PATTERN.matcher(dateTimeString);
            if (tomorrowMatcher.matches()) {
                datePart = today.plusDays(1);
                timePart = LocalTime.parse(tomorrowMatcher.group(1), TIME_ONLY_FORMATTER);
                return LocalDateTime.of(datePart, timePart).atZone(systemZone);
            }

            // 3. Intenta formato "d MMM HH:mm" (ej. "15 may 19:00")
            Matcher dayMonthTimeMatcher = DAY_MONTH_TIME_PATTERN.matcher(dateTimeString);
            if (dayMonthTimeMatcher.matches()) {
                int day = Integer.parseInt(dayMonthTimeMatcher.group(1));
                String monthStrEs = dayMonthTimeMatcher.group(2).toLowerCase();
                Month month = mapSpanishMonth(monthStrEs);
                timePart = LocalTime.parse(dayMonthTimeMatcher.group(3), TIME_ONLY_FORMATTER);

                if (month == null) {
                    logger.warn("Spanish month not recognized: '{}' in string '{}'", monthStrEs, dateTimeString);
                    return null;
                }
                // Asume el año actual. Si el mes del evento ya pasó este año, asume el siguiente año.
                int year = today.getYear();
                if (month.getValue() < today.getMonthValue() || (month.getValue() == today.getMonthValue() && day < today.getDayOfMonth())) {
                    // Si la fecha "d MMM" es anterior a hoy en el año actual, asumimos que es del próximo año.
                    // Esto podría no ser siempre correcto si se manejan fechas históricas que también usan "d MMM".
                    // logger.trace("Date {} {} seems to be in the past for current year {}, assuming next year.", day, monthStrEs, year);
                    // year++; // Descomentar si esta es la lógica deseada. Para la mayoría de "próximos partidos", el año actual está bien.
                }
                datePart = LocalDate.of(year, month, day);
                return LocalDateTime.of(datePart, timePart).atZone(systemZone);
            }

            // 4. Si no coincide con formatos comunes, intenta parsear directamente con DateTimeFormatter (si tuvieras formatos más estándar)
            // Por ejemplo, si esperaras "yyyy-MM-dd HH:mm"
            // try {
            //     LocalDateTime ldt = LocalDateTime.parse(dateTimeString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            //     return ldt.atZone(systemZone);
            // } catch (DateTimeParseException ignored) {}


            logger.warn("Unrecognized dateTimeString format: {}", dateTimeString);
            return null;
        } catch (DateTimeParseException e) {
            logger.error("Error parsing dateTimeString: '{}'. Error: {}", dateTimeString, e.getMessage());
            return null;
        } catch (Exception e) { // Captura general por si algo más falla (ej. NumberFormatException)
            logger.error("Unexpected error parsing dateTimeString: '{}'. Error: {}", dateTimeString, e.getMessage(), e);
            return null;
        }
    }

    private static Month mapSpanishMonth(String monthStrEs) {
        // Asegurar que solo se usen las primeras 3 letras y en minúscula para la comparación
        String normalizedMonth = monthStrEs.length() > 3 ? monthStrEs.substring(0, 3).toLowerCase() : monthStrEs.toLowerCase();
        return switch (normalizedMonth) {
            case "ene" -> Month.JANUARY;
            case "feb" -> Month.FEBRUARY;
            case "mar" -> Month.MARCH;
            case "abr" -> Month.APRIL;
            case "may" -> Month.MAY;
            case "jun" -> Month.JUNE;
            case "jul" -> Month.JULY;
            case "ago" -> Month.AUGUST;
            case "sep", "set" -> Month.SEPTEMBER;
            case "oct" -> Month.OCTOBER;
            case "nov" -> Month.NOVEMBER;
            case "dic" -> Month.DECEMBER;
            default -> null;
        };
    }
}