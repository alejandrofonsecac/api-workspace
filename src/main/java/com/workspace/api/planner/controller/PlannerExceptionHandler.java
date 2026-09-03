package com.workspace.api.planner.controller;

import com.workspace.api.planner.exception.InvalidPlannerSlotException;
import com.workspace.api.planner.exception.OccupiedPlannerSlotException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PlannerExceptionHandler {
    @ExceptionHandler(InvalidPlannerSlotException.class)
    public ResponseEntity<PlannerErrorResponse> handleInvalidSlot(InvalidPlannerSlotException exception) {
        return error(HttpStatus.UNPROCESSABLE_CONTENT, exception.getMessage());
    }

    @ExceptionHandler(OccupiedPlannerSlotException.class)
    public ResponseEntity<PlannerErrorResponse> handleOccupiedSlot(OccupiedPlannerSlotException exception) {
        return error(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<PlannerErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        String message = fieldError != null
                ? fieldError.getDefaultMessage()
                : exception.getBindingResult().getAllErrors().getFirst().getDefaultMessage();
        return error(HttpStatus.UNPROCESSABLE_CONTENT, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<PlannerErrorResponse> handleUnreadableRequest(HttpMessageNotReadableException exception) {
        return error(HttpStatus.UNPROCESSABLE_CONTENT,
                "Invalid request. date must use YYYY-MM-DD and times must use HH:mm.");
    }

    private ResponseEntity<PlannerErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new PlannerErrorResponse(status.value(), message));
    }

    public record PlannerErrorResponse(int status, String message) {
    }
}
