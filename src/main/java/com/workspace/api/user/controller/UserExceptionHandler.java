package com.workspace.api.user.controller;

import com.workspace.api.user.exception.RefreshRejectedException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = UserController.class)
public class UserExceptionHandler {
    @ExceptionHandler(RefreshRejectedException.class)
    public ResponseEntity<SessionError> handleRefreshError(
            RefreshRejectedException exception,
            jakarta.servlet.http.HttpServletResponse response) {
        // A concurrent successful rotation may already have installed a new cookie.
        // Only explicit logout clears it; failures must not overwrite another response's cookie.
        response.setHeader("Cache-Control", "no-store");
        return ResponseEntity.status(401).body(new SessionError(401, "REFRESH_UNAVAILABLE",
                "Session expired or revoked", java.util.List.of()));
    }

    public record SessionError(int status, String code, String message, java.util.List<String> details) {}

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<UserError> handleBusinessError(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode())
                .body(new UserError(exception.getStatusCode().value(), exception.getReason()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<UserError> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .sorted().collect(java.util.stream.Collectors.joining("; "));
        return ResponseEntity.badRequest().body(new UserError(400, message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<UserError> handleUnreadableRequest(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(new UserError(400, "Invalid user request"));
    }

    public record UserError(int status, String message) {}
}
