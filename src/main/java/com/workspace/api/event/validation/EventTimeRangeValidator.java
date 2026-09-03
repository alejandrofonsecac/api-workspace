package com.workspace.api.event.validation;

import com.workspace.api.event.dto.EventRequestDTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalTime;

public class EventTimeRangeValidator implements ConstraintValidator<ValidEventTimeRange, EventRequestDTO> {
    @Override
    public boolean isValid(EventRequestDTO event, ConstraintValidatorContext context) {
        if (event == null || event.startTime() == null || event.endTime() == null) {
            return true;
        }

        LocalTime startTime = event.startTime();
        LocalTime endTime = event.endTime();
        boolean startsOnTheHour = startTime.getMinute() == 0
                && startTime.getSecond() == 0
                && startTime.getNano() == 0;

        //Verifica se o bloco passou de 1 hora
        boolean isOneHourLong = endTime.equals(startTime.plusHours(1));
        //Se está entre as 8 e 20 horas (limite temporario do planner)
        boolean isWithinDailySchedule = !startTime.isBefore(LocalTime.of(8, 0))
                && !endTime.isAfter(LocalTime.of(20, 0));

        if (startsOnTheHour && isOneHourLong && isWithinDailySchedule) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("endTime")
                .addConstraintViolation();
        return false;
    }
}
