package com.workspace.api.note.controller;

import com.workspace.api.note.dto.NoteResponseDTO;
import com.workspace.api.note.service.NoteService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("notes")
@CrossOrigin(origins = "http://localhost:4200")
public class NoteController {
    private NoteService noteService;

    @GetMapping
    public List<NoteResponseDTO> getNotes(){
        return noteService.findAllNotes();
    }

    @PostMapping
    public ResponseEntity<NoteResponseDTO> saveNote(@RequestBody NoteResponseDTO dto){
        NoteResponseDTO savedNote = noteService.saveNote(dto);
        return ResponseEntity.ok().body(savedNote);
    }
}
