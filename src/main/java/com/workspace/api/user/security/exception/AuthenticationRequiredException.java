package com.workspace.api.user.security.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class AuthenticationRequiredException extends ResponseStatusException {
    public AuthenticationRequiredException() {
        super(HttpStatus.UNAUTHORIZED, "Authentication required");
    }
}
