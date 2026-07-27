package com.workspace.api.note.service;

import com.workspace.api.note.dto.NoteResponseDTO;
import com.workspace.api.note.repository.NoteRepository;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
@NoArgsConstructor
public class NoteService {
    private NoteRepository noteRepository;

    public List<NoteResponseDTO> findAllNotes(){
        return noteRepository.findAll().stream()
                .map(NoteResponseDTO::new)
                .toList();
    }
}
