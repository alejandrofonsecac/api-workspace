package com.workspace.api.note.service;

import com.workspace.api.note.domain.Note;
import com.workspace.api.note.dto.NoteRequestDTO;
import com.workspace.api.note.dto.NoteResponseDTO;
import com.workspace.api.note.repository.NoteRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class NoteService {
    private NoteRepository noteRepository;

    public List<NoteResponseDTO> findAllNotes(){
        return noteRepository.findAll().stream()
                .map(NoteResponseDTO::new)
                .toList();
    }

    public NoteResponseDTO saveNote(NoteRequestDTO request){
        Note note = new Note();
        note.setTitle(request.title());
        note.setDescription(request.description());
        note.setEdited(request.edited());
        note.setFavourite(request.favourite());
        note.setFixed(request.fixed());
        note.setFixedAt(request.fixedAt());

        Note noteSaved = noteRepository.save(note);

        return new NoteResponseDTO(noteSaved);
    }
}
