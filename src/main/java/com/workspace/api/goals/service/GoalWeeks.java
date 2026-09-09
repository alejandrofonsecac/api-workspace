package com.workspace.api.goals.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.IsoFields;

public final class GoalWeeks {
    private GoalWeeks() {}

    public static LocalDate start(String weekId) {
        try {
            return LocalDate.parse(weekId + "-1", DateTimeFormatter.ISO_WEEK_DATE);
        } catch (DateTimeParseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid ISO week: " + weekId);
        }
    }

    public static String id(LocalDate date) {
        return "%04d-W%02d".formatted(date.get(IsoFields.WEEK_BASED_YEAR), date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
    }
}
