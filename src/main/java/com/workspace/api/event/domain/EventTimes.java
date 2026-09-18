package com.workspace.api.event.domain;

import java.time.LocalTime;

/**
 * End-of-day is stored as midnight and exposed as 24:00 only for interval ends.
 */
public final class EventTimes {
    private EventTimes() {
    }

    public static int startMinutes(LocalTime time) {
        return time.getHour() * 60 + time.getMinute();
    }

    public static int endMinutes(LocalTime time) {
        return time.equals(LocalTime.MIDNIGHT) ? 1440 : startMinutes(time);
    }

    public static String endText(LocalTime time) {
        return time.equals(LocalTime.MIDNIGHT) ? "24:00" : time.toString();
    }

    public static LocalTime parseEnd(String time) {
        return "24:00".equals(time) ? LocalTime.MIDNIGHT : LocalTime.parse(time);
    }

    public static boolean valid(LocalTime start, String end) {
        if (start == null || end == null || !end.matches("(?:[01][0-9]|2[0-3]):[0-5][0-9]|24:00")) return false;
        if (start.getSecond() != 0 || start.getNano() != 0 || "00:00".equals(end)) return false;
        return endMinutes(parseEnd(end)) > startMinutes(start);
    }
}

