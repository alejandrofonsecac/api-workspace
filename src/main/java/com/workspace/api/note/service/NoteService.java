package com.workspace.api.note.service;

import com.workspace.api.user.security.CurrentUser;
import com.workspace.api.note.domain.Note;
import com.workspace.api.note.dto.NoteRequestDTO;
import com.workspace.api.note.dto.NoteResponseDTO;
import com.workspace.api.note.repository.NoteRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@AllArgsConstructor
public class NoteService {
    private NoteRepository noteRepository;
    private final CurrentUser currentUser;
    private final java.time.Clock clock;

    public List<NoteResponseDTO> findAllNotes(){
        return noteRepository.findAllByUserId(currentUser.id()).stream()
                .map(NoteResponseDTO::new)
                .toList();
    }

    @Transactional
    public NoteResponseDTO saveNote(NoteRequestDTO request){
        requireTitle(request.title());
        Note note = new Note();
        note.setUserId(currentUser.id());
        note.setTitle(request.title());
        note.setDescription(request.description());
        note.setEdited(java.time.LocalDateTime.now(clock));
        note.setFavourite(Boolean.TRUE.equals(request.favourite()));
        note.setFixed(Boolean.TRUE.equals(request.fixed()));
        note.setFixedAt(Boolean.TRUE.equals(request.fixed()) ? java.time.LocalDateTime.now(clock) : null);

        Note noteSaved = noteRepository.save(note);

        return new NoteResponseDTO(noteSaved);
    }

    @Transactional
    public NoteResponseDTO updateNote(String id, NoteRequestDTO request) {
        Note note = noteRepository.findByIdAndUserId(id, currentUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found"));
        if (request.title() != null) { requireTitle(request.title()); note.setTitle(request.title().trim()); }
        if (request.description() != null) note.setDescription(request.description());
        if (request.favourite() != null) note.setFavourite(request.favourite());
        if (request.fixed() != null && !request.fixed().equals(note.getFixed())) {
            note.setFixed(request.fixed());
            note.setFixedAt(request.fixed() ? java.time.LocalDateTime.now(clock) : null);
        }
        note.setEdited(java.time.LocalDateTime.now(clock));
        return new NoteResponseDTO(noteRepository.save(note));
    }

    @Transactional
    public void deleteNote(String id){
        Note note = noteRepository.findByIdAndUserId(id, currentUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found: " + id));
        noteRepository.delete(note);
    }

    private void requireTitle(String title) {
        if (title == null || title.isBlank() || title.length() > 255)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A title of up to 255 characters is required");
    }
}
