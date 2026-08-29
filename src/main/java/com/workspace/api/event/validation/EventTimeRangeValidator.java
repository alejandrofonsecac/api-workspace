package com.workspace.api.event.validation;

import com.workspace.api.event.dto.EventRequestDTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EventTimeRangeValidator implements ConstraintValidator<ValidEventTimeRange, EventRequestDTO> {
    @Override
    public boolean isValid(EventRequestDTO event, ConstraintValidatorContext context) {
        if (event == null || event.startTime() == null || event.endTime() == null) {
            return true;
        }

        if (event.endTime().isAfter(event.startTime())) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("endTime")
                .addConstraintViolation();
        return false;
    }
}
