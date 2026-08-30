package com.workspace.api.note.dto;

import java.time.LocalDateTime;

public record NoteRequestDTO(
        String title,
        String description,
        LocalDateTime edited,
        Boolean favourite,
        Boolean fixed,
        LocalDateTime fixedAt
) {
}
