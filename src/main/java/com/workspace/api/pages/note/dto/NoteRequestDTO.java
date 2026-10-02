package com.workspace.api.pages.note.dto;

import java.time.LocalDateTime;

public record NoteRequestDTO(
        String title,
        String description,
        LocalDateTime edited,
        Boolean favourite,
        Boolean fixed,
        LocalDateTime fixedAt,
        Boolean completed,
        Boolean isRecurring,
        String recurrenceRule
) {
}
