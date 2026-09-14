package com.workspace.api.note.controller;

import com.workspace.api.note.dto.NoteRequestDTO;
import com.workspace.api.note.dto.NoteResponseDTO;
import com.workspace.api.note.service.NoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("notes")
public class NoteController {
    private final NoteService noteService;

    @GetMapping
    public List<NoteResponseDTO> getNotes(){
        return noteService.findAllNotes();
    }

    @PostMapping
    public ResponseEntity<NoteResponseDTO> saveNote(@RequestBody NoteRequestDTO request){
        NoteResponseDTO savedNote = noteService.saveNote(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedNote);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNote(@PathVariable String id){
        noteService.deleteNote(id);
        return ResponseEntity.noContent().build();
    }
}
