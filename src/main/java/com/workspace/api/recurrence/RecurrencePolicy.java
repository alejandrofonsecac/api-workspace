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
    private static final Pattern RULE = Pattern.compile("FREQ=(DAILY|WEEKLY|MONTHLY|YEARLY)(?:;UNTIL=([0-9]{8}))?");

    private RecurrencePolicy() {
    }

    public static List<LocalDate> dates(LocalDate start, Boolean recurring, String rule) {
        var matcher = RULE.matcher(rule.trim());
        var result = new ArrayList<LocalDate>();
        LocalDate until;

        if (rule.isBlank()){
            return Collections.singletonList(start);
        }

        if (!matcher.matches()){
            throw invalid("Unsupported recurrence rule");
        }

        // Existing clients used FREQ alone as retention metadata. Preserve that contract.
        if (matcher.group(2) == null){
            return Collections.singletonList(start);
        }

        if (Boolean.FALSE.equals(recurring) || start == null){
            throw invalid("Recurrence requires a start date and isRecurring=true");
        }

        try {
            until = LocalDate.parse(matcher.group(2), DateTimeFormatter.BASIC_ISO_DATE);
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

            if (result.size() == 1000){
                throw invalid("Use at most 1000 occurrences");
            } 
            result.add(date);
        }
        return result;
    }

    private static ResponseStatusException invalid(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }
}
