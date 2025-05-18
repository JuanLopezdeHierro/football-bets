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
    private static final Pattern DAY_MONTH_TIME_PATTERN = Pattern.compile("(\\d{1,2})\\s+([a-zA-Z]+)\\s+(\\d{2}:\\d{2})");
    private static final Pattern TOMORROW_TIME_PATTERN = Pattern.compile("Mañana\\s+(\\d{2}:\\d{2})", Pattern.CASE_INSENSITIVE);

    public static ZonedDateTime parseCustomDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
            logger.debug("Input dateTimeString is null or empty.");
            return null;
        }

        ZoneId systemZone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(systemZone);
        LocalDate datePart = today;
        LocalTime timePart;

        try {
            if (dateTimeString.matches("\\d{2}:\\d{2}")) {
                timePart = LocalTime.parse(dateTimeString, TIME_ONLY_FORMATTER);
                return LocalDateTime.of(datePart, timePart).atZone(systemZone);
            }

            Matcher tomorrowMatcher = TOMORROW_TIME_PATTERN.matcher(dateTimeString);
            if (tomorrowMatcher.matches()) {
                datePart = today.plusDays(1);
                timePart = LocalTime.parse(tomorrowMatcher.group(1), TIME_ONLY_FORMATTER);
                return LocalDateTime.of(datePart, timePart).atZone(systemZone);
            }

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
                int year = today.getYear();
                datePart = LocalDate.of(year, month, day);
                return LocalDateTime.of(datePart, timePart).atZone(systemZone);
            }

            logger.warn("Unrecognized dateTimeString format: {}", dateTimeString);
            return null;
        } catch (DateTimeParseException e) {
            logger.error("Error parsing dateTimeString: '{}'. Error: {}", dateTimeString, e.getMessage());
            return null;
        } catch (Exception e) {
            logger.error("Unexpected error parsing dateTimeString: '{}'. Error: {}", dateTimeString, e.getMessage(), e);
            return null;
        }
    }

    private static Month mapSpanishMonth(String monthStrEs) {
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