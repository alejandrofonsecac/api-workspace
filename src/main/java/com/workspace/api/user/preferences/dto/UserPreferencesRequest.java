package com.workspace.api.user.preferences.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Substitui todas as preferências do usuário autenticado; não aceita userId do cliente. */
public record UserPreferencesRequest(
        @NotNull(message = "theme is required")
        @Pattern(regexp = "light|dark|high-contrast|system", message = "theme must be light, dark, high-contrast or system")
        String theme,
        @NotNull(message = "accent is required")
        @Pattern(regexp = "indigo|teal|orange|pink|blue", message = "accent must be indigo, teal, orange, pink or blue")
        String accent,
        @NotNull(message = "plannerDisplay is required")
        @Pattern(regexp = "compact|periods|radial", message = "plannerDisplay must be compact, periods or radial")
        String plannerDisplay,
        @NotNull(message = "sleep is required") @Valid Sleep sleep,
        @NotNull(message = "sidebarWidth is required")
        @Min(value = 240, message = "sidebarWidth must be between 240 and 340")
        @Max(value = 340, message = "sidebarWidth must be between 240 and 340")
        Integer sidebarWidth
) {
    /** Horas inteiras locais; o intervalo pode atravessar a meia-noite (ex.: 23 → 7). */
    public record Sleep(
            @NotNull(message = "enabled is required") Boolean enabled,
            @NotNull(message = "start is required")
            @Min(value = 0, message = "start must be between 0 and 23")
            @Max(value = 23, message = "start must be between 0 and 23")
            Integer start,
            @NotNull(message = "end is required")
            @Min(value = 0, message = "end must be between 0 and 23")
            @Max(value = 23, message = "end must be between 0 and 23")
            Integer end
    ) {
    }
}
