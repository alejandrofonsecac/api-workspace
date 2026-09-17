package com.workspace.api.user.controller;

import com.workspace.api.user.dto.request.LoginRequest;
import com.workspace.api.user.dto.request.RegisterRequest;
import com.workspace.api.user.dto.response.AuthResponse;
import com.workspace.api.user.dto.response.UserResponse;
import com.workspace.api.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService users;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) { return users.register(request); }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) { return users.login(request); }

    @GetMapping("/me")
    public UserResponse me() { return users.me(); }
}
