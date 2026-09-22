package com.workspace.api.user.controller;

import com.workspace.api.user.dto.request.LoginRequest;
import com.workspace.api.user.dto.request.RegisterRequest;
import com.workspace.api.user.dto.response.AuthResponse;
import com.workspace.api.user.dto.response.UserResponse;
import com.workspace.api.user.service.UserService;
import com.workspace.api.user.service.RefreshSessionService;
import com.workspace.api.user.security.RefreshCookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService users;
    private final RefreshSessionService sessions;
    private final RefreshCookie cookies;

    private AuthResponse respond(RefreshSessionService.IssuedSession session, HttpServletResponse response) {
        cookies.write(response, session.cookieValue, session.cookieLifetime);
        return session.auth;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody
                                     RegisterRequest request,
                                 @CookieValue(name = RefreshCookie.NAME, required = false) String cookie,
                                 HttpServletResponse response) {
        return respond(sessions.register(request, cookie), response);
    }

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request,
            @CookieValue(name = RefreshCookie.NAME, required = false) String cookie,
                HttpServletResponse response)
    {
        return respond(sessions.login(request, cookie), response);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@CookieValue(name = RefreshCookie.NAME, required = false) String cookie,
                                HttpServletResponse response) {
        return respond(sessions.refresh(cookie), response);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@CookieValue(name = RefreshCookie.NAME, required = false) String cookie,
                       HttpServletResponse response) {
        sessions.logout(cookie);
        cookies.clear(response);
    }

    @PostMapping("/logout-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutAll(HttpServletResponse response) {
        sessions.logoutAll();
        cookies.clear(response);
    }

    @GetMapping("/me")
    public UserResponse me() { return users.me(); }
}
