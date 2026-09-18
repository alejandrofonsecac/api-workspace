package com.workspace.api.event.validation;
import com.workspace.api.event.dto.EventRequestDTO;
import com.workspace.api.event.domain.EventTimes;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
public class EventTimeRangeValidator implements ConstraintValidator<ValidEventTimeRange, EventRequestDTO> {
    public boolean isValid(EventRequestDTO event, ConstraintValidatorContext context) {
        return event == null || EventTimes.valid(event.startTime(), event.endTime());
    }
}
