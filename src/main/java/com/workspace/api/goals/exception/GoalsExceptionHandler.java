package com.workspace.api.goals.exception;

import com.workspace.api.goals.controller.GoalsController;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = GoalsController.class)
public class GoalsExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<GoalsErrorResponse> handleBusinessError(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode())
                .body(new GoalsErrorResponse(exception.getStatusCode().value(), exception.getReason()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<GoalsErrorResponse> handleInvalidParameter(MethodArgumentTypeMismatchException exception) {
        return badRequest("Invalid parameter: " + exception.getName());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GoalsErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return badRequest(message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<GoalsErrorResponse> handleUnreadableRequest(HttpMessageNotReadableException exception) {
        return badRequest("Invalid goal request. Use name, expectedTarget, priority (LOW, NORMAL, HIGH), "
                + "category (WORK, STUDY, HEALTH, MIND, WELLBEING, PERSONAL, PROJECT) and optional weekId.");
    }

    private ResponseEntity<GoalsErrorResponse> badRequest(String message) {
        return ResponseEntity.badRequest().body(new GoalsErrorResponse(HttpStatus.BAD_REQUEST.value(), message));
    }

    public record GoalsErrorResponse(int status, String message) {
    }
}
