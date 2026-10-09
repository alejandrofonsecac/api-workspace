package com.workspace.api.user.preferences.dto;

import com.workspace.api.user.preferences.domain.UserPreferences;
import java.time.Instant;

/** `updatedAt` nulo indica que o usuário ainda não salvou preferências: os valores são os padrões. */
public record UserPreferencesResponse(
        String theme,
        String accent,
        String plannerDisplay,
        Sleep sleep,
        int sidebarWidth,
        Instant updatedAt
) {
    public record Sleep(boolean enabled, int start, int end) {
    }

    public static final UserPreferencesResponse DEFAULTS = new UserPreferencesResponse(
            "light", "blue", "compact", new Sleep(false, 23, 7), 280, null);

    public UserPreferencesResponse(UserPreferences preferences) {
        this(preferences.getTheme(), preferences.getAccent(), preferences.getPlannerDisplay(),
                new Sleep(preferences.isSleepEnabled(), preferences.getSleepStart(), preferences.getSleepEnd()),
                preferences.getSidebarWidth(), preferences.getUpdatedAt());
    }
}
