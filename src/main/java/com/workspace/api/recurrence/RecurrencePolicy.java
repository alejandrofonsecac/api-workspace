package com.workspace.api.recurrence;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Bounded recurrence: dates and limits are authoritative on the server.
 */
public final class RecurrencePolicy {
    // BYDAY only supports the weekdays-only daily variant: Monday to Friday, without holidays.
    private static final Pattern RULE = Pattern.compile("FREQ=(DAILY|WEEKLY|MONTHLY|YEARLY)(;BYDAY=MO,TU,WE,TH,FR)?(?:;UNTIL=([0-9]{8}))?");

    private RecurrencePolicy() {
    }

    public static List<LocalDate> dates(LocalDate start, Boolean recurring, String rule) {
        if (rule == null || rule.isBlank()){
            return Collections.singletonList(start);
        }

        var matcher = RULE.matcher(rule.trim());
        var result = new ArrayList<LocalDate>();
        LocalDate until;

        if (!matcher.matches()){
            throw invalid("Unsupported recurrence rule");
        }

        boolean weekdaysOnly = matcher.group(2) != null;
        if (weekdaysOnly && (!matcher.group(1).equals("DAILY") || matcher.group(3) == null)){
            throw invalid("Weekdays-only recurrence requires FREQ=DAILY and an end date");
        }

        // Existing clients used FREQ alone as retention metadata. Preserve that contract.
        if (matcher.group(3) == null){
            return Collections.singletonList(start);
        }

        if (Boolean.FALSE.equals(recurring) || start == null){
            throw invalid("Recurrence requires a start date and isRecurring=true");
        }

        try {
            until = LocalDate.parse(matcher.group(3), DateTimeFormatter.BASIC_ISO_DATE);
        } catch (DateTimeException error) {
            throw invalid("Invalid recurrence end date");
        }

        if (until.isBefore(start) || until.isAfter(start.plusYears(10))){
            throw invalid("Recurrence must end within ten years of its start");
        }

        for (int index = 0; ; index++) {
            LocalDate date = switch (matcher.group(1)) {
                case "DAILY" -> start.plusDays(index);
                case "WEEKLY" -> start.plusWeeks(index);
                case "MONTHLY" -> start.plusMonths(index);
                default -> start.plusYears(index);
            };

            if (date.isAfter(until)) break;

            // Calendar recurrence skips dates absent in a month/year, without shifting the anchor.
            if ((matcher.group(1)
                    .equals("MONTHLY") || matcher.group(1)
                    .equals("YEARLY")) && date.getDayOfMonth() != start.getDayOfMonth()){
                continue;
            }

            // A weekend start date is skipped as well: the series begins on the next weekday.
            if (weekdaysOnly && isWeekend(date)){
                continue;
            }

            if (result.size() == 1000){
                throw invalid("Use at most 1000 occurrences");
            }
            result.add(date);
        }

        if (result.isEmpty()){
            throw invalid("Recurrence has no occurrences in the chosen period");
        }
        return result;
    }

    private static boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private static ResponseStatusException invalid(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }
}
