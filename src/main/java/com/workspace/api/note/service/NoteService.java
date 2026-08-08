package com.workspace.api.note.service;

import com.workspace.api.note.domain.Note;
import com.workspace.api.note.dto.NoteResponseDTO;
import com.workspace.api.note.repository.NoteRepository;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
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

    public NoteResponseDTO saveNote(NoteResponseDTO dto){
        Note note = new Note();
        note.setTitle(dto.title());
        note.setDescription(dto.description());
        note.setEdited(dto.edited());
        note.setFavourite(dto.favourite());
        note.setFixed(dto.fixed());
        note.setFixedAt(dto.fixedAt());

        Note noteSaved = noteRepository.save(note);

        return new NoteResponseDTO(noteSaved);
    }
}
