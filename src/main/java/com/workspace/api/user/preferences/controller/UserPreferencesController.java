package com.workspace.api.user.preferences.controller;

import com.workspace.api.user.preferences.dto.UserPreferencesResponse;
import com.workspace.api.user.preferences.service.UserPreferencesService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

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

    @PatchMapping
    public UserPreferencesResponse merge(@RequestBody(required = false) Map<String, Object> changes) {
        return preferences.merge(changes);
    }
}
