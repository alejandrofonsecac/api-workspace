package com.workspace.api.user.preferences.controller;

import com.workspace.api.user.preferences.dto.UserPreferencesRequest;
import com.workspace.api.user.preferences.dto.UserPreferencesResponse;
import com.workspace.api.user.preferences.service.UserPreferencesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** Preferências do usuário autenticado; o id vem sempre do token. */
@RestController
@RequestMapping("/users/me/preferences")
@RequiredArgsConstructor
public class UserPreferencesController {
    private final UserPreferencesService preferences;

    @GetMapping
    public UserPreferencesResponse get() {
        return preferences.current();
    }

    @PutMapping
    public UserPreferencesResponse replace(@Valid @RequestBody UserPreferencesRequest request) {
        return preferences.replace(request);
    }
}
