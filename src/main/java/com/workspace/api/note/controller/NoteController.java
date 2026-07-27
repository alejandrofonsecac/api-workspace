package com.workspace.api.note.controller;

import com.workspace.api.note.dto.NoteResponseDTO;
import com.workspace.api.note.service.NoteService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping
@AllArgsConstructor
@RestController
@CrossOrigin(origins = "http://localhost:4200")
public class NoteController {
    private NoteService noteService;

    @GetMapping
    public List<NoteResponseDTO> getNotes(){
        return noteService.findAllNotes();
    }
}
