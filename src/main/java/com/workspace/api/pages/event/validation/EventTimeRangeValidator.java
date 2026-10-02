package com.workspace.api.pages.event.validation;

import com.workspace.api.pages.event.dto.EventRequestDTO;
import com.workspace.api.pages.event.domain.EventTimes;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EventTimeRangeValidator implements ConstraintValidator<ValidEventTimeRange, EventRequestDTO> {
    public boolean isValid(EventRequestDTO event, ConstraintValidatorContext context) {
        return event == null || ((event.date() != null || event.startTime() == null)
                && EventTimes.valid(event.startTime(), event.endTime()));
    }
}
