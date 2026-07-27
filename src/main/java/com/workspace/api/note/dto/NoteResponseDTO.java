package com.workspace.api.note.dto;

import com.workspace.api.note.domain.Note;

import java.time.LocalDateTime;

public record NoteResponseDTO(Long id, String title, String description, LocalDateTime edited, Boolean favorite, Boolean fixed, LocalDateTime fixedAt) {
    public NoteResponseDTO(Note note) {
        this(note.getId(), note.getTitle(), note.getDescription(), note.getEdited(), note.getFavorite(), note.getFixed(), note.getFixedAt());
    }
}
