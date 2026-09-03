package com.workspace.api.event.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = EventTimeRangeValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEventTimeRange {
    String message() default "Invalid time slot. Use one full-hour block between 08:00 and 20:00, such as 08:00-09:00.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
