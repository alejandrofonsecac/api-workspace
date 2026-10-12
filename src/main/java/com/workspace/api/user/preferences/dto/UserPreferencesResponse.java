package com.workspace.api.user.preferences.dto;

import com.workspace.api.user.preferences.domain.UserPreferences;
import java.time.Instant;
import java.util.Map;

/** `updatedAt` nulo indica que o usuário ainda não salvou nenhuma preferência. */
public record UserPreferencesResponse(Map<String, Object> preferences, Instant updatedAt) {
    public static final UserPreferencesResponse EMPTY = new UserPreferencesResponse(Map.of(), null);

    public UserPreferencesResponse(UserPreferences preferences) {
        this(preferences.getData(), preferences.getUpdatedAt());
    }
}
