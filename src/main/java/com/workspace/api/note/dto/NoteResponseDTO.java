package com.workspace.api.note.dto;

import com.workspace.api.note.domain.Note;
import java.time.Instant;
import java.time.LocalDateTime;

public record NoteResponseDTO(String id, String userId, String title, String description, LocalDateTime edited, Boolean favourite, Boolean fixed, LocalDateTime fixedAt,
                              Boolean completed, Instant completedAt, Boolean isRecurring, String recurrenceRule) {
    public NoteResponseDTO(Note note) {
        this(note.getId(), note.getUserId(), note.getTitle(), note.getDescription(), note.getEdited(), note.getFavourite(), note.getFixed(), note.getFixedAt(),
                note.getCompleted(), note.getCompletedAt(), note.isRecurring(), note.getRecurrenceRule());
    }
}
